package nuclear.module.impl.other;

import mods.voicechat.api.events.MergeClientSoundEvent;
import mods.voicechat.plugins.PluginManager;
import mods.voicechat.voice.common.Utils;
import net.minecraft.entity.LivingEntity;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.impl.combat.AttackAura;
import nuclear.utils.SoundUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@Annotation(name = "Soundpad", type = TypeList.Player, desc = "Отправляет звук в микрофон по типу выкидывай шарик когда убиваешь игрока")
public class VoiceModeSound extends Module {

    private Thread audioCaptureThread;
    private boolean running = false;
    private Consumer<MergeClientSoundEvent> eventListener;
    private BlockingQueue<short[]> audioBuffer = new LinkedBlockingQueue<>();
    private Clip localPlaybackClip;
    private List<short[]> audioFrames = new ArrayList<>();
    private boolean soundPlayed = false;
    private boolean targetHealthRecovered = true;
    private int lastTargetId = -1;
    private float lastTargetHealth = -1f;
    private static final float VOICE_VOLUME = 100f;
    private static final float SOUND_VOLUME = 70f;
    private static final String SOUND_FILE = "otdavay.wav";
    private static final float HEALTH_THRESHOLD = 7f;

    public VoiceModeSound() {
        addSettings();
    }

    @Override
    protected void onEnable() {
        soundPlayed = false;
        targetHealthRecovered = true;
        lastTargetId = -1;
        lastTargetHealth = -1f;
        loadAudioFrames();
        registerEventListener();
    }

