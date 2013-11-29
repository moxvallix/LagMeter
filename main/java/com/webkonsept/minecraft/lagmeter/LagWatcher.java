package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.NoAvailableTPSException;

public final class LagWatcher implements Runnable{
	private final LagMeter plugin;
	private boolean stop;

	@Override
	public void run(){
		while(!this.stop){
			try{
				if((this.plugin.getTpsNotificationThreshold() >= this.plugin.getTPS()) && (this.plugin.getTPS() >= 0)){
					this.plugin.notifyLagListeners();
				}
				try{
					Thread.sleep(this.plugin.getCheckMemoryInterval());
				}catch(final InterruptedException e){
					// do nothing, interruption probably means the server is
					// shutting down or reloading.
				}
			}catch(NoAvailableTPSException e){
				try{
					Thread.sleep((this.plugin.getCheckLagInterval()));
				}catch(InterruptedException ex){
					//do nothing
				}
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