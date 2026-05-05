package melonslise.locks.mixin;

import melonslise.locks.Locks;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.util.ILockableProvider;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksGenContext;
import melonslise.locks.common.util.LocksUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"))
    private void onLoad(CompoundTag nbt, CallbackInfo ci) {
        if (!LocksGenContext.IS_PLACING_STRUCTURE.get()) return;
        if (!((Object)this instanceof RandomizableContainerBlockEntity))
        {
            //Locks.LOGGER.info("[BEMixin] Attempted loading a RandomizableContainerBlockEntity");
            return;
        }
        try {
            //Locks.LOGGER.info("[BEMixin] Attempted loading internal");
            onLoadInternal(nbt);
        } catch (Exception e) {
            Locks.LOGGER.error("[BEMixin] Exception during lock generation at {}: {}",
                    ((BlockEntity)(Object)this).getBlockPos(), e.getMessage());
        }
    }

    private void onLoadInternal(CompoundTag nbt) {
       /*Locks.LOGGER.info("[BEMixin] onLoadInternal pos={} hasTag={} block={}",
                ((BlockEntity)(Object)this).getBlockPos(),
                nbt.contains("LootTable"),
                LocksGenContext.CURRENT_LEVEL_ACCESSOR.get() != null ?
                        LocksGenContext.CURRENT_LEVEL_ACCESSOR.get()
                                .getBlockState(((BlockEntity)(Object)this).getBlockPos()).getBlock() : "no accessor");*/

        if (!nbt.contains("LootTable")) return;

        BlockEntity self = (BlockEntity)(Object)this;
        BlockPos pos = self.getBlockPos();

        ServerLevelAccessor levelAccessor = LocksGenContext.CURRENT_LEVEL_ACCESSOR.get();
        if (levelAccessor == null) return;

        ServerLevel serverLevel;
        try {
            serverLevel = levelAccessor.getLevel();
        } catch (Exception e) {
            return;
        }

        if (!LocksConfig.matchString(levelAccessor.getBlockState(pos).getBlock())) return;

        ResourceLocation lootTableId = new ResourceLocation(nbt.getString("LootTable"));

        // Dedup: read from chunk's ILockableProvider directly — avoids the world-level
        // Cardinal Components handler which deadlocks during worldgen
        try {
            ChunkAccess chunk = levelAccessor.getChunk(pos);
            if (((ILockableProvider) chunk).getLockables().stream()
                    .anyMatch(lkb -> lkb.bb.intersects(pos))) return;
        } catch (Exception ignored) {
            Locks.LOGGER.warn("[BEMixin] DEUP FAILED");
            // Dedup failed — proceed rather than skip
        }

        // Use lockWhenGen directly instead of lockChunk/lockCheck to bypass
        // the hasChunk() guard in lockCheck. The BE was just loaded so its chunk
        // IS accessible via levelAccessor.getChunk() even if hasChunk() returns false
        // (which happens for village chests in adjacent chunks of the WorldGenRegion).
        //Locks.LOGGER.info("[BEMixin] Beggining lockWhenGen");
        Lockable lkb = LocksUtil.lockWhenGen(levelAccessor, serverLevel, pos,
                RandomSource.create(), lootTableId);
        if (lkb == null) return;

        try {
            lkb.bb.getContainedChunks((x, z) -> {
                ((ILockableProvider) levelAccessor.getChunk(x, z)).getLockables().add(lkb);
                return true;
            });
        } catch (Exception e) {
            Locks.LOGGER.warn("[BEMixin] Failed adding lock at {}: {}", pos, e.getMessage());
        }
    }
}