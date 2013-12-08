package com.webkonsept.minecraft.lagmeter.exceptions;

public class NoMapHeldException extends Exception{
	public NoMapHeldException(){
		super("You have to be holding a map to do that.");
	}

	public NoMapHeldException(String message){
		super(message);
	}
}