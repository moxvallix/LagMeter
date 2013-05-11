package main.java.com.webkonsept.minecraft.lagmeter.events;

public class HighLagEvent{
	private final float TPS;

	public HighLagEvent(float tps){
		this.TPS = tps;
	}

	public float getCurrentTPS(){
		return this.TPS;
	}
}