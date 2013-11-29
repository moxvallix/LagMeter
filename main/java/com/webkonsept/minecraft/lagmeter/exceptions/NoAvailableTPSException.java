package com.webkonsept.minecraft.lagmeter.exceptions;

public class NoAvailableTPSException extends Exception{
	public NoAvailableTPSException(){
		super("");
	}

	public NoAvailableTPSException(String message){
		super(message);
	}
}