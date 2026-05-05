package melonslise.locks.common.util;

import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Holds shared state between StructureTemplateMixin and BlockEntityMixin.
 * Plain class — avoids Mixin's restriction that static fields in mixin
 * classes must be private.
 */
public final class LocksGenContext {

    private LocksGenContext() {}

    /**
     * Set to true by StructureTemplateMixin at the HEAD of placeInWorld(),
     * cleared at RETURN. BlockEntityMixin reads this to confirm it is being
     * called during server-side structure placement, not client chunk sync.
     * ThreadLocal so it is safe under parallel worldgen worker threads.
     */
    public static final ThreadLocal<Boolean> IS_PLACING_STRUCTURE =
            ThreadLocal.withInitial(() -> false);

    /**
     * The ServerLevelAccessor currently being used by placeInWorld() on this
     * thread. Set alongside IS_PLACING_STRUCTURE so BlockEntityMixin can use
     * it instead of BE.getLevel() (which returns null during structure placement
     * because setLevel() hasn't been called on the BE yet).
     */
    public static final ThreadLocal<ServerLevelAccessor> CURRENT_LEVEL_ACCESSOR =
            ThreadLocal.withInitial(() -> null);
}