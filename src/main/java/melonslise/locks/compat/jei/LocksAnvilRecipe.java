package melonslise.locks.compat.jei;

import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class LocksAnvilRecipe implements IJeiAnvilRecipe {
    private final List<ItemStack> leftInputs;
    private final List<ItemStack> rightInputs;
    private final List<ItemStack> outputs;
    private final ResourceLocation UID;

    public ResourceLocation getUid() {
        return UID;
    }

    public LocksAnvilRecipe(List<ItemStack> leftInputs, List<ItemStack> rightInputs, List<ItemStack> outputs, ResourceLocation UID) {
        this.leftInputs = leftInputs;
        this.rightInputs = rightInputs;
        this.outputs = outputs;
        this.UID = UID;
    }

    @Override
    @Unmodifiable
    public List<ItemStack> getLeftInputs() {
        return leftInputs;
    }

    @Override
    @Unmodifiable
    public List<ItemStack> getRightInputs() {
        return rightInputs;
    }

    @Override
    @Unmodifiable
    public List<ItemStack> getOutputs() {
        return outputs;
    }
}