package com.webkonsept.minecraft.lagmeter.eventhandlers;

import com.webkonsept.minecraft.lagmeter.LagMeter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerJoinListener implements Listener{
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuitEvent(PlayerLoginEvent evt){
		LagMeter.getInstance().addPlayerIP(evt.getPlayer().getName(), evt.getAddress().getHostAddress());
	}
}
