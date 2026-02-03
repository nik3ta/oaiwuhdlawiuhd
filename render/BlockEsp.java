package nuclear.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.minecart.*;
import net.minecraft.state.properties.ChestType;
import net.minecraft.tileentity.*;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.Setting;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.ColorSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Annotation(
        name = "BlockEsp",
        type = TypeList.Render,
        desc = "Подсвечивает определённые блоки"
)
public class BlockEsp extends Module {
    private final MultiBoxSetting blocksSelection = new MultiBoxSetting("Выбрать блоки", new BooleanSetting[]{
            new BooleanSetting("Сундуки", true),
            new BooleanSetting("Эндер Сундуки", false),
            new BooleanSetting("Спавнера", true),
            new BooleanSetting("Шалкера", false),
            new BooleanSetting("Кровати", false),
            new BooleanSetting("Вагонетка", false),
            new BooleanSetting("Бочка", false),
            new BooleanSetting("Головы", false),
            new BooleanSetting("Воронки", false)
    });

    public final BooleanSetting outline = new BooleanSetting("Только обводка", true);

    private final ColorSetting chestColor = new ColorSetting("Цвет сундуков", ColorUtils.rgba(197, 116, 49, 255))
            .setVisible(() -> blocksSelection.get(0));
    private final ColorSetting enderChestColor = new ColorSetting("Цвет эндер сундук", ColorUtils.rgba(125, 49, 197, 255))
            .setVisible(() -> blocksSelection.get(1));
    private final ColorSetting spawnerColor = new ColorSetting("Цвет спавнеров", ColorUtils.rgba(113, 113, 113, 255))
            .setVisible(() -> blocksSelection.get(2));
    private final ColorSetting shulkerColor = new ColorSetting("Цвет шалкеров", ColorUtils.rgba(193, 100, 193, 255))
            .setVisible(() -> blocksSelection.get(3));
    private final ColorSetting bedColor = new ColorSetting("Цвет кроватей", ColorUtils.rgba(255, 255, 255, 255))
            .setVisible(() -> blocksSelection.get(4));
    private final ColorSetting minecartColor = new ColorSetting("Цвет вагонеток", ColorUtils.rgba(112, 109, 109, 255))
            .setVisible(() -> blocksSelection.get(5));
    private final ColorSetting barrelColor = new ColorSetting("Цвет бочек", ColorUtils.rgba(195, 136, 68, 255))
            .setVisible(() -> blocksSelection.get(6));
    private final ColorSetting skullColor = new ColorSetting("Цвет голов", ColorUtils.rgba(89, 77, 77, 255))
            .setVisible(() -> blocksSelection.get(7));
    private final ColorSetting hopperColor = new ColorSetting("Цвет воронок", ColorUtils.rgba(80, 80, 80, 255))
            .setVisible(() -> blocksSelection.get(8));

    public BlockEsp() {
        this.addSettings(new Setting[]{
                blocksSelection,
                outline,
                chestColor,
                enderChestColor,
                spawnerColor,
                shulkerColor,
                bedColor,
                minecartColor,
                barrelColor,
                skullColor,
                hopperColor
        });
    }

    private void renderBlockOutline(MatrixStack matrixStack, VoxelShape shape, BlockPos blockPos, int selectedColor) {
        matrixStack.push();
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrixStack.getLast().getMatrix());

        double renderX = mc.getRenderManager().renderPosX();
        double renderY = mc.getRenderManager().renderPosY();
        double renderZ = mc.getRenderManager().renderPosZ();
        RenderSystem.translated(-renderX, -renderY, -renderZ);

        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        RenderSystem.lineWidth(1);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        int outlineColorValue = selectedColor;
        int fillColorValue = ColorUtils.multAlpha(outlineColorValue, 0.25f);

        for (AxisAlignedBB aabb : shape.toBoundingBoxList()) {
            double minX = blockPos.getX() + aabb.minX;
            double minY = blockPos.getY() + aabb.minY;
            double minZ = blockPos.getZ() + aabb.minZ;
            double maxX = blockPos.getX() + aabb.maxX;
            double maxY = blockPos.getY() + aabb.maxY;
            double maxZ = blockPos.getZ() + aabb.maxZ;

            if (!outline.get()) {
                bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                RenderUtils.Render3D.renderFilledBox(bufferBuilder, minX, minY, minZ, maxX, maxY, maxZ, fillColorValue);
                tessellator.draw();

                AxisAlignedBB bb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
                RenderUtils.Render3D.drawBox(bb, fillColorValue);
            }

            bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            RenderUtils.Render3D.renderWireframeBox(bufferBuilder, minX, minY, minZ, maxX, maxY, maxZ, outlineColorValue);
            tessellator.draw();
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();

        RenderSystem.translated(renderX, renderY, renderZ);
        RenderSystem.popMatrix();
        matrixStack.pop();
    }

