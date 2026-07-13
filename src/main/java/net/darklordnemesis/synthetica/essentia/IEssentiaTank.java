package net.darklordnemesis.synthetica.essentia;

public interface IEssentiaTank {

    EssentiaStack getEssentia();

    /**
     * @return Current amount of essentia in the tank.
     */
    long getEssentiaAmount();

    /**
     * @return Capacity of this essentia tank.
     */
    long getCapacity();

    /**
     * @param stack EssentiaStack holding the Aspect to be queried.
     * @return If the tank can hold the essentia (EVER, not at the time of query).
     */
    boolean isEssentiaValid(EssentiaStack stack);

    /**
     * @param resource EssentiaStack attempting to fill the tank.
     * @param action   If SIMULATE, the fill will only be simulated.
     * @return Amount of essentia that was accepted (or would be, if simulated) by the tank.
     */
    long fill(EssentiaStack resource, IEssentiaHandler.EssentiaAction action);

    /**
     * @param maxDrain Maximum amount of essentia to be removed from the container.
     * @param action   If SIMULATE, the drain will only be simulated.
     * @return Amount of essentia that was removed (or would be, if simulated) from the tank.
     */
    EssentiaStack drain(long maxDrain, IEssentiaHandler.EssentiaAction action);

    /**
     * @param resource Maximum amount of essentia to be removed from the container.
     * @param action   If SIMULATE, the drain will only be simulated.
     * @return EssentiaStack representing essentia that was removed (or would be, if simulated) from the tank.
     */
    EssentiaStack drain(EssentiaStack resource, IEssentiaHandler.EssentiaAction action);
}
