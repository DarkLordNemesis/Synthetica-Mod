package net.darklordnemesis.synthetica.essentia;

public interface IEssentiaHandler {
    public enum EssentiaAction {
        EXECUTE,
        SIMULATE;

        public boolean execute() {
            return this == EXECUTE;
        }

        public boolean simulate() {
            return this == SIMULATE;
        }
    }


    int getTanks();

    /**
     * Returns the EssentiaStack in a given tank.
     *
     * <p>
     * <strong>IMPORTANT:</strong> This EssentiaStack <em>MUST NOT</em> be modified. This method is not for
     * altering internal contents. Any implementers who are able to detect modification via this method
     * should throw an exception. It is ENTIRELY reasonable and likely that the stack returned here will be a copy.
     * </p>
     *
     * <p>
     * <strong><em>SERIOUSLY: DO NOT MODIFY THE RETURNED ESSENTIASTACK</em></strong>
     * </p>
     *
     * @param tank Tank to query.
     * @return EssentiaStack in a given tank. EssentiaStack.EMPTY if the tank is empty.
     */
    EssentiaStack getEssentiaInTank(int tank);

    /**
     * Retrieves the maximum Essentia amount for a given tank.
     *
     * @param tank Tank to query.
     * @return The maximum Essentia amount held by the tank.
     */
    long getTankCapacity(int tank);

    /**
     * This function is a way to determine which essentia can exist inside a given handler. General purpose tanks will
     * basically always return TRUE for this.
     *
     * @param tank  Tank to query for validity
     * @param stack Stack to test with for validity
     * @return TRUE if the tank can hold the EssentiaStack, not considering current state.
     *         (Basically, is a given essentia EVER allowed in this tank?) Return FALSE if the answer to that question is 'no.'
     */
    boolean isEssentiaValid(int tank, EssentiaStack stack);

    /**
     * Fills essentia into internal tanks, distribution is left entirely to the IEssentiaHandler.
     *
     * @param resource EssentiaStack representing the Aspect and maximum amount of essentia to be filled.
     * @param action   If SIMULATE, fill will only be simulated.
     * @return Amount of resource that was (or would have been, if simulated) filled.
     */
    long fill(EssentiaStack resource, EssentiaAction action);

    /**
     * Drains essentia out of internal tanks, distribution is left entirely to the IEssentiaHandler.
     *
     * @param resource EssentiaStack representing the Aspect and maximum amount of essentia to be drained.
     * @param action   If SIMULATE, drain will only be simulated.
     * @return EssentiaStack representing the Essentia and amount that was (or would have been, if
     *         simulated) drained.
     */
    EssentiaStack drain(EssentiaStack resource, EssentiaAction action);

    /**
     * Drains essentia out of internal tanks, distribution is left entirely to the IEssentiaHandler.
     * <p>
     * This method is not Aspect-sensitive.
     *
     * @param maxDrain Maximum amount of essentia to drain.
     * @param action   If SIMULATE, drain will only be simulated.
     * @return EssentiaStack representing the Essentia and amount that was (or would have been, if
     *         simulated) drained.
     */
    EssentiaStack drain(long maxDrain, EssentiaAction action);


    // Default methods

    /**
     * Returns true if the tank is empty.
     * @param tank the tank index
     * @return {@code true} if the tank is empty, {@code false} otherwise
     */
    default boolean isEmpty(int tank) {
        return getEssentiaInTank(tank).isEmpty();
    }

    /**
     * Returns true if all tanks are empty.
     * @return {@code true} if all tanks are empty, {@code false} otherwise
     */
    default boolean isEmpty() {
        for (int tank = 0; tank < getTanks(); tank++) {
            if (!isEmpty(tank)) return false;
        }
        return true;
    }

    /**
     * Returns true if the tank is full.
     * @param tank the tank index
     * @return {@code true} if the tank is full, {@code false} otherwise
     */
    default boolean isFull(int tank) {
        return getEssentiaInTank(tank).getAmount() >= getTankCapacity(tank);
    }

    /**
     * Returns true if all tanks are full.
     * @return {@code true} if all tanks are full, {@code false} otherwise
     */
    default boolean isFull() {
        for (int tank = 0; tank < getTanks(); tank++) {
            if (!isFull(tank)) return false;
        }
        return true;
    }

    /**
     * Returns the total capacity of all tanks.
     * @return the total capacity of all tanks
     */
    default long getTotalCapacity() {
        long capacity = 0;

        for (int tank = 0; tank < getTanks(); tank++) {
            capacity += getTankCapacity(tank);
        }

        return capacity;
    }

    /**
     * Returns the total amount of essentia in all tanks.
     * @return the total amount of essentia
     */
    default long getTotalEssentiaAmount() {
        long amount = 0;

        for (int tank = 0; tank < getTanks(); tank++) {
            amount += getEssentiaInTank(tank).getAmount();
        }

        return amount;
    }

}