    private AxisAlignedBB getDoubleChestBoundingBox(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock)) return null;

        ChestType chestType = state.get(ChestBlock.TYPE);
        if (chestType == ChestType.SINGLE) return null;

        Direction facing = state.get(ChestBlock.FACING);
        BlockPos adjacentPos = pos.offset(chestType == ChestType.RIGHT ? facing.rotateYCCW() : facing.rotateY());

        BlockState adjacentState = mc.world.getBlockState(adjacentPos);
        if (!(adjacentState.getBlock() instanceof ChestBlock) || adjacentState.get(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }

        VoxelShape shape1 = state.getShape(mc.world, pos);
        VoxelShape shape2 = adjacentState.getShape(mc.world, adjacentPos);

        AxisAlignedBB bb1 = shape1.getBoundingBox();
        AxisAlignedBB bb2 = shape2.getBoundingBox();

        double minX = Math.min(pos.getX() + bb1.minX, adjacentPos.getX() + bb2.minX);
        double minY = Math.min(pos.getY() + bb1.minY, adjacentPos.getY() + bb2.minY);
        double minZ = Math.min(pos.getZ() + bb1.minZ, adjacentPos.getZ() + bb2.minZ);
        double maxX = Math.max(pos.getX() + bb1.maxX, adjacentPos.getX() + bb2.maxX);
        double maxY = Math.max(pos.getY() + bb1.maxY, adjacentPos.getY() + bb2.maxY);
        double maxZ = Math.max(pos.getZ() + bb1.maxZ, adjacentPos.getZ() + bb2.maxZ);

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventRender e) {
            if (e.isRender3D()) {
                MatrixStack matrixStack = new MatrixStack();
                Iterator<TileEntity> tileEntityIterator = mc.world.loadedTileEntityList.iterator();
                Set<BlockPos> renderedChests = new HashSet<>();

                while (tileEntityIterator.hasNext()) {
                    TileEntity tileEntity = tileEntityIterator.next();
                    BlockPos pos = tileEntity.getPos();
                    BlockState state = mc.world.getBlockState(pos);
                    VoxelShape shape = state.getShape(mc.world, pos);

                    if (tileEntity instanceof ChestTileEntity && this.blocksSelection.get(0)) {
                        AxisAlignedBB doubleChestBB = getDoubleChestBoundingBox(pos, state);
                        if (doubleChestBB != null) {
                            ChestType chestType = state.get(ChestBlock.TYPE);
                            Direction facing = state.get(ChestBlock.FACING);
                            BlockPos adjacentPos = pos.offset(chestType == ChestType.RIGHT ? facing.rotateYCCW() : facing.rotateY());

                            if (chestType == ChestType.LEFT || !renderedChests.contains(adjacentPos)) {
                                shape = VoxelShapes.create(doubleChestBB.offset(-pos.getX(), -pos.getY(), -pos.getZ()));
                                renderBlockOutline(matrixStack, shape, pos, chestColor.get());
                                renderedChests.add(pos);
                                renderedChests.add(adjacentPos);
                            }
                        } else {
                            if (!renderedChests.contains(pos)) {
                                renderBlockOutline(matrixStack, shape, pos, chestColor.get());
                                renderedChests.add(pos);
                            }
                        }
                    }

                    if (tileEntity instanceof EnderChestTileEntity && this.blocksSelection.get(1)) {
                        renderBlockOutline(matrixStack, shape, pos, enderChestColor.get());
                    }

                    if (tileEntity instanceof MobSpawnerTileEntity && this.blocksSelection.get(2)) {
                        renderBlockOutline(matrixStack, shape, pos, spawnerColor.get());
                    }

                    if (tileEntity instanceof ShulkerBoxTileEntity && this.blocksSelection.get(3)) {
                        renderBlockOutline(matrixStack, shape, pos, shulkerColor.get());
                    }

                    if (tileEntity instanceof BedTileEntity && this.blocksSelection.get(4)) {
                        renderBlockOutline(matrixStack, shape, pos, bedColor.get());
                    }

                    if (tileEntity instanceof BarrelTileEntity && this.blocksSelection.get(6)) {
                        renderBlockOutline(matrixStack, shape, pos, barrelColor.get());
                    }

                    if (tileEntity instanceof SkullTileEntity && this.blocksSelection.get(7)) {
                        renderBlockOutline(matrixStack, shape, pos, skullColor.get());
                    }

                    if (tileEntity instanceof HopperTileEntity && this.blocksSelection.get(8)) {
                        renderBlockOutline(matrixStack, shape, pos, hopperColor.get());
                    }
                }

                Iterator<Entity> entityIterator = mc.world.getAllEntities().iterator();

                while (entityIterator.hasNext()) {
                    Entity entity = entityIterator.next();
                    if ((entity instanceof MinecartEntity || entity instanceof ChestMinecartEntity ||
                            entity instanceof TNTMinecartEntity || entity instanceof FurnaceMinecartEntity ||
                            entity instanceof HopperMinecartEntity || entity instanceof CommandBlockMinecartEntity) &&
                            this.blocksSelection.get(5)) {
                        BlockPos pos = entity.getPosition();
                        VoxelShape shape = VoxelShapes.create(0, 0, 0, 1, 1, 1);
                        renderBlockOutline(matrixStack, shape, pos, minecartColor.get());
                    }
                }
            }
        }
        return false;
    }
}