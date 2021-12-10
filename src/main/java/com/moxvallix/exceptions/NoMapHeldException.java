package com.moxvallix.exceptions;

public class NoMapHeldException extends Exception{
	private static final long serialVersionUID = -1L;

	public NoMapHeldException(){
		this("You have to be holding a map to do that.");
	}

	public NoMapHeldException(String message){
		super(message);
	}
}