package melonslise.locks.common.util;

import melonslise.locks.Locks;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.init.LocksComponents;
import melonslise.locks.common.network.toclient.AddLockablePacket;
import melonslise.locks.mixin.accessor.LootPoolAccessor;
import melonslise.locks.mixin.accessor.LootTableAccessor;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.*;
import static net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT;
import static net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER;

public final class LocksUtil {
    public static ResourceManager resourceManager;

    private LocksUtil() {
    }

    public static void shuffle(byte[] array, Random rng) {
        for (int a = array.length - 1; a > 0; --a) {
            int index = rng.nextInt(a + 1);
            byte temp = array[index];
            array[index] = array[a];
            array[a] = temp;
        }
    }

    public static boolean chance(RandomSource rng, double ch) {
        return ch == 1d || ch != 0d && rng.nextDouble() <= ch;
    }

    public static BlockPos transform(int x, int y, int z, StructurePlaceSettings settings) {
        switch (settings.getMirror()) {
            case LEFT_RIGHT:
                z = -z + 1;
                break;
            case FRONT_BACK:
                x = -x + 1;
                break;
            default:
                break;
        }
        int x1 = settings.getRotationPivot().getX();
        int z1 = settings.getRotationPivot().getZ();
        switch (settings.getRotation()) {
            case COUNTERCLOCKWISE_90:
                return new BlockPos(x1 - z1 + z, y, x1 + z1 - x + 1);
            case CLOCKWISE_90:
                return new BlockPos(x1 + z1 - z + 1, y, z1 - x1 + x);
            case CLOCKWISE_180:
                return new BlockPos(x1 + x1 - x + 1, y, z1 + z1 - z + 1);
            default:
                return new BlockPos(x, y, z);
        }
    }

    public static AttachFace faceFromDir(Direction dir) {
        return dir == Direction.UP ? AttachFace.CEILING : dir == Direction.DOWN ? AttachFace.FLOOR : AttachFace.WALL;
    }

    public static AABB rotateY(AABB bb) {
        return new AABB(bb.minZ, bb.minY, bb.minX, bb.maxZ, bb.maxY, bb.maxX);
    }

    public static AABB rotateX(AABB bb) {
        return new AABB(bb.minX, bb.minZ, bb.minY, bb.maxX, bb.maxZ, bb.maxY);
    }

    public static boolean intersectsInclusive(AABB bb1, AABB bb2) {
        return bb1.minX <= bb2.maxX && bb1.maxX >= bb2.minX && bb1.minY <= bb2.maxY && bb1.maxY >= bb2.minY && bb1.minZ <= bb2.maxZ && bb1.maxZ >= bb2.minZ;
    }

    public static Vec3 sideCenter(AABB bb, Direction side) {
        Vec3i dir = side.getNormal();
        return new Vec3((bb.minX + bb.maxX + (bb.maxX - bb.minX) * dir.getX()) * 0.5d, (bb.minY + bb.maxY + (bb.maxY - bb.minY) * dir.getY()) * 0.5d, (bb.minZ + bb.maxZ + (bb.maxZ - bb.minZ) * dir.getZ()) * 0.5d);
    }

    // Only merges entries, not conditions and functions
    public static LootTable mergeEntries(LootTable table, LootTable inject) {
        List<LootPool> list = Arrays.asList(((LootTableAccessor) table).getPools());
        for (LootPool injectPool : ((LootTableAccessor) inject).getPools()) {
            if (list.contains(injectPool)) {
                ((LootPoolAccessor) injectPool).getEntries().addAll(((LootPoolAccessor) injectPool).getEntries());
            } else {
                list.add(injectPool);
            }
        }
        ((LootTableAccessor) inject).setPools(list.toArray(new LootPool[0]));
        return table;
    }

    public static Stream<Lockable> intersecting(Level world, BlockPos pos) {
        return LocksComponents.LOCKABLE_HANDLER.get(world).getInChunk(pos).values().stream().filter(
                lkb -> lkb.bb.intersects(pos)
        );
    }

