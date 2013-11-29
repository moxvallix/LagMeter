package com.webkonsept.minecraft.lagmeter.exceptions;

public class NoActiveLagMapException extends Exception{
	public NoActiveLagMapException(){
		super("");
	}

	public NoActiveLagMapException(String message){
		super(message);
	}
}