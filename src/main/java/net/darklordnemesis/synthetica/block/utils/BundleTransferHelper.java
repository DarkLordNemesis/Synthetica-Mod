package net.darklordnemesis.synthetica.block.utils;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class BundleTransferHelper {

    /**
     * Transfers a bundle of items from a source to a destination.
     * Returns the number of sets transferred.
     *
     * @param source the source handler
     * @param destination the destination handler
     * @param bundle the bundle definition
     * @param maxSets the amount of sets it tries to transfer at once
     * @return the number of sets transferred
     */
    public static int tryTransferBundle(
            IItemHandler source,
            IItemHandler destination,
            BundleDefinition bundle,
            int maxSets          // how many sets it tries to transfer at once
    ) {
        if (maxSets <= 0) return 0;

        // ── Step 1: How many sets can the source provide? ─────────────────────
        int availableSets = countExtractableSets(source, bundle, maxSets);
        if (availableSets <= 0) return 0;

        // ── Step 2: How many sets can the destination accept? ─────────────────
        int fittingSets = countInsertableSets(destination, bundle, availableSets);
        if (fittingSets <= 0) return 0;

        // ── Step 3: Execute extraction ────────────────────────────────────────
        List<ItemStack> extracted = new ArrayList<>();
        for (ItemStack required : bundle.getStacks()) {
            ItemStack scaled = required.copyWithCount(required.getCount() * fittingSets);
            ItemStack got = extractExact(source, scaled);
            if (got.isEmpty()) {
                // Something changed since simulate — roll back into destination
                // (best effort; source rollback would need a temp buffer)
                extracted.forEach(s -> insertFull(destination, s));
                return 0;
            }
            extracted.add(got);
        }

        // ── Step 4: Execute insertion ─────────────────────────────────────────
        extracted.forEach(s -> insertFull(destination, s));
        return fittingSets;
    }

    // ── Set counting ──────────────────────────────────────────────────────────

    /**
     * Returns how many complete sets the source can provide, up to maxSets.
     * For each bundle item, counts extractable amount via simulate and divides.
     * The bottleneck item (lowest ratio) determines the result.
     */
    private static int countExtractableSets(IItemHandler source, BundleDefinition bundle, int maxSets) {
        int sets = maxSets;
        for (ItemStack required : bundle.getStacks()) {
            int available = countExtractable(source, required);
            // How many complete sets worth of this item are available?
            int possibleSets = available / required.getCount();
            sets = Math.min(sets, possibleSets);
            if (sets <= 0) return 0; // Early exit
        }
        return sets;
    }

    /**
     * Counts how many of a specific item can be extracted via simulation.
     */
    private static int countExtractable(IItemHandler handler, ItemStack required) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (!ItemStack.isSameItemSameComponents(inSlot, required)) continue;
            total += handler.extractItem(slot, Integer.MAX_VALUE, true).getCount();
        }
        return total;
    }


    /**
     * Counts how many sets the destination can accept
     *
     * @param destination the destination handler
     * @param bundle the set of items to transfer
     * @param maxSets the maximum amount of sets that are allowed to be transferred
     * @return the amount of sets the target handler can actually accept
     */
    private static int countInsertableSets(IItemHandler destination, BundleDefinition bundle, int maxSets) {
        if (!canInsertScaledBundle(destination, bundle, 1)) return 0;

        int lo = 0, hi = maxSets;
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (canInsertScaledBundle(destination, bundle, mid)) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    /**
     * Simulates inserting `sets` copies of the full bundle into a snapshot.
     * All items are inserted into the SAME snapshot, so contention is real.
     */
    private static boolean canInsertScaledBundle(IItemHandler destination, BundleDefinition bundle, int sets) {
        SimulatedItemHandler sim = new SimulatedItemHandler(destination);
        for (ItemStack required : bundle.getStacks()) {
            ItemStack scaled = required.copyWithCount(required.getCount() * sets);
            ItemStack remainder = sim.insert(scaled);
            if (!remainder.isEmpty()) return false;
        }
        return true;
    }

    // ── Execute helpers ───────────────────────────────────────────────────────

    private static ItemStack extractExact(IItemHandler handler, ItemStack required) {
        int remaining = required.getCount();
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (!ItemStack.isSameItemSameComponents(inSlot, required)) continue;
            remaining -= handler.extractItem(slot, remaining, false).getCount();
        }
        return remaining <= 0 ? required.copy() : ItemStack.EMPTY;
    }

    private static void insertFull(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        // merge existing slots first before trying to fill empty slots
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            if (handler.getStackInSlot(slot).is(remaining.getItem())) {
                remaining = handler.insertItem(slot, remaining, false);
            }
        }
        // if items remain that cant be merged, fill empty slots
        if (!remaining.isEmpty()) {
            for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
                remaining = handler.insertItem(slot, remaining, false);
            }
        }
    }
}