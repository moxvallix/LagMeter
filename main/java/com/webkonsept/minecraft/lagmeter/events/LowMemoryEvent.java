package main.java.com.webkonsept.minecraft.lagmeter.events;

public class LowMemoryEvent{
	private final double freeMemory;
	private final double usedMemory;
	private final double maxMemory;
	private final double percentageFree;
	private final float currentTPS;

	public LowMemoryEvent(double[] values, float tps){
		this.usedMemory = values[0];
		this.maxMemory = values[1];
		this.freeMemory = values[2];
		this.percentageFree = values[3];
		this.currentTPS = tps;
	}

	public double getMaximumMemory(){
		return this.maxMemory;
	}

	public double getUsedMemory(){
		return this.usedMemory;
	}

	public double getFreeMemory(){
		return this.freeMemory;
	}

	public double getMemoryFreePercentage(){
		return this.percentageFree;
	}

	public float getCurrentTPS(){
		return this.currentTPS;
	}
}