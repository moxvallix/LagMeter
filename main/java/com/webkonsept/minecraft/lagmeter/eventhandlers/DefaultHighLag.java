package com.webkonsept.minecraft.lagmeter.eventhandlers;

import com.webkonsept.minecraft.lagmeter.LagMeter;
import com.webkonsept.minecraft.lagmeter.TimedCommand;
import com.webkonsept.minecraft.lagmeter.events.HighLagEvent;
import com.webkonsept.minecraft.lagmeter.exceptions.EmptyCommandException;
import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import com.webkonsept.minecraft.lagmeter.listeners.LagListener;
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
		try {
			new Thread(new TimedCommand(this.plugin.getLagCommand(), this.plugin)).start();
		}catch(EmptyCommandException| InvalidTimeFormatException e){
			this.plugin.sendConsoleMessage(LagMeter.Severity.SEVERE, "The command configured to run with low TPS/high lag events is improperly configured!");
		}
	}

	public DefaultHighLag(final LagMeter plugin){
		this.plugin = plugin;
	}

	@Override
	public String toString(){
		return super.toString();
	}
}