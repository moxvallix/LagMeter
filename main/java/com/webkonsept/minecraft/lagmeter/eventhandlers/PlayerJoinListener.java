package main.java.com.webkonsept.minecraft.lagmeter.eventhandlers;

import main.java.com.webkonsept.minecraft.lagmeter.LagMeter;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerJoinListener implements Listener {
	public void onPlayerQuitEvent(PlayerLoginEvent evt) {
		LagMeter.getInstance().addPlayerIP(evt.getPlayer().getName(), evt.getAddress().getHostAddress());
	}
}
