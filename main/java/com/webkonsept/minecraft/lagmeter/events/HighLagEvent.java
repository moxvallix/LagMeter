package main.java.com.webkonsept.minecraft.lagmeter.events;

import main.java.com.webkonsept.minecraft.lagmeter.LagMeter;

public class HighLagEvent{
	private final float	TPS;

	public float getCurrentTPS(){
		return this.TPS;
	}

	public LagMeter getLagMeter(){
		return LagMeter.getInstance();
	}

	public HighLagEvent(final float tps){
		this.TPS = tps;
	}
}