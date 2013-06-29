package main.java.com.webkonsept.minecraft.lagmeter;

import main.java.com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;

public class TimedCommand implements Runnable{
	private final String command;
	private final LagMeter plugin;

	public TimedCommand(String command, LagMeter plugin){
		this.command = command;
		this.plugin = plugin;
	}

	@Override
	public void run(){
		if(this.command.contains(";"))
			for(final String cmd: this.command.split(";"))
				this.process(cmd);
		else
			this.process(this.command);
	}

	public void process(String s){
		try{
			Thread.sleep(this.plugin.parseTime(s));
		}catch(final InvalidTimeFormatException e){
		}catch(final InterruptedException e){
		}
		new SyncCommand(s.split("<>")[0]).runTask(this.plugin);
	}
}