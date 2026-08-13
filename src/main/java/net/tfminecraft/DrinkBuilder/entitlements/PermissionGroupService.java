package net.tfminecraft.DrinkBuilder.entitlements;

import org.bukkit.entity.Player;

import net.tfminecraft.DrinkBuilder.Cache;
import net.tfminecraft.DrinkBuilder.objects.PermissionGroupDefinition;

/**
 * Resolve drink creator perks from permission-groups.yml.
 */
public final class PermissionGroupService {

	private PermissionGroupService() {}

	public static int getNameColourStops(Player player) {
		if (player == null) {
			return Cache.defaultNameColourStops;
		}
		int max = Cache.defaultNameColourStops;
		for (PermissionGroupDefinition group : Cache.permissionGroups) {
			if (group == null || group.getPermission().isEmpty()) {
				continue;
			}
			if (!player.hasPermission(group.getPermission())) {
				continue;
			}
			int stops = group.hasNameColourStops()
				? group.getNameColourStops()
				: Cache.defaultNameColourStops;
			if (stops > max) {
				max = stops;
			}
		}
		return max;
	}

	public static boolean getAllowDrinkTexture(Player player) {
		if (player == null) {
			return Cache.defaultAllowDrinkTexture;
		}
		boolean any = Cache.defaultAllowDrinkTexture;
		for (PermissionGroupDefinition group : Cache.permissionGroups) {
			if (group == null || group.getPermission().isEmpty()) {
				continue;
			}
			if (!player.hasPermission(group.getPermission())) {
				continue;
			}
			boolean allow = group.hasAllowDrinkTexture()
				? group.isAllowDrinkTexture()
				: Cache.defaultAllowDrinkTexture;
			if (allow) {
				return true;
			}
		}
		return any;
	}
}
