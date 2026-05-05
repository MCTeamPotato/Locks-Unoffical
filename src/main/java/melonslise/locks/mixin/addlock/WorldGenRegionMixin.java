package melonslise.locks.mixin.addlock;

import melonslise.locks.common.util.LocksUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {
   /* @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "setBlock", at = @At(value = "RETURN", ordinal = 1))
    public void lockBlock(BlockPos blockPos, BlockState blockState, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = this.level;
        RandomSource randomSource = RandomSource.create();
        ChunkAccess chunkAccess = ((WorldGenRegion)(Object)this).getChunk(blockPos);
        LocksUtil.lockChunk((LevelAccessor) this, level, blockPos, randomSource, chunkAccess );
    }*/
}