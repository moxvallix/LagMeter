package com.moxvallix.listeners;

import com.moxvallix.events.LowMemoryEvent;

public interface MemoryListener{
	public void onLowMemoryEvent(LowMemoryEvent evt);
}