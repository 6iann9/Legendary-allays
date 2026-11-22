package net.iann.legendaryallays.registry;

import net.iann.legendaryallays.LegendaryAllays;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, LegendaryAllays.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BIND = register("lute.bind_allay");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNBIND = register("lute.unbind_allay");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLLECT_DROPPED = register("lute.collect_dropped");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLLECT_NEARBY = register("lute.collect_nearby");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLLECT_EVERYWHERE = register("lute.collect_everywhere");
    public static final DeferredHolder<SoundEvent, SoundEvent> IDLE = register("lute.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> FILTER_ADD = register("lute.filter_add");
    public static final DeferredHolder<SoundEvent, SoundEvent> FILTER_CLEAR = register("lute.filter_clear");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(LegendaryAllays.MOD_ID, name)));
    }
}
