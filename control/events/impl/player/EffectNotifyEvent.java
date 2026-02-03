package nuclear.control.events.impl.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import nuclear.control.events.Event;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class EffectNotifyEvent extends Event {
    private final LivingEntity entity;
    private final ItemStack itemStack;
    private final double distanceFactor;
    private final List<EffectInstance> effects;
}
