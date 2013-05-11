package main.java.com.webkonsept.minecraft.lagmeter.listeners;

import main.java.com.webkonsept.minecraft.lagmeter.events.LowMemoryEvent;

public interface MemoryListener{
	public void onLowMemoryEvent(LowMemoryEvent evt);
}