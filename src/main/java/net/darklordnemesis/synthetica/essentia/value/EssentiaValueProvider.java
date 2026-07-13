package net.darklordnemesis.synthetica.essentia.value;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * EssentiaValueProvider
 *
 * Static facade over EssentiaValueMap for clean call sites.
 * Call these from your alchemical furnace, tooltip handler,
 * JEI/REI plugin, or anywhere else that needs essentia values.
 *
 * Usage:
 *   EssentiaValue value = EssentiaValueProvider.get(stack);
 *   if (!value.isEmpty()) { ... }
 */
public final class EssentiaValueProvider {

    private EssentiaValueProvider() {}

    /** Returns the EssentiaValue for the given ItemStack, or EMPTY if none. */
    public static EssentiaValue get(ItemStack stack) {
        return EssentiaValueMap.INSTANCE.getFor(stack);
    }

    /** Returns the EssentiaValue for the given Item, or EMPTY if none. */
    public static EssentiaValue get(Item item) {
        return EssentiaValueMap.INSTANCE.getFor(item);
    }

    /** Returns true if the item has any registered essentia value. */
    public static boolean hasValue(ItemStack stack) {
        return EssentiaValueMap.INSTANCE.hasValue(stack);
    }

    public static boolean hasValue(Item item) {
        return EssentiaValueMap.INSTANCE.hasValue(item);
    }
}