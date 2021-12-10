package com.moxvallix;

import org.bukkit.Bukkit;

public final class UptimeCommand implements Runnable{
	private String command;

	public String getCommand(){
		return this.command;
	}

	@Override
	public void run(){
		for(String s : this.command.split(";")){
			if(s.contains("<>"))
				s = s.split("<>")[0];
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), s);
		}
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