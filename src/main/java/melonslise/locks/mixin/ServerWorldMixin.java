package melonslise.locks.mixin;

import melonslise.locks.common.components.interfaces.ILockableHandler;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.init.LocksComponents;
import melonslise.locks.common.util.Cuboid6i;
import melonslise.locks.common.util.Lockable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerLevel.class)
public class ServerWorldMixin
{
	@Inject(at = @At("HEAD"), method = "sendBlockUpdated")
	private void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flag, CallbackInfo ci)
	{
		ServerLevel world = (ServerLevel) (Object) this;
		ILockableHandler handler = LocksComponents.LOCKABLE_HANDLER.get(world);
		if (LocksConfig.matchString(oldState.getBlock()) && LocksConfig.matchString(newState.getBlock())) {
			if (oldState.hasProperty(BlockStateProperties.CHEST_TYPE)
					&& newState.hasProperty(BlockStateProperties.CHEST_TYPE)
					&& oldState.getValue(BlockStateProperties.CHEST_TYPE) != newState.getValue(BlockStateProperties.CHEST_TYPE)) {
				ChestType newType = newState.getValue(BlockStateProperties.CHEST_TYPE);
				BlockPos otherPos = newType == ChestType.SINGLE ? pos : pos.relative(ChestBlock.getConnectedDirection(newState));
				Cuboid6i newBb = new Cuboid6i(pos, otherPos);
				handler.getInChunk(pos).values().stream()
						.filter(lkb -> lkb.bb.intersects(pos))
						.filter(lkb -> !lkb.lock.isLocked())
						.filter(lkb -> lkb.bb.volume() <= 2)
						.filter(lkb -> isChestOnly(world, lkb))
						.filter(lkb -> !lkb.bb.equals(newBb))
						.toList()
						.forEach(lkb -> updateChestLockable(world, handler, lkb, newBb));
			}
			return;
		}
		// create buffer list because otherwise we will be deleting elements while iterating (BAD!!)
		handler.getInChunk(pos).values().stream().filter(lkb -> lkb.bb.intersects(pos)).toList().forEach(lkb ->
		{
			world.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8f, 0.8f + world.random.nextFloat() * 0.4f);
			world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, lkb.stack));
			handler.remove(lkb.id);
		});




	}

	private static boolean isChestOnly(ServerLevel world, Lockable lkb)
	{
		for (BlockPos bbPos : lkb.bb.getContainedPos())
			if (!(world.getBlockState(bbPos).getBlock() instanceof ChestBlock))
				return false;
		return true;
	}

	private static void updateChestLockable(ServerLevel world, ILockableHandler handler, Lockable lkb, Cuboid6i newBb)
	{
		lkb.lock.deleteObserver(lkb);
		handler.remove(lkb.id);
		Lockable moved = new Lockable(newBb, lkb.lock, lkb.tr, lkb.stack, lkb.id);
		handler.add(moved, world);
	}
}
