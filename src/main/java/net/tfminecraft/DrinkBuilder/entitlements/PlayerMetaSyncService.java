package net.tfminecraft.DrinkBuilder.entitlements;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.tfminecraft.DrinkBuilder.Cache;
import net.tfminecraft.DrinkBuilder.DrinkBuilder;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient;
import net.tfminecraft.DrinkBuilder.api.ProvinceSystemClient.SimpleResult;

/**
 * Push allow_drink_texture to ProvinceSystem (fail-soft).
 */
public final class PlayerMetaSyncService {

	private PlayerMetaSyncService() {}

	public static boolean allowDrinkTexture(Player player) {
		if (player == null) {
			return false;
		}
		List<String> nodes = Cache.texturePermissions;
		if (nodes == null) {
			return false;
		}
		for (String node : nodes) {
			if (node != null && !node.isBlank() && player.hasPermission(node.trim())) {
				return true;
			}
		}
		return false;
	}

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
		boolean allow = allowDrinkTexture(online);
		String body = "{\"player_uuid\":\"" + playerUuid
			+ "\",\"allow_drink_texture\":" + allow + "}";
		SimpleResult result = ProvinceSystemClient.pushPlayerMeta(body);
		if (!result.ok && DrinkBuilder.plugin != null) {
			DrinkBuilder.plugin.getLogger().warning(
				"[player-meta] push failed for " + playerUuid + ": " + result.error
			);
		}
	}
}
