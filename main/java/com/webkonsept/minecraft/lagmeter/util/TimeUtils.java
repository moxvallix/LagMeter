package com.webkonsept.minecraft.lagmeter.util;

import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;

public class TimeUtils{


	/**
	 * Parses a string to get the amount of ticks equal to what the string
	 * passed represents.
	 *
	 * @param timeString The "human-readable" representation of time, where:
	 *                   <ul>
	 *                   <b>s</b> is seconds;<br>
	 *                   <b>m</b> is minutes;<br>
	 *                   <b>h</b> is hours;<br>
	 *                   <b>d</b> is days; and finally,<br>
	 *                   <b>w</b> is weeks.
	 *                   </ul>
	 *
	 * @return Amount of ticks which corresponds to this string of time.
	 *
	 * @throws com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException If the time format given is invalid (contains time delimiters other than s, m, h, d or w) or is empty.
	 * @see com.webkonsept.minecraft.lagmeter.LagMeter#parseTimeMS(String)
	 */
	public static long parseTime(String timeString) throws InvalidTimeFormatException{
		if(timeString.length() == 0){
			throw new InvalidTimeFormatException("Empty time string provided");
		}
		long time = 0L;
		StringBuilder z = new StringBuilder();
		for(int i = 0; i < timeString.length(); i++){
			final String c = timeString.substring(i, i + 1);
			if(c.matches("[^wdhms]")){
				z.append(c);
			}else{
				try{
					if(c.equalsIgnoreCase("w")){
						time += 12096000L * Long.parseLong(z.toString());
					}else if(c.equalsIgnoreCase("d")){
						time += 1728000L * Long.parseLong(z.toString());
					}else if(c.equalsIgnoreCase("h")){
						time += 72000L * Long.parseLong(z.toString());
					}else if(c.equalsIgnoreCase("m")){
						time += 1200L * Long.parseLong(z.toString());
					}else if(c.equalsIgnoreCase("s")){
						time += 20L * Long.parseLong(z.toString());
					}
					z = new StringBuilder();
				}catch(final NumberFormatException e){
					throw new InvalidTimeFormatException("The time for the time string " + timeString + " is invalid: the time string contains characters other than 0-9, w/d/h/m/s. Top-level exception: " + e.getMessage());
				}
			}
		}
		return time;
	}

	/**
	 * Parses the timeString given and returns milliseconds instead of ticks.
	 * Works in the same fashion as parseTime(String).
	 *
	 * @param timeString The string of time, where:
	 *                   <ul>
	 *                   <b>s</b> is seconds;<br>
	 *                   <b>m</b> is minutes;<br>
	 *                   <b>h</b> is hours;<br>
	 *                   <b>d</b> is days; and finally,<br>
	 *                   <b>w</b> is weeks.
	 *                   </ul>
	 *
	 * @return The amount of milliseconds that would equate to the time string
	 * given.
	 *
	 * @throws com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException If the timeString is in an invalid format (i.e. invalid
	 *                                                                                 characters) or the result is less than 1.
	 * @see com.webkonsept.minecraft.lagmeter.LagMeter#parseTime(String)
	 */
	public static long parseTimeMS(String timeString) throws InvalidTimeFormatException{
		return (parseTime(timeString) * 50L);
	}
}
