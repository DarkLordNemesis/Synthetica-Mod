package net.darklordnemesis.synthetica.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record KatanaSyncInfo(ResourceLocation positionId, boolean isDrawn) {
    // Defines how to send this specific object over the network
    public static final StreamCodec<ByteBuf, KatanaSyncInfo> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, KatanaSyncInfo::positionId,
            ByteBufCodecs.BOOL, KatanaSyncInfo::isDrawn,
            KatanaSyncInfo::new
    );
}