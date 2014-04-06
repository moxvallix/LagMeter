package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

//@RunWith(JUnit4.class)
public class LagMeterTest{
	private static LagMeter lagMeter;

	static{
		assertNull(lagMeter);
		lagMeter = new LagMeter();
		assertNotNull(lagMeter);
	}

	public static LagMeter get(){
		return lagMeter;
	}

	public LagMeterTest(){
	}
}