package melonslise.locks.common.components.interfaces;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import melonslise.locks.common.util.Lockable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Observer;


    public interface ILockableHandler extends  Observer , Component, AutoSyncedComponent {
    int nextId();

    Int2ObjectMap<Lockable> getLoaded();

    Int2ObjectMap<Lockable> getInChunk(BlockPos pos);

    boolean add(Lockable lkb, Level level);

    boolean remove(int id);

}
