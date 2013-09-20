package main.java.com.webkonsept.minecraft.lagmeter;

final class MemoryWatcher implements Runnable{
	private final LagMeter	plugin;
	private boolean			stop;

	@Override
	public void run(){
		while(!this.stop){
			if(this.plugin.getMemory()[3] <= this.plugin.getMemoryNotificationThreshold()){
				this.plugin.notifyMemoryListeners();
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

	public MemoryWatcher(final LagMeter plugin){
		this.plugin = plugin;
		this.stop = false;
	}
}