package com.moxvallix.eventhandlers;

import com.moxvallix.LagMeter;
import com.moxvallix.TimedCommand;
import com.moxvallix.events.HighLagEvent;
import com.moxvallix.listeners.LagListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DefaultHighLag implements LagListener{
	private final LagMeter plugin;

	@Override
	public void onHighLagEvent(final HighLagEvent evt){
		for(final Player p : Bukkit.getServer().getOnlinePlayers()){
			if(this.plugin.permit(p, "lagmeter.notify.lag") || p.isOp()){
				p.sendMessage(ChatColor.GOLD + "[LagMeter] " + ChatColor.RED + "The server's TPS has dropped below " + this.plugin.getTpsNotificationThreshold() + "!");
			}
		}
		this.plugin.sendConsoleMessage(LagMeter.Severity.SEVERE, "The server's TPS has dropped below " + this.plugin.getTpsNotificationThreshold() + "! Executing command (if configured).");
		new Thread(new TimedCommand(this.plugin.getLagCommand(), this.plugin)).start();
	}

	public DefaultHighLag(final LagMeter plugin){
		this.plugin = plugin;
	}

	@Override
	public String toString(){
		return super.toString();
	}
}