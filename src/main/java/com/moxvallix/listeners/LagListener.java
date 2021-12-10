package com.moxvallix.listeners;

import com.moxvallix.events.HighLagEvent;

public interface LagListener{
	public void onHighLagEvent(HighLagEvent evt);
}