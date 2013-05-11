package main.java.com.webkonsept.minecraft.lagmeter;

import main.java.com.webkonsept.minecraft.lagmeter.events.LowMemoryEvent;
import main.java.com.webkonsept.minecraft.lagmeter.listeners.MemoryListener;

final class MemoryWatcher implements Runnable{
	private final LagMeter plugin;
	private boolean stop;

	public MemoryWatcher(final LagMeter plugin){
		this.plugin = plugin;
		this.stop = false;
	}

	@Override
	public void run(){
		while(!this.stop){
			if(this.plugin.getMemory()[3]<=this.plugin.getMemoryNotificationThreshold()){
				final LowMemoryEvent e = new LowMemoryEvent(this.plugin.getMemory(), this.plugin.getTPS());
				for(final MemoryListener m: this.plugin.getMemoryListeners())
					new Thread(new Runnable(){
						@Override
						public void run(){
							m.onLowMemoryEvent(e);
						}
					}).start();
			}
			try{
				Thread.sleep(this.plugin.getCheckMemoryInterval());
			}catch(final InterruptedException e){
				// do nothing, interruption probably means the server is shutting down or reloading.
			}
		}
	}

	public void stop(){
		this.stop = true;
	}
}