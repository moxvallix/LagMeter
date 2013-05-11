package main.java.com.webkonsept.minecraft.lagmeter.listeners;

import main.java.com.webkonsept.minecraft.lagmeter.events.HighLagEvent;

public interface LagListener{
	public void onHighLagEvent(HighLagEvent evt);
}