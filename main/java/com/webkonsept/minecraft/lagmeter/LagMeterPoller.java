package main.java.com.webkonsept.minecraft.lagmeter;

public class LagMeterPoller implements Runnable{
	private long lastPoll = System.currentTimeMillis()-3000;
	private long polls = 0;
	private int logInterval = 40;
	private final LagMeter plugin;

	protected LagMeterPoller(final LagMeter instance){
		this.plugin = instance;
	}

	protected LagMeterPoller(final LagMeter instance, final int logInterval){
		this.logInterval = logInterval;
		this.plugin = instance;
	}

	@Override
	public void run(){
		final long now = System.currentTimeMillis();
		long timeSpent = (now-this.lastPoll)/1000;
		final String newLine = this.plugin.newLineForLogStats ? "\n" : "  ";
		final String players = this.plugin.playerLoggingEnabled ? newLine+"Players online: "+this.plugin.getServer().getOnlinePlayers().length+"/"+this.plugin.getServer().getMaxPlayers() : "";
		if(timeSpent==0)
			timeSpent = 1;
		final float tps = this.plugin.interval/timeSpent;
		this.plugin.ticksPerSecond = tps;
		this.plugin.history.add(tps);
		this.lastPoll = now;
		this.polls++;
		if(this.plugin.logger.isEnabled()&&this.polls%this.logInterval==0){
			this.plugin.updateMemoryStats();
			float aTPS = 0F;
			if(this.plugin.useAverage)
				aTPS = this.plugin.history.getAverage();
			else
				aTPS = this.plugin.getTPS();
			this.plugin.logger.log("TPS: "+aTPS+newLine+"Memory free: "+this.plugin.memFree+"/"+this.plugin.memMax+" ("+(int) this.plugin.percentageFree+"%)"+players);
		}
	}

	public void setLogInterval(final int interval){
		this.logInterval = interval;
	}
}