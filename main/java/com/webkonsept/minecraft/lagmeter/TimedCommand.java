package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import com.webkonsept.minecraft.lagmeter.util.SyncCommand;

public class TimedCommand implements Runnable{
	private final String command;
	private final LagMeter plugin;

	public void process(String s){
		try{
			Thread.sleep(this.plugin.parseTimeMS(s));
		}catch(final InvalidTimeFormatException e){
			e.printStackTrace();
		}catch(final InterruptedException e){
			//probably interrupted by server shutdown or plugin stop/reload
		}
		new SyncCommand(s.split("<>")[0]).runTask(this.plugin);
	}

	@Override
	public void run(){
		if(this.command.contains(";")){
			for(final String cmd : this.command.split(";")){
				this.process(cmd);
			}
		}else{
			this.process(this.command);
		}
	}

	public TimedCommand(String command, LagMeter plugin){
		super();
		this.command = command;
		this.plugin = plugin;
	}

	public String toString(){
		return "TimedCommand@"+hashCode()+"{\n\t"+command+"\n}";
	}
}