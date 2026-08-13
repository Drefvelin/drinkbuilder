package net.tfminecraft.DrinkBuilder.pack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.DrinkBuilder.Cache;

/**
 * Ensure an empty ItemsAdder tfmc_drinks namespace scaffold exists on disk.
 */
public final class IaDrinksScaffold {

	private IaDrinksScaffold() {}

	public static void ensure(JavaPlugin plugin) {
		if (plugin == null) {
			return;
		}
		String configured = Cache.itemsAdderTfmcDrinks;
		if (configured == null || configured.isBlank()) {
			return;
		}
		File root = resolvePath(plugin, configured.trim());
		File configs = new File(root, "configs");
		File itemsYml = new File(configs, "items.yml");
		File textures = new File(root, "resourcepack/tfmc_drinks/textures/item");

		try {
			if (!configs.exists() && !configs.mkdirs()) {
				plugin.getLogger().warning(
					"[ia] could not create configs dir: " + configs.getAbsolutePath()
				);
				return;
			}
			if (!itemsYml.exists()) {
				String body = ""
					+ "info:\n"
					+ "  namespace: tfmc_drinks\n"
					+ "items: {}\n";
				Files.writeString(itemsYml.toPath(), body, StandardCharsets.UTF_8);
				plugin.getLogger().info(
					"[ia] wrote empty tfmc_drinks scaffold: " + itemsYml.getAbsolutePath()
				);
			}
			if (!textures.exists() && !textures.mkdirs()) {
				plugin.getLogger().warning(
					"[ia] could not create textures dir: " + textures.getAbsolutePath()
				);
			}
		} catch (IOException e) {
			plugin.getLogger().warning("[ia] scaffold failed: " + e.getMessage());
		}
	}

	private static File resolvePath(JavaPlugin plugin, String configured) {
		File asIs = new File(configured);
		if (asIs.isAbsolute()) {
			return asIs;
		}
		File serverRoot = plugin.getDataFolder().getParentFile();
		if (serverRoot != null) {
			serverRoot = serverRoot.getParentFile();
		}
		if (serverRoot == null) {
			return asIs;
		}
		return new File(serverRoot, configured);
	}
}
