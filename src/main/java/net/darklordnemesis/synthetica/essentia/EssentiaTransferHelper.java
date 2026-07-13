package net.darklordnemesis.synthetica.essentia;

import net.darklordnemesis.synthetica.essentia.IEssentiaHandler.EssentiaAction;
import net.minecraft.core.Holder;

/**
 * EssentiaTransfer
 *
 * A stateless utility class for moving essentia between two IEssentiaHandler
 * instances. All methods follow the same pattern:
 *
 *  1. SIMULATE a drain from the source to find out what is actually available
 *  2. SIMULATE a fill into the destination to find out what actually fits
 *  3. Take the minimum of the two — the real transferable amount
 *  4. EXECUTE the drain and fill for that amount
 *  5. Return how much was actually moved
 *
 * Simulating before executing is critical — never assume the full requested
 * amount will be accepted. A handler may be nearly full, aspect-locked to a
 * different type, or have a custom validator that rejects certain stacks.
 *
 * All methods return the amount actually transferred so callers can react
 * (e.g. play a sound, update a GUI, trigger a game event).
 */
public final class EssentiaTransferHelper {

    // Static utility class — no instances needed
    private EssentiaTransferHelper() {}

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Transfer any aspect from source to destination, up to maxAmount.
     *
     * Drains whatever the source currently holds without caring about aspect.
     * Useful for pipes or funnels that move essentia generically.
     *
     * @param source      the handler to drain from
     * @param destination the handler to fill into
     * @param maxAmount   upper bound on how much to move in one call
     * @return the amount actually transferred
     */
    public static long transferAny(IEssentiaHandler source, IEssentiaHandler destination,
                                   long maxAmount) {
        if (maxAmount <= 0) return 0;

        // Simulate draining up to maxAmount from any tank in the source
        EssentiaStack drained = source.drain(maxAmount, EssentiaAction.SIMULATE);
        if (drained.isEmpty()) return 0;

        return executeTransfer(source, destination, drained);
    }

    /**
     * Transfer a specific aspect from source to destination, up to maxAmount.
     *
     * If the source does not hold the requested aspect, nothing is moved.
     * Useful for machines that require a particular aspect to operate.
     *
     * @param source      the handler to drain from
     * @param destination the handler to fill into
     * @param aspect      the specific aspect to transfer
     * @param maxAmount   upper bound on how much to move in one call
     * @return the amount actually transferred
     */
    public static long transferAspect(IEssentiaHandler source, IEssentiaHandler destination,
                                      Holder<Aspect> aspect, long maxAmount) {
        if (maxAmount <= 0 || aspect == null) return 0;

        // Build a probe stack for the requested aspect at the max amount
        EssentiaStack probe = new EssentiaStack(aspect, maxAmount);

        // Simulate draining — returns EMPTY if the source doesn't hold this aspect
        EssentiaStack drained = source.drain(probe, EssentiaAction.SIMULATE);
        if (drained.isEmpty()) return 0;

        return executeTransfer(source, destination, drained);
    }

    /**
     * Transfer essentia defined by an EssentiaStack (aspect + max amount).
     *
     * The stack's aspect acts as the filter and its amount as the upper bound.
     * This is the most precise overload — mirrors how ItemStack transfers work
     * in vanilla where one stack describes both what and how much.
     *
     * @param source      the handler to drain from
     * @param destination the handler to fill into
     * @param resource    the aspect to transfer and the maximum amount
     * @return the amount actually transferred
     */
    public static long transfer(IEssentiaHandler source, IEssentiaHandler destination,
                                EssentiaStack resource) {
        if (resource == null || resource.isEmpty()) return 0;
        return transferAspect(source, destination, resource.getAspect(), resource.getAmount());
    }

    // =========================================================================
    // Simulate-only variants
    // Returns how much WOULD be transferred without changing any state.
    // Useful for pipes deciding whether to bother connecting, or GUIs
    // showing a preview of a pending transfer.
    // =========================================================================

    /** How much of any aspect could be moved from source to destination. */
    public static long simulateAny(IEssentiaHandler source, IEssentiaHandler destination,
                                   long maxAmount) {
        if (maxAmount <= 0) return 0;
        EssentiaStack drained = source.drain(maxAmount, EssentiaAction.SIMULATE);
        if (drained.isEmpty()) return 0;
        return simulateAmount(destination, drained);
    }

    /** How much of a specific aspect could be moved from source to destination. */
    public static long simulateAspect(IEssentiaHandler source, IEssentiaHandler destination,
                                      Holder<Aspect> aspect, long maxAmount) {
        if (maxAmount <= 0 || aspect == null) return 0;
        EssentiaStack drained = source.drain(new EssentiaStack(aspect, maxAmount),
                EssentiaAction.SIMULATE);
        if (drained.isEmpty()) return 0;
        return simulateAmount(destination, drained);
    }

    /** How much of the resource stack could be moved from source to destination. */
    public static long simulate(IEssentiaHandler source, IEssentiaHandler destination,
                                EssentiaStack resource) {
        if (resource == null || resource.isEmpty()) return 0;
        return simulateAspect(source, destination, resource.getAspect(), resource.getAmount());
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Core transfer execution.
     *
     * Given a simulated drain result (we already know the source CAN provide
     * this stack), simulate the fill into the destination to find how much
     * actually fits, then execute both operations for the minimum of the two.
     *
     * @param source      the handler to drain from
     * @param destination the handler to fill into
     * @param available   the result of a prior SIMULATE drain — what is available
     * @return the amount actually transferred
     */
    private static long executeTransfer(IEssentiaHandler source, IEssentiaHandler destination,
                                        EssentiaStack available) {
        // Simulate fill — find out how much of the available stack the destination accepts
        long accepted = destination.fill(available, EssentiaAction.SIMULATE);
        if (accepted <= 0) return 0;

        // The real transferable amount is the minimum of what's available and what fits
        long toTransfer = Math.min(available.getAmount(), accepted);
        if (toTransfer <= 0) return 0;

        // EXECUTE the drain for exactly toTransfer units
        EssentiaStack actuallyDrained = source.drain(
                available.copyWithAmount(toTransfer), EssentiaAction.EXECUTE);

        if (actuallyDrained.isEmpty()) return 0;

        // EXECUTE the fill — use the amount actually drained (may be less than toTransfer
        // if the source had a race condition or unusual validator)
        long actuallyFilled = destination.fill(actuallyDrained, EssentiaAction.EXECUTE);

        // If the fill accepted less than we drained (shouldn't happen but be safe),
        // return the excess to the source. In practice this path should never fire
        // if both simulate calls were accurate, but defensive coding matters here.
        if (actuallyFilled < actuallyDrained.getAmount()) {
            long leftover = actuallyDrained.getAmount() - actuallyFilled;
            source.fill(actuallyDrained.copyWithAmount(leftover), EssentiaAction.EXECUTE);
        }

        return actuallyFilled;
    }

    /**
     * Simulates how much of the given stack the destination can accept.
     * Used by all simulate-only public methods.
     */
    private static long simulateAmount(IEssentiaHandler destination, EssentiaStack stack) {
        return destination.fill(stack, EssentiaAction.SIMULATE);
    }
}