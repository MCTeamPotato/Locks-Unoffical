package melonslise.locks.common.components;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import melonslise.locks.Locks;
import melonslise.locks.common.components.interfaces.ILockableHandler;
import melonslise.locks.common.components.interfaces.ILockableStorage;
import melonslise.locks.common.init.LocksComponents;
import melonslise.locks.common.util.Lockable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

//Shit is required to save the world file.
import net.minecraft.nbt.Tag;

import java.util.Objects;

/*
 * Internal storage for lockables with almost no handling logic
 * Also stores lockables which are shared by multiple chunks. Duplicate shared lockables are handled by checking if they have already been loaded before
 */
public class LockableStorage implements ILockableStorage
{
	public static final ResourceLocation ID = new ResourceLocation(Locks.ID, "lockable_storage");

	public final ChunkAccess chunk;

	public Int2ObjectMap<Lockable> lockables = new Int2ObjectLinkedOpenHashMap<Lockable>();

	public LockableStorage(ChunkAccess chunk)
	{
		this.chunk = chunk;
	}

	@Override
	public Int2ObjectMap<Lockable> get()
	{
		return this.lockables;
	}

	@Override
	public void add(Lockable lkb)
	{
		this.lockables.put(lkb.id, lkb);
		this.chunk.setUnsaved(true);
	}

	@Override
	public void remove(int id)
	{
		this.lockables.remove(id);
		this.chunk.setUnsaved(true);
	}

	@Override
	public void readFromNbt(CompoundTag nbt) {
		int size = nbt.getInt("LockablesSize"); //Not really used anymore since it didn't save locks into worldfile
		ListTag lockables = nbt.getList("Lockables", Tag.TAG_COMPOUND);
		ILockableHandler handler;
		if(this.chunk instanceof LevelChunk levelChunk){
			handler =LocksComponents.LOCKABLE_HANDLER.get(levelChunk.getLevel());
		}else {
			BlockPos blockPos = this.chunk.getBlockEntitiesPos().stream().findFirst().get();
			handler = LocksComponents.LOCKABLE_HANDLER.get(Objects.requireNonNull(Objects.requireNonNull(this.chunk.getBlockEntity(blockPos)).getLevel()).getChunkAt(blockPos));
		}
		Int2ObjectMap<Lockable> lkbs = handler.getLoaded();
		for(int a = 0; a < lockables.size(); ++a)
		{
			CompoundTag nbt1 = lockables.getCompound(a);
			Lockable lkb = lkbs.get(Lockable.idFromNbt(nbt1));
			if(lkb == lkbs.defaultReturnValue())
			{
				if (lkb==null) return;
				lkb = Lockable.fromNbt(nbt1);
				lkb.addObserver(handler);
				lkbs.put(lkb.id, lkb);
			}
			this.lockables.put(lkb.id, lkb);
		}

	}

	@Override
	public void writeToNbt(CompoundTag compoundTag) {
		ListTag list = new ListTag();
		for(Lockable lkb : this.lockables.values())
			list.add(Lockable.toNbt(lkb));
		compoundTag.put("Lockables", list);
		compoundTag.putInt("LockablesSize", this.lockables.size());
	}
}
