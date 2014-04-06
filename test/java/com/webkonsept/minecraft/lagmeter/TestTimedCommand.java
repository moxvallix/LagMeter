package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestTimedCommand{
	@Test
	public void singleCommand(){
		assertArrayEquals(new String[]{"/command"}, new TimedCommand("/command<>5s", null).getCommands());
	}

	@Test
	public void multipleCommands(){
		assertArrayEquals(new String[]{"/command", "/frogs"}, new TimedCommand("/command;/frogs<>5s", null).getCommands());
	}

	@Test
	public void singleCommandTime() throws InvalidTimeFormatException{
		assertEquals(new TimedCommand("/command<>500s", null).getInterval(), 500000L);
	}

	@Test
	public void multipleCommandsTime() throws InvalidTimeFormatException{
		assertEquals(new TimedCommand("/command;/frogs<>500s", null).getInterval(), 500000L);
	}
}