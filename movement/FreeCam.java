package nuclear.module.impl.movement;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.events.impl.player.EventLivingUpdate;
import nuclear.control.events.impl.player.EventMotion;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.FreeCamUtils;
import nuclear.utils.font.Fonts;
import nuclear.utils.move.MoveUtil;

@SuppressWarnings("all")
@Annotation(name = "FreeCam", type = TypeList.Movement)
public class FreeCam extends Module {
    private final SliderSetting speed = new SliderSetting("Скорость по XZ", 1.0f, 0.1f, 2.0f, 0.05f);
    private final SliderSetting motionY = new SliderSetting("Скорость Y", 0.65f, 0.1f, 1.5f, 0.05f);
    private Vector3d clientPosition = null;
    public FreeCamUtils player = null;
    private boolean oldIsFlying;
    private MovementInput originalMovementInput;
    private boolean oldViewBobbing;

    public FreeCam() {
        addSettings(speed, motionY);
    }

    @Override
    public boolean onEvent(Event event) {
        if (mc.player == null || mc.player.movementInput == null || mc.world == null) {
            return false;
        }

        mc.player.setVelocity(0, 0, 0);
        MoveUtil.setMotionFreecam(0);

        if (event instanceof EventLivingUpdate livingUpdateEvent && player != null) {
            player.noClip = true;
            player.setOnGround(false);
            MoveUtil.setMotionFreecam(speed.getValue().floatValue(), player);
            float ySpeed = 0.0f;
            if (Minecraft.getInstance().gameSettings.keyBindJump.isKeyDown()) {
                ySpeed += motionY.getValue().floatValue();
            }
            if (Minecraft.getInstance().gameSettings.keyBindSneak.isKeyDown()) {
                ySpeed -= motionY.getValue().floatValue();
            }
            player.setVelocity(player.getMotion().x, ySpeed, player.getMotion().z);
            player.abilities.isFlying = true;
            player.inventory.copyInventory(mc.player.inventory);
            player.setHealth(mc.player.getHealth());
            player.setAbsorptionAmount(mc.player.getAbsorptionAmount());
            player.copyMaxHealthFrom(mc.player);
            player.copyFoodStatsFrom(mc.player);
        }

        if (event instanceof EventPacket e && player != null) {
            if (e.getPacket() instanceof CPlayerPacket p) {
                if (p.moving) {
                    p.x = player.getPosX();
                    p.y = player.getPosY();
                    p.z = player.getPosZ();
                }
                p.onGround = player.isOnGround();
                if (p.rotating) {
                    p.yaw = player.rotationYaw;
                    p.pitch = player.rotationPitch;
                }
            }
        }

        if (event instanceof EventMotion motionEvent) {
            handleMotionEvent(motionEvent);
        }

        if (event instanceof EventRender && ((EventRender) event).isRender2D()) {
            handleRender2DEvent((EventRender) event);
        }
        return false;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        oldIsFlying = mc.player.abilities.isFlying;
        originalMovementInput = mc.player.movementInput;

        mc.player.setMotion(Vector3d.ZERO);
        mc.player.moveForward = 0;
        mc.player.moveStrafing = 0;
        mc.player.moveVertical = 0;
        mc.gameSettings.viewBobbing = false;
        mc.player.movementInput = new MovementInput() {
            public void updatePlayerMoveState() {
                this.forwardKeyDown = false;
                this.backKeyDown = false;
                this.leftKeyDown = false;
                this.rightKeyDown = false;
                this.jump = false;
            }
        };

        initializeFakePlayer();
        addFakePlayer();
        player.spawn();
        mc.setRenderViewEntity(player);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player == null || mc.world == null) {
            return;
        }
        mc.gameSettings.viewBobbing = oldViewBobbing;
        mc.player.movementInput = originalMovementInput != null ? originalMovementInput : new MovementInputFromOptions(mc.gameSettings);

        mc.setRenderViewEntity(mc.player);
        removeFakePlayer();
    }

    private void handleMotionEvent(EventMotion motionEvent) {
        motionEvent.setCancel(true);
    }


    private void handleRender2DEvent(EventRender renderEvent) {
        if (clientPosition == null || player == null || Fonts.newcode == null || Fonts.newcode.length <= 13 || Fonts.newcode[13] == null) {
            return;
        }

        MainWindow resolution = mc.getMainWindow();
        int xPosition = (int) (player.getPosX() - mc.player.getPosX());
        int yPosition = (int) (player.getPosY() - mc.player.getPosY());
        int zPosition = (int) (player.getPosZ() - mc.player.getPosZ());

        String position = "X:" + xPosition + " Y:" + yPosition + " Z:" + zPosition;
        Fonts.blod[13].drawString(renderEvent.matrixStack, position,
                (double) ((float) resolution.getScaledWidth() / 2.0F) - (Fonts.blod[13].getWidth(position) / 2),
                (double) ((float) resolution.getScaledHeight() / 2.0F + 13.0F), -1);
    }

    private void initializeFakePlayer() {
        if (mc.player == null) {
            return;
        }
        clientPosition = mc.player.getPositionVec();
        player = new FreeCamUtils(getUniqueEntityId());
        player.copyLocationAndAnglesFrom(mc.player);
        player.rotationYawHead = mc.player.rotationYawHead;
        player.inventory.copyInventory(mc.player.inventory);
        player.setHealth(mc.player.getHealth());
        player.setAbsorptionAmount(mc.player.getAbsorptionAmount());
        player.copyMaxHealthFrom(mc.player);
        player.copyArmorFrom(mc.player);
        player.copyFoodStatsFrom(mc.player);
    }


    private void addFakePlayer() {
        if (mc.world == null || player == null) {
            return;
        }
        clientPosition = mc.player.getPositionVec();
        mc.world.addEntity(player.getEntityId(), player);
    }


    private void removeFakePlayer() {
        if (mc.world == null || player == null) {
            return;
        }
        resetFlying();
        mc.world.removeEntityFromWorld(player.getEntityId());
        player = null;
        clientPosition = null;
    }

    private void resetFlying() {
        if (mc.player != null && oldIsFlying) {
            mc.player.abilities.isFlying = false;
        }
    }

    private int getUniqueEntityId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }
}