package melonslise.locks.common.item;

import java.util.List;
import java.util.stream.Collectors;

import melonslise.locks.common.container.KeyRingContainer;
import melonslise.locks.common.container.KeyRingInventory;
import melonslise.locks.common.init.LocksSoundEvents;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class KeyRingItem extends Item
{
	public final int rows;

	public KeyRingItem(int rows, Properties props)
	{
		super(props.stacksTo(1));
		this.rows = rows;
	}

	/**
	 * Checks if the ring has a key that matches the given id. (used for some LockEvent hard coded method)
	 * @param stack - Current keyring in mainhand
	 * @param id - Lock Pattern id
	 * @return - Whether it matches or not
	 */
	public static boolean containsId(ItemStack stack, int id)
	{
		//IItemHandler inv = LocksComponents.ITEM_HANDLER.get(stack);
		KeyRingInventory ring = new KeyRingInventory(stack, 9);
		ring.fromTag(stack.getOrCreateTag());

		for(int a = 0; a < ring.getContainerSize(); ++a)
			if(LockingItem.getOrSetId(ring.getItem(a)) == id)
				return true;
		return false;
	}


	/**
	 * Provides key ring menu to the player.
	 *
	 * @param world - Level.
	 * @param player - Specific player in the server
	 * @param hand - The item in hand
	 * @return - wuierd thing
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(player instanceof ServerPlayer serverPlayer){
			serverPlayer.openMenu(new KeyRingContainer.Provider(stack));
		}
		//	NetworkHooks.openScreen((ServerPlayer) player, new KeyRingContainer.Provider(stack), new KeyRingContainer.Writer(hand));
		return new InteractionResultHolder<>(InteractionResult.PASS, stack);
	}

	/**
	 * Checks if the keys in your ring can open a chest.
	 *
	 * @param ctx - context to the player's action
	 * @return - Result of interaction.
	 */
	@Override
	public InteractionResult useOn(UseOnContext ctx)
	{
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		ItemStack stack = ctx.getItemInHand();
		//IItemHandler inv = LocksComponents.ITEM_HANDLER.get(ctx.getItemInHand());

		//Gets the lock and the keys in the ring.
		KeyRingInventory ring = new KeyRingInventory(stack, 9);
		List<Lockable> intersect = LocksUtil.intersecting(world, pos).collect(Collectors.toList());
		ring.fromTag(stack.getOrCreateTag());

		if(intersect.isEmpty() || ring.isEmpty()) return InteractionResult.PASS;

		for(int a = 0; a < ring.getContainerSize(); ++a)
		{
			//Gets id from the key in inventory, checks if the lock matches the key and if so stores it into the "match" List.
			int id = LockingItem.getOrSetId(ring.getItem(a));
			List<Lockable> match = LocksUtil.intersecting(world, pos).filter(lkb -> lkb.lock.id == id).collect(Collectors.toList());
			if(match.isEmpty()) continue;

			world.playSound(ctx.getPlayer(), pos, LocksSoundEvents.LOCK_OPEN, SoundSource.BLOCKS, 1f, 1f);

			/*if(world.isClientSide)
				return InteractionResult.SUCCESS;*/
			for(Lockable lkb : match) {
				lkb.lock.setLocked(!lkb.lock.isLocked());
				//Locks.LOGGER.warn(lkb.lock.isLocked());
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Gets the amount of keys (max is 3) in the ring. used for texture updates
	 * @param stack - ring being held
	 * @return - amount of keys
	 */
	public float getKeyCount(ItemStack stack) {
		KeyRingInventory inv = new KeyRingInventory(stack, 9);
		inv.fromTag(stack.getOrCreateTag());

		return inv.getKeyCount(stack);
	}
}