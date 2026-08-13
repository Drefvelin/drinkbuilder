package net.tfminecraft.DrinkBuilder;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.DrinkBuilder.catalog.CatalogSyncService;
import net.tfminecraft.DrinkBuilder.loaders.ConfigLoader;
import net.tfminecraft.DrinkBuilder.loaders.IngredientsLoader;
import net.tfminecraft.DrinkBuilder.managers.CommandManager;
import net.tfminecraft.DrinkBuilder.managers.PlayerJoinMetaListener;
import net.tfminecraft.DrinkBuilder.pack.CmdAllocator;
import net.tfminecraft.DrinkBuilder.pack.DeferredDrinkIaReload;
import net.tfminecraft.DrinkBuilder.pack.IaDrinksScaffold;
import net.tfminecraft.DrinkBuilder.pack.ItemsAdderPackListener;
import net.tfminecraft.DrinkBuilder.pack.PendingReloadQueue;

public class DrinkBuilder extends JavaPlugin {

	public static DrinkBuilder plugin;

	private final ConfigLoader configLoader = new ConfigLoader();
	private final IngredientsLoader ingredientsLoader = new IngredientsLoader();
	private final CommandManager commandManager = new CommandManager();
	private final PlayerJoinMetaListener joinMetaListener = new PlayerJoinMetaListener();
	private CmdAllocator cmdAllocator;
	private PendingReloadQueue pendingReloadQueue;
	private DeferredDrinkIaReload deferredIaReload;

	@Override
	public void onEnable() {
		plugin = this;
		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}
		saveDefaultConfigs();
		reloadAll();
		cmdAllocator = new CmdAllocator(this);
		cmdAllocator.reloadBounds();

		pendingReloadQueue = new PendingReloadQueue(this);
		pendingReloadQueue.load();
		deferredIaReload = new DeferredDrinkIaReload(this, pendingReloadQueue);

		IaDrinksScaffold.ensure(this);

		if (getCommand(commandManager.cmd1) != null) {
			getCommand(commandManager.cmd1).setExecutor(commandManager);
			getCommand(commandManager.cmd1).setTabCompleter(commandManager);
		} else {
			getLogger().severe("Command drinkbuilder missing from plugin.yml");
		}

		getServer().getPluginManager().registerEvents(joinMetaListener, this);
		getServer().getPluginManager().registerEvents(deferredIaReload, this);
		ItemsAdderPackListener.registerIfPresent(deferredIaReload);
		CatalogSyncService.pushAsync(this);

		getLogger().info(
			"DrinkBuilder enabled (ingredients="
				+ Cache.ingredients.size()
				+ ", blacklist="
				+ Cache.effectsBlacklist.size()
				+ ", nextCmd="
				+ cmdAllocator.peekNext()
				+ ", pendingIa="
				+ pendingReloadQueue.size()
				+ ", api="
				+ (Cache.apiBaseUrl.isEmpty() ? "(unset)" : Cache.apiBaseUrl)
				+ ")"
		);
	}

	@Override
	public void onDisable() {
		plugin = null;
	}

	public CmdAllocator getCmdAllocator() {
		return cmdAllocator;
	}

	public DeferredDrinkIaReload getDeferredIaReload() {
		return deferredIaReload;
	}

	public void reloadAll() {
		configLoader.load(new File(getDataFolder(), "config.yml"));
		ingredientsLoader.loadIngredients(new File(getDataFolder(), "ingredients.yml"));
		ingredientsLoader.loadEffectsBlacklist(
			new File(getDataFolder(), "effects-blacklist.yml")
		);
		if (cmdAllocator != null) {
			cmdAllocator.reloadBounds();
		}
		IaDrinksScaffold.ensure(this);
	}

	private void saveDefaultConfigs() {
		saveIfMissing("config.yml");
		saveIfMissing("ingredients.yml");
		saveIfMissing("effects-blacklist.yml");
	}

	private void saveIfMissing(String name) {
		File out = new File(getDataFolder(), name);
		if (!out.exists()) {
			saveResource(name, false);
		}
	}
}
