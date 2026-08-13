package net.tfminecraft.DrinkBuilder.pack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.DrinkBuilder.Cache;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient.DownloadResult;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient.PendingDrink;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient.SimpleResult;

/**
 * Write potion PNG + ItemsAdder item entry under tfmc_drinks.
 */
public final class IaDrinksWriter {

	private IaDrinksWriter() {}

	public static final class WriteResult {
		public final int cmd;
		public final String iaItemId;

		public WriteResult(int cmd, String iaItemId) {
			this.cmd = cmd;
			this.iaItemId = iaItemId;
		}
	}

	public static WriteResult write(
		JavaPlugin plugin,
		PendingDrink drink,
		CmdAllocator allocator,
		Logger log
	) throws IOException {
		if (drink == null || drink.id == null || drink.id.isBlank()) {
			throw new IOException("drink id required");
		}
		String sid = drink.id.trim();
		String textureId = drink.textureId == null ? "" : drink.textureId.trim();
		if (textureId.isEmpty()) {
			throw new IOException("texture_id required for IA write");
		}

		DownloadResult dl = ProvinceSystemClient.downloadSubmissionFile(sid, "texture.png");
		if (!dl.ok) {
			throw new IOException("download texture.png: " + dl.error);
		}

		IaDrinksScaffold.ensure(plugin);
		File root = resolvePath(plugin, Cache.itemsAdderTfmcDrinks);
		File texDir = new File(root, "resourcepack/tfmc_drinks/textures/item");
		if (!texDir.exists() && !texDir.mkdirs()) {
			throw new IOException("could not create textures dir: " + texDir);
		}
		File png = new File(texDir, sid + ".png");
		Files.write(png.toPath(), dl.data);

		int cmd = allocator.allocate();
		String iaItemId = "tfmc_drinks:" + sid;

		File itemsYml = new File(root, "configs/items.yml");
		FileConfiguration yaml = itemsYml.exists()
			? YamlConfiguration.loadConfiguration(itemsYml)
			: new YamlConfiguration();
		if (!yaml.isConfigurationSection("info")) {
			yaml.set("info.namespace", "tfmc_drinks");
		}
		ConfigurationSection items = yaml.getConfigurationSection("items");
		if (items == null) {
			items = yaml.createSection("items");
		}
		String display = drink.displayName == null || drink.displayName.isBlank()
			? sid
			: drink.displayName.trim();
		ConfigurationSection item = items.createSection(sid);
		item.set("display_name", display);
		item.set("resource.material", "POTION");
		item.set("resource.generate", true);
		item.set("resource.textures", java.util.List.of("item/" + sid));
		item.set("resource.model_id", cmd);
		yaml.save(itemsYml);

		SimpleResult assigned = ProvinceSystemClient.assignTextureCmd(textureId, cmd, iaItemId);
		if (!assigned.ok) {
			throw new IOException("assign CMD on PS: " + assigned.error);
		}

		if (log != null) {
			log.info("[ia] wrote " + iaItemId + " cmd=" + cmd + " png=" + png.getName());
		}
		return new WriteResult(cmd, iaItemId);
	}

	static File resolvePath(JavaPlugin plugin, String configured) {
		File asIs = new File(configured == null ? "" : configured);
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
