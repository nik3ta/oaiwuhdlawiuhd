package nuclear.module.impl.player;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockRayTraceResult;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventAttack;
import nuclear.control.events.impl.player.EventBlockDamage;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;

@Annotation(name = "AutoTool", type = TypeList.Player, desc = "Автоматически берет нужный предмет или меч")
public class AutoTool extends Module {
    public static BooleanSetting sword = new BooleanSetting("Брать меч из хотбара", true);
    public static BooleanSetting packet = new BooleanSetting("Пакетный", true);
    private int oldSlot = -1;
    private boolean status;
    private long lastActionTime = 0;
    private int lastSlot = -1;
    private static final long DELAY_MS = 150;

    public AutoTool() {
        addSettings(sword, packet);
    }

    @Override
    public boolean onEvent(Event event) {
        if (mc.player != null && mc.world != null && mc.player.connection != null) {
            if (packet.get()) {
                if (event instanceof EventBlockDamage e) {
                    if (e.getState() == EventBlockDamage.State.START) {
                        this.lastSlot = mc.player.inventory.currentItem;
                        int bestToolSlot = findBestToolForBlock(e.getBlockState());
                        if (bestToolSlot != -1) {
                            mc.player.inventory.currentItem = bestToolSlot;
                        }
                    } else if (this.lastSlot != -1) {
                        mc.player.inventory.currentItem = this.lastSlot;
                        this.lastSlot = -1;
                    }
                }
            } else {
                if (mc.objectMouseOver != null && mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver instanceof BlockRayTraceResult) {
                    BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) mc.objectMouseOver;
                    Block block = mc.world.getBlockState(blockRayTraceResult.getPos()).getBlock();
                    if (block != Blocks.AIR) {
                        int bestSlot = this.findBestSlot();
                        if (bestSlot == -1) {
                            return false;
                        }

                        this.status = true;
                        this.lastActionTime = System.currentTimeMillis();
                        if (this.oldSlot == -1) {
                            this.oldSlot = mc.player.inventory.currentItem;
                        }
                        mc.player.inventory.currentItem = bestSlot;
                    } else if (this.status && System.currentTimeMillis() - this.lastActionTime >= DELAY_MS) {
                        if (this.oldSlot != -1) {
                            mc.player.inventory.currentItem = this.oldSlot;
                        }
                        this.reset();
                    }
                } else if (this.status && System.currentTimeMillis() - this.lastActionTime >= DELAY_MS) {
                    if (this.oldSlot != -1) {
                        mc.player.inventory.currentItem = this.oldSlot;
                    }
                    this.reset();
                }
            }

            if (event instanceof EventAttack && sword.getValue()) {
                EventAttack attackEvent = (EventAttack) event;
                Entity target = attackEvent.getTarget();

                if (target != null) {
                    if (target instanceof net.minecraft.entity.item.EnderCrystalEntity) {
                        return false;
                    }

                    ItemStack heldItem = mc.player.getHeldItemMainhand();

                    if (heldItem.getItem() == Items.GOLDEN_APPLE
                            || heldItem.getItem() == Items.ENCHANTED_GOLDEN_APPLE
                            || heldItem.getItem().isFood()
                            || heldItem.getItem() instanceof PotionItem) {
                        return false;
                    }

                    int bestSwordSlot = findSwordSlot();

                    if (bestSwordSlot != -1 && !(heldItem.getItem() instanceof SwordItem)) {
                        this.lastActionTime = System.currentTimeMillis();
                        mc.player.inventory.currentItem = bestSwordSlot;
                        mc.player.connection.sendPacket(new CHeldItemChangePacket(bestSwordSlot));
                    }
                }
            }
        }
        return false;
    }

    private void reset() {
        this.oldSlot = -1;
        this.status = false;
        this.lastActionTime = 0;
    }

    public static int findBestToolForBlock(BlockState blockState) {
        if (mc.player == null) {
            return -1;
        } else {
            int bestSlot = -1;
            float bestSpeed = 1.0F;
            
            // Check if block requires scissors (cobweb, leaves, wool, vines, etc.)
            Block block = blockState.getBlock();
            boolean requiresShears = block == Blocks.COBWEB 
                    || blockState.isIn(BlockTags.LEAVES) 
                    || blockState.isIn(BlockTags.WOOL)
                    || block == Blocks.GRASS
                    || block == Blocks.FERN
                    || block == Blocks.DEAD_BUSH
                    || block == Blocks.VINE
                    || block == Blocks.TRIPWIRE;

            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    
                    // Prioritize scissors for blocks that require them
                    if (requiresShears && item instanceof ShearsItem) {
                        float speed = mc.player.calculateItemDigSpeed(stack, blockState);
                        if (speed > bestSpeed) {
                            bestSpeed = speed;
                            bestSlot = i;
                        }
                    } else if (item instanceof ToolItem) {
                        float speed = mc.player.calculateItemDigSpeed(stack, blockState);
                        if (speed > bestSpeed) {
                            bestSpeed = speed;
                            bestSlot = i;
                        }
                    } else if (item instanceof ShearsItem && !requiresShears) {
                        // Also check scissors for other blocks (they might still be useful)
                        float speed = mc.player.calculateItemDigSpeed(stack, blockState);
                        if (speed > bestSpeed) {
                            bestSpeed = speed;
                            bestSlot = i;
                        }
                    }
                }
            }

            return bestSlot;
        }
    }

    private int findBestSlot() {
        if (mc.objectMouseOver instanceof BlockRayTraceResult) {
            BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) mc.objectMouseOver;
            BlockState blockState = mc.world.getBlockState(blockRayTraceResult.getPos());
            Block block = blockState.getBlock();
            int bestSlot = -1;
            float bestSpeed = 1.0F;
            
            // Check if block requires scissors (cobweb, leaves, wool, vines, etc.)
            boolean requiresShears = block == Blocks.COBWEB 
                    || blockState.isIn(BlockTags.LEAVES) 
                    || blockState.isIn(BlockTags.WOOL)
                    || block == Blocks.GRASS
                    || block == Blocks.FERN
                    || block == Blocks.DEAD_BUSH
                    || block == Blocks.VINE
                    || block == Blocks.TRIPWIRE;

            for (int slot = 0; slot < 9; ++slot) {
                ItemStack stack = mc.player.inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    
                    // Prioritize scissors for blocks that require them
                    if (requiresShears && item instanceof ShearsItem) {
                        float speed = stack.getDestroySpeed(blockState);
                        if (speed > bestSpeed) {
                            bestSpeed = speed;
                            bestSlot = slot;
                        }
                    } else {
                        float speed = stack.getDestroySpeed(blockState);
                        if (speed > bestSpeed) {
                            bestSpeed = speed;
                            bestSlot = slot;
                        }
                    }
                }
            }

            return bestSlot;
        } else {
            return -1;
        }
    }

    private int findSwordSlot() {
        int bestSwordSlot = -1;
        float bestDamage = -1.0F;

        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = mc.player.inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof SwordItem) {
                float damage = ((SwordItem) stack.getItem()).getAttackDamage();
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSwordSlot = slot;
                }
            }
        }

        return bestSwordSlot;
    }

    @Override
    protected void onDisable() {
        this.reset();
        super.onDisable();
    }
}