package net.darklordnemesis.synthetica.essentia;

import java.util.function.Predicate;

public class SingleEssentiaTank implements IEssentiaHandler, IEssentiaTank {

    protected Predicate<EssentiaStack> validator;
    protected EssentiaStack essentia = EssentiaStack.EMPTY;
    protected long capacity;

    public SingleEssentiaTank(long capacity) {
        this(capacity, e -> true);
    }

    public SingleEssentiaTank(long capacity, Predicate<EssentiaStack> validator) {
        this.capacity = capacity;
        this.validator = validator;
    }

    public SingleEssentiaTank setValidator(Predicate<EssentiaStack> validator) {
        if (validator != null) {
            this.validator = validator;
        }
        return this;
    }

    public SingleEssentiaTank setCapacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    public SingleEssentiaTank setEssentia(EssentiaStack essentia) {
        this.essentia = essentia.copy();
        return this;
    }

    public boolean isEmpty() {
        return essentia.isEmpty();
    }

    public long getSpace() {
        return Math.max(0, capacity - essentia.getAmount());
    }

    protected void onContentsChanged() {}


    // Implementation of IEssentiaHandler and IEssentiaTank

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public EssentiaStack getEssentiaInTank(int tank) {
        return this.getEssentia();
    }

    @Override
    public long getTankCapacity(int tank) {
        return this.getCapacity();
    }

    @Override
    public boolean isEssentiaValid(int tank, EssentiaStack stack) {
        return this.isEssentiaValid(stack);
    }

    @Override
    public EssentiaStack getEssentia() {
        return essentia.copy();
    }

    @Override
    public long getEssentiaAmount() {
        return essentia.getAmount();
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public boolean isEssentiaValid(EssentiaStack stack) {
        return validator.test(stack);
    }

    @Override
    public long fill(EssentiaStack resource, EssentiaAction action) {
        if (resource.isEmpty() || !isEssentiaValid(resource)) {
            return 0;
        }
        if (action.simulate()) {
            if (essentia.isEmpty()) {
                return Math.min(capacity, resource.getAmount());
            }
            if (!EssentiaStack.isSameAspect(essentia, resource)) {
                return 0;
            }
            return Math.min(capacity - essentia.getAmount(), resource.getAmount());
        }
        if (essentia.isEmpty()) {
            essentia = resource.copyWithAmount(Math.min(capacity, resource.getAmount()));
            onContentsChanged();
            return essentia.getAmount();
        }
        if (!EssentiaStack.isSameAspect(essentia, resource)) {
            return 0;
        }
        long filled = capacity - essentia.getAmount();

        if (resource.getAmount() < filled) {
            essentia.grow(resource.getAmount());
            filled = resource.getAmount();
        } else {
            essentia.setAmount(capacity);
        }
        if (filled > 0)
            onContentsChanged();
        return filled;
    }

    @Override
    public EssentiaStack drain(EssentiaStack resource, EssentiaAction action) {
        if (essentia.isEmpty() || !resource.isSameAspect(essentia)) {
            return EssentiaStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public EssentiaStack drain(long maxDrain, EssentiaAction action) {
        if (essentia.isEmpty()) {
            return EssentiaStack.EMPTY;
        }
        long drained = maxDrain;
        if (essentia.getAmount() < drained) {
            drained = essentia.getAmount();
        }
        EssentiaStack stack = essentia.copyWithAmount(drained);
        if (action.execute() && drained > 0) {
            essentia.shrink(drained);
            onContentsChanged();
            if (essentia.isEmpty()) {
                this.clear();
            }
        }
        return stack;
    }

    public void clear() {
        essentia = EssentiaStack.EMPTY;
        onContentsChanged();
    }
}
