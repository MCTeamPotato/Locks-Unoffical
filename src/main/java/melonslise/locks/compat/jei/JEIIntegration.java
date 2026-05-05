package melonslise.locks.compat.jei;

import net.fabricmc.loader.api.FabricLoader;

public class JEIIntegration {
    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("jei");
    }
}