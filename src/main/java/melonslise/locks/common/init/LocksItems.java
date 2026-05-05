package melonslise.locks.common.init;

import melonslise.locks.Locks;
import melonslise.locks.common.item.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class LocksItems {

    public static List<Item> items = new ArrayList<>();

    public static final Item
            INTEGRATED_CIRCUIT = add("integrated_circuit", () -> new Item(new Item.Properties())),
            SPRING = add("spring", () -> new Item(new Item.Properties())),
            WOOD_LOCK_MECHANISM = add("wood_lock_mechanism", () -> new Item(new Item.Properties())),
            IRON_LOCK_MECHANISM = add("iron_lock_mechanism", () -> new Item(new Item.Properties())),
            STEEL_LOCK_MECHANISM = add("steel_lock_mechanism", () -> new Item(new Item.Properties())),
            KEY_BLANK = add("key_blank", () -> new Item(new Item.Properties())),
            DIAMOND_LOCK_FRAGMENTS = add("diamond_lock_fragments", () -> new Item(new Item.Properties())),
            WOOD_LOCK = add("wood_lock", () -> new LockItem(3, 14, 2, new Item.Properties())),
            COPPER_LOCK = add("copper_lock", () -> new LockItem(5, 15, 4, new Item.Properties())),
            IRON_LOCK = add("iron_lock", () -> new LockItem(7, 14, 12, new Item.Properties())),
            STEEL_LOCK = add("steel_lock", () -> new LockItem(9, 12, 20, new Item.Properties())),
            GOLD_LOCK = add("gold_lock", () -> new LockItem(6, 22, 6, new Item.Properties())),
            DIAMOND_LOCK = add("diamond_lock", () -> new LockItem(11, 12, 100, new Item.Properties())),
            NETHERITE_LOCK = add("netherite_lock", () -> new LockItem(15, 14, 100, new Item.Properties().fireResistant())),
            NETHERITE_SMART_LOCK = add("smart_netherite_lock", () -> new SmartLockItem(12, new Item.Properties().fireResistant())),
            KEY = add("key", () -> new KeyItem(new Item.Properties())),
            MASTER_KEY = add("master_key", () -> new MasterKeyItem(new Item.Properties())),
            KEY_RING = add("key_ring", () -> new KeyRingItem(2, new Item.Properties())),
            WOOD_LOCK_PICK = add("wood_lock_pick", () -> new LockPickItem(0.05f, new Item.Properties())),
            COPPER_LOCK_PICK = add("copper_lock_pick", () -> new LockPickItem(0.25f, new Item.Properties())),
            IRON_LOCK_PICK = add("iron_lock_pick", () -> new LockPickItem(0.5f, new Item.Properties())),
            STEEL_LOCK_PICK = add("steel_lock_pick", () -> new LockPickItem(0.75f, new Item.Properties())),
            GOLD_LOCK_PICK = add("gold_lock_pick", () -> new LockPickItem(0.37f, new Item.Properties())),
            DIAMOND_LOCK_PICK = add("diamond_lock_pick", () -> new LockPickItem(0.9f, new Item.Properties())),
            NETHERITE_LOCK_PICK = add("netherite_lock_pick", () -> new LockPickItem(0.97f, new Item.Properties().fireResistant()));

    public static void register()
    {
    }

    public static Item add(String resourceLocation, Supplier<Item> itemSupplier) {
        Item register = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(Locks.ID, resourceLocation), itemSupplier.get());
        items.add(register);
        return register;
    }

    public static final CreativeModeTab TABS = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Locks.ID, CreativeModeTab
            .builder(CreativeModeTab.Row.TOP, 9)
            .icon(() -> new ItemStack(LocksItems.IRON_LOCK))
            .title(Component.translatable("itemGroup.locks"))
            .displayItems((parameters, output) -> {
//                for (Enchantment enchantmentRegistryObject : LocksEnchantments.ENCHANTMENTS.getEntries()) {
//                    Enchantment enchantment = enchantmentRegistryObject;
//                    ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
//                    enchantedBook.enchant(enchantment, enchantment.getMaxLevel());
//                    output.accept(enchantedBook);
//                }
                for (Item itemRegistryObject : items) {
                    output.accept(itemRegistryObject);
                }
            })
            .build());
}