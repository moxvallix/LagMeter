package main.java.com.webkonsept.minecraft.lagmeter;

import org.bukkit.Bukkit;

final class UptimeCommand implements Runnable{
	private String command;

	public UptimeCommand(final String command){
		this.command = command;
	}

	public String getCommand(){
		return this.command;
	}

	public void setCommand(String newCommand){
		this.command = newCommand;
	}

	@Override
	public void run(){
		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), this.command);
	}
}