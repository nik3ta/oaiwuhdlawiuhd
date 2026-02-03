package nuclear.module.impl.combat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import nuclear.control.Manager;
import nuclear.control.events.Event;
import nuclear.control.events.impl.player.EventUpdate;
import nuclear.control.events.impl.render.EventRender;
import nuclear.module.TypeList;
import nuclear.module.api.Annotation;
import nuclear.module.api.Module;
import nuclear.module.settings.imp.BooleanSetting;
import nuclear.module.settings.imp.SliderSetting;
import nuclear.utils.render.ColorUtils;
import nuclear.utils.render.RenderUtils;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.List;

import static nuclear.ui.clickgui.Panel.getColorByName;

@Annotation(name = "BackTrack", type = TypeList.Combat, desc = "Задерживает хитбокс")
public class BackTrack extends Module {

    private final SliderSetting time = new SliderSetting("Время", 6f, 1f, 10f, 1f);
    private final BooleanSetting display = new BooleanSetting("Отображать", true);

    public BackTrack() {
        addSettings(time, display);
    }

    @Override
    public boolean onEvent(Event event) {
        if (AttackAura.getTarget() == null || !(AttackAura.getTarget() instanceof PlayerEntity)) {
            return false;
        }

        PlayerEntity target = (PlayerEntity) AttackAura.getTarget();

        if (event instanceof EventUpdate) {
            if (mc.world == null || mc.player == null) return false;
            if (target == null || target == mc.player) return false;
            if (target.getName() == null || (Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(target.getName().getString()))) return false;
            if (AntiBot.checkBot(target)) return false;

            List<Position> backtrack = target.getBacktrack();
            if (backtrack == null) return false;

            if (backtrack.size() > 2) {
                backtrack.remove(0);
            }

            long maxTime = (long) ((time.getValue().floatValue() * 1000) * 5);
            if (maxTime > 0) {
                backtrack.removeIf(pos -> pos != null && (System.currentTimeMillis() - pos.getTime()) > maxTime);
            }
        }

        if (event instanceof EventRender e && e.isRender3D() && display.get()) {
            if (mc.world == null || mc.player == null) return false;
            if (target == null || target == mc.player) return false;
            if (target.getName() == null || (Manager.FRIEND_MANAGER != null && Manager.FRIEND_MANAGER.isFriend(target.getName().getString()))) return false;
            if (AntiBot.checkBot(target)) return false;
            if (mc.getRenderManager() == null || mc.getRenderManager().info == null) return false;

            List<Position> backtrack = target.getBacktrack();
            if (backtrack == null || backtrack.isEmpty()) return false;

            Vector3d viewPos = mc.getRenderManager().info.getProjectedView();
            if (viewPos == null) return false;

            long maxTime = (long) ((time.getValue().floatValue() * 1000) * 5);
            if (maxTime <= 0) return false;

            Iterator<Position> iterator = backtrack.iterator();
            while (iterator.hasNext()) {
                Position position = iterator.next();
                if (position == null) {
                    iterator.remove();
                    continue;
                }

                try {
                    Vector3d pos = position.getPos();
                    if (pos == null) {
                        iterator.remove();
                        continue;
                    }

                    double x = pos.x - viewPos.getX();
                    double y = pos.y - viewPos.getY();
                    double z = pos.z - viewPos.getZ();

                    GL11.glPushMatrix();
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glLineWidth(2);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);

                    // Получаем цвет из стиля и применяем альфу
                    int styleColor = getColorByName("primaryColor");
                    float alpha = 1 - (float) (System.currentTimeMillis() - position.getTime()) / maxTime;
                    alpha = MathHelper.clamp(alpha, 0.0f, 1.0f);
                    int color = ColorUtils.applyOpacity(styleColor, alpha);

                    AxisAlignedBB bb = new AxisAlignedBB(
                            x - 0.3f, y, z - 0.3f,
                            x + 0.3f, y + target.getHeight(), z + 0.3f
                    );

                    RenderUtils.Render3D.drawBox(bb, color);

                    GL11.glLineWidth(1);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glPopMatrix();

                    if ((System.currentTimeMillis() - position.getTime()) > maxTime) {
                        iterator.remove();
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                    iterator.remove();
                }
            }
        }

        return false;
    }

    @Override
    public void onDisable() {
        if (AttackAura.getTarget() != null && AttackAura.getTarget() instanceof PlayerEntity) {
            PlayerEntity target = (PlayerEntity) AttackAura.getTarget();
            List<Position> backtrack = target.getBacktrack();
            if (backtrack != null) {
                backtrack.clear();
            }
        }
    }

    @Override
    public void onEnable() {

    }

    @Getter
    @AllArgsConstructor
    public static class Position {
        private final Vector3d pos;
        private final long time;
    }
}
