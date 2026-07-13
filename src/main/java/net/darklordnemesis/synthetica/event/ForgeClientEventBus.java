package net.darklordnemesis.synthetica.event;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.essentia.EssentiaStack;
import net.darklordnemesis.synthetica.essentia.value.EssentiaValue;
import net.darklordnemesis.synthetica.essentia.value.EssentiaValueProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Synthetica.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEventBus {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        EssentiaValue value = EssentiaValueProvider.get(event.getItemStack());
        if (value.isEmpty()) return;

        event.getToolTip().add(Component.translatable("tooltip.synthetica.essentia_value")
                .withStyle(ChatFormatting.DARK_PURPLE));

        for (EssentiaStack stack : value.stacks()) {
            if (stack.isEmpty() || stack.getAspect() == null) continue;
            event.getToolTip().add(
                    Component.literal("  ")
                            .append(stack.getAspect().value().getDisplayName(stack.getAspect()))
                            .append(Component.literal(": " + stack.getAmount()))
                            .withStyle(ChatFormatting.GRAY)
            );
        }

    }
}
