package nuclear.module.impl.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.*;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.utils.ClientUtils;
import nuclear.utils.misc.TimerUtil;
import nuclear.utils.move.MoveUtil;

import java.util.List;

@Getter
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Annotation(name = "AutoExplosion", type = TypeList.Combat, desc = "Автоматически размещает и взрывает кристаллы")
public class AutoExplosion extends Module {

    private BlockPos position = null;
    private Entity crystalEntity = null;
    private int oldSlot = -1;
    private final TimerUtil timerUtil = new TimerUtil();

    private BlockPos pendingCrystalPlace = null;

    private final MultiBoxSetting protection = new MultiBoxSetting("Не взрывать",
            new BooleanSetting("Себя", true),
            new BooleanSetting("Друзей", true),
            new BooleanSetting("Предметы", true));

    public static Vector2f server;

    private static final double MAX_ROTATION_DISTANCE = 4;
    private static final double MAX_ATTACK_DISTANCE = 4;
    private static final double MAX_PLACE_DISTANCE = 4;

    public AutoExplosion() {
        addSettings(protection);
    }

    public boolean check() {
        return state && server != null && crystalEntity != null && position != null;
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventWorldChanged) {
            resetCrystalState();
            return false;
        }

        if (event instanceof EventInput e) {
            handleInput(e);
        }

        if (mc.player == null || mc.world == null) return false;

        if (check() && crystalEntity != null && isValidForRotation(crystalEntity) && server != null && event != null) {
            ClientUtils.look(event, server, ClientUtils.Correction.FULL, null);
        } else if (server != null && (crystalEntity == null || !isValidForRotation(crystalEntity))) {
            // Сбрасываем server если кристалл невалиден для ротации
            server = null;
        }

        if (event instanceof EventPlace eventPlace && eventPlace.getBlock() == Blocks.OBSIDIAN) {
            handleObsidianPlace(eventPlace.getPos());
        } else if (event instanceof EventUpdate updateEvent) {
            handleUpdateEvent(updateEvent);
        } else if (event instanceof EventMotion motionEvent) {
            handleMotionEvent(motionEvent);
        }

