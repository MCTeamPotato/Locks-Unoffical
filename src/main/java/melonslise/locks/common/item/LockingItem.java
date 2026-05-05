package melonslise.locks.common.item;

import melonslise.locks.Locks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LockingItem extends Item
{
	public LockingItem(Properties props)
	{
		super(props.stacksTo(1));
	}

	public static final String KEY_ID = "Id";

	public static ItemStack copyId(ItemStack from, ItemStack to)
	{
		to.getOrCreateTag().putInt(KEY_ID, getOrSetId(from));
		return to;
	}

	public static boolean isSmart(ItemStack stack) { return stack.getItem() instanceof SmartLockItem;}

	public static int getOrSetId(ItemStack stack)
	{
		CompoundTag nbt = stack.getOrCreateTag();
		if(!nbt.contains(KEY_ID))
			nbt.putInt(KEY_ID, ThreadLocalRandom.current().nextInt());
		return nbt.getInt(KEY_ID);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected)
	{
		if(!world.isClientSide)
			getOrSetId(stack);
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> lines, TooltipFlag flag)
	{
		if(stack.hasTag() && stack.getTag().contains(KEY_ID))
			lines.add(Component.translatable(Locks.ID + ".tooltip.id", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(getOrSetId(stack))).withStyle(ChatFormatting.DARK_GREEN));
	}
}