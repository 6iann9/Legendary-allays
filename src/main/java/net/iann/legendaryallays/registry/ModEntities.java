package net.iann.legendaryallays.registry;

import net.iann.legendaryallays.LegendaryAllays;
import net.iann.legendaryallays.entity.BoundAllay;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.allay.Allay;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, LegendaryAllays.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<BoundAllay>> BOUND_ALLAY = ENTITIES.register("bound_allay",
            () -> EntityType.Builder.of(BoundAllay::new, MobCategory.MISC).sized(0.35F, 0.6F)
                    .clientTrackingRange(10).updateInterval(2).noSave().noSummon()
                    .build("legendaryallays:bound_allay"));

    private ModEntities() {}
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(BOUND_ALLAY.get(), Allay.createAttributes().build());
    }
}
