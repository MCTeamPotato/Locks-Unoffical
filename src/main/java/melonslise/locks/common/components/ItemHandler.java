package melonslise.locks.common.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import melonslise.locks.common.components.interfaces.IItemHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.List;

public record ItemHandler(List<ItemStack> items) implements IItemHandler {


    public static final Codec<ItemHandler> CODEC = RecordCodecBuilder.create(itemHandlerInstance ->
            itemHandlerInstance.group(
                    Codec.list(ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter(ItemHandler::items)
            ).apply(itemHandlerInstance, ItemHandler::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemHandler> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, ItemHandler::items,
            ItemHandler::new
    );

    public ItemHandler() {
        this(NonNullList.withSize(0, ItemStack.EMPTY));
    }

    public ItemHandler {
        NonNullList<ItemStack> mutable = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            mutable.set(i, items.get(i));
        }
        items = mutable;
    }

    @Override
    public void readFromNbt(CompoundTag compoundTag, HolderLookup.Provider provider) {
        this.items.clear();
        int i = compoundTag.getInt("size");
        this.items.addAll(NonNullList.withSize(i, ItemStack.EMPTY));
        NonNullList<ItemStack> list = NonNullList.create();
        list.addAll(this.items);
        ContainerHelper.loadAllItems(compoundTag, list,provider);
    }

    @Override
    public void writeToNbt(CompoundTag compoundTag, HolderLookup.Provider provider) {
        compoundTag.putInt("size", this.items.size());
        NonNullList<ItemStack> list = NonNullList.create();
        list.addAll(this.items);
        ContainerHelper.saveAllItems(compoundTag, list,provider);
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    @Override
    public ItemStack getItem(int i) {
        return this.items.get(i);
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        ItemStack result = ContainerHelper.removeItem(this.items, i, j);
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ContainerHelper.takeItem(this.items, i);
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.items.set(i, itemStack);
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public List<ItemStack> getItems() {
        return this.items;
    }

    @Override
    public void setItems(List<ItemStack> items) {
        this.items.clear();
        this.items.addAll(NonNullList.withSize(items.size(), ItemStack.EMPTY));
        Iterator<ItemStack> iterator = items.iterator();
        for (int i = 0; i < items.size(); i++) {
            this.items.set(i, iterator.next());
        }
    }

    @Override
    public int getSlots() {
        return this.items.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.items.get(slot);
    }
}
