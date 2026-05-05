package melonslise.locks.mixin;

import melonslise.locks.Locks;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.util.ILockableProvider;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksGenContext;
import melonslise.locks.common.util.LocksUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("DataFlowIssue")
@Mixin(RandomizableContainerBlockEntity.class)
public class RandomizableContainerBlockEntityMixin {

    /**
     * setLootTable in 1.20.1 Mojmap is a static utility method:
     *   setLootTable(BlockGetter world, RandomSource rng, BlockPos pos, ResourceLocation lootTable)
     *
     * Called by IglooPiece and OceanRuinPiece (and others) after placeInWorld()
     * to assign a loot table directly on the block entity — bypassing both
     * BlockEntityMixin (no LootTable NBT tag) and StructurePieceMixin (no createChest()).
     *
     * Worldgen context is detected by checking if the level is a WorldGenRegion
     * OR if IS_PLACING_STRUCTURE is set (for StructureTemplate-based structures).
     */
    @Inject(
            method = "setLootTable(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceLocation;)V",
            at = @At("HEAD")
    )
    private static void onSetLootTable(BlockGetter pLevel, RandomSource pRandom,
                                       BlockPos pPos, ResourceLocation pLootTable,
                                       CallbackInfo ci) {
        /*Locks.LOGGER.info("[RCBEMixin] setLootTable fired: level={} pos={} lootTable={}",
                pLevel.getClass().getSimpleName(), pPos, pLootTable);*/
        try {
            if (!(pLevel instanceof ServerLevelAccessor levelAccessor)) return;

            // Only act during worldgen
            boolean isGenerating = levelAccessor instanceof net.minecraft.server.level.WorldGenRegion
                    || LocksGenContext.IS_PLACING_STRUCTURE.get();
            if (!isGenerating) return;

            ServerLevel serverLevel;
            try {
                serverLevel = levelAccessor.getLevel();
            } catch (Exception e) {
                return;
            }

            if (!LocksConfig.matchString(levelAccessor.getBlockState(pPos).getBlock())) return;

            // Dedup: check chunk-level lockables directly to avoid Cardinal Components deadlock
            try {
                ChunkAccess chunk = levelAccessor.getChunk(pPos);
                if (((ILockableProvider) chunk).getLockables().stream()
                        .anyMatch(lkb -> lkb.bb.intersects(pPos))) return;
            } catch (Exception ignored) {}

            Lockable lkb = LocksUtil.lockWhenGen(levelAccessor, serverLevel, pPos,
                    RandomSource.create(), pLootTable);
            if (lkb == null) return;

            lkb.bb.getContainedChunks((x, z) -> {
                ((ILockableProvider) levelAccessor.getChunk(x, z)).getLockables().add(lkb);
                return true;
            });
        } catch (Exception e) {
            Locks.LOGGER.error("[RCBEMixin] Exception in onSetLootTable at {}: {}", pPos, e.getMessage());
        }
    }

    @Inject(method = "getItem(I)Lnet/minecraft/world/item/ItemStack;", at = @At(value = "RETURN"), cancellable = true)
    private void lockRandomizableContainerBlockEntity(int slot, CallbackInfoReturnable<ItemStack> cir) {
        BlockPos pos = ((BaseContainerBlockEntity)(Object)this).getBlockPos();
        Level level  = ((BaseContainerBlockEntity)(Object)this).getLevel();
        if (level == null) {
            Locks.LOGGER.warn("Tried to access RandomizableContainerBlockEntity before level was initialized.");
            return;
        }
        if (level.isClientSide) return;
        if (LocksUtil.locked(level, pos)) {
            cir.setReturnValue(Items.AIR.getDefaultInstance());
        }
    }
}