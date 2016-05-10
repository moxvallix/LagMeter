package com.webkonsept.minecraft.lagmeter.exceptions;

public class EmptyCommandException extends Exception{
    public EmptyCommandException(){
        this("Command cannot be empty");
    }

    public EmptyCommandException(String m){
        super(m);
    }
}
