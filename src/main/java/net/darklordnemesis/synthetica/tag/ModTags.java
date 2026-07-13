package net.darklordnemesis.synthetica.tag;

import net.darklordnemesis.synthetica.Synthetica;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static final TagKey<Item> HAS_SHEATH = createItemTag("has_sheath");


    private static TagKey<Item> createItemTag(String key) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Synthetica.MOD_ID, key));
    }
}
