package net.darklordnemesis.synthetica.item.custom;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class KatanaItem extends SwordItem {

    public KatanaItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ResourceLocation defaultPos = ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "hip_left");

        // Read the component (or the default if newly crafted)
        ResourceLocation currentPos = stack.getOrDefault(ModDataComponents.SHEATH_POSITION.get(), defaultPos);

        // Optional: Clean up the path string so "hip_left" looks like "Hip Left" in the tooltip
        String formattedName = formatPositionName(currentPos.getPath());

        // Add the colored text to the tooltip
        tooltipComponents.add(Component.literal("Sheath Position: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(formattedName).withStyle(ChatFormatting.GOLD)));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    // A quick helper to make the JSON filenames look nice in-game
    private String formatPositionName(String path) {
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return result.toString().trim();
    }
}