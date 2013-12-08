package com.webkonsept.minecraft.lagmeter.exceptions;

public class NoActiveLagMapException extends Exception{
	public NoActiveLagMapException(){
		super("You don't currently have a LagMap active.");
	}

	public NoActiveLagMapException(String message){
		super(message);
	}
}