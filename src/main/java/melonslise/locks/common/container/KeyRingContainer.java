package melonslise.locks.common.container;

import melonslise.locks.Locks;
import melonslise.locks.common.components.interfaces.IItemHandler;
import melonslise.locks.common.container.KeyRingInventory;
import melonslise.locks.common.init.LocksComponents;
import melonslise.locks.common.init.LocksContainerTypes;
import melonslise.locks.common.init.LocksSoundEvents;
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
import net.minecraft.world.item.Item;
import melonslise.locks.common.init.LocksItems;
import java.util.function.Predicate;

import net.minecraft.world.inventory.MenuType;

import java.util.function.Consumer;

public class KeyRingContainer extends AbstractContainerMenu
{
	public static class KeyRingSlot extends Slot
	{
		public final Player player;
		private final Item allowedItem;

		public KeyRingSlot(KeyRingInventory container, Item allowedItem, Player player, int index, int x, int y)
		{
			super(container, index, x, y);
			this.player = player;
			this.allowedItem = allowedItem;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.getItem() == allowedItem;
		}

		// TODO PITCH
		@Override
		public void set(ItemStack stack)
		{
			super.set(stack);
			if(!this.player.level().isClientSide)
				this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), LocksSoundEvents.KEY_RING, SoundSource.PLAYERS, 1f, 1f);
		}

		@Override
		public void onTake(Player player, ItemStack stack)
		{
			if(!this.player.level().isClientSide)
				this.player.level().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(), LocksSoundEvents.KEY_RING, SoundSource.PLAYERS, 1f, 1f);
			super.onTake(player, stack);
		}
	}



	public final ItemStack stack;
	public final IItemHandler inv;
	public final int rows;
	public final KeyRingInventory container;

	public KeyRingContainer(int id, Player player, ItemStack stack)
	{
		super(LocksContainerTypes.KEY_RING, id);
		this.stack = stack;
		this.inv = LocksComponents.ITEM_HANDLER.get(stack);
		this.rows = 1;
		container = new KeyRingInventory(stack, 9);

		//this.rows = inv.getSlots() / 9;
		for(int row = 0; row < rows; row++)
			for(int col = 0; col < 9; col++)
				this.addSlot(new KeyRingSlot(container, LocksItems.KEY, player, col + row * 9, 8 + col * 18, 18 + row * 18));

		/*
		Same thing but with regular chest slots
		for(int row  = 0; row < this.rows; ++row) {
			for(int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}*/

		//Gets players inventory and displays it
		int offset = (rows - 4) * 18;
		for(int row = 0; row < 3; ++row)
			for (int col = 0; col < 9; ++col)
				this.addSlot(new Slot(player.getInventory(), col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + offset));

		//Gets player's hotbar and displays it
		for(int coll = 0; coll < 9; ++coll)
			this.addSlot(new Slot(player.getInventory(), coll, 8 + coll * 18, 161 + offset));
	}

	@Override
	public boolean stillValid(Player player)
	{
		return !this.stack.isEmpty();
	}

	/*@Override
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
		return stack;
	}*/

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack originalStack = slot.getItem();
			ItemStack copy = originalStack.copy();

			// Custom inventory size (e.g., 9 slots)
			int containerSlotCount = 9;

			if (index < containerSlotCount) {
				// From container to player inventory
				if (!this.moveItemStackTo(originalStack, containerSlotCount, this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				// From player inventory to container
				if (!this.moveItemStackTo(originalStack, 0, containerSlotCount, false)) {
					return ItemStack.EMPTY;
				}
			}

			if (originalStack.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			return copy;
		}

		return ItemStack.EMPTY;
	}

	public static final ExtendedScreenHandlerType.ExtendedFactory<KeyRingContainer> FACTORY = (id, inv, buffer) ->
	{
		return new KeyRingContainer(id, inv.player, inv.player.getItemInHand(buffer.readEnum(InteractionHand.class)));
	};

	public static class Provider implements ExtendedScreenHandlerFactory
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
		public void writeScreenOpeningData(ServerPlayer serverPlayer, FriendlyByteBuf friendlyByteBuf) {
			friendlyByteBuf.writeEnum(InteractionHand.MAIN_HAND);
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