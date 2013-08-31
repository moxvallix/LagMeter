package main.java.com.webkonsept.minecraft.lagmeter.exceptions;

public class InvalidTimeFormatException extends Exception{
	private static final long serialVersionUID = 4210399963053309020L;
	private final String message;

	@Override
	public String getMessage(){
		return this.message;
	}

	@Override
	public void printStackTrace(){
		System.out.println(InvalidTimeFormatException.class.getPackage()+"InvalidTimeFormatException: "+this.getMessage());
	}

	public InvalidTimeFormatException(){
		this.message = "";
	}

	public InvalidTimeFormatException(final String message){
		this.message = message;
	}
}