package com.webkonsept.minecraft.lagmeter.exceptions;

public class NoAvailableTPSException extends Exception{
	public NoAvailableTPSException(){
		super("No TPS has been polled yet; wait until the delay expires.");
	}

	public NoAvailableTPSException(String message){
		super(message);
	}
}