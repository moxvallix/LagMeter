package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.exceptions.EmptyCommandException;
import com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import com.webkonsept.minecraft.lagmeter.util.SyncCommand;
import com.webkonsept.minecraft.lagmeter.util.TimeUtils;

import java.util.logging.Level;

public class TimedCommand implements Runnable{
	private final String[] commands;
	private final long time;
	private final LagMeter plugin;
	@Deprecated
	private final String command;


	/**
	 * For <i>n</i> commands, each command from <i>command 0</i> to
	 * <i>command n</i> should be semicolon-delimited. After all commands,
	 * A time string should follow, separated from the commands by {@code <>}.<br />
	 * <br />
	 * An example that would follow this pattern is:
	 * {@code /command 0;/command 1;...;/command n<>15s}
	 * @param command The commands to be run and the time to run it after.
	 * @param plugin The LagMeter instance running on the server.
	 *
	 * @throws EmptyCommandException If the command is empty. This was going to be
	 * IllegalArgumentException, but runtime exceptions aren't good for anyone
	 * @throws InvalidTimeFormatException If the
	 */
	public TimedCommand(String command, LagMeter plugin) throws EmptyCommandException, InvalidTimeFormatException{
		String[] s = command.split("<>");

		if(s.length == 0)
			throw new EmptyCommandException();
		else if(s.length == 1) {
			this.time = 0;
		}else{
			this.time = TimeUtils.parseTimeMS(s[1]);
		}

		this.commands = s[0].split(";");

		this.command = command;
		this.plugin = plugin;
	}

	/**
	 * Gets the time after which this command will run, in
	 * milliseconds, not ticks.
	 *
	 * @return The number of milliseconds after which this command
	 * will run
     */
	public long getTime(){
		return this.time;
	}

	/**
	 * Gets the commands that will be executed by
	 * this timed command grouping.
	 *
	 * This is intended to be immutable; it is
	 * simply used to get what commands will be
	 * executed.
	 *
	 * @return The commands to be executed by this
	 * timed command "group"
     */
	public String[] getCommands(){
		return this.commands;
	}

	/**
	 * This will block the thread running this code for as long as
	 * is necessary for the command wait time. After the thread is woken
	 * up by the OS, it will execute the command and immediately exit.<br />
	 * <br />
	 * If an instance of this class is kept in memory, the reference will stay
	 * around (though the thread will still exit), so be sure to check
	 * references.<br />
	 * <br />
	 * As this code blocks the thread running it, it should <b>never</b>
	 * be executed on the main thread.
	 */
	@Override
	public void run(){
		try{
			Thread.sleep(time);
		}catch(final InterruptedException e){
			//probably interrupted by server shutdown or plugin stop/reload
			StringBuilder c = new StringBuilder();
			for(String x : commands){
				c.append(x);
				c.append(' ');
			}
			plugin.getLogger().log(Level.WARNING, "Delayed command(s) %1$s interrupted early, is the server reloading or shutting down?", c.toString());
		}

		for(String cmd : this.getCommands()){
			new SyncCommand(cmd.startsWith("/") ? cmd.replaceFirst("/", "") : cmd).runTask(this.plugin);
		}
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("TimedCommand@");
		sb.append(hashCode());
		sb.append('{');

		for(String s : this.getCommands()){
			sb.append("\n\t");
			sb.append(s);
		}
		sb.append("\n}");

		return sb.toString();
	}

	/**
	 * Gets the time string for this command, unformatted,
	 * or the default (0s) for one that does not exist.<br />
	 * <br />
	 * This method will be removed as soon as possible;
	 * you should instead use {@link #getTime()}.
	 *
	 * @return An unformatted time string that may or may not contain
	 * time delimiters (h, m, s), and may be invalid. If it does not exist,
	 * {@code "0s"} will be returned instead.
	 */
	@Deprecated
	public String getTimeString(){
		String[] s = this.command.split("<>");
		return s.length > 1 ? s[1] : "0s";
	}

	/**
	 *
	 *
	 * @return
	 * @throws InvalidTimeFormatException If the time is not a valid
	 * time string as per {@link TimeUtils#parseTimeMS(String)}
	 */
	@Deprecated
	public long getInterval() throws InvalidTimeFormatException{
		return TimeUtils.parseTimeMS(this.getTimeString());
	}

	/**
	 * This method's functionality has been moved to
	 * {@link #run()}. This method will be removed soon, do not use it.<br />
	 *
	 * @see #run()
	 * @param s No longer used.
     */
	@Deprecated
	public void process(String s){
		this.run();
	}
}