    @Override
    protected void onDisable() {
        stopAudioCapture();
        stopLocalPlayback();
        unregisterEventListener();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            checkTargetHealth();
        }
        return false;
    }

    private void checkTargetHealth() {
        LivingEntity target = AttackAura.getTarget();
        if (target == null) {
            if (lastTargetId != -1) {
                lastTargetId = -1;
            }
            return;
        }

        int currentTargetId = target.getEntityId();
        float targetHealth = target.getHealth() + target.getAbsorptionAmount();

        if (currentTargetId != lastTargetId) {
            lastTargetId = currentTargetId;
            lastTargetHealth = targetHealth;

            if (targetHealth < HEALTH_THRESHOLD) {
                soundPlayed = true;
                targetHealthRecovered = false;
            } else {
                soundPlayed = false;
                targetHealthRecovered = true;
            }
            return;
        }

        if (targetHealth >= HEALTH_THRESHOLD) {
            if (!targetHealthRecovered) {
                targetHealthRecovered = true;
                soundPlayed = false;
            }
            lastTargetHealth = targetHealth;
            return;
        }

        if (targetHealth < HEALTH_THRESHOLD) {
            if (targetHealthRecovered && !soundPlayed && lastTargetHealth >= HEALTH_THRESHOLD) {
                playSoundOnce();
                soundPlayed = true;
                targetHealthRecovered = false;
            }
            lastTargetHealth = targetHealth;
        }
    }

    private void playSoundOnce() {
        startLocalPlayback();

        startAudioCapture();
    }

    private void registerEventListener() {
        try {
            PluginManager pluginManager = PluginManager.instance();
            Field eventsField = PluginManager.class.getDeclaredField("events");
            eventsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Class<? extends mods.voicechat.api.events.Event>, List<Consumer<? extends mods.voicechat.api.events.Event>>> events =
                    (Map<Class<? extends mods.voicechat.api.events.Event>, List<Consumer<? extends mods.voicechat.api.events.Event>>>) eventsField.get(pluginManager);

            eventListener = (MergeClientSoundEvent event) -> {
                if (!state) return;

                short[] audioToMerge = getNextAudioFrame();
                if (audioToMerge != null) {
                    event.mergeAudio(audioToMerge);
                }
            };

            events.computeIfAbsent(MergeClientSoundEvent.class, k -> new ArrayList<>())
                    .add((Consumer<? extends mods.voicechat.api.events.Event>) eventListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterEventListener() {
        try {
            PluginManager pluginManager = PluginManager.instance();
            Field eventsField = PluginManager.class.getDeclaredField("events");
            eventsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Class<? extends mods.voicechat.api.events.Event>, List<Consumer<? extends mods.voicechat.api.events.Event>>> events =
                    (Map<Class<? extends mods.voicechat.api.events.Event>, List<Consumer<? extends mods.voicechat.api.events.Event>>>) eventsField.get(pluginManager);

            List<Consumer<? extends mods.voicechat.api.events.Event>> listeners = events.get(MergeClientSoundEvent.class);
            if (listeners != null && eventListener != null) {
                listeners.remove(eventListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAudioFrames() {
        new Thread(() -> {
            try {
                if (mc == null || mc.getResourceManager() == null) {
                    Thread.sleep(100);
                    if (mc == null || mc.getResourceManager() == null) return;
                }

                InputStream is = mc.getResourceManager().getResource(
                        new net.minecraft.util.ResourceLocation("nuclear/sounds/" + SOUND_FILE)).getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);

                AudioFormat sourceFormat = audioInputStream.getFormat();
                AudioFormat convertedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        Utils.SAMPLE_RATE,
                        16,
                        1,
                        2,
                        Utils.SAMPLE_RATE,
                        false
                );

                if (!sourceFormat.matches(convertedFormat)) {
                    audioInputStream = AudioSystem.getAudioInputStream(convertedFormat, audioInputStream);
                }

                audioFrames.clear();
                byte[] buffer = new byte[Utils.FRAME_SIZE * 2];
                int bytesRead;

                while ((bytesRead = audioInputStream.read(buffer)) != -1) {
                    short[] audioFrame = convertToShortArray(buffer, bytesRead);
                    if (audioFrame.length == Utils.FRAME_SIZE) {
                        applyVolume(audioFrame, VOICE_VOLUME);
                        audioFrames.add(audioFrame);
                    }
                }

                audioInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "VoiceModeSound-LoadFrames").start();
    }

    private void startAudioCapture() {
        if (running || audioFrames.isEmpty()) return;
        running = true;
        audioBuffer.clear();

        audioCaptureThread = new Thread(() -> {
            try {
                for (short[] frame : audioFrames) {
                    if (!running) break;
                    audioBuffer.offer(frame.clone());
                    Thread.sleep(20);
                }
                running = false;
            } catch (Exception e) {
                e.printStackTrace();
                running = false;
            }
        }, "VoiceModeSound-Capture");
        audioCaptureThread.setDaemon(true);
        audioCaptureThread.start();
    }

    private void startLocalPlayback() {
        SoundUtils.playSound(SOUND_FILE, SOUND_VOLUME, false);
    }

    private void stopLocalPlayback() {
        if (localPlaybackClip != null) {
            try {
                if (localPlaybackClip.isRunning()) {
                    localPlaybackClip.stop();
                }
                localPlaybackClip.close();
                localPlaybackClip = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopAudioCapture() {
        running = false;
        audioBuffer.clear();

        if (audioCaptureThread != null) {
            try {
                audioCaptureThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    private short[] convertToShortArray(byte[] bytes, int length) {
        int samples = length / 2;
        short[] shorts = new short[samples];

        for (int i = 0; i < samples; i++) {
            int byteIndex = i * 2;
            if (byteIndex + 1 < length) {
                shorts[i] = (short) ((bytes[byteIndex + 1] << 8) | (bytes[byteIndex] & 0xFF));
            }
        }

        return shorts;
    }

    private void applyVolume(short[] audio, float volumePercent) {
        float vol = volumePercent / 100f;
        for (int i = 0; i < audio.length; i++) {
            audio[i] = (short) (audio[i] * vol);
        }
    }

    private short[] getNextAudioFrame() {
        short[] frame = audioBuffer.poll();
        if (frame == null || frame.length != Utils.FRAME_SIZE) {
            return new short[Utils.FRAME_SIZE];
        }
        return frame;
    }
}

