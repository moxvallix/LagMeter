package main.java.com.webkonsept.minecraft.lagmeter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

final class LagWatcher implements Runnable{
	final LagMeter plugin;

	public LagWatcher(final LagMeter plugin){
		this.plugin = plugin;
	}

	@Override
	public void run(){
		if(this.plugin.getTpsNotificationThreshold()>=this.plugin.getTPS()){
			final Player[] players = Bukkit.getServer().getOnlinePlayers();
			for(final Player p: players)
				if(this.plugin.permit(p, "lagmeter.notify.lag")||p.isOp())
					p.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.RED+"The server's TPS has dropped below "+this.plugin.getTpsNotificationThreshold()+"! If you configured a server command to execute at this time, it will run now.");
			this.plugin.severe("The server's TPS has dropped below "+this.plugin.getTpsNotificationThreshold()+"! Executing command (if configured).");
			if(this.plugin.getLagCommand().contains(";"))
				for(final String cmd: this.plugin.getLagCommand().split(";"))
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceFirst("/", ""));
			else
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), this.plugin.getLagCommand().replaceFirst("/", ""));
		}
	}
}