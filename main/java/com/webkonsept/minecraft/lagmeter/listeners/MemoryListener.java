package com.webkonsept.minecraft.lagmeter.listeners;

import com.webkonsept.minecraft.lagmeter.events.LowMemoryEvent;

public interface MemoryListener{
	public void onLowMemoryEvent(LowMemoryEvent evt);
}