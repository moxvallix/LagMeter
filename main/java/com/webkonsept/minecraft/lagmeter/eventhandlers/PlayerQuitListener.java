package main.java.com.webkonsept.minecraft.lagmeter.eventhandlers;

import main.java.com.webkonsept.minecraft.lagmeter.LagMeter;

import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener{
	public void onPlayerQuitEvent(PlayerQuitEvent evt){
		LagMeter.getInstance().removePlayerIP(evt.getPlayer().getName());
	}
}