package net.darklordnemesis.synthetica.item;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Synthetica.MOD_ID);

    public static final Supplier<CreativeModeTab> SYNTHETICA_TAB = CREATIVE_MODE_TAB.register("synthetica_tab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.BISMUTH.get()))
            .title(Component.translatable("creativetab.synthetica.synthetica_tab"))
            .displayItems((itemDisplayParameter, output) -> {
                output.accept(ModItems.BISMUTH.get());
                output.accept(ModItems.RAW_BISMUTH.get());
                output.accept(ModBlocks.BISMUTH_BLOCK.get());
                output.accept(ModBlocks.BISMUTH_ORE.get());
                output.accept(ModItems.CHISEL.get());

                output.accept(ModBlocks.PEDESTAL.get());
            }).build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TAB.register(modEventBus);
    }
}
