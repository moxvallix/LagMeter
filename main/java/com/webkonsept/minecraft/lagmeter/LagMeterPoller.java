package main.java.com.webkonsept.minecraft.lagmeter;

public class LagMeterPoller implements Runnable{
	private long lastPoll = System.currentTimeMillis()-3000;
	private long polls = 0;
	private int logInterval = 40;
	private final LagMeter plugin;

	@Override
	public void run(){
		final long now = System.currentTimeMillis();
		long timeSpent = (now-this.lastPoll)/1000;
		final String newLine = this.plugin.isUsingNewLineForLogStats() ? "\n" : "  ";
		final String players = this.plugin.isPlayerLoggingEnabled() ? newLine+"Players online: "+this.plugin.getServer().getOnlinePlayers().length+"/"+this.plugin.getServer().getMaxPlayers() : "";
		if(timeSpent==0){
			timeSpent = 1;
		}
		final float tps = this.plugin.getInterval()/timeSpent;
		this.plugin.setTicksPerSecond(tps);
		this.plugin.addHistory(tps);
		this.lastPoll = now;
		this.polls++;
		if(this.plugin.getLMLogger().isEnabled()&&this.polls%this.logInterval==0){
			this.plugin.updateMemoryStats();
			float aTPS = 0F;
			if(this.plugin.isAveraging()){
				aTPS = this.plugin.getHistory().getAverage();
			}else{
				aTPS = this.plugin.getTPS();
			}
			final double[] d = this.plugin.getMemory();
			this.plugin.getLMLogger().log("TPS: "+aTPS+newLine+"Memory free: "+String.format("%,.2f", d[2])+"/"+String.format("%,.2f", d[1])+" ("+String.format("%,.2f", d[3])+"%)"+players);
		}
	}

	public void setLogInterval(final int interval){
		this.logInterval = interval;
	}

	protected LagMeterPoller(final LagMeter instance){
		this.plugin = instance;
	}

	protected LagMeterPoller(final LagMeter instance, final int logInterval){
		this.logInterval = logInterval;
		this.plugin = instance;
	}
}