package melonslise.locks.client.init;

import melonslise.locks.Locks;
import melonslise.locks.common.components.interfaces.IItemHandler;
import melonslise.locks.common.init.LocksComponents;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.item.LockItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

import melonslise.locks.common.item.KeyRingItem;
import melonslise.locks.common.container.KeyRingInventory;

@Environment(EnvType.CLIENT)
public final class LocksItemModelsProperties
{
	private LocksItemModelsProperties() {}

	public static void register()
	{
		ItemProperties.register(LocksItems.KEY_RING, new ResourceLocation(Locks.ID, "key_count"), (stack, world, entity, speed) ->
		{
			if (!(stack.getItem() instanceof KeyRingItem keyRingItem)) return 0;
			return keyRingItem.getKeyCount(stack);
			//return 0f;
		});
		ResourceLocation id = new ResourceLocation(Locks.ID, "open");
		ClampedItemPropertyFunction getter = (stack, world, entity, speed) -> LockItem.isOpen(stack) ? 1f : 0f;
		ItemProperties.register(LocksItems.WOOD_LOCK, id, getter);
		ItemProperties.register(LocksItems.COPPER_LOCK, id, getter);
		ItemProperties.register(LocksItems.IRON_LOCK, id, getter);
		ItemProperties.register(LocksItems.STEEL_LOCK, id, getter);
		ItemProperties.register(LocksItems.GOLD_LOCK, id, getter);
		ItemProperties.register(LocksItems.DIAMOND_LOCK, id, getter);
		ItemProperties.register(LocksItems.NETHERITE_LOCK, id, getter);
		ItemProperties.register(LocksItems.NETHERITE_SMART_LOCK, id, getter);
	}
}