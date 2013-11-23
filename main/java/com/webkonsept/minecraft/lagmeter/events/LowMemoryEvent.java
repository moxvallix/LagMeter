package main.java.com.webkonsept.minecraft.lagmeter.events;

import main.java.com.webkonsept.minecraft.lagmeter.LagMeter;

public class LowMemoryEvent{
    private final double freeMemory;
    private final double usedMemory;
    private final double maxMemory;
    private final double percentageFree;
    private final double currentTPS;

    public double getCurrentTPS(){
        return this.currentTPS;
    }

    public double getFreeMemory(){
        return this.freeMemory;
    }

    public LagMeter getLagMeter(){
        return LagMeter.getInstance();
    }

    public double getMaximumMemory(){
        return this.maxMemory;
    }

    public double getMemoryFreePercentage(){
        return this.percentageFree;
    }

    public double getUsedMemory(){
        return this.usedMemory;
    }

    public LowMemoryEvent(final double[] values, final double tps){
        this.usedMemory = values[0];
        this.maxMemory = values[1];
        this.freeMemory = values[2];
        this.percentageFree = values[3];
        this.currentTPS = tps;
    }
}