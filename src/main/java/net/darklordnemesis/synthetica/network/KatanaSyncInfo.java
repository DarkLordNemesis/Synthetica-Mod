package net.darklordnemesis.synthetica.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

// We added itemId here!
public record KatanaSyncInfo(ResourceLocation itemId, ResourceLocation positionId, boolean isDrawn) {
    public static final StreamCodec<ByteBuf, KatanaSyncInfo> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, KatanaSyncInfo::itemId,
            ResourceLocation.STREAM_CODEC, KatanaSyncInfo::positionId,
            ByteBufCodecs.BOOL, KatanaSyncInfo::isDrawn,
            KatanaSyncInfo::new
    );
}