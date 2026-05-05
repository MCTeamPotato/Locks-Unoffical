package melonslise.locks.mixin.addlock;

import melonslise.locks.Locks;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.util.ILockableProvider;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksUtil;
import melonslise.locks.mixin.accessor.RandomizableContainerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TemplateStructurePiece.class)
public abstract class TemplateStructurePieceMixin {

    @Shadow protected BlockPos templatePosition;

    /**
     * Injects at the TAIL of postProcess() — after placeInWorld() and all
     * handleDataMarker() calls have completed, including any other mod's
     * injections (e.g. Lootr). This avoids any redirect that would break
     * other mods by intercepting the handleDataMarker call site.
     *
     * We scan the full bounding box for RandomizableContainerBlockEntities
     * that have a loot table assigned. For each one that passes
     * LocksConfig.matchString(), we generate a lock via lockWhenGen().
     *
     * The dedup check on the chunk's lockable list prevents double-generation
     * if both this mixin and RCBEMixin fire for the same chest.
     */
    @Inject(
            method = "postProcess(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/core/BlockPos;)V",
            at = @At("TAIL")
    )
    private void onPostProcessTail(WorldGenLevel worldGenLevel, StructureManager structureManager,
                                   ChunkGenerator chunkGenerator, RandomSource random,
                                   BoundingBox boundingBox, ChunkPos chunkPos, BlockPos blockPos,
                                   CallbackInfo ci) {
        try {
            ServerLevelAccessor levelAccessor = worldGenLevel;

            ServerLevel serverLevel;
            try {
                serverLevel = levelAccessor.getLevel();
            } catch (Exception e) {
                return;
            }

            // Walk every block position in this piece's bounding box
            BoundingBox bb = ((TemplateStructurePiece)(Object)this).getBoundingBox();
            for (int x = bb.minX(); x <= bb.maxX(); x++) {
                for (int y = bb.minY(); y <= bb.maxY(); y++) {
                    for (int z = bb.minZ(); z <= bb.maxZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);

                        if (!LocksConfig.matchString(worldGenLevel.getBlockState(pos).getBlock())) continue;

                        BlockEntity be = worldGenLevel.getBlockEntity(pos);
                        if (!(be instanceof RandomizableContainerBlockEntity)) continue;

                        ResourceLocation lootTable = ((RandomizableContainerAccessor) be).getLootTable();
                        if (lootTable == null) continue;

                        // Dedup: skip if a lock already exists at this position
                        try {
                            ChunkAccess chunk = levelAccessor.getChunk(pos);
                            if (((ILockableProvider) chunk).getLockables().stream()
                                    .anyMatch(lkb -> lkb.bb.intersects(pos))) continue;
                        } catch (Exception ignored) {}

                        Lockable lkb = LocksUtil.lockWhenGen(levelAccessor, serverLevel, pos,
                                random, lootTable);
                        if (lkb == null) continue;

                        lkb.bb.getContainedChunks((cx, cz) -> {
                            ((ILockableProvider) levelAccessor.getChunk(cx, cz)).getLockables().add(lkb);
                            return true;
                        });
                    }
                }
            }
        } catch (Exception e) {
            Locks.LOGGER.error("[TemplateStructurePieceMixin] Exception in onPostProcessTail: {}", e.getMessage());
        }
    }
}