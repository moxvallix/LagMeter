package com.webkonsept.minecraft.lagmeter.listeners;

import com.webkonsept.minecraft.lagmeter.events.HighLagEvent;

public interface LagListener{
	public void onHighLagEvent(HighLagEvent evt);
}