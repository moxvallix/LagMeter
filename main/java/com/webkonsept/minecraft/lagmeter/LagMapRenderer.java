package com.webkonsept.minecraft.lagmeter;

import org.bukkit.entity.Player;
import org.bukkit.map.*;

public class LagMapRenderer extends MapRenderer{
	private int ticks;

	public LagMapRenderer(){
		this.ticks = 0;
	}

	public void render(MapView map, MapCanvas canvas, Player p){
		if(this.ticks++ % 100 == 0){
			if(this.ticks > 1500){
				double[] d = LagMeter.getInstance().getMemory();
				canvas.drawText(0, 8, MinecraftFont.Font, String.format("TPS: %,.2f", LagMeter.getInstance().getTPS()));
				canvas.drawText(0, 16, MinecraftFont.Font, String.format("Memory used: %,.2f MB", d[0]));
				canvas.drawText(0, 24, MinecraftFont.Font, String.format("Memory free: %,.2f MB", d[1]));
				canvas.drawText(0, 32, MinecraftFont.Font, String.format("Memory %% used: %,.2f", d[3]));
			}else{
				canvas.drawText(0, 8, MinecraftFont.Font, "Waiting for polling");
				canvas.drawText(0, 16, MinecraftFont.Font, "delay to expire...");
			}
		}
	}
}