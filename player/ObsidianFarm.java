package nuclear.module.impl.player;

import net.minecraft.client.util.ITooltipFlag;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.utils.world.WorldUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Comparator;
import java.util.List;

@Annotation(
        name = "ObsidianFarm",
        type = TypeList.Player,
        desc = "Автоматически копает и сейвит обсидиан"
)
public class ObsidianFarm extends Module {
    private BlockPos targetPos;
    private static final double MAX_RANGE = 5;
    private static final double MAX_RANGE_SQ = MAX_RANGE * MAX_RANGE;

    public ObsidianFarm() {
        this.addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            updateNuker();
        }
        return false;
    }

    private void updateNuker() {
        if (mc.player == null || mc.world == null) {
            targetPos = null;
            return;
        }

        boolean veinMiner = isVeinMinerActive();

        if (targetPos != null && (!isObsidian(targetPos) || !isInRange(targetPos) || !hasBlockBelow(targetPos, veinMiner))) {
            mc.playerController.resetBlockRemoving();
            targetPos = null;
        }

        BlockPos playerPos = mc.player.getPosition();
        BlockPos from = playerPos.add(-5, -5, -5);
        BlockPos to = playerPos.add(5, 5, 5);

        List<BlockPos> blocks = WorldUtils.Blocks.getAllInBox(from, to);

        BlockPos newTargetPos;
        if (veinMiner) {
            int playerY = mc.player.getPosition().getY();
            
            newTargetPos = blocks.stream()
                    .filter(this::isObsidian)
                    .filter(this::isInRange)
                    .filter(pos -> hasBlockBelow(pos, true))
                    .filter(pos -> pos.getY() > playerY)
                    .filter(pos -> countObsidianIn3x3(pos) >= 2)
                    .max(Comparator.comparing(BlockPos::getY)
                            .thenComparing((BlockPos pos) -> -getDistanceToCenterOf3x3Area(pos)))
                    .orElse(null);
            
            if (newTargetPos == null) {
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() > playerY)
                        .max(Comparator.comparing(BlockPos::getY)
                                .thenComparing(pos -> mc.player.getDistanceSq(Vector3d.copyCentered(pos))))
                        .orElse(null);
            }
            
            if (newTargetPos == null) {
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() == playerY)
                        .filter(pos -> countObsidianIn3x3(pos) >= 2)
                        .max(Comparator.comparing((BlockPos pos) -> -getDistanceToCenterOf3x3Area(pos)))
                        .orElse(null);
            }

            if (newTargetPos == null) {
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() == playerY) // На уровне игрока
                        .min(Comparator.comparing(pos -> mc.player.getDistanceSq(Vector3d.copyCentered(pos)))) // Ближе к игроку
                        .orElse(null);
            }

            if (newTargetPos == null) {
                // Сначала ищем блоки 3x3 ниже
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() < playerY)
                        .filter(pos -> countObsidianIn3x3(pos) >= 2)
                        .max(Comparator.comparing(BlockPos::getY)
                                .thenComparing((BlockPos pos) -> -getDistanceToCenterOf3x3Area(pos)))
                        .orElse(null);
            }
            
            if (newTargetPos == null) {
                newTargetPos = blocks.stream()
                        .filter(this::isObsidian)
                        .filter(this::isInRange)
                        .filter(pos -> hasBlockBelow(pos, true))
                        .filter(pos -> pos.getY() < playerY)
                        .max(Comparator.comparing(BlockPos::getY)
                                .thenComparing(pos -> mc.player.getDistanceSq(Vector3d.copyCentered(pos))))
                        .orElse(null);
            }
        } else {
            newTargetPos = blocks.stream()
                    .filter(this::isObsidian)
                    .filter(this::isInRange)
                    .filter(pos -> hasBlockBelow(pos, false))
                    .max(Comparator.comparing(BlockPos::getY)
                            .thenComparing(pos -> mc.player.getDistanceSq(Vector3d.copyCentered(pos))))
                    .orElse(null);
        }

        if (newTargetPos != null && !newTargetPos.equals(targetPos)) {
            mc.playerController.resetBlockRemoving();
            targetPos = newTargetPos;
        } else if (newTargetPos == null && targetPos != null) {
            mc.playerController.resetBlockRemoving();
            targetPos = null;
        } else if (newTargetPos != null) {
            targetPos = newTargetPos;
        }

        if (targetPos != null && isObsidian(targetPos) && isInRange(targetPos) && hasBlockBelow(targetPos, veinMiner)) {
            boolean breaking = mc.playerController.onPlayerDamageBlock(targetPos, Direction.UP);
            if (breaking || mc.playerController.getIsHittingBlock()) {
                mc.player.swingArm(Hand.MAIN_HAND);
            }
        }
    }

    private boolean isInRange(BlockPos pos) {
        if (mc.player == null) return false;
        double distanceSq = mc.player.getDistanceSq(Vector3d.copyCentered(pos));
        return distanceSq <= MAX_RANGE_SQ;
    }

    private boolean isObsidian(BlockPos pos) {
        if (mc.world == null) return false;
        BlockState state = mc.world.getBlockState(pos);
        return state.getBlock() == Blocks.OBSIDIAN;
    }

    private boolean hasBlockBelow(BlockPos pos, boolean isVeinMiner) {
        return true;
    }

    private boolean isVeinMinerActive() {
        if (mc.player == null) return false;
        ItemStack mainHand = mc.player.getHeldItemMainhand();
        
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof PickaxeItem)) {
            return false;
        }

        List<ITextComponent> tooltip = mainHand.getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL);
        for (ITextComponent component : tooltip) {
            String text = TextFormatting.getTextWithoutFormattingCodes(component.getString()).toLowerCase();
            if (text.contains("бур")) {
                return true;
            }
        }
        
        return false;
    }

    private int countObsidianIn3x3(BlockPos center) {
        int count = 0;
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos pos = new BlockPos(centerX + x, centerY, centerZ + z);
                if (isObsidian(pos) && isInRange(pos) && hasBlockBelow(pos, true)) {
                    count++;
                }
            }
        }
        
        return count;
    }

    /**
     * Вычисляет расстояние от блока до геометрического центра области 3x3
     * Чем меньше расстояние, тем ближе блок к центру области
     */
    private double getDistanceToCenterOf3x3Area(BlockPos pos) {
        int centerX = pos.getX();
        int centerY = pos.getY();
        int centerZ = pos.getZ();
        
        double sumX = 0, sumZ = 0;
        int count = 0;
        
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = new BlockPos(centerX + x, centerY, centerZ + z);
                if (isObsidian(checkPos) && isInRange(checkPos) && hasBlockBelow(checkPos, true)) {
                    sumX += checkPos.getX();
                    sumZ += checkPos.getZ();
                    count++;
                }
            }
        }
        
        if (count == 0) return Double.MAX_VALUE;
        
        double avgX = sumX / count;
        double avgZ = sumZ / count;
        
        double dx = pos.getX() - avgX;
        double dz = pos.getZ() - avgZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}