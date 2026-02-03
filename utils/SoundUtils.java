package nuclear.utils;

import net.minecraft.util.ResourceLocation;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundUtils implements IMinecraft {
    private static Clip currentClip = null;

    public static void playSound(String sound, float value, boolean nonstop) {
        new Thread(() -> {
            try {
                if (currentClip != null && currentClip.isRunning()) {
                    currentClip.stop();
                }

                currentClip = AudioSystem.getClip();
                ResourceLocation location = new ResourceLocation("nuclear/sounds/" + sound);
                InputStream is = mc.getResourceManager().getResource(location).getInputStream();

                BufferedInputStream bis = new BufferedInputStream(is);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);

                currentClip.open(audioInputStream);
                currentClip.start();

                FloatControl floatControl = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = floatControl.getMinimum();
                float max = floatControl.getMaximum();
                float volumeInDecibels = (float) (min * (1 - (value / 100.0)) + max * (value / 100.0));
                floatControl.setValue(volumeInDecibels);

                if (nonstop) {
                    currentClip.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } catch (Exception exception) {
                System.out.println("Ошибка при воспроизведении звука: " + sound);
                exception.printStackTrace();
            }
        }).start();
    }
}