package com.moxvallix.events;

import com.moxvallix.LagMeter;

public class HighLagEvent{
	private final double TPS;

	public double getCurrentTPS(){
		return this.TPS;
	}

	public LagMeter getLagMeter(){
		return LagMeter.getInstance();
	}

	public HighLagEvent(final double tps){
		this.TPS = tps;
	}

	@Override
	public String toString(){
		return "HighLagEvent@"+hashCode()+"{\n\tTPS = "+this.TPS+"\n}";
	}
}