package melonslise.locks;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.event.LocksEvents;
import melonslise.locks.common.init.*;
import net.fabricmc.api.ModInitializer;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MOD init and registration.
 */
public final class Locks implements ModInitializer
{
	public static final String ID = "locks";

	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitialize() {

		//EnumModifier.run();
		//TODO: run the EnumModifier class to add enchantments to the game

		LocksItems.register();
		LocksEnchantments.register();
		LocksSoundEvents.register();
		LocksContainerTypes.register();
		LocksRecipeSerializers.register();
		LocksVillagerTrades.register();
		LocksEvents.register();
		LocksNetwork.register();
		LocksFeatures.register();
		ForgeConfigRegistry.INSTANCE.register(ID, Type.COMMON, LocksConfig.SPEC);
		ForgeConfigRegistry.INSTANCE.register(ID, Type.SERVER, LocksServerConfig.SPEC);
		LocksConfig.init();
	}
}