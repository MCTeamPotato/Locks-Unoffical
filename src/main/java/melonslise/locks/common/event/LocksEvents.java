package melonslise.locks.common.event;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import melonslise.locks.Locks;
import melonslise.locks.common.components.LockableHandler;
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
import melonslise.locks.common.item.SmartLockItem;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksPredicates;
import melonslise.locks.common.util.LocksUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Arrays;
import java.util.Optional;


public final class LocksEvents
{
	public static final Component LOCKED_MESSAGE = Component.translatable(Locks.ID + ".status.locked");

	private LocksEvents() {}



	public static void onLootTableLoad(ResourceManager resourceManager, LootDataManager lootManager, ResourceLocation id, LootTable.Builder tableBuilder, LootTableSource source)
	{
		// Only modify if it was a vanilla chest loot table

        if (!id.getNamespace().equals("minecraft") || !id.getPath().startsWith("chests"))
			return;
		// And only if there is a corresponding inject table...
		ResourceLocation injectLoc = new ResourceLocation(Locks.ID, "loot_tables/inject/" + id.getPath() + ".json");
		if (LocksUtil.resourceManager.getResource(injectLoc).isEmpty())
			return;
		// todo (kota): bring back

	}


	public static InteractionResult onRightClick(Player player, Level world, InteractionHand hand, BlockHitResult result)
	{
		//Gets the lock being interacted with and the handler
		BlockPos pos = result.getBlockPos();
		ILockableHandler handler = LocksComponents.LOCKABLE_HANDLER.get(world);
		Lockable[] intersect = handler.getInChunk(pos).values().stream()
				.filter(lkb -> lkb.bb.intersects(pos))
				.toArray(Lockable[]::new);

		//
		if(intersect.length == 0) return InteractionResult.PASS;
		if(hand != InteractionHand.MAIN_HAND) return InteractionResult.FAIL; // FIXME Better way to prevent firing multiple times

		//Gets the item in hand and the
		ItemStack stack = player.getItemInHand(hand);
		Optional<Lockable> locked = Arrays.stream(intersect)
				.filter(LocksPredicates.LOCKED)
				.findFirst();

		if(locked.isPresent())
		{
			//The locked being interacted with and the item in hand.
			Lockable lkb = locked.get();
			Item item = stack.getItem();

			if(lkb.isSmart()) SmartLockItem.getOrSetOwner(stack, player).equals(player.getUUID());

			// FIXME erase this ugly ass hard coded shit from the face of the earth and make a proper way to do this (maybe mixin to where the right click event is fired from)
			if(!canUnlock(lkb, stack, player))
			{
				lkb.swing(20);
				world.playSound(player, pos, LocksSoundEvents.LOCK_RATTLE, SoundSource.BLOCKS, 1f, 1f);
			}
			player.swing(InteractionHand.MAIN_HAND);

			//Also very hardcoded, should fix the error where you cant open the lock without crouching.
			if(!player.isShiftKeyDown() && hasMatchingKey(lkb, stack, player))
			{
				lkb.lock.setLocked(!lkb.lock.isLocked());
				world.playSound(player, pos, LocksSoundEvents.LOCK_OPEN, SoundSource.BLOCKS, 1f, 1f);

				if (!world.isClientSide) {
					// Delay sync so swing isn't cut short immediately
					((ServerLevel) world).getServer().execute(() -> {

						// Cardinal Components sync method
						LocksComponents.LOCKABLE_HANDLER.sync(world);
						// Mark chunk as dirty
						(world.getChunk(pos)).setUnsaved(true);
					});
				}
			}

			if(!(player instanceof ServerPlayer)) return InteractionResult.PASS;

			if(world.isClientSide && LocksClientConfig.DEAF_MODE.get()) player.displayClientMessage(LOCKED_MESSAGE, true);

			// Avoids rendering when it is opened with a key while standing
			if(!lkb.isSmart() && player.isShiftKeyDown() && hasMatchingKey(lkb, stack, player)) { return InteractionResult.PASS; }



			if(canUnlock(lkb,stack,player) && !hasMatchingKey(lkb, stack, player)) {
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

	public static boolean canUnlock(Lockable lkb, ItemStack stack, Player player) {
		//Checks for matching keys
		if (hasMatchingKey(lkb, stack, player)) return true;

		// Check if it's a lockpick and the lock can be lockpicked
		if (stack.is(LocksItemTags.LOCK_PICKS) && !lkb.isSmart()) return true;

		return false;
	}

	public static boolean hasMatchingKey(Lockable lkb, ItemStack stack, Player player) {
		int lockId = lkb.lock.id;
		Item item = stack.getItem();

		// Check if it's a matching key
		if (stack.is(LocksItemTags.KEYS) && LockingItem.getOrSetId(stack) == lockId) return true;

		// Check if it's the master key
		if (item == LocksItems.MASTER_KEY) return true;

		// Check if it's a keyring containing the correct key
		if (item == LocksItems.KEY_RING && KeyRingItem.containsId(stack, lockId)) return true;

		//Checks if the player is the owner of the smart lock.
		if (!stack.is(LocksItemTags.LOCK_PICKS) && lkb.isSmart() && SmartLockItem.getOrSetOwner(lkb.stack, player).equals(player.getUUID())) return true;

		return false;
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