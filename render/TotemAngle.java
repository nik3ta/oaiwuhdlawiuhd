package nuclear.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import nuclear.control.events.Event;
import nuclear.control.events.impl.packet.EventPacket;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.SliderSetting;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static nuclear.ui.clickgui.Panel.getColorByName;

@Annotation(name = "TotemAngel", type = TypeList.Render, desc = "Запоминает позу при тотеме и рисует летящий призрак")
public class TotemAngle extends Module {
    private final SliderSetting riseHeight = new SliderSetting("Высота подъема", 4f, 0.2f, 5.0f, 0.1f);
    private final SliderSetting duration = new SliderSetting("Время жизни", 3f, 0.2f, 6.0f, 0.1f);

    private final List<TotemGhost> ghosts = new CopyOnWriteArrayList<>();

    public TotemAngle() {
        addSettings(riseHeight, duration);
    }

    @Override
    protected void onDisable() {
        ghosts.clear();
        super.onDisable();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventPacket packetEvent && packetEvent.isReceivePacket() && packetEvent.getPacket() instanceof SEntityStatusPacket packet && packet.getOpCode() == 35) {
            if (mc.world == null) return false;
            AbstractClientPlayerEntity player = packet.getEntity(mc.world) instanceof AbstractClientPlayerEntity clientPlayer ? clientPlayer : null;
            if (player == null) return false;
            addGhost(player);
        }

