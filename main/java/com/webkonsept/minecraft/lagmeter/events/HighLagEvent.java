package main.java.com.webkonsept.minecraft.lagmeter.events;

import main.java.com.webkonsept.minecraft.lagmeter.LagMeter;

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
}