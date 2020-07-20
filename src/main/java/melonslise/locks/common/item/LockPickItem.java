package melonslise.locks.common.item;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import melonslise.locks.Locks;
import melonslise.locks.common.container.LockPickingContainer;
import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.util.Lockable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;

public class LockPickItem extends Item
{
	public LockPickItem(Properties props)
	{
		super(props);
	}

	public static final float DEFAULT_STRENGTH = 0.3f;

	public static final String KEY_STRENGTH = "Strength";

	public static float getOrSetStrength(ItemStack stack)
	{
		CompoundNBT nbt = stack.getOrCreateTag();
		if(!nbt.contains(KEY_STRENGTH))
			nbt.putFloat(KEY_STRENGTH, DEFAULT_STRENGTH);
		return nbt.getFloat(KEY_STRENGTH);
	}

	@Override
	public ActionResultType onItemUse(ItemUseContext ctx)
	{
		World world = ctx.getWorld();
		BlockPos pos = ctx.getPos();
		return world.getCapability(LocksCapabilities.LOCKABLES)
			.map(lockables ->
			{
				List<Lockable> matching = lockables.get().values().stream().filter(lockable1 -> lockable1.lock.isLocked() && lockable1.box.intersects(pos)).collect(Collectors.toList());
				if(matching.isEmpty())
					return ActionResultType.PASS;
				if(world.isRemote)
					return ActionResultType.SUCCESS;
				NetworkHooks.openGui((ServerPlayerEntity) ctx.getPlayer(), new LockPickingContainer.Provider(pos, matching.get(0)), new LockPickingContainer.Writer(pos, matching.get(0)));
				return ActionResultType.SUCCESS;
			})
			.orElse(ActionResultType.PASS);
	}

	@Override
	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items)
	{
		if(!this.isInGroup(group))
			return;
		ItemStack stack = new ItemStack(this);
		getOrSetStrength(stack);
		items.add(stack);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> lines, ITooltipFlag flag)
	{
		if(stack.hasTag() && stack.getTag().contains(KEY_STRENGTH))
			lines.add(new TranslationTextComponent(Locks.ID + ".tooltip.strength", ItemStack.DECIMALFORMAT.format(getOrSetStrength(stack))).applyTextStyle(TextFormatting.DARK_GREEN));
	}
}