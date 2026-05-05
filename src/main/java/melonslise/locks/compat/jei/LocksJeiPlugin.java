package melonslise.locks.compat.jei;

import melonslise.locks.common.init.LocksItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import melonslise.locks.common.init.LocksItemTags;
import java.util.List;
import java.util.Arrays;
import net.minecraft.world.item.crafting.Ingredient;

@JeiPlugin
public class LocksJeiPlugin implements IModPlugin {
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> leftInputs = List.of(new ItemStack(LocksItems.NETHERITE_LOCK));
        List<ItemStack> rightInputs = List.of(new ItemStack(LocksItems.INTEGRATED_CIRCUIT));
        List<ItemStack> outputs = List.of(new ItemStack(LocksItems.NETHERITE_SMART_LOCK));
        ResourceLocation UID = new ResourceLocation("locks", "netherite_smart_lock_upgrade");

        //Safe way to register the recipe
        IJeiAnvilRecipe recipe = new LocksAnvilRecipe(leftInputs, rightInputs, outputs, UID);



        IJeiAnvilRecipe recipe2 = new LocksAnvilRecipe(Arrays.asList(Ingredient.of(LocksItemTags.LOCKS).getItems()),
                                                        Arrays.asList(Ingredient.of(LocksItemTags.LOCKS).getItems()),
                                                        Arrays.asList(Ingredient.of(LocksItemTags.LOCKS).getItems()),
                                                        new ResourceLocation("locks", "copy_lock"));

        ShapelessRecipe recipe3 = new ShapelessRecipe(
                new ResourceLocation("locks", "key"),
                "locks", // group
                CraftingBookCategory.MISC,
                new ItemStack(LocksItems.KEY),
                NonNullList.of(
                        Ingredient.EMPTY,
                        Ingredient.of(LocksItemTags.LOCKS),
                        Ingredient.of(LocksItems.KEY_BLANK)
                )
        );

        registration.addRecipes(RecipeTypes.ANVIL, List.of(recipe));
        registration.addRecipes(RecipeTypes.ANVIL, List.of(recipe2));
        registration.addRecipes(RecipeTypes.CRAFTING, List.of(recipe3));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("locks", "jei_plugin");
    }
}