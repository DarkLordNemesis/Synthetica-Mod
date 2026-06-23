package net.darklordnemesis.synthetica.datagen;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.item.ModItems;
import net.darklordnemesis.synthetica.item.armor.AbstractArmorItem;
import net.darklordnemesis.synthetica.item.armor.FrozenBlazeArmorItem;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Synthetica.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.BISMUTH.get());
        basicItem(ModItems.RAW_BISMUTH.get());

        basicItem(ModItems.CHISEL.get());

        armorSet(ModItems.FROZEN_BLAZE_ARMOR);

    }


    private <T extends AbstractArmorItem> void armorSet (Map<ArmorItem.Type, DeferredItem<T>> armorSet) {
        for (DeferredItem<T> item : armorSet.values()) {
            basicItem(item.get());
        }
    }

}
