package melonslise.locks.compat.rei;

import net.minecraft.world.item.ItemStack;
import me.shedaniel.rei.plugin.common.displays.anvil.AnvilRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Collections;
import java.util.List;

public class CustomAnvilRecipe extends AnvilRecipe {
    private final int levelCost;
    private final int materialCost;

    public CustomAnvilRecipe(ResourceLocation id, ItemStack leftInput, ItemStack rightInput,
                             ItemStack output, int levelCost, int materialCost) {
        super(id,
                Collections.singletonList(leftInput),
                Collections.singletonList(rightInput),
                Collections.singletonList(output)
        );
        this.levelCost = levelCost;
        this.materialCost = materialCost;
    }

    public int getLevelCost() {
        return this.levelCost;
    }

    public int getMaterialCost() {
        return this.materialCost;
    }

    /*@Override
    public List<Component> getTooltip() {
        List<Component> tooltip = super.getTooltip();
        tooltip.add(Component.literal("XP Cost: " + this.levelCost + " levels"));
        tooltip.add(Component.literal("Consumes: " + this.materialCost + " items"));
        return tooltip;
    }*/
}