package melonslise.locks.mixin;

import melonslise.locks.common.capability.ILockableHandler;
import melonslise.locks.common.config.LocksCommonConfig;
import melonslise.locks.common.init.LocksCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerWorldMixin {
    @Inject(at = @At("HEAD"), method = "sendBlockUpdated")
    private void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flag, CallbackInfo ci) {
        if (LocksCommonConfig.matchString(oldState.getBlock()) && LocksCommonConfig.matchString(newState.getBlock())) return;
        if (oldState.getBlock() == newState.getBlock()) return; // property-only updates (doors opening, etc.) must keep the lock intact
        ServerLevel world = (ServerLevel) (Object) this;
        ILockableHandler handler = world.getCapability(LocksCapabilities.LOCKABLE_HANDLER).orElse(null);
        if (handler == null) return;
        // create buffer list because otherwise we will be deleting elements while iterating (BAD!!)
        handler.getInChunk(pos).values().stream().filter(lkb -> lkb.bb.intersects(pos)).toList().forEach(lkb ->
        {
            world.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.8f, 0.8f + world.random.nextFloat() * 0.4f);
            world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, lkb.stack));
            handler.remove(lkb.id);
        });
    }
}