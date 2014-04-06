package com.webkonsept.minecraft.lagmeter.util;

import com.webkonsept.minecraft.lagmeter.LagMeter;
import com.webkonsept.minecraft.lagmeter.LagMeter.Severity;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
		super();
		this.sender = sender;
		this.severity = severity;
		this.message = message;
	}

	@Override
	public String toString(){
		return "SyncSendMessage@"+hashCode()+"{\n\ttarget = "+sender.getName() + " (UUID: "+(this.sender instanceof Player ? ((Player)this.sender).getUniqueId() : "null -- non-player entity")+")\n\tseverity = "+this.severity+"\n\tmessage = "+this.message+"\n}";
	}
}