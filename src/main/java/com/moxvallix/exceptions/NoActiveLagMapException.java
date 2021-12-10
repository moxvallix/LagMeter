package com.moxvallix.exceptions;

public class NoActiveLagMapException extends Exception{
	public NoActiveLagMapException(){
		this("You don't currently have a LagMap active.");
	}

	public NoActiveLagMapException(String message){
		super(message);
	}
}