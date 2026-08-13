package net.tfminecraft.DrinkBuilder.entitlements;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.tfminecraft.DrinkBuilder.DrinkBuilder;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient.SimpleResult;

/**
 * Push allow_drink_texture + name_colour_stops to ProvinceSystem (fail-soft).
 */
public final class PlayerMetaSyncService {

	private PlayerMetaSyncService() {}

	public static void pushForPlayer(Player player) {
		if (player == null) {
			return;
		}
		pushAsync(player.getUniqueId());
	}

	public static void pushAsync(UUID playerUuid) {
		if (playerUuid == null || DrinkBuilder.plugin == null) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(
			DrinkBuilder.plugin,
			() -> pushNow(playerUuid)
		);
	}

	public static void pushAllOnlineAsync() {
		if (DrinkBuilder.plugin == null) {
			return;
		}
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player != null) {
				pushAsync(player.getUniqueId());
			}
		}
	}

	public static void pushNow(UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}
		Player online = Bukkit.getPlayer(playerUuid);
		if (online == null || !online.isOnline()) {
			return;
		}
		boolean allow = PermissionGroupService.getAllowDrinkTexture(online);
		int stops = PermissionGroupService.getNameColourStops(online);
		String body = "{\"player_uuid\":\"" + playerUuid
			+ "\",\"allow_drink_texture\":" + allow
			+ ",\"name_colour_stops\":" + stops
			+ "}";
		SimpleResult result = ProvinceSystemClient.pushPlayerMeta(body);
		if (!result.ok && DrinkBuilder.plugin != null) {
			DrinkBuilder.plugin.getLogger().warning(
				"[player-meta] push failed for " + playerUuid + ": " + result.error
			);
		}
	}
}
