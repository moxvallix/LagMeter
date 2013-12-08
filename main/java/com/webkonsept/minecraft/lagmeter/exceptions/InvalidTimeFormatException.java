package com.webkonsept.minecraft.lagmeter.exceptions;

public class InvalidTimeFormatException extends Exception{

	public InvalidTimeFormatException(){
		super("An uptime command has an invalid format. Please fix your configuration.");
	}

	public InvalidTimeFormatException(final String message){
		super(message);
	}
}