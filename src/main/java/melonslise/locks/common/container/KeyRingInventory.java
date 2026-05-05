package melonslise.locks.common.container;

import melonslise.locks.Locks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;


public class KeyRingInventory implements Container {
    private final ItemStack stack;
    private final NonNullList<ItemStack> items;

    public KeyRingInventory(ItemStack stack, int size) {
        this.stack = stack;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.loadFromNbt();
    }

    private void loadFromNbt() {
        if (stack.hasTag() && stack.getTag().contains("KeyRing")) {
            ContainerHelper.loadAllItems(stack.getTag().getCompound("KeyRing"), this.items);
        }
    }

    private void saveToNbt() {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag keyTag = new CompoundTag();
        ContainerHelper.saveAllItems(keyTag, this.items);
        tag.put("KeyRing", keyTag);
    }

    public List<ItemStack> getItems()
    {
        return Collections.<ItemStack>unmodifiableList(this.items);
    }

    /**
     * Mainly used for texture updates. Dont expect the actual amount of keys
     *
     * @param stack
     * @return
     */
    public float getKeyCount(ItemStack stack) {
        int count = 0;

        for (ItemStack s : this.items) {
            if (!s.isEmpty()) count++;
        }

        return (Math.min(count, 3) / 3f);
    }

    public void clearContent() {
        this.items.clear();
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int index) {
        return this.items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack result = ContainerHelper.removeItem(this.items, index, count);
        this.saveToNbt();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack result = ContainerHelper.takeItem(this.items, index);
        this.saveToNbt();
        return result;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.items.set(index, stack);
        this.saveToNbt();
    }

    @Override
    public void setChanged() {
        this.saveToNbt();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public void fromTag(CompoundTag tag) {
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < items.size(); i++) {
            setItem(i, items.get(i));
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items);
        return tag;
    }
}
