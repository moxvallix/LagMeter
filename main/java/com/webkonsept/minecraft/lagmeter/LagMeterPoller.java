package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.NoAvailableTPSException;

public class LagMeterPoller implements Runnable{
	private long lastPoll = System.currentTimeMillis() - 3000;
	private long polls = 0;
	private int logInterval = 40;
	private final LagMeter plugin;

	@Override
	public void run(){
		final long now = System.currentTimeMillis();
		long timeSpent = (now - this.lastPoll) / 1000;
		final String newLine = this.plugin.isUsingNewLineForLogStats() ? "\n" : "  ";
		final String players = this.plugin.isPlayerLoggingEnabled() ? newLine + "Players online: " + this.plugin.getServer().getOnlinePlayers().length + "/" + this.plugin.getServer().getMaxPlayers() : "";
		if(timeSpent == 0){
			timeSpent = 1;
		}
		final float tps = ((float)this.plugin.getInterval()) / ((float)timeSpent);
		this.plugin.setTicksPerSecond(tps);
		this.plugin.addHistory(tps);
		this.lastPoll = now;
		this.polls++;
		if(this.plugin.getLMLogger().isEnabled() && ((this.polls % this.logInterval) == 0)){
			this.plugin.updateMemoryStats();
			double aTPS;
			if(this.plugin.isAveraging()){
				aTPS = this.plugin.getHistory().getAverage();
			}else{
				try{
					aTPS = this.plugin.getTPS();
				}catch(NoAvailableTPSException e){
					return;
				}
			}
			final double[] d = this.plugin.getMemory();
			this.plugin.getLMLogger().log("TPS: " + aTPS + newLine + "Memory free: " + String.format("%,.2f", d[2]) + "/" + String.format("%,.2f", d[1]) + " (" + String.format("%,.2f", d[3]) + "%)" + players);
		}
	}

	public void setLogInterval(final int interval){
		this.logInterval = interval;
	}

	LagMeterPoller(final LagMeter instance){
		this(instance, Integer.MAX_VALUE);
	}

	protected LagMeterPoller(final LagMeter instance, final int logInterval){
		super();
		this.logInterval = logInterval;
		this.plugin = instance;
	}

	public String toString(){
		return "LagMeterPoller@"+hashCode()+"{\n\tlastPoll = "+this.lastPoll+"\n\tpolls = "+this.polls+"\n\tlogInterval = "+this.logInterval+"\n}";
	}
}
