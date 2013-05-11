package main.java.com.webkonsept.minecraft.lagmeter;

import main.java.com.webkonsept.minecraft.lagmeter.events.HighLagEvent;
import main.java.com.webkonsept.minecraft.lagmeter.listeners.LagListener;

final class LagWatcher implements Runnable{
	private final LagMeter plugin;
	private boolean stop;

	public LagWatcher(final LagMeter plugin){
		this.plugin = plugin;
		this.stop = false;
	}

	@Override
	public void run(){
		while(!this.stop){
			if(this.plugin.getTpsNotificationThreshold()>=this.plugin.getTPS()){
				final HighLagEvent e = new HighLagEvent(this.plugin.getTPS());
				for(final LagListener l: this.plugin.getLagListeners())
					new Thread(new Runnable(){
						@Override
						public void run(){
							l.onHighLagEvent(e);
						}
					}).start();
			}
			try{
				Thread.sleep(this.plugin.getCheckMemoryInterval());
			}catch(final InterruptedException e){
				e.printStackTrace();
			}
		}
	}

	public void stop(){
		this.stop = true;
	}
}