        if (event instanceof EventRender render && render.isRender3D()) {
            renderGhosts(render);
        }
        return false;
    }

    private void addGhost(AbstractClientPlayerEntity player) {
        float partialTicks = mc.getRenderPartialTicks();
        boolean slim = "slim".equalsIgnoreCase(player.getSkinType());
        PlayerModel<AbstractClientPlayerEntity> model = new PlayerModel<>(0.0f, slim);

        applyModelVisibilities(player, model);

        model.isChild = player.isChild();
        model.isSneak = player.isCrouching();
        model.isSitting = player.isPassenger();
        model.swingProgress = player.getSwingProgress(partialTicks);

        float bodyYaw = MathHelper.interpolateAngle(partialTicks, player.prevRenderYawOffset, player.renderYawOffset);
        float headYaw = MathHelper.interpolateAngle(partialTicks, player.prevRotationYawHead, player.rotationYawHead);
        float headPitch = MathHelper.lerp(partialTicks, player.prevRotationPitch, player.rotationPitch);
        float netHeadYaw = headYaw - bodyYaw;

        float limbSwingAmount = MathHelper.lerp(partialTicks, player.prevLimbSwingAmount, player.limbSwingAmount);
        float limbSwing = player.limbSwing - player.limbSwingAmount * (1.0F - partialTicks);

        if (player.isChild()) limbSwing *= 3.0F;
        if (limbSwingAmount > 1.0F) limbSwingAmount = 1.0F;

        float ageInTicks = player.ticksExisted + partialTicks;
        model.setLivingAnimations(player, limbSwing, limbSwingAmount, partialTicks);
        model.setRotationAngles(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        double x = MathHelper.lerp(partialTicks, player.lastTickPosX, player.getPosX());
        double y = MathHelper.lerp(partialTicks, player.lastTickPosY, player.getPosY());
        double z = MathHelper.lerp(partialTicks, player.lastTickPosZ, player.getPosZ());
        Vector3d pos = new Vector3d(x, y, z);

        ghosts.add(new TotemGhost(model, pos, player.getLocationSkin(), bodyYaw, System.currentTimeMillis()));
    }

    private void renderGhosts(EventRender event) {
        if (ghosts.isEmpty()) return;

        IRenderTypeBuffer.Impl buffer = mc.getRenderTypeBuffers().getBufferSource();
        Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
        long now = System.currentTimeMillis();

        List<TotemGhost> toRemove = new ArrayList<>();

        for (TotemGhost ghost : ghosts) {
            float progress = (now - ghost.startTime) / (duration.getValue().floatValue() * 1000.0f);

            if (progress >= 1.0f) {
                toRemove.add(ghost);
                continue;
            }

            double motionY = riseHeight.getValue().floatValue() * ease(progress);
            float alpha = (float) easeOutAlpha(progress);

            int color = resolveColor(progress);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            MatrixStack matrixStack = new MatrixStack();
            matrixStack.push();

            double renderX = ghost.position.x - cameraPos.x;
            double renderY = ghost.position.y - cameraPos.y + motionY;
            double renderZ = ghost.position.z - cameraPos.z;

            matrixStack.translate(renderX, renderY, renderZ);

            matrixStack.rotate(Vector3f.YP.rotationDegrees(180.0f - ghost.bodyYaw));

            matrixStack.scale(-1f, -1f, 1f);
            matrixStack.translate(0.0D, -1.501D, 0.0D);

            float effectiveAlpha = MathHelper.clamp(alpha, 0.0f, 0.75f);
            renderFlatModel(matrixStack, r, g, b, effectiveAlpha, ghost.model, false);

            matrixStack.pop();
        }

        if (!toRemove.isEmpty()) {
            ghosts.removeAll(toRemove);
        }

        buffer.finish();
    }

    private void renderFlatModel(MatrixStack stack, float r, float g, float b, float a, PlayerModel<AbstractClientPlayerEntity> model, boolean chams) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (chams) {
            RenderSystem.disableDepthTest();
        }

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        renderFlatPart(stack, buf, model.bipedHead, -4, -8, -4, 8, 8, 8, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedBody, -4, 0, -2, 8, 12, 4, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedRightArm, -3, -2, -2, 4, 12, 4, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedLeftArm, -3, -2, -2, 4, 12, 4, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedRightLeg, -2, 0, -2, 4, 12, 4, r, g, b, a);
        renderFlatPart(stack, buf, model.bipedLeftLeg, -2, 0, -2, 4, 12, 4, r, g, b, a);

        tess.draw();

        RenderSystem.enableCull();
        if (chams) {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.enableTexture();
    }

    private void renderFlatPart(MatrixStack parent, BufferBuilder buf, ModelRenderer part, float minX, float minY, float minZ, float sizeX, float sizeY, float sizeZ, float r, float g, float b, float a) {
        parent.push();
        float unit = 0.0625f;
        parent.translate(part.rotationPointX * unit, part.rotationPointY * unit, part.rotationPointZ * unit);
        if (part.rotateAngleZ != 0.0f) parent.rotate(Vector3f.ZP.rotation(part.rotateAngleZ));
        if (part.rotateAngleY != 0.0f) parent.rotate(Vector3f.YP.rotation(part.rotateAngleY));
        if (part.rotateAngleX != 0.0f) parent.rotate(Vector3f.XP.rotation(part.rotateAngleX));

        float x1 = minX * unit;
        float y1 = minY * unit;
        float z1 = minZ * unit;
        float x2 = (minX + sizeX) * unit;
        float y2 = (minY + sizeY) * unit;
        float z2 = (minZ + sizeZ) * unit;

        Matrix4f matrix = parent.getLast().getMatrix();

        // Front
        buf.pos(matrix, x1, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z2).color(r, g, b, a).endVertex();

        // Back
        buf.pos(matrix, x2, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z1).color(r, g, b, a).endVertex();

        // Left
        buf.pos(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z1).color(r, g, b, a).endVertex();

        // Right
        buf.pos(matrix, x2, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z2).color(r, g, b, a).endVertex();

        // Top
        buf.pos(matrix, x1, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y2, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y2, z1).color(r, g, b, a).endVertex();

        // Bottom
        buf.pos(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(matrix, x2, y1, z2).color(r, g, b, a).endVertex();
        buf.pos(matrix, x1, y1, z2).color(r, g, b, a).endVertex();

        parent.pop();
    }

    private int resolveColor(float progress) {
        return getColorByName("primaryColor");
    }

    private double ease(double t) {
        t = MathHelper.clamp(t, 0.0D, 0.75D);
        return 1.0D - Math.pow(1.0D - t, 3);
    }

    private double easeOutAlpha(double t) {
        t = MathHelper.clamp(t, 0.0D, 1.0D);
        double invT = 1.0D - t;
        return 0.75D * (1.0D - Math.pow(invT, 3));
    }

    private void applyModelVisibilities(AbstractClientPlayerEntity player, PlayerModel<AbstractClientPlayerEntity> model) {
        model.setVisible(true);
        model.bipedHeadwear.showModel = player.isWearing(PlayerModelPart.HAT);
        model.bipedBodyWear.showModel = player.isWearing(PlayerModelPart.JACKET);
        model.bipedLeftLegwear.showModel = player.isWearing(PlayerModelPart.LEFT_PANTS_LEG);
        model.bipedRightLegwear.showModel = player.isWearing(PlayerModelPart.RIGHT_PANTS_LEG);
        model.bipedLeftArmwear.showModel = player.isWearing(PlayerModelPart.LEFT_SLEEVE);
        model.bipedRightArmwear.showModel = player.isWearing(PlayerModelPart.RIGHT_SLEEVE);
        model.isSneak = player.isCrouching();

        BipedModel.ArmPose mainPose = getArmPose(player, Hand.MAIN_HAND);
        BipedModel.ArmPose offPose = getArmPose(player, Hand.OFF_HAND);

        if (mainPose.func_241657_a_()) {
            offPose = player.getHeldItemOffhand().isEmpty() ? BipedModel.ArmPose.EMPTY : BipedModel.ArmPose.ITEM;
        }

        if (player.getPrimaryHand() == HandSide.RIGHT) {
            model.rightArmPose = mainPose;
            model.leftArmPose = offPose;
        } else {
            model.rightArmPose = offPose;
            model.leftArmPose = mainPose;
        }
    }

    private BipedModel.ArmPose getArmPose(AbstractClientPlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (stack.isEmpty()) {
            return BipedModel.ArmPose.EMPTY;
        }

        if (player.getActiveHand() == hand && player.getItemInUseCount() > 0) {
            UseAction action = stack.getUseAction();
            if (action == UseAction.BLOCK) return BipedModel.ArmPose.BLOCK;
            if (action == UseAction.BOW) return BipedModel.ArmPose.BOW_AND_ARROW;
            if (action == UseAction.SPEAR) return BipedModel.ArmPose.THROW_SPEAR;
            if (action == UseAction.CROSSBOW && hand == player.getActiveHand())
                return BipedModel.ArmPose.CROSSBOW_CHARGE;
        } else if (!player.isSwingInProgress && stack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(stack)) {
            return BipedModel.ArmPose.CROSSBOW_HOLD;
        }

        return BipedModel.ArmPose.ITEM;
    }

    private static class TotemGhost {
        private final PlayerModel<AbstractClientPlayerEntity> model;
        private final Vector3d position;
        private final ResourceLocation skin;
        private final float bodyYaw;
        private final long startTime;

        private TotemGhost(PlayerModel<AbstractClientPlayerEntity> model, Vector3d position, ResourceLocation skin, float bodyYaw, long startTime) {
            this.model = model;
            this.position = position;
            this.skin = skin;
            this.bodyYaw = bodyYaw;
            this.startTime = startTime;
        }
    }
}

