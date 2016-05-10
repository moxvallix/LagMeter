package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.EmptyCommandException;
import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestTimedCommand{
	@Test
	public void singleCommand() throws InvalidTimeFormatException, EmptyCommandException{
		assertArrayEquals(new String[]{"/command"}, new TimedCommand("/command<>5s", null).getCommands());
	}

	@Test
	public void multipleCommands() throws InvalidTimeFormatException, EmptyCommandException{
		assertArrayEquals(new String[]{"/command", "/frogs"}, new TimedCommand("/command;/frogs<>5s", null).getCommands());
	}

	@Test
	public void singleCommandTime() throws InvalidTimeFormatException, EmptyCommandException{
		assertEquals(new TimedCommand("/command<>500s", null).getTime(), 500000L);
	}

	@Test
	public void multipleCommandsTime() throws InvalidTimeFormatException, EmptyCommandException{
		assertEquals(new TimedCommand("/command;/frogs<>500s", null).getTime(), 500000L);
	}

	@Test
	public void noTime() throws InvalidTimeFormatException, EmptyCommandException{
		assertEquals(0L, new TimedCommand("/command", null).getInterval());
		assertArrayEquals(new String[]{"/command"}, new TimedCommand("/command", null).getCommands());
	}
}