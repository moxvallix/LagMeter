package main.java.com.webkonsept.minecraft.lagmeter;

public class InvalidTimeFormatException extends Throwable{
	private static final long serialVersionUID = 4210399963053309020L;
	private final String message;

	public InvalidTimeFormatException(){
		this.message = "";
	}

	public InvalidTimeFormatException(String message){
		this.message = message;
	}

	@Override
	public String getMessage(){
		return this.message;
	}

	@Override
	public void printStackTrace(){
		System.out.println(InvalidTimeFormatException.class.getPackage()+"InvalidTimeFormatException: "+this.getMessage());
	}
}