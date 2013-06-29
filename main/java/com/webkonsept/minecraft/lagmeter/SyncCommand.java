package main.java.com.webkonsept.minecraft.lagmeter;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class SyncCommand extends BukkitRunnable{
	String command;

	@Override
	public void run(){
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), this.command.replaceFirst("/", ""));
	}

	public SyncCommand(String command){
		this.command = command;
	}
}