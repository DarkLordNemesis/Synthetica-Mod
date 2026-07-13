package net.darklordnemesis.synthetica.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.function.Function;

public class Codecs {
    public static final Codec<Long> POSITIVE_LONG = longRangeWithMessage(1, Long.MAX_VALUE, aLong -> "Value must be positive: " + aLong);


    private static Codec<Long> longRangeWithMessage(long min, long max, Function<Long, String> errorMessage) {
        return Codec.LONG
                .validate(
                        aLong -> aLong.compareTo(min) >= 0 && aLong.compareTo(max) <= 0
                                ? DataResult.success(aLong)
                                : DataResult.error(() -> errorMessage.apply(aLong))
                );
    }

}
