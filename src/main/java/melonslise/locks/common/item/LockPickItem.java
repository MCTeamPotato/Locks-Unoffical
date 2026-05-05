package melonslise.locks.common.item;

import melonslise.locks.Locks;
import melonslise.locks.common.container.LockPickingContainer;
import melonslise.locks.common.init.LocksEnchantments;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksPredicates;
import melonslise.locks.common.util.LocksUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.stream.Collectors;

public class LockPickItem extends Item
{
	public static final Component TOO_COMPLEX_MESSAGE = Component.translatable(Locks.ID + ".status.too_complex");

	public final float strength;

	public LockPickItem(float strength, Properties props)
	{
		super(props);
		this.strength = strength;
	}

	public static final String KEY_STRENGTH = "Strength";

	// WARNING: EXPECTS LOCKPICKITEM STACK
	public static float getOrSetStrength(ItemStack stack)
	{
		CompoundTag nbt = stack.getOrCreateTag();
		if(!nbt.contains(KEY_STRENGTH))
			nbt.putFloat(KEY_STRENGTH, ((LockPickItem) stack.getItem()).strength);
		return nbt.getFloat(KEY_STRENGTH);
	}

	/**
	 *	Verifies if the lockpick is able to bypass the complexity due to its strength.
	 *
	 * @param stack - The lock pick
	 * @param cmp - The enchantment level of complexity within the lock
	 * @return - Whether it can lock pick or not
	 */
	public static boolean canPick(ItemStack stack, int cmp)
	{
		return (getOrSetStrength(stack) > cmp * 0.25f);
	}

	public static boolean canPick(ItemStack stack, Lockable lkb)
	{
		return canPick(stack, EnchantmentHelper.getItemEnchantmentLevel(LocksEnchantments.COMPLEXITY, lkb.stack));
	}

	/**
	 * Should start lock picking if a lock is found.
	 *
	 * @param ctx - Context to the action done by the player
	 * @return - Consequences from the action
	 */
	@Override
	public InteractionResult useOn(UseOnContext ctx)
	{
		//Defines player, world, position of interacted block, and a list of possible locks interacted with.
		Level world = ctx.getLevel();
		Player player = ctx.getPlayer();
		BlockPos pos = ctx.getClickedPos();
		List<Lockable> match = LocksUtil.intersecting(world, pos).filter(LocksPredicates.LOCKED).collect(Collectors.toList());

		//If no locks are in the matching position or if it is a smart lock, it ignores. Otherwise it verifies if the lock has the complexity enchantment and carries out its effect.
		if(match.isEmpty() || match.get(0).isSmart())
			return InteractionResult.PASS;
		Lockable lkb = match.get(0);
		if(!canPick(ctx.getItemInHand(), lkb))
		{
			Locks.LOGGER.warn(!canPick(ctx.getItemInHand(), lkb));
			if(world.isClientSide) player.displayClientMessage(TOO_COMPLEX_MESSAGE, true);
			return InteractionResult.PASS;
		}
		if(world.isClientSide) return InteractionResult.SUCCESS;

		//It opens the lock picking minigame.
		InteractionHand hand = ctx.getHand();
		if(player instanceof ServerPlayer && !match.get(0).isSmart()) {
			player.openMenu(new LockPickingContainer.Provider(hand, lkb));
		}
		return InteractionResult.SUCCESS;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> lines, TooltipFlag flag)
	{
		super.appendHoverText(stack, world, lines, flag);
		lines.add(Component.translatable(Locks.ID + ".tooltip.strength", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(stack.hasTag() && stack.getTag().contains(KEY_STRENGTH) ? stack.getTag().getFloat(KEY_STRENGTH) : this.strength)).withStyle(ChatFormatting.DARK_GREEN));
	}
}