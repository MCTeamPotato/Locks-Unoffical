package melonslise.locks.common.container;

import melonslise.locks.common.components.interfaces.IItemHandler;
import melonslise.locks.common.init.LocksContainerTypes;
import melonslise.locks.common.init.LocksItemTags;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.init.LocksSoundEvents;
import melonslise.locks.common.item.KeyRingItem;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class KeyRingContainer extends AbstractContainerMenu
{
	public static class KeyRingSlot extends Slot
	{
		public final Player player;
		public final ItemStack ringStack;

		public KeyRingSlot(Player player, IItemHandler inv, ItemStack ringStack, int index, int x, int y)
		{
			super(inv, index, x, y);
			this.player = player;
			this.ringStack = ringStack;
		}

		// TODO PITCH
		@Override
		public void set(ItemStack stack)
		{
			super.set(stack);
			KeyRingItem.updateKeyCount(this.ringStack, (IItemHandler) this.container);
			if(!this.player.level().isClientSide)
				this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), LocksSoundEvents.KEY_RING, SoundSource.PLAYERS, 1f, 1f);
		}

		@Override
		public boolean mayPlace(ItemStack stack)
		{
			return stack.is(LocksItemTags.KEYS) || stack.is(LocksItems.MASTER_KEY);
		}

		@Override
		public void onTake(Player player, ItemStack stack)
		{
			KeyRingItem.updateKeyCount(this.ringStack, (IItemHandler) this.container);
			if(!this.player.level().isClientSide)
				this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), LocksSoundEvents.KEY_RING, SoundSource.PLAYERS, 1f, 1f);
			super.onTake(player, stack);
		}
	}

	public final ItemStack stack;
	public final IItemHandler inv;
	public final int rows;

	public KeyRingContainer(int id, Player player, ItemStack stack)
	{
		super(LocksContainerTypes.KEY_RING, id);
		this.stack = stack;
		this.inv = KeyRingItem.getOrCreateHandler(stack);

		this.rows = inv.getSlots() / 9;
		for(int row = 0; row < rows; ++row)
			for(int col = 0; col < 9; ++col)
				this.addSlot(new KeyRingSlot(player, inv, this.stack, col + row * 9, 8 + col * 18, 18 + row * 18));

		int offset = (rows - 4) * 18;
		for(int row = 0; row < 3; ++row)
			for (int col = 0; col < 9; ++col)
				this.addSlot(new Slot(player.getInventory(), col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + offset));

		for(int coll = 0; coll < 9; ++coll)
			this.addSlot(new Slot(player.getInventory(), coll, 8 + coll * 18, 161 + offset));
	}

	@Override
	public boolean stillValid(Player player)
	{
		return !this.stack.isEmpty();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		ItemStack stack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if(slot == null || !slot.hasItem())
			return stack;
		ItemStack stack1 = slot.getItem();
		stack = stack1.copy();
		if(index < this.inv.getSlots())
		{
			if(!this.moveItemStackTo(stack1, this.inv.getSlots(), this.slots.size(), true))
				return ItemStack.EMPTY;
		}
		else if(!this.moveItemStackTo(stack1, 0, this.inv.getSlots(), false))
			return ItemStack.EMPTY;
		if(stack1.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		KeyRingItem.updateKeyCount(this.stack, this.inv);
		return stack;
	}

	public static final ExtendedScreenHandlerType.ExtendedFactory<KeyRingContainer,Integer> FACTORY = (id, inv, i) ->
	{
		return new KeyRingContainer(id, inv.player, inv.player.getItemInHand(InteractionHand.values()[i]));
	};

	public static class Provider implements ExtendedScreenHandlerFactory<Integer>
	{
		public final ItemStack stack;

		public Provider(ItemStack stack)
		{
			this.stack = stack;
		}

		@Override
		public AbstractContainerMenu createMenu(int id, Inventory inv, Player player)
		{
			return new KeyRingContainer(id, player, this.stack);
		}

		@Override
		public Component getDisplayName()
		{
			return this.stack.getHoverName();
		}


		@Override
		public Integer getScreenOpeningData(ServerPlayer serverPlayer) {
			return InteractionHand.MAIN_HAND.ordinal();
		}
	}

	public static class Writer implements Consumer<FriendlyByteBuf>
	{
		public final InteractionHand hand;

		public Writer(InteractionHand hand)
		{
			this.hand = hand;
		}

		@Override
		public void accept(FriendlyByteBuf buffer)
		{
			buffer.writeEnum(this.hand);
		}
	}
}
