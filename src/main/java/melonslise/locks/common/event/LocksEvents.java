package melonslise.locks.common.event;

import melonslise.locks.Locks;
import melonslise.locks.common.components.interfaces.ILockableHandler;
import melonslise.locks.common.config.LocksClientConfig;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.container.LockPickingContainer;
import melonslise.locks.common.init.LocksComponents;
import melonslise.locks.common.init.LocksItemTags;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.init.LocksSoundEvents;
import melonslise.locks.common.item.KeyRingItem;
import melonslise.locks.common.item.LockingItem;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksPredicates;
import melonslise.locks.common.util.LocksUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;


public final class LocksEvents
{
	public static final Component LOCKED_MESSAGE = Component.translatable(Locks.ID + ".status.locked");

	private LocksEvents() {}




	private static void onLootTableLoad(ResourceKey<LootTable> lootTableResourceKey, LootTable.Builder builder, LootTableSource lootTableSource, HolderLookup.Provider provider)
	{

		// Only modify if it was a vanilla chest loot table

        if (!lootTableSource.isBuiltin() || !lootTableResourceKey.location().getPath().startsWith("chests"))
			return;
		// And only if there is a corresponding inject table...
		ResourceLocation injectLoc = ResourceLocation.fromNamespaceAndPath(Locks.ID, "loot_tables/inject/" + lootTableResourceKey.location().getPath() + ".json");
//		if (LocksUtil.resourceManager.getResource(injectLoc).isEmpty())
//			return;
		// todo (kota): bring back

	}


	public static InteractionResult onRightClick(Player player, Level world, InteractionHand hand, BlockHitResult result)
	{

		BlockPos pos = result.getBlockPos();
		ILockableHandler handler = LocksComponents.LOCKABLE_HANDLER.get(world);
		Lockable[] intersect = handler.getInChunk(pos).values().stream().filter(lkb -> lkb.bb.intersects(pos)).toArray(Lockable[]::new);
		if(intersect.length == 0)
			return InteractionResult.PASS;
		if(hand != InteractionHand.MAIN_HAND) // FIXME Better way to prevent firing multiple times
		{
			return InteractionResult.FAIL;
		}
		ItemStack stack = player.getItemInHand(hand);
		Optional<Lockable> locked = Arrays.stream(intersect).filter(LocksPredicates.LOCKED).findFirst();
		if(locked.isPresent())
		{
			Lockable lkb = locked.get();
			Item item = stack.getItem();
			boolean hasMatchingKey = item == LocksItems.MASTER_KEY
					|| (stack.is(LocksItemTags.KEYS) && LockingItem.getOrSetId(stack) == lkb.lock.lockRecord.id())
					|| (item == LocksItems.KEY_RING && KeyRingItem.containsId(stack, lkb.lock.lockRecord.id()));
			boolean f=true;
			// FIXME erase this ugly ass hard coded shit from the face of the earth and make a proper way to do this (maybe mixin to where the right click event is fired from)
			if(!stack.is(LocksItemTags.LOCK_PICKS) && !hasMatchingKey)
			{
				lkb.swing(20);
				world.playSound(player, pos, LocksSoundEvents.LOCK_RATTLE, SoundSource.BLOCKS, 1f, 1f);
				f=false;
			}
			player.swing(InteractionHand.MAIN_HAND);

			if(!(player instanceof ServerPlayer))
				return InteractionResult.PASS;
			if(world.isClientSide && LocksClientConfig.DEAF_MODE.get())
				player.displayClientMessage(LOCKED_MESSAGE, true);
			if (hasMatchingKey) {
				if (lkb.lock.isLocked()) {
					world.playSound(player, pos, LocksSoundEvents.LOCK_OPEN, SoundSource.BLOCKS, 1f, 1f);
					if (!world.isClientSide)
						lkb.lock.setLocked(false);
					return InteractionResult.CONSUME;
				}
				return InteractionResult.PASS;
			}
			if(f) {
				player.openMenu(new LockPickingContainer.Provider(hand, lkb));
				return InteractionResult.CONSUME;
			}
			else
				return InteractionResult.FAIL;
		}
		if(LocksServerConfig.ALLOW_REMOVING_LOCKS.get() && player.isShiftKeyDown() && stack.isEmpty())
		{
			Lockable[] match = Arrays.stream(intersect).filter(LocksPredicates.NOT_LOCKED).toArray(Lockable[]::new);
			if(match.length == 0)
				return InteractionResult.PASS;
			world.playSound(player, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8f, 0.8f + world.random.nextFloat() * 0.4f);
			player.swing(InteractionHand.MAIN_HAND);
			if(player instanceof ServerPlayer) {
				Locks.LOGGER.info("Removing lockable");
				for (Lockable lkb : match) {
					world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, lkb.stack));
					handler.remove(lkb.id);
				}
				return InteractionResult.CONSUME;
			}else {
				return InteractionResult.PASS;
			}
		}
		return InteractionResult.PASS;
	}


	public static boolean canBreakLockable(Level world,Player player, BlockPos pos)
	{
		return (LocksServerConfig.PROTECT_LOCKABLES.get() &&
				!player.isCreative() &&
				LocksUtil.lockedAndRelated(world, pos));
	}

	public static boolean onBlockBreaking(Level world, Player player, BlockPos pos, BlockState state,@Nullable BlockEntity entity)
	{
        return !canBreakLockable(world,player, pos);
	}

	public static void onBlockBreak(Level world, Player player, BlockPos pos, BlockState state,@Nullable BlockEntity entity)
	{
//		if(!canBreakLockable(world,player, pos)) {
//			world.setBlockAndUpdate(pos, state);
//		}
	}

	public static void register()
	{
		LootTableEvents.MODIFY.register(LocksEvents::onLootTableLoad);
		PlayerBlockBreakEvents.BEFORE.register(LocksEvents::onBlockBreaking);
		PlayerBlockBreakEvents.AFTER.register(LocksEvents::onBlockBreak);
		UseBlockCallback.EVENT.register(LocksEvents::onRightClick);
	}


}
