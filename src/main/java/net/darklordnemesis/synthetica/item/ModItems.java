package net.darklordnemesis.synthetica.item;

import net.darklordnemesis.synthetica.Synthetica;
import net.darklordnemesis.synthetica.item.armor.AbstractArmorItem;
import net.darklordnemesis.synthetica.item.armor.FrozenBlazeArmorItem;
import net.darklordnemesis.synthetica.item.custom.ChiselItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Synthetica.MOD_ID);

    public static final DeferredItem<Item> BISMUTH = ITEMS.register("bismuth", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_BISMUTH = ITEMS.register("raw_bismuth", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHISEL = ITEMS.register("chisel", () -> new ChiselItem(new Item.Properties().durability(100)));

    public static final DeferredItem<Item> KATANA = ITEMS.register("katana", () -> new SwordItem(Tiers.IRON, new Item.Properties().durability(250).attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -1.2F))));

    public static final Map<ArmorItem.Type, DeferredItem<FrozenBlazeArmorItem>> FROZEN_BLAZE_ARMOR = AbstractArmorItem.createRegistry(ITEMS, "frozen_blaze", FrozenBlazeArmorItem::new);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
