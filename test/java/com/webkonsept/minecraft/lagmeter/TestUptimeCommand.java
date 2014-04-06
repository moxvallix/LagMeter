package com.webkonsept.minecraft.lagmeter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class TestUptimeCommand{
	@Test
	public void withoutTime(){
		assertEquals(new UptimeCommand("/command").getCommand(), "/command");
	}

	public void time(){
		assertEquals(new UptimeCommand("/command<>5m").getCommand(), "/command");
	}
}