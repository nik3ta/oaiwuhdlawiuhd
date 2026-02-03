package nuclear.module.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.MultiBoxSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.anim.animations.Easing;
import nuclear.utils.anim.animations.TimeAnim;
import nuclear.utils.font.Fonts;
import nuclear.utils.math.TargetUtil;
import nuclear.utils.move.MoveUtil;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import org.joml.Vector4i;

import static nuclear.ui.clickgui.Panel.getColorByName;

@Annotation(name = "Arrows", type = TypeList.Render, desc = "Создаёт стрелочки к игрокам и предметам в мире")
public class Arrows extends Module {
    public final MultiBoxSetting targets = new MultiBoxSetting("Отображать",
            new BooleanSetting("Игроков", true),
            new BooleanSetting("Монстров", false),
            new BooleanSetting("Друзей", true),
            new BooleanSetting("Животных", false),
            new BooleanSetting("Жителей", false),
            new BooleanSetting("Голых", true),
            new BooleanSetting("Предметы", false)
    );
    public final BooleanSetting valuableItems = new BooleanSetting("Только ценные", true).setVisible(() -> targets.get("Предметы"));
    public final SliderSetting size3 = new SliderSetting("Размер", 17, 15, 20, 1);
    private final SliderSetting size2 = new SliderSetting("Радиус", 58, 30, 110, 2);
    private final BooleanSetting dinam = new BooleanSetting("Динамический", true);
    private final TimeAnim animationStep = new TimeAnim(Easing.EASE_OUT_EXPO, 1000L);
    private final MultiBoxSetting elements = new MultiBoxSetting("Показывать",
            new BooleanSetting("Дистанция", true),
            new BooleanSetting("Ник", true));

    private float animatedYaw;
    private float animatedPitch;

    public Arrows() {
        addSettings(targets, valuableItems, elements, size3, size2, dinam);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender render && render.isRender2D()) {
            render2D(render);
        }
        return false;
    }

    private void render2D(EventRender render) {
        long currentTime = System.currentTimeMillis();

        float size = size2.getValue().floatValue();
        if (mc.currentScreen instanceof InventoryScreen) {
            size += 10f;
        }
        if (MoveUtil.isMoving() && dinam.get()) {
            size += 10.0f;
        }
        this.animationStep.run(size);

        for (Entity entity : mc.world.getAllEntities()) {
            if (entity == mc.player) continue;

            if (entity instanceof ItemEntity itemEntity) {
                if (!targets.get("Предметы")) continue;

                boolean isValuable = itemEntity.getItem().getItem() == Items.ELYTRA || itemEntity.getItem().getItem() == Items.PLAYER_HEAD;
                if (valuableItems.get() && !isValuable) continue;

                drawEntityArrow(render, entity, currentTime);
                continue;
            }

            if (!TargetUtil.isEntityTarget(entity, targets)) continue;

            if (entity instanceof PlayerEntity player) {
                if (!player.botEntity || player == Minecraft.player) {
                    continue;
                }
            }

            drawEntityArrow(render, entity, currentTime);
        }
    }

    private void drawEntityArrow(EventRender render, Entity entity, long currentTime) {
        double x = entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * mc.getRenderPartialTicks() - mc.getRenderManager().info.getProjectedView().getX();
        double z = entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * mc.getRenderPartialTicks() - mc.getRenderManager().info.getProjectedView().getZ();
        renderArrow(render, x, z, entity, currentTime);
    }

    private void renderArrow(EventRender render, double x, double z, Entity entity, long currentTime) {
        double cos = MathHelper.cos((float) (mc.getRenderManager().info.getYaw() * (Math.PI / 180)));
        double sin = MathHelper.sin((float) (mc.getRenderManager().info.getYaw() * (Math.PI / 180)));
        double rotY = -(z * cos - x * sin);
        double rotX = -(x * cos + z * sin);
        float angle = (float) (Math.atan2(rotY, rotX) * 180.0 / Math.PI);

        MainWindow mainWindow = mc.getMainWindow();
        double x2 = this.animationStep.getValue() * MathHelper.cos((float) Math.toRadians(angle)) + mainWindow.getScaledWidth() / 2.0f;
        double y2 = this.animationStep.getValue() * MathHelper.sin((float) Math.toRadians(angle)) + mainWindow.getScaledHeight() / 2.0f;

        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        GlStateManager.translated(x2 + this.animatedYaw, y2 + this.animatedPitch, 0.0);
        GlStateManager.rotatef(angle + 90.0f, 0.0f, 0.0f, 1.0f);

        int color = getColorByName("primaryColor");

        if (entity instanceof PlayerEntity player && Manager.FRIEND_MANAGER.isFriend(player.getName().getString())) {
            color = ColorUtils.rgba(0, 255, 0, 255);
        } else if (entity instanceof ItemEntity) {
            color = ColorUtils.rgba(255, 255, 255, 255); // Белый для предметов
        }

        RenderUtils.Render2D.drawImageAlph(new ResourceLocation("nuclear/images/arrow/arrow.png"),
                1.5f - (size3.getValue().floatValue() / 2) - 2.3f, 26.0f,
                size3.getValue().floatValue(), size3.getValue().floatValue(),
                new Vector4i(ColorUtils.setAlpha(color, 200), ColorUtils.setAlpha(color, 200), ColorUtils.setAlpha(color, 200), ColorUtils.setAlpha(color, 200)));

        String distText = (int) mc.player.getDistance(entity) + "m";
        String nameText = entity instanceof ItemEntity ? ((ItemEntity) entity).getItem().getDisplayName().getString() : entity.getName().getString();
        
        boolean isFriend = entity instanceof PlayerEntity player && Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(player.getName().getString());
        boolean shouldProtect = isFriend && Manager.FUNCTION_MANAGER != null && Manager.FUNCTION_MANAGER.nameProtect != null && Manager.FUNCTION_MANAGER.nameProtect.state && Manager.FUNCTION_MANAGER.nameProtect.friends != null && Manager.FUNCTION_MANAGER.nameProtect.friends.get();
        
        if (shouldProtect) {
            nameText = "Protect";
        }

        if (nameText.length() > 8) nameText = nameText.substring(0, 8);

        if (elements.get("Ник")) {
            int nameColor = -1;
            Fonts.newcode[11].drawString(render.matrixStack, nameText, 1.5f - (size3.getValue().floatValue() / 3f) - (Fonts.newcode[11].getWidth(nameText) / 2) + 4, 25.0f, nameColor);
        }

        if (elements.get("Дистанция")) {
            float yOffset = elements.get("Ник") ? 30.5f : 25.0f;
            Fonts.newcode[11].drawString(render.matrixStack, distText, 1.5f - (size3.getValue().floatValue() / 3f) - (Fonts.newcode[11].getWidth(distText) / 2) + 4, yOffset, -1);
        }

        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }
}

