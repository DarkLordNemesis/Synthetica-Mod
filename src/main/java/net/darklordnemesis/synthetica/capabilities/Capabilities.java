package net.darklordnemesis.synthetica.capabilities;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.essentia.IEssentiaHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

public class Capabilities {
    public static final class EssentiaHandler {
        public static final BlockCapability<IEssentiaHandler, @Nullable Direction> BLOCK = BlockCapability.createSided(create("essentia_handler"), IEssentiaHandler.class);
        public static final EntityCapability<IEssentiaHandler, @Nullable Direction> ENTITY = EntityCapability.createSided(create("essentia_handler"), IEssentiaHandler.class);
        public static final ItemCapability<IEssentiaHandler, Void> ITEM = ItemCapability.createVoid(create("essentia_handler"), IEssentiaHandler.class);

        private EssentiaHandler() {}
    }


    private static ResourceLocation create(String path) {
        return ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, path);
    }

    private Capabilities() {}
}
