package nuclear.utils.tps;

import lombok.Getter;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.util.math.MathHelper;
import nuclear.control.events.impl.game.EventHandler;
import nuclear.control.events.impl.packet.EventPacket;

@Getter
public class TPSCalc {

    private float TPS = 20;
    private float adjustTicks = 0;

    private long timestamp;

    public TPSCalc() {

    }

    @EventHandler
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof SUpdateTimePacket) {
            updateTPS();
        }
    }

    private static final int SAMPLE_SIZE = 20;
    private final float[] tpsSamples = new float[SAMPLE_SIZE];
    private int sampleIndex = 0;

    private void updateTPS() {
        long delay = System.nanoTime() - timestamp;
        timestamp = System.nanoTime();

        float maxTPS = 20f;
        float rawTPS = maxTPS * (1e9f / delay);
        float boundedTPS = MathHelper.clamp(rawTPS, 0, maxTPS);

        tpsSamples[sampleIndex % SAMPLE_SIZE] = boundedTPS;
        sampleIndex++;

        float sum = 0;
        for (float sample : tpsSamples) {
            sum += sample;
        }

        TPS = (float) round(sum / SAMPLE_SIZE);
        adjustTicks = TPS - maxTPS;
    }


    public double round(final double input) {
        return Math.round(input * 10.0) / 10.0;
    }
}