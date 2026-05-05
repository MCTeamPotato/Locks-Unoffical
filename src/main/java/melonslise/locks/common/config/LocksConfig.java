package melonslise.locks.common.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.collect.Lists;
import melonslise.locks.Locks;
import melonslise.locks.common.util.ConfigString;
import melonslise.locks.common.util.LocksUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class LocksConfig {

    // ── ForgeConfigSpec (locks-common.toml) ──────────────────────────────────
    // GENERATED_LOCKS, GENERATED_LOCK_WEIGHTS, GENERATION_CHANCE, and
    // GENERATION_ENCHANT_CHANCE have been removed. All generation settings
    // are now configured exclusively in locks-generation.toml.

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GEN_LOCKABLE_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue RANDOMIZE_LOADED_LOCKS;

    static {
        ForgeConfigSpec.Builder cfg = new ForgeConfigSpec.Builder();

        GEN_LOCKABLE_BLOCKS = cfg
                .comment("Blocks that can be locked during world generation (supports regex).")
                .comment("当世界生成时锁定的方块")
                .defineList("Lockable Generated Blocks",
                        Lists.newArrayList("minecraft:chest", "minecraft:barrel", "lootr:.*", "quark:.*_chest"),
                        e -> e instanceof String);

        RANDOMIZE_LOADED_LOCKS = cfg
                .comment("Randomize lock IDs and combinations when loading from a structure file.")
                .comment("从结构文件加载锁ID和组合时进行随机化。随机化的方式与世界生成时相同。")
                .define("Randomize Loaded Locks", true);

        SPEC = cfg.build();
    }

    // ── Resolved at init() ────────────────────────────────────────────────────

    public static Pattern[] lockableGenBlocks;

    private static List<Enchantment> lockEnchantments = null;

    // Per-loot-table entries loaded from locks-generation.toml.
    // Items are resolved lazily on first roll() call, not at load time,
    // to avoid reading the item registry before it is fully populated.
    private static Map<String, LootTableEntry> lootTableEntries;

    // ── Inner class ───────────────────────────────────────────────────────────

    static final class LootTableEntry {
        // Raw lock type strings + weights stored at load time
        private final List<String[]> rawLocks; // each String[]: [itemId, weight]
        final double genChance;
        final double enchChance;

        // Resolved lazily on first roll()
        private NavigableMap<Integer, Item> weightedLocks = null;
        private int weightTotal = 0;

        LootTableEntry(List<?> lockList, double genChance, double enchChance) {
            this.genChance  = genChance;
            this.enchChance = enchChance;
            this.rawLocks   = new ArrayList<>();

            for (Object raw : lockList) {
                if (!(raw instanceof Config entry)) continue;
                Object typeRaw   = entry.get("lockType");
                Object weightRaw = entry.get("weight");
                if (typeRaw == null || weightRaw == null) continue;
                int weight = ((Number) weightRaw).intValue();
                if (weight <= 0) continue;
                String lockType = typeRaw.toString();
                String itemId = lockType.contains(":") ? lockType : "locks:" + lockType + "_lock";
                rawLocks.add(new String[]{ itemId, String.valueOf(weight) });
            }
        }

        // Resolve item registry lookups deferred to first use,
         // by which point all items are guaranteed to be registered.
        // synchronized might be needed to avoid future bugs....
        private void resolveIfNeeded() {
            if (weightedLocks != null) return;
            weightedLocks = new TreeMap<>();
            int total = 0;
            for (String[] entry : rawLocks) {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(entry[0]));
                // Skip air/missing items
                if (item == net.minecraft.world.item.Items.AIR) {
                    //Locks.LOGGER.warn("[LocksConfig] Item '{}' not found in registry, skipping.", entry[0]);
                    continue;
                }
                int weight = Integer.parseInt(entry[1]);
                total += weight;
                weightedLocks.put(total, item);
            }
            weightTotal = total;
            /*if (weightTotal == 0) {
                Locks.LOGGER.warn("[LocksConfig] LootTableEntry resolved with no valid items!");
            }*/
        }

        boolean isValid() {
            // Check raw locks before resolution so we can validate at load time
            return !rawLocks.isEmpty();
        }

        boolean isValidResolved() {
            resolveIfNeeded();
            return weightTotal > 0 && !weightedLocks.isEmpty();
        }

        ItemStack roll(RandomSource rng) {
            if (!LocksUtil.chance(rng, genChance))
            {
                //Locks.LOGGER.info("[LootTableEntry] rng said nono Lock. Not generating lock. (chance={})", genChance);
                return null;
            }
            resolveIfNeeded();
            //Locks.LOGGER.info(getInfo());//"[LootTableEntry] weightTotal={} locks={}", weightTotal, weightedLocks);

            resolveIfNeeded();
            if (weightTotal == 0 || weightedLocks.isEmpty()) return null;

            Map.Entry<Integer, Item> found = weightedLocks.ceilingEntry(rng.nextInt(weightTotal) + 1);
            if (found == null) return null; // safety guard
            ItemStack stack = new ItemStack(found.getValue());

            /*Locks.LOGGER.info("[LootTableEntry] enchChance (chance={})", enchChance);
            ItemStack result = enchantLock(stack, rng); //EnchantmentHelper.enchantItem(rng, stack, 5 + rng.nextInt(30), true);
            ItemStack result = LocksUtil.chance(rng, enchChance)
                    ? EnchantmentHelper.enchantItem(rng, stack, 5 + rng.nextInt(30), false)
                    : stack;
            Locks.LOGGER.info("[roll] stack before enchant: {} NBT={}, isEnchanted={}", stack, stack.getTag(), stack.isEnchanted());
            Locks.LOGGER.info("[roll] result after enchant: {} NBT={}, isEnchanted={}", result, result.getTag(), result.isEnchanted());
            Locks.LOGGER.info("[roll] enchantmentValue={}", stack.getItem().getEnchantmentValue());
            return result;*/
            return LocksUtil.chance(rng, enchChance)
                    ? enchantLock(stack, rng)
                    : stack;
        }

        public String getInfo() {
            resolveIfNeeded();
            return "weightTotal=" + weightTotal
                    + " genChance=" + genChance
                    + " enchChance=" + enchChance
                    + " locks=" + weightedLocks;
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private LocksConfig() {}

    public static synchronized void init() {
        Locks.LOGGER.info("[LocksConfig] init() called on thread: {}", Thread.currentThread().getName());
        /*/Locks.LOGGER.info("[LocksConfig] SHOCKING in registry: {}",
                BuiltInRegistries.ENCHANTMENT.containsKey(new ResourceLocation("locks:shocking")));*/
        for (Enchantment e : BuiltInRegistries.ENCHANTMENT) {
            if (e.canEnchant(new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("locks:wood_lock"))))) {
                Locks.LOGGER.info("[LocksConfig] Compatible enchantment found: {}",
                        BuiltInRegistries.ENCHANTMENT.getKey(e));
            }
        }

        buildEnchantmentList();
        Locks.LOGGER.info("[LocksConfig] init() called");
        lockableGenBlocks = GEN_LOCKABLE_BLOCKS.get().stream()
                .map(Pattern::compile).toArray(Pattern[]::new);
        loadLootTableConfig();
    }

    public static List<Enchantment> getLockEnchantments() {
        return lockEnchantments != null ? lockEnchantments : List.of();
    }

    private static void buildEnchantmentList() {
        ItemStack probe = new ItemStack(
                BuiltInRegistries.ITEM.get(new ResourceLocation("locks:wood_lock")));
        lockEnchantments = new ArrayList<>();
        for (Enchantment e : BuiltInRegistries.ENCHANTMENT) {
            if (e.canEnchant(probe)) {
                lockEnchantments.add(e);
                Locks.LOGGER.info("[LocksConfig] Added to lock enchantment pool: {}",
                        BuiltInRegistries.ENCHANTMENT.getKey(e));
            }
        }
    }

    // ── locks-generation.toml ─────────────────────────────────────────────────

    private static final String GEN_CONFIG_FILE = "locks-generation.toml";

    private static void  loadLootTableConfig() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path file = configDir.resolve(GEN_CONFIG_FILE);

        //Locks.LOGGER.info("[LocksConfig] Looking for: {}", file.toAbsolutePath());

        if (!Files.exists(file)) {
            //Locks.LOGGER.info("[LocksConfig] Not found, writing defaults...");
            writeDefaultLootTableConfig(file);
        }

        lootTableEntries = new HashMap<>();
        try (FileConfig cfg = FileConfig.builder(file.toFile(), TomlFormat.instance()).sync().build()) {
            cfg.load();

            Config section = cfg.get("lootTable");
            if (section == null) {
                Locks.LOGGER.warn("[LocksConfig] No [lootTable] section in {}.", GEN_CONFIG_FILE);
                return;
            }

            Locks.LOGGER.info("[LocksConfig] Found [lootTable] keys: {}", section.valueMap().keySet());

            for (Map.Entry<String, Object> kv : section.valueMap().entrySet()) {
                String key = kv.getKey();
                if (!(kv.getValue() instanceof Config entry)) continue;

                List<?> lockList    = entry.get("locks");
                Number  genChanceN  = entry.get("genChance");
                Number  enchChanceN = entry.get("enchChance");
                if (lockList == null) {
                    Locks.LOGGER.warn("[LocksConfig] Entry '{}' missing 'locks', skipping.", key);
                    continue;
                }

                double genChance  = genChanceN  != null ? genChanceN.doubleValue()  : 0.85;
                double enchChance = enchChanceN != null ? enchChanceN.doubleValue() : 0.40;
                LootTableEntry resolved = new LootTableEntry(lockList, genChance, enchChance);
                if (resolved.isValid()) {
                    lootTableEntries.put(key, resolved);
                    Locks.LOGGER.info("[LocksConfig] Loaded entry '{}' ({} locks, genChance={}, enchChance={})",
                            key, resolved.rawLocks.size(), genChance, enchChance);
                } else {
                    Locks.LOGGER.warn("[LocksConfig] Entry '{}' has no valid locks, skipping.", key);
                }
            }
            Locks.LOGGER.info("[LocksConfig] Loaded {} loot-table entries total.", lootTableEntries.size());
        } catch (Exception e) {
            Locks.LOGGER.error("[LocksConfig] Failed to load {}: {}", GEN_CONFIG_FILE, e.getMessage(), e);
        }
    }

    private static void writeDefaultLootTableConfig(Path path) {
        try { Files.createDirectories(path.getParent()); }
        catch (IOException e) {
            Locks.LOGGER.error("[LocksConfig] Could not create config dir: {}", e.getMessage());
            return;
        }

        try {
            Files.writeString(path, ConfigString.config);
            Locks.LOGGER.info("[LocksConfig] Created default {}.", GEN_CONFIG_FILE);
        } catch (IOException e) {
            Locks.LOGGER.error("[LocksConfig] Failed to write {}: {}", GEN_CONFIG_FILE, e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static boolean matchString(Block block) {
        String name = BuiltInRegistries.BLOCK.getKey(block).toString();
        if (lockableGenBlocks == null) init();
        for (Pattern p : lockableGenBlocks) {
            if (p.matcher(name).matches()) return true;
        }
        return false;
    }

    /**
     * Main lock generation entry point.
     * Tries specific loot table entry, then "default", then returns null.
     * Item registry lookups are deferred to first call so registration is complete.
     */
    public static ItemStack rollLock(RandomSource rng, ResourceLocation lootTableId) {
        if (lootTableEntries == null) init();

        LootTableEntry entry = null;
        if (lootTableId != null) entry = lootTableEntries.get(lootTableId.toString());
        if (entry == null)
        {
            //Locks.LOGGER.warn("[LocksConfig] Unable to roll for {}, now attempting default.", lootTableId.toString());
            entry = lootTableEntries.get("default");
        }
        if (entry == null)
        {
            Locks.LOGGER.warn("[LocksConfig] Unable to roll for default as well. returning no lock. \n Might be due to: \nMay have failed to load (A log has been printed for this)\nIF locks-generation.toml IS EMPTY, PLEASE DELETE FILE.");
            return null;
        }

        /*Locks.LOGGER.info("[LocksConfig] rollLock id='{}' entry={} genChance={} weightTotal={}",
                lootTableId, entry != null ? "found" : "null(using default)",
                entry != null ? entry.genChance : -1,
                entry != null ? entry.weightTotal : -1);*/

        return entry.roll(rng);
    }

    /**
     * Used by StructureTemplateMixin for RANDOMIZE_LOADED_LOCKS.
     * Uses "default" entry from locks-generation.toml.
     */
    public static ItemStack getRandomLock(RandomSource rng) {
        if (lootTableEntries == null) init();
        LootTableEntry def = lootTableEntries.get("default");
        if (def == null) return null;
        // For RANDOMIZE_LOADED_LOCKS we always generate (skip genChance roll)
        def.resolveIfNeeded();
        if (def.weightTotal == 0 || def.weightedLocks.isEmpty()) return null;
        Map.Entry<Integer, Item> found = def.weightedLocks.ceilingEntry(rng.nextInt(def.weightTotal) + 1);
        if (found == null) return null; //EnchantmentHelper.enchantItem(rng, stack, 5 + rng.nextInt(30), false)
        ItemStack stack = new ItemStack(found.getValue());
        return LocksUtil.chance(rng, 1f)
                ? enchantLock(stack, rng)
                : stack;
    }

    /*public static boolean canGen(RandomSource rng, Block block) {
        if (lootTableEntries == null) init();
        LootTableEntry def = lootTableEntries.get("default");
        double genChance = def != null ? def.genChance : 0.85;
        return LocksUtil.chance(rng, genChance) && matchString(block);
    }

    public static boolean canEnchant(RandomSource rng) {
        if (lootTableEntries == null) init();
        LootTableEntry def = lootTableEntries.get("default");
        double enchChance = 1f;//def != null ? def.enchChance : 0.40;
        return LocksUtil.chance(rng, enchChance);
    }*/

    public static ItemStack enchantLock(ItemStack stack, RandomSource rng)
    {
        List<Enchantment> available = LocksConfig.getLockEnchantments();
        if (!available.isEmpty()) {
            Enchantment ench = available.get(rng.nextInt(available.size()));
            int level = 1 + rng.nextInt(ench.getMaxLevel());
            stack.enchant(ench, level);
        }

        return stack;
    }
}