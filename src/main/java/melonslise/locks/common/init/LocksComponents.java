package melonslise.locks.common.init;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.ladysnake.cca.api.v3.chunk.ChunkComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.chunk.ChunkComponentInitializer;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import org.ladysnake.cca.api.v3.item.ItemComponentInitializer;
import org.ladysnake.cca.api.v3.item.ItemComponentMigrationRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;
import melonslise.locks.Locks;
import melonslise.locks.common.components.ItemHandler;
import melonslise.locks.common.components.LockableHandler;
import melonslise.locks.common.components.LockableStorage;
import melonslise.locks.common.components.Selection;
import melonslise.locks.common.components.interfaces.IItemHandler;
import melonslise.locks.common.components.interfaces.ILockableHandler;
import melonslise.locks.common.components.interfaces.ILockableStorage;
import melonslise.locks.common.components.interfaces.ISelection;
import melonslise.locks.common.item.LockItem;
import net.minecraft.resources.ResourceLocation;

public class LocksComponents implements EntityComponentInitializer, WorldComponentInitializer, ChunkComponentInitializer, ItemComponentInitializer {

    public static final ComponentKey<ILockableHandler> LOCKABLE_HANDLER =
            ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath(Locks.ID,"lockable_handler"),ILockableHandler.class);

    public static final ComponentKey<ILockableStorage> LOCKABLE_STORAGE =
            ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath(Locks.ID,"lockable_storage"), ILockableStorage.class);

    public static final ComponentKey<ISelection> SELECTION =
            ComponentRegistry.getOrCreate(ResourceLocation.fromNamespaceAndPath(Locks.ID,"selection"), ISelection.class);

    public static final DataComponentType<ItemHandler> ITEM_HANDLER =
            DataComponentType.<ItemHandler>builder().persistent(ItemHandler.CODEC).build();

    public static void register() {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ResourceLocation.fromNamespaceAndPath(Locks.ID, "item_handler"), ITEM_HANDLER);
    }

    @Override
    public void registerChunkComponentFactories(ChunkComponentFactoryRegistry chunkComponentFactoryRegistry) {
        chunkComponentFactoryRegistry.register(LOCKABLE_STORAGE, LockableStorage::new);
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry entityComponentFactoryRegistry) {
        entityComponentFactoryRegistry.registerForPlayers(SELECTION, (player) -> new Selection(player.getOnPos()), RespawnCopyStrategy.ALWAYS_COPY);
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry worldComponentFactoryRegistry) {
        worldComponentFactoryRegistry.register(LOCKABLE_HANDLER, LockableHandler::new);
    }

    @Override
    public void registerItemComponentMigrations(ItemComponentMigrationRegistry itemComponentMigrationRegistry) {
        itemComponentMigrationRegistry.registerMigration(ResourceLocation.fromNamespaceAndPath(Locks.ID,"item_handler"),ITEM_HANDLER);

    }
}
