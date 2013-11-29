package com.webkonsept.minecraft.lagmeter;

import org.bukkit.entity.Player;
import org.bukkit.map.*;

public class LagMapRenderer extends MapRenderer{
	private int ticks;
	private int targetTicks;

	public LagMapRenderer(){
		this.ticks = 0;
		this.targetTicks = 100;
	}

	public LagMapRenderer(int seconds){
		this.ticks = 0;
		this.targetTicks = seconds * 20;
	}

	@Override
	public void render(MapView map, MapCanvas canvas, Player p){
		if(this.ticks++ % this.targetTicks == 0){
			if(this.ticks > 1500){
				for(int x = 0; x < 128; x++)
					for(int y = 0; y < 128; y++)
						canvas.setPixel(x, y, MapPalette.TRANSPARENT);

				double[] d = LagMeter.getInstance().getMemory();
				canvas.drawText(0, 8, MinecraftFont.Font, String.format("TPS: %,.2f", LagMeter.getInstance().getTPS()));
				canvas.drawText(0, 16, MinecraftFont.Font, String.format("Memory used: %,.2f MB", d[0]));
				canvas.drawText(0, 24, MinecraftFont.Font, String.format("Memory free: %,.2f MB", d[1]));
				canvas.drawText(0, 32, MinecraftFont.Font, String.format("%% of memory used: %,.2f%%", d[3]));
			}else{
				canvas.drawText(0, 8, MinecraftFont.Font, "Waiting for polling");
				canvas.drawText(0, 16, MinecraftFont.Font, "delay to expire...");
			}
			p.sendMap(map);
		}
	}
}