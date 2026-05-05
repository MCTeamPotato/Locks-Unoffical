package melonslise.locks.common.init;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocksVillagerTrades {

    public static void addVillagerTrades()
    {
        Int2ObjectMap<VillagerTrades.ItemListing[]> levels = VillagerTrades.TRADES.get(VillagerProfession.TOOLSMITH);
        List<VillagerTrades.ItemListing> trades;
        trades = new ArrayList<>(List.of(levels.get(1)));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.COPPER_LOCK_PICK), 1, 2, 16, 2, 0.05f));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.WOOD_LOCK_PICK), 1, 2, 16, 2, 0.05f));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.WOOD_LOCK_MECHANISM), 2, 1, 12, 1, 0.2f));
        levels.replace(1, trades.toArray(new VillagerTrades.ItemListing[0]));
        trades = new ArrayList<>(List.of(levels.get(2)));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.IRON_LOCK_PICK), 2, 2, 16, 5, 0.05f));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.IRON_LOCK_MECHANISM), 3, 1, 12, 5, 0.2f));
        levels.replace(2, trades.toArray(new VillagerTrades.ItemListing[0]));
        trades = new ArrayList<>(List.of(levels.get(3)));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.GOLD_LOCK_PICK), 6, 2, 12, 20, 0.05f));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.IRON_LOCK_MECHANISM), 5, 1, 8, 10, 0.2f));
        levels.replace(3, trades.toArray(new VillagerTrades.ItemListing[0]));
        trades = new ArrayList<>(List.of(levels.get(4)));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.STEEL_LOCK_PICK), 4, 2, 16, 20, 0.05f));
        levels.replace(4, trades.toArray(new VillagerTrades.ItemListing[0]));
        trades = new ArrayList<>(List.of(levels.get(5)));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.DIAMOND_LOCK_PICK), 8, 2, 12, 30, 0.05f));
        trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.STEEL_LOCK_MECHANISM), 8, 1, 8, 30, 0.2f));
        levels.replace(5, trades.toArray(new VillagerTrades.ItemListing[0]));
        VillagerTrades.TRADES.put(VillagerProfession.TOOLSMITH, levels);
    }

    public static void addWandererTrades()
    {
        Int2ObjectMap<VillagerTrades.ItemListing[]> wanderingTraderTrades = VillagerTrades.WANDERING_TRADER_TRADES;
        List<VillagerTrades.ItemListing> trades = new ArrayList<>(Arrays.stream(wanderingTraderTrades.get(1)).toList());
        trades.add(new VillagerTrades.ItemsForEmeralds(LocksItems.GOLD_LOCK_PICK, 5, 2, 6, 1));
        trades.add(new VillagerTrades.ItemsForEmeralds(LocksItems.STEEL_LOCK_PICK, 3, 2, 8, 1));
        trades.add(new VillagerTrades.EnchantedItemForEmeralds(LocksItems.STEEL_LOCK, 16, 4, 1));
        trades.add(new VillagerTrades.ItemsForEmeralds(LocksItems.STEEL_LOCK_MECHANISM, 6, 1, 4, 1));
        trades.add(new VillagerTrades.EnchantedItemForEmeralds(LocksItems.DIAMOND_LOCK, 28, 4, 1));
        wanderingTraderTrades.replace(1, trades.toArray(new VillagerTrades.ItemListing[0]));
        VillagerTrades.WANDERING_TRADER_TRADES = wanderingTraderTrades;
    }

    public static void register() {
        addVillagerTrades();
        addWandererTrades();
    }
}
