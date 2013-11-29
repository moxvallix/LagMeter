package com.webkonsept.minecraft.lagmeter.exceptions;

public class NoMapHeldException extends Exception{
	public NoMapHeldException(){
		super("");
	}

	public NoMapHeldException(String message){
		super(message);
	}
}