package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import com.webkonsept.minecraft.lagmeter.util.SyncCommand;
import com.webkonsept.minecraft.lagmeter.util.TimeUtils;

public class TimedCommand implements Runnable{
	private final String command;
	private final LagMeter plugin;

	public void process(String s){
		try{
			String[] n = s.split("<>");
			if(n.length > 1)
				Thread.sleep(TimeUtils.parseTimeMS(n[1]));
		}catch(final InvalidTimeFormatException e){
			e.printStackTrace();
		}catch(final InterruptedException e){
			//probably interrupted by server shutdown or plugin stop/reload
		}
		for(String cmd : this.getCommands()){
			new SyncCommand(cmd.startsWith("/") ? cmd.replaceFirst("/", "") : cmd).runTask(this.plugin);
		}
	}

	public String getTimeString(){
		return this.command.split("<>")[1];
	}

	public long getInterval() throws InvalidTimeFormatException{
		return TimeUtils.parseTimeMS(this.getTimeString());
	}

	public String[] getCommands(){
		return this.command.split("<>")[0].split(";");
	}

	@Override
	public void run(){
		this.process(this.command);
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