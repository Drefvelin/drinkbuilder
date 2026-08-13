package net.tfminecraft.DrinkBuilder.managers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.tfminecraft.DrinkBuilder.entitlements.PlayerMetaSyncService;

public final class PlayerJoinMetaListener implements Listener {

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		PlayerMetaSyncService.pushForPlayer(event.getPlayer());
	}
}