    public static boolean lockedAndRelated(Level world, BlockPos pos) {
        BlockPos above = pos.above();
        Block aboveBlock = world.getBlockState(above).getBlock();
        boolean checkAbove = LocksUtil.locked(world, above) && aboveBlock instanceof DoorBlock;
        return locked(world, pos) || checkAbove;
    }

    public static boolean locked(Level world, BlockPos pos) {
        return intersecting(world, pos).anyMatch(LocksPredicates.LOCKED);
    }

    // ── Lock geometry resolver ────────────────────────────────────────────────
    //
    // Used by both lockWhenGen overloads. Resolves the final BlockPos and
    // Direction for a lockable block. Returns null if the block is not lockable.
    // Returns Object[]{ BlockPos pos1, Direction dir } on success.

    private static Object[] resolveGeometry(LevelAccessor levelAccessor, BlockPos blockPos) {
        BlockState state = levelAccessor.getBlockState(blockPos);
        BlockPos pos1 = blockPos;
        Direction dir = null;

        if (state.hasProperty(FACING)) {
            dir = state.getValue(FACING);
        } else if (state.hasProperty(HORIZONTAL_FACING)) {
            dir = state.getValue(HORIZONTAL_FACING);
        } else {
            return null;
        }

        if (state.hasProperty(CHEST_TYPE)) {
            switch (state.getValue(CHEST_TYPE)) {
                case LEFT -> pos1 = blockPos.relative(ChestBlock.getConnectedDirection(state));
                case RIGHT -> { return null; }
            }
        }

        if (state.hasProperty(DOUBLE_BLOCK_HALF)) {
            if (state.getValue(DOUBLE_BLOCK_HALF) == LOWER) return null;
            pos1 = blockPos.below();
            if (state.hasProperty(DOOR_HINGE)) {
                if (state.hasProperty(DOOR_HINGE) && state.hasProperty(HORIZONTAL_FACING)) {
                    BlockPos pos2 = pos1.relative(state.getValue(DOOR_HINGE) == LEFT
                            ? dir.getClockWise() : dir.getCounterClockWise());
                    if (levelAccessor.getBlockState(pos2).is(state.getBlock())) {
                        if (state.getValue(DOOR_HINGE) == LEFT) return null;
                        pos1 = pos2;
                    }
                }
                dir = dir.getOpposite();
            }
        }

        return new Object[]{ pos1, dir };
    }

    // ── lockWhenGen ───────────────────────────────────────────────────────────

    /**
     * Loot-table-aware lock generation.
     *
     * LocksConfig.rollLock() handles the full fallback chain:
     *   specific table entry -> "default" entry -> global pool
     */
    public static Lockable lockWhenGen(LevelAccessor levelAccessor, ServerLevel level,
                                       BlockPos blockPos, RandomSource randomSource,
                                       ResourceLocation lootTableId) {
        BlockState state = levelAccessor.getBlockState(blockPos);
        Block block = state.getBlock();
        if (!LocksConfig.matchString(block))
        {
            //Locks.LOGGER.info("[LocksUtil] Block '{}' was not in the config list", block);
            return null;
        }

        //Locks.LOGGER.info("[LocksUtil] Generating geo");
        Object[] geo = resolveGeometry(levelAccessor, blockPos);
        if (geo == null) return null;
        BlockPos  pos1 = (BlockPos)  geo[0];
        Direction dir  = (Direction) geo[1];

        //Locks.LOGGER.info("[LocksUtil] Generating stack");
        ItemStack stack = LocksConfig.rollLock(randomSource, lootTableId);
        if (stack == null)
        {
            //Locks.LOGGER.info("[LocksUtil] Not generating lock! lockWhenGen stack=null at {} for {}", blockPos, lootTableId);
            return null;
        }

        //Locks.LOGGER.info("[LocksUtil] Generating lockable");
        Cuboid6i  bb   = new Cuboid6i(blockPos, pos1);
        Lock      lock = Lock.from(stack);
        Transform tr   = Transform.fromDirection(dir, dir);
        return new Lockable(bb, lock, tr, stack, level);
    }

