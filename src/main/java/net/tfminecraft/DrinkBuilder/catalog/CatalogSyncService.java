package net.tfminecraft.DrinkBuilder.catalog;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.DrinkBuilder.Cache;
import net.tfminecraft.DrinkBuilder.Cache.Ingredient;
import net.tfminecraft.DrinkBuilder.DrinkBuilder;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient.CatalogPushResult;

/**
 * Build drink catalog JSON and push to ProvinceSystem (fail-soft).
 */
public final class CatalogSyncService {

	private CatalogSyncService() {}

	public static String buildPayloadJson() {
		StringBuilder sb = new StringBuilder(2048);
		sb.append("{\"ingredients\":[");
		boolean first = true;
		if (Cache.ingredients != null) {
			for (Ingredient ingredient : Cache.ingredients) {
				if (ingredient == null || ingredient.id.isEmpty()) {
					continue;
				}
				if (!first) {
					sb.append(',');
				}
				first = false;
				sb.append("{\"id\":\"").append(escape(ingredient.id)).append('"');
				sb.append(",\"brewery_token\":\"")
					.append(escape(ingredient.breweryToken)).append('"');
				sb.append(",\"label\":\"").append(escape(ingredient.label)).append('"');
				sb.append(",\"category\":\"")
					.append(escape(ingredient.category)).append('"');
				if (ingredient.type != null && !ingredient.type.isEmpty()) {
					sb.append(",\"type\":\"").append(escape(ingredient.type)).append('"');
				}
				sb.append('}');
			}
		}
		sb.append("],\"effects_blacklist\":[");
		first = true;
		if (Cache.effectsBlacklist != null) {
			for (String effect : Cache.effectsBlacklist) {
				if (effect == null || effect.isBlank()) {
					continue;
				}
				if (!first) {
					sb.append(',');
				}
				first = false;
				sb.append('"').append(escape(effect.trim().toLowerCase())).append('"');
			}
		}
		sb.append("],\"version\":").append(Cache.catalogVersion).append('}');
		return sb.toString();
	}

	public static void pushAsync(JavaPlugin plugin) {
		if (plugin == null) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			CatalogPushResult result = pushNow();
			Logger log = plugin.getLogger();
			if (result.ok) {
				log.info("[catalog] synced to ProvinceSystem: ingredients="
					+ result.ingredients
					+ (result.updatedAt != null ? (" updated_at=" + result.updatedAt) : ""));
			} else {
				log.warning("[catalog] sync failed: " + result.error);
			}
		});
	}

	public static CatalogPushResult pushNow() {
		return ProvinceSystemClient.pushCatalog(buildPayloadJson());
	}

	public static void pushAsyncFromPlugin() {
		DrinkBuilder plugin = DrinkBuilder.plugin;
		if (plugin != null) {
			pushAsync(plugin);
		}
	}

	private static String escape(String raw) {
		if (raw == null) {
			return "";
		}
		StringBuilder out = new StringBuilder(raw.length() + 8);
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			switch (c) {
				case '\\' -> out.append("\\\\");
				case '"' -> out.append("\\\"");
				case '\n' -> out.append("\\n");
				case '\r' -> out.append("\\r");
				case '\t' -> out.append("\\t");
				default -> out.append(c);
			}
		}
		return out.toString();
	}
}
