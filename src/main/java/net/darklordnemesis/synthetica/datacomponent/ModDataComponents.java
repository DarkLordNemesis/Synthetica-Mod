package net.darklordnemesis.synthetica.datacomponent;

import net.darklordnemesis.synthetica.Synthetica;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Synthetica.MOD_ID);

    // This component stores a ResourceLocation (e.g., "synthetica:back_center")
    // persistent() saves it to the disk, networkSynchronized() automatically sends it to the client!
    public static final Supplier<DataComponentType<ResourceLocation>> SHEATH_POSITION = DATA_COMPONENTS.registerComponentType("sheath_position",
            builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC)
    );

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}