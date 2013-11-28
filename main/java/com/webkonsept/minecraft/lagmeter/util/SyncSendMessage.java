package com.webkonsept.minecraft.lagmeter.util;

import com.webkonsept.minecraft.lagmeter.LagMeter;
import com.webkonsept.minecraft.lagmeter.LagMeter.Severity;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class SyncSendMessage extends BukkitRunnable{
	CommandSender sender;
	Severity severity;
	String message;

	@Override
	public void run(){
		LagMeter.getInstance().sendMessage(this.sender, this.severity, this.message);
	}

	public SyncSendMessage(final CommandSender sender, final Severity severity, final String message){
		this.sender = sender;
		this.severity = severity;
		this.message = message;
	}
}