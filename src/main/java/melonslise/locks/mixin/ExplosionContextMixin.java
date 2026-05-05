package melonslise.locks.mixin;

import melonslise.locks.common.util.LocksUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Set;

@Mixin(Explosion.class)
public class ExplosionContextMixin
{
	@Shadow @Final private Level level;

	@Inject(at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z", shift = At.Shift.AFTER), method = "explode", locals = LocalCapture.CAPTURE_FAILSOFT)
	private void removeBlockSet(CallbackInfo ci, Set<BlockPos> set, int i, int j, int k, int l, double d, double e, double f, double g, float h, double m, double n, double o, float p, BlockPos blockPos, BlockState blockState)
	{
		if (LocksUtil.lockedAndRelated(this.level, blockPos)) {
			set.remove(blockPos);
		}
	}
}