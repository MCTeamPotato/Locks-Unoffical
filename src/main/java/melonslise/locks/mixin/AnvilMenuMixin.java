package melonslise.locks.mixin;

import melonslise.locks.common.init.LocksItemTags;
import melonslise.locks.common.init.LocksItems;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.player.Player;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow private int repairItemCountCost;
    //@Shadow @Final private Container inputSlots;
    @Shadow private DataSlot cost;  // Now a DataSlot instead of int

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void addNetheriteSmartLockRecipe(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();

        if (!left.isEmpty() && !right.isEmpty()) {
            if (left.is(LocksItems.NETHERITE_LOCK) && right.is(LocksItems.INTEGRATED_CIRCUIT)) {

                ItemStack result = new ItemStack(LocksItems.NETHERITE_SMART_LOCK);

                // Copy NBT data
                if (left.hasTag()) {
                    result.setTag(left.getTag().copy());
                }

                // Copy custom name
                if (left.hasCustomHoverName()) {
                    result.setHoverName(left.getHoverName());
                }

                // Set output and costs
                menu.getSlot(2).set(result);
                this.cost.set(5);  // Use set() method for DataSlot
                this.repairItemCountCost = 1;  // Material cost

                ci.cancel();
            }
            else if (left.is(LocksItemTags.LOCKS) && right.is(LocksItemTags.LOCKS))
            {
                ItemStack result = right.copy();

                if (left.hasTag()) {
                    result.setTag(left.getTag().copy());
                }

                if (left.hasCustomHoverName()) {
                    result.setHoverName(left.getHoverName());
                }

                menu.getSlot(2).set(result);
                this.cost.set(10);  // Use set() method for DataSlot
                this.repairItemCountCost = 1;  // Material cost
                ci.cancel();
            }
        }
    }

    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void onTakeCustomRecipe(Player player, ItemStack stack, CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();

        if (left.is(LocksItemTags.LOCKS) && right.is(LocksItemTags.LOCKS)) {
            if (!player.getAbilities().instabuild) {
                player.giveExperienceLevels(-this.cost.get());
            }

            /*
            First if value causes a bug where the length of the lock changes put into an anvil
            if (!right.isEmpty() && right.getCount() > 1) {
                right.shrink(1);
                menu.getSlot(1).set(right);
            } */
            menu.getSlot(1).set(ItemStack.EMPTY);

            // DO NOT clear slot 0 (left)
            // DO NOT clear slot 1 (right) because repairItemCountCost = 0

            this.cost.set(0); // Reset cost
            ci.cancel(); // Skip vanilla logic
        }
    }
}