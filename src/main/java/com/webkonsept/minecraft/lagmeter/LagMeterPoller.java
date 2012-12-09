package main.java.com.webkonsept.minecraft.lagmeter;

public class LagMeterPoller implements Runnable{
	long lastPoll = System.currentTimeMillis()-3000;
	long polls = 0;
	int logInterval = 150;
	LagMeter plugin;

	LagMeterPoller(LagMeter instance){
		this.plugin = instance;
	}
	LagMeterPoller(LagMeter instance, int logInterval){
		this.logInterval = logInterval;
		this.plugin = instance;
	}

	public void setLogInterval(int interval){
		logInterval = interval;
	}
	@Override
	public void run(){
		long now = System.currentTimeMillis();
		long timeSpent = (now-lastPoll)/1000;
		String newLine = plugin.newLineForLogStats?"\n":"  ";
		String players = plugin.playerLoggingEnabled?newLine+"Players online: "+plugin.getServer().getOnlinePlayers().length+"/"+plugin.getServer().getMaxPlayers():"";
		if(timeSpent == 0)
			timeSpent = 1;
		float tps = plugin.interval/timeSpent;
		plugin.ticksPerSecond = tps;
		plugin.history.add(tps);
		lastPoll = now;
		polls++;
		if(plugin.logger.enabled() && polls % logInterval == 0){
			plugin.updateMemoryStats();
			float aTPS = 0F;
			if(plugin.useAverage)
				aTPS = plugin.history.getAverage();
			else
				aTPS = plugin.getTPS();
			plugin.logger.log("TPS: "+aTPS+newLine+"Memory free: "+plugin.memFree+"/"+plugin.memMax+" ("+(int)plugin.percentageFree+"%)"+players);
		}
	}
}