    /**
     * No-loot-table version. Used by WorldGenRegionMixin for feature-placed
     * chests. Passes null to rollLock() so it hits the "default" entry in
     * locks-generation.toml rather than reading GENERATED_LOCKS directly.
     */
    public static Lockable lockWhenGen(LevelAccessor levelAccessor, ServerLevel level,
                                       BlockPos blockPos, RandomSource randomSource) {
        BlockState state = levelAccessor.getBlockState(blockPos);
        Block block = state.getBlock();
        if (!LocksConfig.matchString(block)) return null;

        Object[] geo = resolveGeometry(levelAccessor, blockPos);
        /*Locks.LOGGER.info("[LocksUtil] lockWhenGen pos={} block={} geo={}", blockPos,
                BuiltInRegistries.BLOCK.getKey(block), geo == null ? "NULL" : "OK");*/
        if (geo == null) return null;
        BlockPos  pos1 = (BlockPos)  geo[0];
        Direction dir  = (Direction) geo[1];

        // Pass null — rollLock will use "default" entry, then global fallback
        //Locks.LOGGER.info("[LocksUtil] Generating stack");
        ItemStack stack = LocksConfig.rollLock(randomSource, null);
        if (stack == null) return null;

        //Locks.LOGGER.info("[LocksUtil] Returning lockable");
        Cuboid6i  bb   = new Cuboid6i(blockPos, pos1);
        Lock      lock = Lock.from(stack);
        Transform tr   = Transform.fromDirection(dir, dir);
        return new Lockable(bb, lock, tr, stack, level);
    }

    // ── lockCheck ─────────────────────────────────────────────────────────────

    public static Lockable lockCheck(LevelAccessor levelAccessor, ServerLevel level,
                                     BlockPos blockPos, RandomSource randomSource,
                                     ResourceLocation lootTableId) {
        if (!levelAccessor.hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4)) return null;
        return lockWhenGen(levelAccessor, level, blockPos, randomSource, lootTableId);
    }

    public static Lockable lockCheck(LevelAccessor levelAccessor, ServerLevel level,
                                     BlockPos blockPos, RandomSource randomSource) {
        if (!levelAccessor.hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4)) return null;
        return lockWhenGen(levelAccessor, level, blockPos, randomSource);
    }

    // ── lockChunk ─────────────────────────────────────────────────────────────

    /**
     * Loot-table-aware version. Called by BlockEntityMixin.
     * Adds the lock to whichever chunks the lockable's bounding box spans.
     */
    public static boolean lockChunk(LevelAccessor levelAccessor, ServerLevel level,
                                    BlockPos blockPos, RandomSource randomSource,
                                    ResourceLocation lootTableId) {
        Lockable lkb = lockCheck(levelAccessor, level, blockPos, randomSource, lootTableId);
        if (lkb == null) return false;
        lkb.bb.getContainedChunks((x, z) -> {
            ((ILockableProvider) levelAccessor.getChunk(x, z)).getLockables().add(lkb);
            return true;
        });
        return true;
    }

    /** Called by WorldGenRegionMixin — no loot table context, uses global fallback. */
    public static boolean lockChunk(LevelAccessor levelAccessor, ServerLevel level,
                                    BlockPos blockPos, RandomSource randomSource,
                                    ChunkAccess chunkAccess) {
        Lockable lkb = lockCheck(levelAccessor, level, blockPos, randomSource);
        if (lkb == null) return false;
        ((ILockableProvider) chunkAccess).getLockables().add(lkb);
        return true;
    }

    /** No-ChunkAccess variant — used by StructurePieceMixin and other callers. */
    public static boolean lockChunk(LevelAccessor levelAccessor, ServerLevel level,
                                    BlockPos blockPos, RandomSource randomSource) {
        Lockable lkb = lockCheck(levelAccessor, level, blockPos, randomSource);
        if (lkb == null) return false;
        lkb.bb.getContainedChunks((x, z) -> {
            ((ILockableProvider) levelAccessor.getChunk(x, z)).getLockables().add(lkb);
            return true;
        });
        return true;
    }
}