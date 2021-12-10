package com.moxvallix.exceptions;

public class NoAvailableTPSException extends Exception{
	public NoAvailableTPSException(){
		this("No TPS has been polled yet; wait until the delay expires.");
	}

	public NoAvailableTPSException(String message){
		super(message);
	}
}