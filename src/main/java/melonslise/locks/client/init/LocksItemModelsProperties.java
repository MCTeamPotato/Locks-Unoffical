package melonslise.locks.client.init;

import melonslise.locks.Locks;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.item.KeyRingItem;
import melonslise.locks.common.item.LockItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public final class LocksItemModelsProperties
{
	private LocksItemModelsProperties() {}

	public static void register()
	{
		ItemProperties.register(LocksItems.KEY_RING, ResourceLocation.fromNamespaceAndPath(Locks.ID, "keys"), (stack, world, entity, speed) ->
		{
			CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			int keys = data.copyTag().getInt(KeyRingItem.KEY_COUNT);
			if(keys <= 0)
				return 0f;
			if(keys == 1)
				return 0.1f;
			if(keys == 2)
				return 0.21f;
			return 0.32f;
		});
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Locks.ID, "open");
		ClampedItemPropertyFunction getter = (stack, world, entity, speed) -> LockItem.isOpen(stack) ? 1f : 0f;
		ItemProperties.register(LocksItems.WOOD_LOCK, id, getter);
		ItemProperties.register(LocksItems.IRON_LOCK, id, getter);
		ItemProperties.register(LocksItems.COPPER_LOCK, id, getter);
		ItemProperties.register(LocksItems.GOLD_LOCK, id, getter);
		ItemProperties.register(LocksItems.DIAMOND_LOCK, id, getter);
	}
}
