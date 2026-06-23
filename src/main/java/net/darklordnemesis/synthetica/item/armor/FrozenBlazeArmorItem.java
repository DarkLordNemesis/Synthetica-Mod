package net.darklordnemesis.synthetica.item.armor;

import net.darklordnemesis.synthetica.Synthetica;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

public class FrozenBlazeArmorItem extends AbstractArmorItem {
    private static final ResourceLocation TEXTURE_LOCATION = makeCustomTextureLocation(Synthetica.MOD_ID, "frozen_blaze");

    public FrozenBlazeArmorItem(Type pType) {
        super(ArmorMaterials.DIAMOND, pType, new Properties().rarity(Rarity.RARE).durability(1000));
    }


    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEXTURE_LOCATION;
    }
}
