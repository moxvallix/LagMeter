package main.java.com.webkonsept.minecraft.lagmeter.eventhandlers;

import main.java.com.webkonsept.minecraft.lagmeter.LagMeter;
import main.java.com.webkonsept.minecraft.lagmeter.events.LowMemoryEvent;
import main.java.com.webkonsept.minecraft.lagmeter.listeners.MemoryListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DefaultLowMemory implements MemoryListener{
	private final LagMeter plugin;

	public DefaultLowMemory(LagMeter plugin){
		this.plugin = plugin;
	}

	@Override
	public void onLowMemoryEvent(LowMemoryEvent evt){
		Player[] players;
		players = Bukkit.getServer().getOnlinePlayers();
		for(final Player p: players)
			if(this.plugin.permit(p, "lagmeter.notify.mem")||p.isOp())
				p.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.RED+"The server's free memory pool has dropped below "+this.plugin.getMemoryNotificationThreshold()+"%! If you configured a server command to execute at this time, it will run now.");
		this.plugin.severe("The server's free memory pool has dropped below "+this.plugin.getMemoryNotificationThreshold()+"%! Executing command (if configured).");
		if(this.plugin.getLagCommand().contains(";"))
			for(final String cmd: this.plugin.getLagCommand().split(";"))
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceFirst("/", ""));
		else
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), this.plugin.getLagCommand().replaceFirst("/", ""));
	}
}