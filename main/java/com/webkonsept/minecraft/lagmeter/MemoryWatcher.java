package main.java.com.webkonsept.minecraft.lagmeter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

final class MemoryWatcher implements Runnable{
	LagMeter plugin = null;

	public MemoryWatcher(final LagMeter plugin){
		this.plugin = plugin;
	}

	@Override
	public void run(){
		if(this.plugin.getMemoryNotificationThreshold()>=this.plugin.getMemory()[3]){
			Player[] players;
			players = Bukkit.getServer().getOnlinePlayers();
			for(final Player p: players)
				if(this.plugin.permit(p, "lagmeter.notify.mem")||p.isOp())
					p.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.RED+"The server's free memory pool has dropped below "+this.plugin.getMemoryNotificationThreshold()+"%! If you configured a server command to execute at this time, it will run now.");
			this.plugin.severe("The server's free memory pool has dropped below "+this.plugin.getMemoryNotificationThreshold()+"! Executing command (if configured).");
			if(this.plugin.getMemoryCommand().contains(";"))
				for(final String cmd: this.plugin.getMemoryCommand().split(";"))
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceFirst("/", ""));
			else
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), this.plugin.getMemoryCommand().replaceFirst("/", ""));
		}
	}
}