package net.darklordnemesis.synthetica.block.utils;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * An in-memory mutable snapshot of an IItemHandler's slot contents.
 * Used to simulate a sequence of insertions without touching the real handler.
 */
public class SimulatedItemHandler {

    private final ItemStack[] slots;
    private final int[] limits;

    public SimulatedItemHandler(IItemHandler real) {
        int size = real.getSlots();
        slots  = new ItemStack[size];
        limits = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i]  = real.getStackInSlot(i).copy();
            limits[i] = real.getSlotLimit(i);
        }
    }

    /**
     * Attempts to insert stack into this snapshot.
     * Returns the remainder that couldn't fit, empty means full success.
     * Mutates this snapshot so subsequent inserts see previous ones' effects.
     */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = stack.copy();

        // Pass 1: fill existing stacks of the same type
        for (int i = 0; i < slots.length && !remaining.isEmpty(); i++) {
            if (slots[i].isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(slots[i], remaining)) continue;

            int limit     = Math.min(limits[i], slots[i].getMaxStackSize());
            int space     = limit - slots[i].getCount();
            int toInsert  = Math.min(space, remaining.getCount());
            if (toInsert <= 0) continue;

            slots[i].grow(toInsert);
            remaining.shrink(toInsert);
        }

        // Pass 2: fill empty slots
        for (int i = 0; i < slots.length && !remaining.isEmpty(); i++) {
            if (!slots[i].isEmpty()) continue;

            int limit    = Math.min(limits[i], remaining.getMaxStackSize());
            int toInsert = Math.min(limit, remaining.getCount());
            if (toInsert <= 0) continue;

            slots[i] = remaining.copy();
            slots[i].setCount(toInsert);
            remaining.shrink(toInsert);
        }

        return remaining;
    }
}