package melonslise.locks.compat.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.Items;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;

public class LocksReiPlugin implements REIClientPlugin {
    @Override
    public void registerDisplays(DisplayRegistry registry) {
        //registry.add(new NetheriteSmartLockRecipe());
        REIIntegration.registerDisplays(registry);
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.addWorkstations(BuiltinPlugin.ANVIL, EntryStacks.of(Items.ANVIL));
    }
}