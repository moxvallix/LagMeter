package com.webkonsept.minecraft.lagmeter;

import org.bukkit.Bukkit;

public final class UptimeCommand implements Runnable{
	private String command;

	public String getCommand(){
		return this.command;
	}

	@Override
	public void run(){
		for(String s : this.command.split(";"))
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), s);
	}

	public void setCommand(final String newCommand){
		this.command = newCommand;
	}

	public UptimeCommand(final String command){
		this.command = command;
	}

	public String toString(){
		return "UptimeCommand@"+hashCode()+"{\n\tcommand = "+this.command+"\n}";
	}
}