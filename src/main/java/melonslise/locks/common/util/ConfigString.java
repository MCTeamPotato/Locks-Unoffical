package melonslise.locks.common.util;

public class ConfigString {
    public static final String config = """
            # Locks – Per Loot Table Lock Generation Config (locks-generation.toml)
            # =====================================================================
            #
            # HOW TO ADD AN ENTRY
            # -------------------
            # [lootTable."modId:path/to/loot_table"]
            # locks      = [{ lockType = "material", weight = N }, ...]
            # genChance  = 0.0 ~ 1.0   (chance a lock spawns on this chest type)
            # enchChance = 0.0 ~ 1.0   (chance the lock is enchanted)
            #
            # "default" matches any chest whose loot table is not explicitly listed.
            # If a chest has no loot table at all, it also falls back to "default".
            #
            # lockType short names: wood, copper, gold, iron, steel, diamond
            #   (expands to locks:wood_lock etc.)
            # Full item ids also work: e.g. "mymod:my_custom_lock"
            #
            # weight: positive integer - higher = more likely to be chosen.
                
            [lootTable."default"]
            locks = [
                { lockType = "wood",    weight = 6 },
                { lockType = "copper",  weight = 5 },
                { lockType = "gold",    weight = 4 },
                { lockType = "iron",    weight = 3 },
                { lockType = "steel",   weight = 2 },
                { lockType = "diamond", weight = 1 }
            ]
            genChance  = 0.85
            enchChance = 0.40
                
            [lootTable."minecraft:chests/simple_dungeon"]
            locks = [
                { lockType = "wood",   weight = 6 },
                { lockType = "copper", weight = 6 },
                { lockType = "gold",   weight = 4 }
            ]
            genChance  = 0.65
            enchChance = 0.10
                
            [lootTable."minecraft:chests/abandoned_mineshaft"]
            locks = [
                { lockType = "wood",   weight = 5 },
                { lockType = "copper", weight = 4 },
                { lockType = "iron",   weight = 2 }
            ]
            genChance  = 0.50
            enchChance = 0.15
                
            [lootTable."minecraft:chests/desert_pyramid"]
            locks = [
                { lockType = "copper", weight = 5 },
                { lockType = "gold",   weight = 6 },
                { lockType = "iron",   weight = 3 }
            ]
            genChance  = 0.10
            enchChance = 0.25
                
            [lootTable."minecraft:chests/jungle_temple"]
            locks = [
                { lockType = "wood",   weight = 4 },
                { lockType = "copper", weight = 5 },
                { lockType = "gold",   weight = 4 }
            ]
            genChance  = 0.65
            enchChance = 0.20
                
            [lootTable."minecraft:chests/pillager_outpost"]
            locks = [
                { lockType = "copper", weight = 4 },
                { lockType = "iron",   weight = 5 },
                { lockType = "steel",  weight = 3 }
            ]
            genChance  = 0.75
            enchChance = 0.25
                
            [lootTable."minecraft:chests/woodland_mansion"]
            locks = [
                { lockType = "iron",    weight = 5 },
                { lockType = "steel",   weight = 4 },
                { lockType = "diamond", weight = 2 }
            ]
            genChance  = 0.85
            enchChance = 0.40
                
            [lootTable."minecraft:chests/shipwreck_treasure"]
            locks = [
                { lockType = "copper", weight = 4 },
                { lockType = "gold",   weight = 5 },
                { lockType = "iron",   weight = 3 }
            ]
            genChance  = 0.75
            enchChance = 0.30
                
            [lootTable."minecraft:chests/shipwreck_map"]
            locks = [
                { lockType = "wood",   weight = 5 },
                { lockType = "copper", weight = 4 },
                { lockType = "gold",   weight = 2 }
            ]
            genChance  = 0.60
            enchChance = 0.10
                
            [lootTable."minecraft:chests/shipwreck_supply"]
            locks = [
                { lockType = "wood",   weight = 6 },
                { lockType = "copper", weight = 3 }
            ]
            genChance  = 0.50
            enchChance = 0.10
                
            [lootTable."minecraft:chests/nether_bridge"]
            locks = [
                { lockType = "gold",  weight = 5 },
                { lockType = "iron",  weight = 4 },
                { lockType = "steel", weight = 2 }
            ]
            genChance  = 0.75
            enchChance = 0.35
                
            [lootTable."minecraft:chests/bastion_treasure"]
            locks = [
                { lockType = "netherite",    weight = 1 },
                { lockType = "steel",   weight = 4 },
                { lockType = "diamond", weight = 3 }
            ]
            genChance  = 0.90
            enchChance = 0.55
                
            [lootTable."minecraft:chests/bastion_other"]
            locks = [
                { lockType = "gold",  weight = 5 },
                { lockType = "iron",  weight = 4 },
                { lockType = "steel", weight = 2 }
            ]
            genChance  = 0.70
            enchChance = 0.30
                
            [lootTable."minecraft:chests/end_city_treasure"]
            locks = [
                { lockType = "steel",   weight = 4 },
                { lockType = "diamond", weight = 5 },
                { lockType = "netherite", weight = 1 }
            ]
            genChance  = 0.95
            enchChance = 0.65
                
            [lootTable."minecraft:chests/stronghold_corridor"]
            locks = [
                { lockType = "iron",    weight = 5 },
                { lockType = "steel",   weight = 4 },
                { lockType = "gold", weight = 1 }
            ]
            genChance  = 0.80
            enchChance = 0.40
                
            [lootTable."minecraft:chests/stronghold_library"]
            locks = [
                { lockType = "iron",    weight = 4 },
                { lockType = "steel",   weight = 4 },
                { lockType = "gold", weight = 3 }
            ]
            genChance  = 0.90
            enchChance = 0.55
                
            [lootTable."minecraft:chests/ancient_city"]
            locks = [
                { lockType = "steel",   weight = 6 },
                { lockType = "diamond", weight = 1}
            ]
            genChance  = 0.98
            enchChance = 0.60
                
            [lootTable."minecraft:chests/buried_treasure"]
            locks = [
                { lockType = "steel",    weight = 5 },
                { lockType = "diamond",   weight = 3 },
                { lockType = "netherite", weight = 1 }
            ]
            genChance  = 1
            enchChance = 0
                
            [lootTable."minecraft:chests/igloo_chest"]
            locks = [
                { lockType = "wood",   weight = 5 },
                { lockType = "copper", weight = 3 }
            ]
            genChance  = 0.60
            enchChance = 0.10
                
            [lootTable."minecraft:chests/underwater_ruin_small"]
            locks = [
                { lockType = "wood",   weight = 5 },
                { lockType = "copper", weight = 4 },
                { lockType = "gold",   weight = 2 }
            ]
            genChance  = 0.55
            enchChance = 0.15
                
            [lootTable."minecraft:chests/underwater_ruin_big"]
            locks = [
                { lockType = "copper", weight = 4 },
                { lockType = "gold",   weight = 4 },
                { lockType = "iron",   weight = 3 }
            ]
            genChance  = 0.65
            enchChance = 0.20
                
            [lootTable."minecraft:chests/ruined_portal"]
            locks = [
                { lockType = "gold",  weight = 6 },
                { lockType = "iron",  weight = 3 },
                { lockType = "steel", weight = 1 },
                { lockType = "copper", weight = 1 }
            ]
            genChance  = 0.65
            enchChance = 0.30
                
            [lootTable."minecraft:chests/ancient_city_ice_box"]
            locks = [
                { lockType = "iron",  weight = 4 },
                { lockType = "steel", weight = 3 }
            ]
            genChance  = 0.70
            enchChance = 0.35
                
            [lootTable."minecraft:chests/village/village_plains_house"]
            locks = [
                { lockType = "wood",   weight = 6 },
                { lockType = "copper", weight = 3 }
            ]
            genChance  = 0.30
            enchChance = 0.05
                
            [lootTable."minecraft:chests/village/village_desert_house"]
            locks = [
                { lockType = "wood",   weight = 6 },
                { lockType = "copper", weight = 3 }
            ]
            genChance  = 0.30
            enchChance = 0.05
                
            [lootTable."minecraft:chests/village/village_savanna_house"]
            locks = [
                { lockType = "wood",   weight = 6 },
                { lockType = "copper", weight = 3 }
            ]
            genChance  = 0.30
            enchChance = 0.05
                
            [lootTable."minecraft:chests/village/village_snowy_house"]
            locks = [
                { lockType = "wood",   weight = 6 },
                { lockType = "copper", weight = 3 }
            ]
            genChance  = 0.30
            enchChance = 0.05
                
            [lootTable."minecraft:chests/village/village_taiga_house"]
            locks = [
                { lockType = "wood",   weight = 6 },
                { lockType = "copper", weight = 3 }
            ]
            genChance  = 0.30
            enchChance = 0.05
                
            [lootTable."minecraft:chests/village/village_weaponsmith"]
            locks = [
                { lockType = "iron",  weight = 5 },
                { lockType = "steel", weight = 4 }
            ]
            genChance  = 0.70
            enchChance = 0.20
                
            [lootTable."minecraft:chests/village/village_toolsmith"]
            locks = [
                { lockType = "gold", weight = 5 },
                { lockType = "iron",   weight = 4 }
            ]
            genChance  = 0.75
            enchChance = 0.15
                
            [lootTable."minecraft:chests/village/village_armorer"]
            locks = [
                { lockType = "iron",  weight = 5 },
                { lockType = "steel", weight = 3 }
            ]
            genChance  = 0.65
            enchChance = 0.20
                
            [lootTable."minecraft:chests/village/village_cartographer"]
            locks = [
                { lockType = "copper", weight = 5 },
                { lockType = "gold",   weight = 3 }
            ]
            genChance  = 0.50
            enchChance = 0.10
                
            [lootTable."minecraft:chests/village/village_mason"]
            locks = [
                { lockType = "wood",   weight = 5 },
                { lockType = "copper", weight = 3 }
            ]
            genChance  = 0.35
            enchChance = 0.05
                
            [lootTable."minecraft:chests/village/village_shepherd"]
            locks = [
                { lockType = "wood",   weight = 6 },
                { lockType = "copper", weight = 2 }
            ]
            genChance  = 0.25
            enchChance = 0.05
                
            [lootTable."minecraft:chests/village/village_butcher"]
            locks = [
                { lockType = "wood",   weight = 5 },
                { lockType = "copper", weight = 3 }
            ]
            genChance  = 0.25
            enchChance = 0.05
                
            [lootTable."minecraft:chests/village/village_tannery"]
            locks = [
                { lockType = "wood",   weight = 6 },
                { lockType = "copper", weight = 2 }
            ]
            genChance  = 0.25
            enchChance = 0.05
                
            [lootTable."minecraft:chests/village/village_temple"]
            locks = [
                { lockType = "copper", weight = 4 },
                { lockType = "gold",   weight = 5 }
            ]
            genChance  = 0.55
            enchChance = 0.20
            """;
}
