package com.moxvallix;

import com.moxvallix.exceptions.NoAvailableTPSException;
import org.bukkit.entity.Player;
import org.bukkit.map.*;

public class LagMapRenderer extends MapRenderer{
	private int ticks;
	private Integer targetTicks;

	public LagMapRenderer(){
		this(100);
	}

	public LagMapRenderer(int seconds){
		super();
		this.ticks = 0;
		this.targetTicks = seconds * 20;
	}

	@Override
	public void render(MapView map, MapCanvas canvas, Player p){
		if(this.ticks++ % this.targetTicks == 0)
			try{
				double[] d;

				for(int x = 0; x < 128; x++)
					for(int y = 0; y < 128; y++)
						canvas.setPixel(x, y, MapPalette.TRANSPARENT);

				canvas.drawText(0, 8, MinecraftFont.Font, String.format("TPS: %,.2f", LagMeter.getInstance().getTPS()));

				d = LagMeter.getInstance().getMemory();
				canvas.drawText(0, 16, MinecraftFont.Font, String.format("Memory used: %,.2f MB", d[0]));
				canvas.drawText(0, 24, MinecraftFont.Font, String.format("Max memory: %,.2f MB", d[1]));
				canvas.drawText(0, 32, MinecraftFont.Font, String.format("Memory free: %,.2f MB", d[2]));
				canvas.drawText(0, 40, MinecraftFont.Font, String.format("%% of memory free: %,.2f%%", d[3]));
			}catch(NoAvailableTPSException e){
				canvas.drawText(0, 8, MinecraftFont.Font, "Waiting for polling");
				canvas.drawText(0, 16, MinecraftFont.Font, "delay to expire...");
			}finally{
				p.sendMap(map);
			}
	}

	@Override
	public String toString(){
		return "LagMapRenderer@"+hashCode()+"{\n\tticks = "+this.ticks+"\n\ttargetTicks = "+this.targetTicks+"\n}";
	}
}