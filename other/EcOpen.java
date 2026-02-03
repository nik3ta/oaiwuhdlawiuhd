package nuclear.module.impl.other;

import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.events.Event;
import nuclear.control.events.impl.game.EventKey;
import nuclear.control.events.impl.player.EventInput;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BindSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.world.WorldUtils;

@Annotation(name = "EcOpen", type = TypeList.Other, desc = "Открывает эндер сундук в радиусе 6 блоков по бинду")
public class EcOpen extends Module {
    public BindSetting openKey = new BindSetting("Открыть", 0);
    
    private BlockPos targetChest = null;
    private boolean shouldRotate = false;
    public Vector2f server = null;

    public EcOpen() {
        addSettings(openKey);
    }

    public boolean check() {
        return state && shouldRotate && targetChest != null && server != null;
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKey e) {
            if (e.key == openKey.getKey()) {
                findEnderChest();
            }
        }
        
        if (event instanceof EventInput e && check()) {
            MoveUtil.fixMovement(e, server.x);
        }
        
        if (check() && targetChest != null) {
            ClientUtils.look(event, server, ClientUtils.Correction.FULL, null);
            
            if (event instanceof EventMotion && shouldRotate) {
                openEnderChest(targetChest);
                shouldRotate = false;
                targetChest = null;
                server = null;
            }
        }
        
        return false;
    }

    private void findEnderChest() {
        if (mc.player == null || mc.world == null) return;

        BlockPos enderChestPos = WorldUtils.TotemUtil.getBlock(6.0f, Blocks.ENDER_CHEST);

        if (enderChestPos != null) {
            targetChest = enderChestPos;
            shouldRotate = true;
            
            Vector3d target = new Vector3d(
                    enderChestPos.getX() + 0.5,
                    enderChestPos.getY() + 0.5,
                    enderChestPos.getZ() + 0.5
            );
            server = ClientUtils.get(target);
        }
    }

    private void openEnderChest(BlockPos pos) {
        if (mc.player == null || mc.world == null) return;

        Vector3d hitVec = new Vector3d(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );
        
        BlockRayTraceResult rayTraceResult = new BlockRayTraceResult(
                hitVec,
                Direction.UP,
                pos,
                false
        );

        mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, rayTraceResult);
        mc.player.swingArm(Hand.MAIN_HAND);
    }
}
