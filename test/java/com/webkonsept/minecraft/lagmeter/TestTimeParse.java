package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import com.webkonsept.minecraft.lagmeter.util.TimeUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class TestTimeParse{
	@Test
	public void seconds() throws InvalidTimeFormatException{
		assertEquals(5000L, TimeUtils.parseTimeMS("5s"));
	}

	@Test
	public void seconds2() throws InvalidTimeFormatException{
		assertEquals(500000L, TimeUtils.parseTimeMS("500s"));
	}

	@Test
	public void minutes() throws InvalidTimeFormatException{
		assertEquals(300000L, TimeUtils.parseTimeMS("5m"));
	}

	@Test
	public void hours() throws InvalidTimeFormatException{
		assertEquals(18000000L, TimeUtils.parseTimeMS("5h"));
	}

	@Test
	public void days() throws InvalidTimeFormatException{
		assertEquals(432000000L, TimeUtils.parseTimeMS("5d"));
	}

	@Test
	public void weeks() throws InvalidTimeFormatException{
		assertEquals(3024000000L, TimeUtils.parseTimeMS("5w"));
	}

	@Test
	public void everything() throws InvalidTimeFormatException{
		assertEquals(3474305000L, TimeUtils.parseTimeMS("5w5d5h5m5s"));
	}
}
