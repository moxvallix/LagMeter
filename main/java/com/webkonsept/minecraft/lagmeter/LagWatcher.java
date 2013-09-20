package main.java.com.webkonsept.minecraft.lagmeter;

final class LagWatcher implements Runnable{
	private final LagMeter	plugin;
	private boolean			stop;

	@Override
	public void run(){
		while(!this.stop){
			if((this.plugin.getTpsNotificationThreshold() >= this.plugin.getTPS()) && (this.plugin.getTPS() >= 0)){
				this.plugin.notifyLagListeners();
			}
			try{
				Thread.sleep(this.plugin.getCheckMemoryInterval());
			}catch(final InterruptedException e){
				// do nothing, interruption probably means the server is
				// shutting down or reloading.
			}
		}
	}

	public void stop(){
		this.stop = true;
	}

	public LagWatcher(final LagMeter plugin){
		this.plugin = plugin;
		this.stop = false;
	}
}