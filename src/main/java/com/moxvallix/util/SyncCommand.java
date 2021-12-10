package com.moxvallix.util;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class SyncCommand extends BukkitRunnable{
	String command;

	@Override
	public void run(){
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), this.command.replaceFirst("/", ""));
	}

	public SyncCommand(String command){
		super();
		this.command = command;
	}

	@Override
	public String toString(){
		return "SyncCommand@"+hashCode()+"{\n\tcommand = "+this.command+"\n}";
	}
}