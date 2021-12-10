package com.moxvallix.exceptions;

public class InvalidTimeFormatException extends Exception{
	public InvalidTimeFormatException(){
		this("An uptime command has an invalid format. Please fix your configuration.");
	}

	public InvalidTimeFormatException(final String message){
		super(message);
	}
}