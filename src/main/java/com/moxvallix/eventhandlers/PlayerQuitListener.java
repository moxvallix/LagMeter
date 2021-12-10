package com.moxvallix.eventhandlers;

import com.moxvallix.LagMeter;
import com.moxvallix.exceptions.NoActiveLagMapException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener{
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuitEvent(PlayerQuitEvent evt){
		LagMeter.getInstance().removePlayerIP(evt.getPlayer().getName());
		try{
			LagMeter.getInstance().turnLagMapOff(evt.getPlayer());
		}catch(NoActiveLagMapException e){
			//do nothing
		}
	}

	@Override
	public String toString(){
		return super.toString();
	}
}