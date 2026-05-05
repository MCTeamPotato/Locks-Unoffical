package melonslise.locks.common.item;

import melonslise.locks.Locks;
import melonslise.locks.common.components.interfaces.ILockableHandler;
import melonslise.locks.common.components.interfaces.ISelection;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.init.LocksComponents;
import melonslise.locks.common.init.LocksSoundEvents;
import melonslise.locks.common.util.Cuboid6i;
import melonslise.locks.common.util.Lock;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.Transform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;

public class LockItem extends LockingItem
{
	public final int length;
	public final int enchantmentValue;
	public final int resistance;

	public LockItem(int length, int enchVal, int resist, Properties props)
	{
		super(props);
		this.length = length;
		this.enchantmentValue = enchVal;
		this.resistance = resist;
	}

	public static final String KEY_OPEN = "Open";
	public static final String KEY_LENGTH = "Length";

	public static boolean isOpen(ItemStack stack)
	{
		return stack.getOrCreateTag().getBoolean(KEY_OPEN);
	}

	public static void setOpen(ItemStack stack, boolean open)
	{
		stack.getOrCreateTag().putBoolean(KEY_OPEN, open);
	}

	// WARNING: EXPECTS LOCKITEM STACK
	public static byte getOrSetLength(ItemStack stack)
	{
		CompoundTag nbt = stack.getOrCreateTag();
		if(!nbt.contains(KEY_LENGTH))
			nbt.putByte(KEY_LENGTH, (byte) ((LockItem) stack.getItem()).length);
		return nbt.getByte(KEY_LENGTH);
	}

	// WARNING: EXPECTS LOCKITEM STACK
	public static int getResistance(ItemStack stack)
	{
		return ((LockItem) stack.getItem()).resistance;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx)
	{
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		if (!LocksServerConfig.canLock(world, pos) ||  LocksComponents.LOCKABLE_HANDLER.get(ctx.getLevel()).getInChunk(pos).values().stream().anyMatch(lkb -> lkb.bb.intersects(pos)))
			return InteractionResult.PASS;
		return LocksServerConfig.EASY_LOCK.get() ? this.easyLock(ctx) : this.freeLock(ctx);
	}

	public InteractionResult freeLock(UseOnContext ctx)
	{
		Player player = ctx.getPlayer();
		BlockPos pos = ctx.getClickedPos();
		if(player ==null)
			return InteractionResult.PASS;
        ISelection select = LocksComponents.SELECTION.get(player);
		BlockPos pos1 = select.get();
		if (pos1 == null)
			select.set(pos);
		else
		{
			Level world = ctx.getLevel();
			select.set(null);
			// FIXME Go through the add checks here as well
			world.playSound(player, pos, LocksSoundEvents.LOCK_CLOSE, SoundSource.BLOCKS, 1f, 1f);
			if (world.isClientSide)
				return InteractionResult.SUCCESS;
			ItemStack stack = ctx.getItemInHand();
			ItemStack lockStack = stack.copy();
			lockStack.setCount(1);
			ILockableHandler handler = LocksComponents.LOCKABLE_HANDLER.get(world);
			if (!handler.add(new Lockable(new Cuboid6i(pos1, pos), Lock.from(stack), Transform.fromDirection(ctx.getClickedFace(), player.getDirection().getOpposite()), lockStack, world),world))
				return InteractionResult.PASS;
			if (!player.isCreative())
				stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	public InteractionResult easyLock(UseOnContext ctx)
	{
		Player player = ctx.getPlayer();
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		world.playSound(player, pos, LocksSoundEvents.LOCK_CLOSE, SoundSource.BLOCKS, 1f, 1f);
		if(world.isClientSide) return InteractionResult.SUCCESS;
		BlockState state = world.getBlockState(pos);
		BlockPos pos1 = pos;
		if(state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) {
			pos1 = pos.relative(ChestBlock.getConnectedDirection(state));
		}
		else if(state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))
		{
			pos1 = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
			if(state.hasProperty(BlockStateProperties.DOOR_HINGE) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
			{
				Direction dir = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
				BlockPos pos2 = pos1.relative(state.getValue(BlockStateProperties.DOOR_HINGE) == DoorHingeSide.LEFT ? dir.getClockWise() : dir.getCounterClockWise());
				if(world.getBlockState(pos2).is(state.getBlock()))
					pos1 = pos2;
			}
		}
		ItemStack stack = ctx.getItemInHand();
		ItemStack lockStack = stack.copy();
		lockStack.setCount(1);
		ILockableHandler handler = LocksComponents.LOCKABLE_HANDLER.get(world);
		Lockable lockable = new Lockable(new Cuboid6i(pos, pos1), Lock.from(stack), Transform.fromDirection(ctx.getClickedFace(), player.getDirection().getOpposite()), lockStack, world);
		//Locks.LOGGER.info(lockable.bb.toString(), lockable.lock.id);
		if (!handler.add(lockable,world))
			return InteractionResult.PASS;
		if (!player.isCreative())
			stack.shrink(1);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		if(!isOpen(stack))
			return super.use(world, player, hand);
		setOpen(stack, false);
		world.playSound(player, player.getX(), player.getY(), player.getZ(), LocksSoundEvents.PIN_MATCH, SoundSource.PLAYERS, 1f, 1f);
		return super.use(world, player, hand);
	}

	@Override
	public boolean isEnchantable(ItemStack p_77616_1_)
	{
		return true;
	}

	@Override
	public int getEnchantmentValue()
	{
		return this.enchantmentValue;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> lines, TooltipFlag flag)
	{
		super.appendHoverText(stack, world, lines, flag);
		lines.add(Component.translatable(Locks.ID + ".tooltip.length", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(stack.hasTag() && stack.getTag().contains(KEY_LENGTH) ? stack.getTag().getByte(KEY_LENGTH) : this.length)).withStyle(ChatFormatting.DARK_GREEN));
	}
}