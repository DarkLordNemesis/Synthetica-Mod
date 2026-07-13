package net.darklordnemesis.synthetica.essentia.value;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.darklordnemesis.synthetica.essentia.Aspect;
import net.darklordnemesis.synthetica.essentia.EssentiaStack;
import net.minecraft.core.Holder;

import java.util.List;

/**
 * EssentiaValue
 *
 * Holds the list of EssentiaStacks that an item is worth when processed.
 * For example, a piece of coal might yield 4 Ignis and 2 Terra.
 *
 * This is intentionally a thin data wrapper — all the lookup logic lives
 * in EssentiaValueMap. Keeping them separate means EssentiaValue can be
 * used anywhere (recipes, tooltips, GUI) without pulling in the map.
 *
 * KEY CONCEPTS DEMONSTRATED:
 *  - Codec.list() — wraps an existing Codec<T> to produce Codec<List<T>>.
 *    Any codec can be promoted to a list codec this way.
 *  - RecordCodecBuilder for a single-field record — still worth using over
 *    a raw xmap so the JSON key name ("values") is explicit and documented.
 */
public record EssentiaValue(List<EssentiaStack> stacks) {

    /**
     * CODEC — serializes to/from JSON.
     *
     * JSON format:
     * {
     *   "values": [
     *     { "aspect": "synthetica:ignis", "amount": 4 },
     *     { "aspect": "synthetica:terra", "amount": 2 }
     *   ]
     * }
     *
     * EssentiaStack.CODEC handles each individual stack entry.
     * Codec.list() wraps it to handle the array.
     */
    public static final Codec<EssentiaValue> CODEC =
            EssentiaStack.CODEC.listOf()
                    .xmap(EssentiaValue::new, EssentiaValue::stacks);


    /** Convenience: total amount of a specific aspect across all stacks. */
    public long getAmountFor(Holder<Aspect> aspect) {
        return stacks.stream()
                .filter(s -> s.is(aspect))
                .mapToLong(EssentiaStack::getAmount)
                .sum();
    }

    /** True if this value contains at least one non-empty stack. */
    public boolean isEmpty() {
        return stacks.isEmpty() || stacks.stream().allMatch(EssentiaStack::isEmpty);
    }

    public static final EssentiaValue EMPTY = new EssentiaValue(List.of());
}