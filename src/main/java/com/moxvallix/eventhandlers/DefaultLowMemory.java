package com.moxvallix.eventhandlers;

import com.moxvallix.LagMeter;
import com.moxvallix.TimedCommand;
import com.moxvallix.events.LowMemoryEvent;
import com.moxvallix.listeners.MemoryListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DefaultLowMemory implements MemoryListener{
	private final LagMeter plugin;

	@Override
	public void onLowMemoryEvent(final LowMemoryEvent evt){
		for(final Player p : Bukkit.getServer().getOnlinePlayers()){
			if(this.plugin.permit(p, "lagmeter.notify.mem") || p.isOp()){
				p.sendMessage(ChatColor.GOLD + "[LagMeter] " + ChatColor.RED + "The server's free memory pool has dropped below " + this.plugin.getMemoryNotificationThreshold() + "%!");
			}
		}
		this.plugin.sendConsoleMessage(LagMeter.Severity.SEVERE, "The server's free memory pool has dropped below " + this.plugin.getMemoryNotificationThreshold() + "%! Executing command (if configured).");
		new Thread(new TimedCommand(this.plugin.getMemoryCommand(), this.plugin)).start();
	}

	public DefaultLowMemory(final LagMeter plugin){
		this.plugin = plugin;
	}

	@Override
	public String toString(){
		return super.toString();
	}
}