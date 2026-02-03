package nuclear.module.impl.movement;

import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import nuclear.control.events.Event;
import nuclear.control.events.impl.game.EventKey;
import nuclear.control.events.impl.player.EventTravel;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BindSetting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ModeSetting;
import nuclear.utils.move.MoveUtil;

import static net.minecraft.client.Minecraft.player;

@Annotation(name = "WaterSpeed", type = TypeList.Movement, desc = "Ускоряет ваше движение в воде")
public class WaterSpeed extends Module {
    public final ModeSetting mode = new ModeSetting("Мод", "MetaHvH", "MetaHvH", "Grim");
    public final BooleanSetting mininjump = new BooleanSetting("Мини прыжки", false).setVisible(() -> mode.is("Grim"));
    public BindSetting boostkey = new BindSetting("Кнопка буста", -1).setVisible(() -> mode.is("MetaHvH"));
    private long boostEndTime = 0;
    private boolean isBoosting = false;
    private boolean boostPressed = false;
    private final float s20 = 0.7015F;
    private final float s0 = 0.595F;
    private final float s15 = 0.6499F;
    private final float s25 = 0.749F;

    public WaterSpeed() {
        addSettings(mode, boostkey, mininjump);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKey e) {
            if (e.key == boostkey.getKey()) {
                boostPressed = true;
            }
        }

        if (boostPressed) {
            isBoosting = true;
            boostEndTime = System.currentTimeMillis() + 900;
            boostPressed = false;
        }

        if (isBoosting && System.currentTimeMillis() > boostEndTime) {
            isBoosting = false;
        }
        if (mode.is("Grim")) {
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                if (mc.player.isInWater()) {
                    double waterLevel;
                    BlockPos playerPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
                    try {
                        waterLevel = mc.player.world.getFluidState(playerPos).getActualHeight(mc.player.world, playerPos);
                    } catch (NoSuchMethodError e) {
                        if (mc.player.world.getBlockState(playerPos).getMaterial() == Material.WATER) {
                            waterLevel = playerPos.x + 1.0;
                        } else {
                            waterLevel = playerPos.y;
                        }
                    }

                    double playerEyeY = mc.player.getPosY() + mc.player.getEyeHeight();
                    if (playerEyeY >= waterLevel - 0.2 && playerEyeY <= waterLevel + 0.2) {
                        mc.player.motionY = 0.2;
                        MoveUtil.setMotion(MoveUtil.getMotion() * 2.0);
                    }
                } else if (mc.player.isInWater()) {
                    mc.player.motionY = Math.max(mc.player.motionY, 0.03);
                }
            }
        }
        if (mode.is("MetaHvH")) {
            if (event instanceof EventTravel move) {
                if (player.collidedVertically || player.collidedHorizontally) {
                    return false;
                }

                if ((player.isInWater() || player.isInLava()) && player.isSwimming()) {
                    ItemStack offHandItem = player.getHeldItemOffhand();
                    EffectInstance speedEffect = player.getActivePotionEffect(Effects.SPEED);
                    EffectInstance deEffect = player.getActivePotionEffect(Effects.SLOWNESS);
                    String itemName = offHandItem.getDisplayName().getString();
                    float appliedSpeed = 0;

                    if (speedEffect != null) {
                        if (speedEffect.getAmplifier() == 2) {
                            if (itemName.contains("Шар Геракла 2") || itemName.contains("Шар CHAMPION") || itemName.contains("Шар Аида 2") || itemName.contains("Шар GOD") || itemName.contains("КУБИК-РУБИК") || itemName.contains("Шар BUNNY")) {
                                appliedSpeed = s20 * 1.14F;
                            } else if (itemName.contains("Талисман Венома")) {
                                appliedSpeed = s25 * 1.14F;
                            } else if (itemName.contains("Талисман Картеля")) {
                                appliedSpeed = s15 * 1.14F;
                            } else {
                                appliedSpeed = s0 * 1.14F;
                            }
                        } else if (speedEffect.getAmplifier() == 1) {
                            if (itemName.contains("Шар Геракла 2") || itemName.contains("Шар CHAMPION") || itemName.contains("Шар Аида 2") || itemName.contains("Шар GOD") || itemName.contains("КУБИК-РУБИК") || itemName.contains("Шар BUNNY")) {
                                appliedSpeed = s20;
                            } else if (itemName.contains("Талисман Венома")) {
                                appliedSpeed = s25;
                            } else if (itemName.contains("Талисман Картеля")) {
                                appliedSpeed = s15;
                            } else {
                                appliedSpeed = s0;
                            }
                        }
                    } else {
                        if (itemName.contains("Шар Геракла 2") || itemName.contains("Шар CHAMPION") || itemName.contains("Шар Аида 2") || itemName.contains("Шар GOD") || itemName.contains("КУБИК-РУБИК") || itemName.contains("Шар BUNNY")) {
                            appliedSpeed = s20 * 0.68F;
                        } else if (itemName.contains("Талисман Венома")) {
                            appliedSpeed = s25 * 0.68F;
                        } else if (itemName.contains("Талисман Картеля")) {
                            appliedSpeed = s15 * 0.68F;
                        } else {
                            appliedSpeed = s0 * 0.68F;
                        }
                    }

                    if (deEffect != null) {
                        appliedSpeed *= 0.85f;
                    }

                    if (isBoosting) {
                        appliedSpeed *= 1.75F;
                    }

                    MoveUtil.setSpeed(appliedSpeed);
                }
            }
        }
        return false;
    }
}