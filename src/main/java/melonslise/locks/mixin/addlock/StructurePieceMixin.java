package melonslise.locks.mixin.addlock;

import melonslise.locks.Locks;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.util.ILockableProvider;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin {

    /**
     * Injects after StructurePiece.createChest() returns successfully.
     *
     * This handles all structures that use StructurePiece.createChest() to
     * place their loot chests — stronghold, shipwreck, buried treasure,
     * woodland mansion, jungle temple, desert temple, nether fortress, end city
     * and others. These structures do NOT go through StructureTemplate.placeInWorld()
     * so the BlockEntityMixin/IS_PLACING_STRUCTURE path never fires for them.
     *
     * createChest() has the ResourceLocation loot table id as a direct parameter,
     * which is exactly what we need — no NBT reading required.
     *
     * We use ordinal=0 (the false/early return) and ordinal=1 (the true/success
     * return) — we only want to add a lock when the chest was actually placed,
     * so we inject at ordinal=1 (return true).
     */
    @Inject(
            method = "createChest(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At(value = "RETURN", ordinal = 1)
    )
    public void onCreateChest(ServerLevelAccessor levelAccessor, BoundingBox boundingBox,
                              RandomSource randomSource, BlockPos blockPos,
                              ResourceLocation lootTable, BlockState blockState,
                              CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!LocksConfig.matchString(levelAccessor.getBlockState(blockPos).getBlock())) return;

            ServerLevel serverLevel = levelAccessor.getLevel();

            Lockable lkb = LocksUtil.lockWhenGen(levelAccessor, serverLevel, blockPos,
                    RandomSource.create(), lootTable);
            if (lkb == null) return;

            lkb.bb.getContainedChunks((x, z) -> {
                ((ILockableProvider) levelAccessor.getChunk(x, z)).getLockables().add(lkb);
                return true;
            });
        } catch (Exception e) {
            Locks.LOGGER.error("[StructurePieceMixin] Exception at {}: {}", blockPos, e.getMessage());
        }
    }
}