package com.webkonsept.minecraft.lagmeter.exceptions;

public class InvalidTimeFormatException extends Exception{
	private static final long serialVersionUID = 4210399963053309020L;

	public InvalidTimeFormatException(){
		super("");
	}

	public InvalidTimeFormatException(final String message){
		super(message);
	}

	@Override
	public void printStackTrace(){
		System.out.println(InvalidTimeFormatException.class.getPackage() + ".InvalidTimeFormatException" + (super.getMessage().length() > 0 ? ": " + super.getMessage() : ""));
	}
}