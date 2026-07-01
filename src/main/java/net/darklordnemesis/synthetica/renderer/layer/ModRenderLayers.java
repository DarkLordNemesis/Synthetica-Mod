package net.darklordnemesis.synthetica.renderer.layer;

import net.darklordnemesis.synthetica.Synthetica;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class ModRenderLayers {

    // Our ModelLayerLocation.
    public static final ModelLayerLocation SHEATH_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "sheath_layer"),
            "main"
    );

    public static final ModelLayerLocation EMPTY_SHEATH_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, "empty_sheath_layer"),
            "main"
    );
}