        return false;
    }

    private void handleInput(EventInput event) {
        if (event == null || mc.player == null) return;

        if (check() && crystalEntity != null && isValidForRotation(crystalEntity) && server != null) {
            MoveUtil.fixMovement(event, server.x);
        } else if (server != null && (crystalEntity == null || !isValidForRotation(crystalEntity))) {
            // Сбрасываем server если кристалл невалиден для ротации
            server = null;
        }
    }

    private void handleUpdateEvent(EventUpdate updateEvent) {
        if (updateEvent == null || mc.player == null || mc.world == null) return;

        if (pendingCrystalPlace != null) {
            final int crystalSlot = getSlotWithCrystal();
            if (crystalSlot == -1) {
                pendingCrystalPlace = null;
                return;
            }

            if (mc.player.inventory == null) {
                pendingCrystalPlace = null;
                return;
            }

            Vector3d placeTarget = new Vector3d(
                    pendingCrystalPlace.getX() + 0.5,
                    pendingCrystalPlace.getY() + 1.0,
                    pendingCrystalPlace.getZ() + 0.5
            );

            oldSlot = mc.player.inventory.currentItem;
            mc.player.inventory.currentItem = crystalSlot;

            BlockRayTraceResult rayTrace = new BlockRayTraceResult(
                    placeTarget,
                    Direction.UP,
                    pendingCrystalPlace,
                    false
            );

            if (mc.playerController != null) {
                ActionResultType result = mc.playerController.processRightClickBlock(
                        mc.player, mc.world, Hand.MAIN_HAND, rayTrace
                );

                if (result == ActionResultType.SUCCESS) {
                    mc.player.swingArm(Hand.MAIN_HAND);
                }
            }

            if (oldSlot != -1 && mc.player.inventory != null) {
                mc.player.inventory.currentItem = oldSlot;
            }
            oldSlot = -1;

            this.position = pendingCrystalPlace;
            this.pendingCrystalPlace = null;
        }

        if (position != null && mc.world != null) {
            List<Entity> crystals = mc.world.getEntitiesWithinAABBExcludingEntity(null,
                            new AxisAlignedBB(
                                    position.getX(), position.getY(), position.getZ(),
                                    position.getX() + 1.0, position.getY() + 2.0, position.getZ() + 1.0
                            ))
                    .stream()
                    .filter(e -> e instanceof EnderCrystalEntity && e.isAlive())
                    .toList();

            if (crystalEntity != null && !crystalEntity.isAlive()) {
                resetCrystalState();
                return;
            }

            if (!crystals.isEmpty()) {
                crystalEntity = crystals.get(0);
            }

            if (crystalEntity != null && isValidForAttack(crystalEntity)) {
                attackEntity(crystalEntity);
            }
        }

        if (crystalEntity != null && !crystalEntity.isAlive()) {
            resetCrystalState();
        }

        // Проверяем валидность кристалла и сбрасываем server если он невалиден
        if (crystalEntity != null && !isValidForRotation(crystalEntity)) {
            server = null;
        }
    }

    private void handleObsidianPlace(BlockPos pos) {
        if (pos == null || mc.player == null || !isPlaceDistanceValid(pos)) return;

        this.pendingCrystalPlace = pos;
        this.position = null;
    }

    private void handleMotionEvent(EventMotion motionEvent) {
        if (motionEvent == null || mc.player == null) return;

        if (crystalEntity == null || !isValidForRotation(crystalEntity)) {
            // Сбрасываем server если кристалл невалиден
            server = null;
            return;
        }

        Vector3d crystalCenter = crystalEntity.getPositionVec().add(0, 0.5, 0);
        server = ClientUtils.get(crystalCenter);

        if (server != null) {
            motionEvent.setYaw(server.x);
            motionEvent.setPitch(server.y);
            mc.player.rotationYawHead = server.x;
            mc.player.renderYawOffset = server.x;
            mc.player.rotationPitchHead = server.y;
        }
    }

    private void attackEntity(Entity base) {
        if (base == null || mc.player == null || mc.playerController == null) return;
        if (!isValidForAttack(base)) return;
        if (mc.player.getCooledAttackStrength(1.0f) < 1.0f) return;
        if (!timerUtil.finished(80)) return;

        mc.playerController.attackEntity(mc.player, base);
        mc.player.swingArm(Hand.MAIN_HAND);
        timerUtil.reset();
    }

    private int getSlotWithCrystal() {
        if (mc.player == null || mc.player.inventory == null) return -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i) != null &&
                    mc.player.inventory.getStackInSlot(i).getItem() == Items.END_CRYSTAL) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPlaceDistanceValid(BlockPos pos) {
        if (pos == null || mc.player == null) return false;
        Vector3d playerPos = mc.player.getPositionVec();
        if (playerPos == null) return false;

        return playerPos.distanceTo(
                new Vector3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5)
        ) <= MAX_PLACE_DISTANCE;
    }

    private boolean shouldNotExplodeBecauseOfSelf() {
        if (crystalEntity == null || mc.player == null) return false;
        return protection.get("Себя") && crystalEntity.getPosY() <= mc.player.getPosY() + 0.1;
    }

    private boolean shouldNotExplodeBecauseOfFriend() {
        if (!protection.get("Друзей")) return false;
        if (crystalEntity == null || mc.world == null || mc.player == null) return false;

        return mc.world.getEntitiesWithinAABB(PlayerEntity.class, crystalEntity.getBoundingBox().grow(6.0))
                .stream()
                .anyMatch(entity -> entity != null
                        && entity != mc.player
                        && entity.isAlive()
                        && entity.getName() != null
                        && Manager.FRIEND_MANAGER != null
                        && Manager.FRIEND_MANAGER.isFriend(entity.getName().getString()));
    }

    private boolean shouldNotExplodeBecauseOfItems() {
        if (!protection.get("Предметы")) return false;
        if (crystalEntity == null || mc.world == null) return false;

        return mc.world.getEntitiesWithinAABB(ItemEntity.class, crystalEntity.getBoundingBox().grow(6.0))
                .stream()
                .filter(e -> e != null && e instanceof ItemEntity)
                .map(e -> {
                    ItemEntity itemEntity = (ItemEntity) e;
                    return itemEntity.getItem() != null ? itemEntity.getItem().getItem() : null;
                })
                .filter(item -> item != null)
                .anyMatch(item -> item == Items.TOTEM_OF_UNDYING ||
                        item == Items.END_CRYSTAL ||
                        item == Items.ENCHANTED_GOLDEN_APPLE ||
                        item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE ||
                        item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS ||
                        item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD ||
                        item == Items.ELYTRA || item == Items.TRIDENT);
    }

    private boolean hasProtectionBlock() {
        return shouldNotExplodeBecauseOfSelf() ||
                shouldNotExplodeBecauseOfFriend() ||
                shouldNotExplodeBecauseOfItems();
    }

    private boolean isValidForRotation(Entity base) {
        if (base == null || !base.isAlive()) return false;
        if (mc.player == null) return false;
        if (hasProtectionBlock()) return false;
        return mc.player.getDistance(base) <= MAX_ROTATION_DISTANCE;
    }

    private boolean isValidForAttack(Entity base) {
        if (base == null || mc.player == null) return false;
        if (!isValidForRotation(base)) return false;
        if (hasProtectionBlock()) return false;
        return mc.player.getDistance(base) <= MAX_ATTACK_DISTANCE;
    }


    private void resetCrystalState() {
        crystalEntity = null;
        position = null;
        pendingCrystalPlace = null;
        server = null;
        oldSlot = -1;
    }

    @Override
    public void onDisable() {
        resetCrystalState();
        super.onDisable();
    }
}