package com.moxvallix;

import com.moxvallix.LagMeter.Severity;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LagMeterLogger{
	private final LagMeter plugin;
	private String error = "*shrug* Dunno.";
	private boolean logMemory = true;
	private boolean logTPS = true;
	private boolean enabled = false;
	private String timeFormat;
	private File logfile;
	private PrintWriter log;

	private boolean beginLogging() throws Exception{
		boolean ret = true;
		if(this.logfile == null){
			this.error("Logfile is null");
			ret = false;
		}else if(!this.logMemory && !this.logTPS){
			this.error("Both logMemory and logTPS are disabled. Nothing to log!");
			ret = false;
		}else{
			try{
				if(!this.logfile.exists()){
					if(!this.logfile.createNewFile()){
						throw new Exception("Unable to create new logfile!");
					}
				}
				this.log = new PrintWriter(new FileWriter(this.logfile, true));
				this.log("Logging enabled.");
			}catch(final IOException e){
				e.printStackTrace();
				this.error("IOException opening logfile!");
				ret = false;
			}
		}
		this.enabled = true;
		return ret;
	}

	private void closeLog() throws Exception{
		if((this.log != null) && this.enabled){
			this.log.flush();
			this.log.close();
			this.log = null;
			this.enabled = false;
		}
	}

	public void disable() throws Exception{
		if(this.plugin.isLoggingEnabled()){
			this.closeLog();
		}
	}

	public boolean enable(){
		if(!this.plugin.isUsingLogFolder()){
			this.plugin.sendConsoleMessage(Severity.INFO, "Not using logs folder.");
			return this.enable(new File(this.plugin.getDataFolder(), "lag.log"));
		}else{
			this.plugin.sendConsoleMessage(Severity.INFO, "Using logs folder. This will create a new log for each day (it might log data from tomorrow in today's file if you leave the server running without reloading/restarting).");
			return this.enable(new File("plugins" + File.separator + "LagMeter" + File.separator + "logs", "LagMeter-" + this.today() + ".log"));
		}
	}

	public boolean enable(final File logTo){
		this.logfile = logTo;
		try{
			return this.beginLogging();
		}catch(final Exception e){
			e.printStackTrace();
		}
		return false;
	}

	private void error(final String errorMessage){
		this.error = errorMessage;
	}

	public String getError(){
		return this.error;
	}

	public String getFilename(){
		if(this.logfile != null){
			return this.logfile.getAbsolutePath();
		}else{
			return "!! UNKNOWN !!";
		}
	}

	public String getTimeFormat(){
		return this.timeFormat;
	}

	public boolean isEnabled(){
		return this.enabled;
	}

	protected void log(String message){
		if(this.enabled){
			message = "[" + this.now() + "] " + message;
			final String newLine = this.plugin.isUsingNewLineForLogStats() ? "\n" : "  ";
			this.log.print(message);
			if(this.plugin.isLoggingChunks()){
				int totalChunks = 0;
				for(final World world : Bukkit.getServer().getWorlds()){
					totalChunks += world.getLoadedChunks().length;
					if(!this.plugin.isLoggingTotalChunksOnly()){
						this.log.print(newLine + "Chunks loaded in world \"" + world.getName() + "\": " + world.getLoadedChunks().length);
					}
				}
				this.log.print(newLine + "Total chunks loaded: " + totalChunks);
			}
			if(this.plugin.isLoggingEntities()){
				int totalEntities = 0;
				for(final World world : Bukkit.getServer().getWorlds()){
					totalEntities += world.getEntities().size();
					if(!this.plugin.isLoggingTotalEntitiesOnly()){
						this.log.print(newLine + "Entities in world \"" + world.getName() + "\": " + world.getEntities().size());
					}
				}
				this.log.print(newLine + "Total entities: " + totalEntities);
			}
			if(this.plugin.isUsingNewBlockEveryLog()){
				this.log.println();
			}
			this.log.println();
			this.log.flush();
		}
	}

	public boolean logMemory(){
		return this.logMemory;
	}

	public void logMemory(final boolean set){
		this.logMemory = set;
		if((!this.logMemory) && (!this.logTPS)){
			try{
				this.disable();
			}catch(final FileNotFoundException e){
				e.printStackTrace();
			}catch(final IOException e){
				e.printStackTrace();
			}catch(final Exception e){
				e.printStackTrace();
			}
			this.error("Both log outputs disabled! Logging disabled.");
		}
	}

	public boolean logTPS(){
		return this.logTPS;
	}

	public void logTPS(final boolean set){
		this.logTPS = set;
		if((!this.logMemory) && (!this.logTPS)){
			try{
				this.disable();
			}catch(final FileNotFoundException e){
				e.printStackTrace();
			}catch(final IOException e){
				e.printStackTrace();
			}catch(final Exception e){
				e.printStackTrace();
			}
			this.error("Both log outputs disabled! Logging disabled.");
		}
	}

	public String now(){
		final Calendar cal = Calendar.getInstance();
		final SimpleDateFormat sdf = new SimpleDateFormat(this.getTimeFormat());
		return sdf.format(cal.getTime());
	}

	public void setTimeFormat(final String newFormat){
		this.timeFormat = newFormat;
	}

	public String today(){
		final Calendar calendar = Calendar.getInstance();
		final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
		return sdf.format(calendar.getTime());
	}

	protected LagMeterLogger(final LagMeter instance){
		this(instance, false);
	}

	protected LagMeterLogger(final LagMeter instance, final boolean enable){
		super();
		this.plugin = instance;
		this.timeFormat = "MM-dd-yyyy HH:mm:ss";
		this.logfile = null;
		this.log = null;

		if(enable)
			this.enable();
	}

	public String toString(){
		return "LagMeterLogger@"+hashCode()+"{\n\tlogMemory = "+this.logMemory+"\n\tlogTPS = "+this.logTPS+"\n\tenabled = "+this.enabled+"\n\ttimeFormat = "+this.timeFormat+"logfile = "+this.logfile.getAbsolutePath()+"\n}";
	}
}