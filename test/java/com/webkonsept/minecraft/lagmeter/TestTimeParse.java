package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class TestTimeParse{
	private LagMeter lagMeter;

	public TestTimeParse(){
		this.lagMeter = LagMeterTest.get();
	}

	@Test
	public void seconds() throws InvalidTimeFormatException{
		assertEquals(5000L, this.lagMeter.parseTimeMS("<>5s"));
	}

	@Test
	public void seconds2() throws InvalidTimeFormatException{
		assertEquals(500000L, this.lagMeter.parseTimeMS("<>500s"));
	}

	@Test
	public void minutes() throws InvalidTimeFormatException{
		assertEquals(300000L, this.lagMeter.parseTimeMS("<>5m"));
	}

	@Test
	public void hours() throws InvalidTimeFormatException{
		assertEquals(18000000L, this.lagMeter.parseTimeMS("<>5h"));
	}

	@Test
	public void days() throws InvalidTimeFormatException{
		assertEquals(432000000L, this.lagMeter.parseTimeMS("<>5d"));
	}

	@Test
	public void weeks() throws InvalidTimeFormatException{
		assertEquals(18000000L, this.lagMeter.parseTimeMS("<>5h"));
	}
}
