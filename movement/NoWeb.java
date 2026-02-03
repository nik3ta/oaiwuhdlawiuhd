package nuclear.module.impl.movement;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventIgnoreHitbox;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.utils.move.MoveUtil;

@Annotation(
        name = "NoWeb",
        type = TypeList.Movement,
        desc = "Убирает вам замедление в паутине"
)
public class NoWeb extends Module {
    public final BooleanSetting nobreak = new BooleanSetting("Ломать через паутину", false);

    public NoWeb() {
        addSettings(nobreak);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventIgnoreHitbox e && nobreak.getValue()) {
            onBlockCollide(e);
        }

        if (event instanceof EventUpdate) {
            boolean inWeb = isInWeb();

            if (inWeb) {
                mc.player.setVelocity(mc.player.getMotion().x, 0, mc.player.getMotion().z);

                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    mc.player.setVelocity(mc.player.getMotion().x, 0.9, mc.player.getMotion().z);
                }

                if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    mc.player.setVelocity(mc.player.getMotion().x, -0.9, mc.player.getMotion().z);
                }

                MoveUtil.setMotion(0.21);
            }
        }
        return false;
    }

    public void onBlockCollide(EventIgnoreHitbox e) {
        if (mc.world.getBlockState(e.getPos()).getBlock() == Blocks.COBWEB) e.setCancel(true);
    }

    private boolean isInWeb() {
        double minX = mc.player.getBoundingBox().minX + 0.001;
        double minZ = mc.player.getBoundingBox().minZ + 0.001;
        double maxX = mc.player.getBoundingBox().maxX - 0.001;
        double maxZ = mc.player.getBoundingBox().maxZ - 0.001;

        double baseMinY = mc.player.getBoundingBox().minY;

        double baseMaxY = mc.player.getBoundingBox().maxY;

        double height = baseMaxY - baseMinY;
        double shrinkAmount = height * 0.119;

        double minY = baseMinY + (shrinkAmount / 2);
        double maxY = baseMaxY - (shrinkAmount / 2);

        double stepX = (maxX - minX) / 4.0;
        double stepZ = (maxZ - minZ) / 4.0;
        double stepY = (maxY - minY) / 8.0;


        for (double x = minX; x <= maxX + 0.0001; x += stepX) {
            for (double y = minY; y <= maxY + 0.0001; y += stepY) {
                for (double z = minZ; z <= maxZ + 0.0001; z += stepZ) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                        return true;
                    }
                }
            }
        }

        double[][] cornerOffsets = {
                {minX, minY, minZ}, {minX, minY, maxZ},
                {minX, maxY, minZ}, {minX, maxY, maxZ},
                {maxX, minY, minZ}, {maxX, minY, maxZ},
                {maxX, maxY, minZ}, {maxX, maxY, maxZ}
        };

        for (double[] offset : cornerOffsets) {
            BlockPos pos = new BlockPos(offset[0], offset[1], offset[2]);
            if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB) {
                return true;
            }
        }

        BlockPos headPos = new BlockPos(mc.player.getPosX(), maxY + 0.1, mc.player.getPosZ());
        BlockPos feetPos = new BlockPos(mc.player.getPosX(), minY - 0.1, mc.player.getPosZ());

        return mc.world.getBlockState(headPos).getBlock() == Blocks.COBWEB ||
                mc.world.getBlockState(feetPos).getBlock() == Blocks.COBWEB;
    }
}

