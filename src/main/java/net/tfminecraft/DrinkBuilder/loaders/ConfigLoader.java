package net.tfminecraft.DrinkBuilder.loaders;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.DrinkBuilder.Cache;

public final class ConfigLoader {

	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
			return;
		}

		String base = config.getString("api.base-url", "");
		if (base == null) {
			base = "";
		}
		base = base.trim();
		while (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		Cache.apiBaseUrl = base;

		String key = config.getString("api.plugin-key", "");
		Cache.pluginKey = key == null ? "" : key.trim();

		String brewery = config.getString("paths.breweryx-folder", "plugins/BreweryX");
		Cache.breweryxFolder = brewery == null ? "plugins/BreweryX" : brewery.trim();

		String ia = config.getString(
			"paths.itemsadder-tfmc-drinks",
			"plugins/ItemsAdder/contents/tfmc_drinks"
		);
		Cache.itemsAdderTfmcDrinks = ia == null
			? "plugins/ItemsAdder/contents/tfmc_drinks"
			: ia.trim();

		Cache.cmdMin = config.getInt("cmd.min", 20000);
		Cache.cmdMax = config.getInt("cmd.max", 29999);
		if (Cache.cmdMax < Cache.cmdMin) {
			int swap = Cache.cmdMin;
			Cache.cmdMin = Cache.cmdMax;
			Cache.cmdMax = swap;
		}

		Cache.iaReloadDelaySeconds = Math.max(0, config.getInt("ia-reload-delay-seconds", 8));

		List<String> perms = Cache.newStringList();
		List<?> raw = config.getList("texture-permissions");
		if (raw != null) {
			for (Object item : raw) {
				if (item == null) {
					continue;
				}
				String node = String.valueOf(item).trim();
				if (!node.isEmpty()) {
					perms.add(node);
				}
			}
		}
		if (perms.isEmpty()) {
			perms.add("rpchar.group.gilded");
			perms.add("rpchar.group.ascended");
			perms.add("rpchar.group.legacy");
		}
		Cache.texturePermissions = List.copyOf(perms);
	}
}
