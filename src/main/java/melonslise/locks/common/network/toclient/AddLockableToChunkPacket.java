package melonslise.locks.common.network.toclient;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import melonslise.locks.Locks;
import melonslise.locks.common.components.interfaces.ILockableHandler;
import melonslise.locks.common.components.interfaces.ILockableStorage;
import melonslise.locks.common.init.LocksComponents;
import melonslise.locks.common.util.Lockable;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;

/*
    Adds lockable to a chunk.
 */
public class AddLockableToChunkPacket implements FabricPacket {
    public static final ResourceLocation ID = new ResourceLocation(Locks.ID, "add_lockable2chunk");
    private final Lockable lockable;
    private final int x, z;

    public static final PacketType<AddLockableToChunkPacket> TYPE = PacketType.create(ID, AddLockableToChunkPacket::new);

    //  Generates a lock into a chunk.
    public static class Handler implements ClientPlayNetworking.PlayPacketHandler<AddLockableToChunkPacket>
    {
        @Override
        public void receive(AddLockableToChunkPacket pkt, LocalPlayer localPlayer, PacketSender packetSender)
        {
            Level level = localPlayer.level();

            if(level.getChunk(pkt.x, pkt.z) instanceof EmptyLevelChunk) return;

            //  Gets storage units within the chunk.
            ILockableStorage st = LocksComponents.LOCKABLE_STORAGE.get(level.getChunk(pkt.x, pkt.z));
            ILockableHandler handler = LocksComponents.LOCKABLE_HANDLER.get(level);
            Int2ObjectMap<Lockable> lkbs = handler.getLoaded();
            Lockable lkb = lkbs.get(pkt.lockable.id);
            if (lkb == lkbs.defaultReturnValue()) {
                lkb = pkt.lockable;
                lkb.addObserver(handler);
                lkbs.put(lkb.id, lkb);
            }
            st.add(lkb);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        Lockable.toBuf(buf, this.lockable);
        buf.writeInt(this.x);
        buf.writeInt(this.z);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }


    public AddLockableToChunkPacket(Lockable lkb, int x, int z) {
        this.lockable = lkb;
        this.x = x;
        this.z = z;
    }

    public AddLockableToChunkPacket(Lockable lkb, ChunkPos pos) {
        this(lkb, pos.x, pos.z);
    }

    public AddLockableToChunkPacket(Lockable lkb, LevelChunk ch) {
        this(lkb, ch.getPos());
    }

    public AddLockableToChunkPacket(FriendlyByteBuf buf) {
        this(Lockable.fromBuf(buf), buf.readInt(), buf.readInt());
    }


}