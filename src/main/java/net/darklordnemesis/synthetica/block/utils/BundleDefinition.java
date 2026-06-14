package net.darklordnemesis.synthetica.block.utils;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public class BundleDefinition {
    private final List<ItemStack> stacks;

    public BundleDefinition(List<ItemStack> stacks) {
        // Defensive copy, never store the live list
        this.stacks = stacks.stream()
                .map(ItemStack::copy)
                .toList();
    }

    public List<ItemStack> getStacks() { return stacks; }

    /** Convenience factory: BundleDefinition.of(new ItemStack(Items.REDSTONE, 1), new ItemStack(Items.IRON_INGOT, 4)) */
    public static BundleDefinition of(ItemStack... stacks) {
        return new BundleDefinition(List.of(stacks));
    }
}