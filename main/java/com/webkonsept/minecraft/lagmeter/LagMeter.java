package main.java.com.webkonsept.minecraft.lagmeter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class LagMeter extends JavaPlugin{
	protected LagMeterLogger logger;
	protected LagMeterPoller poller;
	protected LagMeterStack history;
	protected float ticksPerSecond = 20;
	protected long uptime;
	protected int averageLength = 10, sustainedLagTimer;
	protected double memUsed = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576, memMax = Runtime.getRuntime().maxMemory()/1048576, memFree = this.memMax-this.memUsed, percentageFree = 100/this.memMax*this.memFree;
	// Configurable Values - mostly booleans
	protected int interval = 40, logInterval = 150, lagNotifyInterval, memNotifyInterval, lwTaskID, mwTaskID;
	protected float tpsNotificationThreshold, memoryNotificationThreshold;
	protected boolean useAverage = true, enableLogging = true, useLogsFolder = true, AutomaticLagNotificationsEnabled, AutomaticMemoryNotificationsEnabled, displayEntities, playerLoggingEnabled, displayChunksOnLoad, sendChunks, logChunks, logTotalChunksOnly, logEntities, logTotalEntitiesOnly, newBlockPerLog, displayEntitiesOnLoad, newLineForLogStats, repeatingUptimeCommands;
	protected List<String> uptimeCommands;
	protected String highLagCommand, lowMemCommand, pingDomain;
	/** Static accessor */
	public static LagMeter p;

	public static void main(String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(final ClassNotFoundException e){
			e.printStackTrace();
		}catch(final InstantiationException e){
			e.printStackTrace();
		}catch(final IllegalAccessException e){
			e.printStackTrace();
		}catch(final UnsupportedLookAndFeelException e){
			e.printStackTrace();
		}finally{
			JOptionPane.showMessageDialog(null, "Sorry, but LagMeter is a Bukkit plugin, and cannot be run directly like you've attempted.\nTo use the plugin, download and set up a Bukkit Minecraft server, and in the root directory, create a folder called\n\"plugins\" (no quotes, and assuming it hasn't already been created for you), and put this JAR file (LagMeter.jar) there.\nWhen you've done that, start the Bukkit server using the command line java -jar \"path to Bukkit.jar\",\nor if it's already running, type \"reload\" (no quotes) into the command-line.", "LagMeter", JOptionPane.OK_OPTION);
			System.exit(0);
		}
	}

	@Override
	public void onEnable(){
		this.uptime = System.currentTimeMillis();
		final File logsFolder = new File("plugins"+File.separator+"LagMeter"+File.separator+"logs");
		LagMeter.p = this;
		this.logger = new LagMeterLogger(this);
		this.poller = new LagMeterPoller(this);
		this.history = new LagMeterStack();
		new LagMeterConfig(this).loadConfig();
		if(!logsFolder.exists()&&this.useLogsFolder&&this.enableLogging){
			this.info("Logs folder not found. Attempting to create one for you.");
			logsFolder.mkdir();
			if(!logsFolder.exists())
				this.severe("Error! Couldn't create the folder!");
			else
				this.info("Logs folder created.");
		}
		if(this.enableLogging){
			this.poller.setLogInterval(this.logInterval);
			if(!this.logger.enable())
				this.severe("Logging is disabled because: "+this.logger.getError());
		}
		this.history.setMaxSize(this.averageLength);
		super.getServer().getScheduler().scheduleSyncRepeatingTask(this, this.poller, 0, this.interval);
		final String loggingMessage = this.enableLogging ? " Logging to "+this.logger.getFilename()+"." : "";
		this.info("Enabled! Polling every "+this.interval+" server ticks."+loggingMessage);
		this.lwTaskID = this.AutomaticLagNotificationsEnabled ? super.getServer().getScheduler().scheduleSyncRepeatingTask(this, new LagWatcher(), this.lagNotifyInterval*1200, this.lagNotifyInterval*1200) : -1;
		this.mwTaskID = this.AutomaticMemoryNotificationsEnabled ? super.getServer().getScheduler().scheduleSyncRepeatingTask(this, new MemoryWatcher(), this.memNotifyInterval*1200, this.memNotifyInterval*1200) : -1;
		if(this.uptimeCommands!=null)
			for(final String s: this.uptimeCommands){
				long time;
				try{
					time = this.parseTime(s);
					if(this.repeatingUptimeCommands)
						super.getServer().getScheduler().scheduleSyncRepeatingTask(this, new UptimeCommand(s.split(";")[0]), time, time);
					else
						super.getServer().getScheduler().scheduleSyncDelayedTask(this, new UptimeCommand(s.split(";")[0]), time);
				}catch(final InvalidTimeFormatException e){
					e.printStackTrace();
				}
			}
		if(this.displayChunksOnLoad){
			this.info("Chunks loaded:");
			int total = 0;
			for(final World world: super.getServer().getWorlds()){
				final int chunks = world.getLoadedChunks().length;
				this.info("World \""+world.getName()+"\": "+chunks+".");
				total += chunks;
			}
			this.info("Total chunks loaded: "+total);
		}
		if(this.displayEntitiesOnLoad){
			this.info("Entities:");
			int total = 0;
			for(final World world: super.getServer().getWorlds()){
				final int entities = world.getEntities().size();
				this.info("World \""+world.getName()+"\": "+entities+".");
				total += entities;
			}
			this.info("Total entities: "+total);
		}
	}

	@Override
	public void onDisable(){
		this.info("Disabled!");
		if(!this.logger.isEnabled())
			try{
				this.logger.disable();
			}catch(final FileNotFoundException e){
				e.printStackTrace();
			}catch(final IOException e){
				e.printStackTrace();
			}catch(final Exception e){
				e.printStackTrace();
			}
		super.getServer().getScheduler().cancelTasks(this);
	}

	/**
	 * Gets the current memory used, maxmimum memory, memory free, and percentage of memory free. Returned in a single array of doubles.
	 * 
	 * @since 1.11.0-SNAPSHOT
	 * @return memory[], which is an array of doubles, containing four values, where: <br /> <b><i>memory[0]</i></b> is the currently used memory;<br /> <b><i>memory[1]</i></b> is the current maximum memory;<br /> <b><i>memory[2]</i></b> is the current free memory;<br /> <b><i>memory[3]</i></b> is the percentage memory free (note this may be an irrational number, so you might want to truncate it if you use this).
	 */
	public double[] getMemory(){
		final double[] memory = {0D, 0D, 0D, 0D};
		this.updateMemoryStats();
		memory[0] = this.memUsed;
		memory[1] = this.memMax;
		memory[2] = this.memFree;
		memory[3] = this.percentageFree;
		return memory;
	}

	/**
	 * Gets the ticks per second.
	 * 
	 * @since 1.8
	 * @return ticksPerSecond
	 */
	public float getTPS(){
		if(this.useAverage)
			return this.history.getAverage();
		return this.ticksPerSecond;
	}

	protected void handleBaseCommand(final CommandSender sender, final String[] args){
		if(args[0].equalsIgnoreCase("reload")){
			if(this.permit(sender, "lagmeter.command.lagmeter.reload")||this.permit(sender, "lagmeter.reload")){
				new LagMeterConfig(this).loadConfig();
				this.sendMessage(sender, 0, "Configuration reloaded!");
			}
		}else if(args[0].equalsIgnoreCase("help")){
			if(this.permit(sender, "lagmeter.command.lagmeter.help")||this.permit(sender, "lagmeter.help")){
				if(args.length==1||args[1].trim().equals("0")||args[1].trim().equals("1")){
					this.sendMessage(sender, 0, "*           *Help for LagMeter [1/2]*           *");
					if(this.permit(sender, "lagmeter.command.lag"))
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lag"+ChatColor.GOLD+" - Check the server's TPS. If configuChatColor.RED, may also display chunks loaded and/or entities alive.");
					if(this.permit(sender, "lagmeter.command.mem"))
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/mem"+ChatColor.GOLD+" - Displays how much memory the server currently has free.");
					if(this.permit(sender, "lagmeter.command.lagmem")||this.permit(sender, "lagmeter.command.lm"))
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lagmem|/lm"+ChatColor.GOLD+" - A combination of both /lag and /mem.");
					if(this.permit(sender, "lagmeter.command.lchunks"))
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lchunks"+ChatColor.GOLD+" - Shows how many chunks are currently loaded in each world, then with a total.");
					if(this.permit(sender, "lagmeter.command.lmobs")||this.permit(sender, "lagmeter.command.lentities"))
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lmobs|/lentities"+ChatColor.GOLD+" - Shows how many entities are currently alive in each world, then with a total.");
					if(this.permit(sender, "lagmeter.command.lmp"))
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lmp"+ChatColor.GOLD+" - Has the same function as /lagmem, but includes a player count.");
					if(this.permit(sender, "lagmeter.command.lagmeter"))
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lagmeter|/lm"+ChatColor.GOLD+" - Shows the current version and gives sub-commands.");
					if(this.permit(sender, "lagmeter.command.lagmeter.reload")||this.permit(sender, "lagmeter.reload"))
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lagmeter|/lm"+ChatColor.GREEN+" <reload|r> "+ChatColor.GOLD+" - Allows the player to reload the configuration.");
				}else if(args.length>1&&args[1].trim().equals("2")){
					this.sendMessage(sender, 0, "*           *Help for LagMeter [2/2]*           *");
					this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lagmeter|/lm"+ChatColor.GREEN+" <help|?> [page]"+ChatColor.GOLD+" - This command. Gives the user a list of commands that they are able to use in this plugin.");
					if(this.permit(sender, "lagmeter.command.ping")||this.permit(sender, "lagmeter.command.lping"))
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN+"/ping|/lping"+ChatColor.GREEN+" [hops] "+ChatColor.GOLD+" - Pings google.com from the server. Specify an amount of hops to specify more packets."+ChatColor.RED+" Warning: server-intensive above 4 hops.");
				}else
					this.sendMessage(sender, 1, "Invalid page number.");
			}else
				this.sendMessage(sender, 1, "Sorry, but you don't have access to the help command.");
		}else{
			this.sendMessage(sender, 1, "Invalid sub-command. "+ChatColor.GOLD+"Try one of these:");
			this.sendMessage(sender, 0, "Available sub-commands: /lagmeter|lm <reload|r>|/lagmeter|lm <help|?>");
		}
	}

	public void info(final String message){
		this.getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"[LagMeter "+this.getDescription().getVersion()+"] "+ChatColor.GREEN+message);
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String commandLabel, final String[] args){
		if(!this.isEnabled())
			return false;
		boolean success = false;
		if(this.permit(sender, "lagmeter.command."+command.getName().toLowerCase())||!(sender instanceof Player)){
			if(command.getName().equalsIgnoreCase("lag")){
				success = true;
				this.sendLagMeter(sender);
			}else if(command.getName().equalsIgnoreCase("mem")){
				success = true;
				this.sendMemMeter(sender);
			}else if(command.getName().equalsIgnoreCase("lagmem")){
				success = true;
				this.sendLagMeter(sender);
				this.sendMemMeter(sender);
			}else if(command.getName().equalsIgnoreCase("uptime")){
				success = true;
				this.sendMessage(sender, 0, "Current server uptime: "+this.convertUptime());
			}else if(command.getName().equalsIgnoreCase("lm")){
				success = true;
				if(args.length==0){
					this.sendLagMeter(sender);
					this.sendMemMeter(sender);
				}else
					this.handleBaseCommand(sender, args);
			}else if(command.getName().equalsIgnoreCase("lmp")){
				success = true;
				this.sendLagMeter(sender);
				this.sendMemMeter(sender);
				this.sendMessage(sender, 0, "Players online: "+ChatColor.GOLD+super.getServer().getOnlinePlayers().length);
			}else if(command.getName().equalsIgnoreCase("lchunks")){
				success = true;
				this.sendChunks(sender);
			}else if(command.getName().equalsIgnoreCase("lentities")||command.getName().equalsIgnoreCase("lmobs")){
				success = true;
				this.sendEntities(sender);
			}else if(command.getName().equalsIgnoreCase("ping")){
				success = true;
				this.ping(sender, args);
			}else if(command.getName().equalsIgnoreCase("lping")){
				success = true;
				this.ping(sender, args);
			}else if(command.getName().equalsIgnoreCase("LagMeter")){
				success = true;
				if(args.length==0){
					this.sendMessage(sender, 0, "Version: "+this.getDescription().getVersion());
					this.sendMessage(sender, 0, "Available sub-commands: /lagmeter|lm <reload|r>|/lagmeter|lm <help|?>");
				}else
					this.handleBaseCommand(sender, args);
			}
			return success;
		}else{
			success = true;
			this.sendMessage(sender, 1, "Sorry, permission lagmeter.command."+command.getName().toLowerCase()+" was denied.");
		}
		return success;
	}

	protected boolean permit(final CommandSender sender, final String perm){
		if(sender instanceof Player){
			if(sender.hasPermission("lagmeter.*"))
				return true;
			else if(sender.hasPermission(perm))
				return true;
			else
				return sender.isOp();
		}else
			return true;
	}

	protected boolean permit(final Player player, final String perm){
		if(player!=null&&player instanceof Player){
			if(player.hasPermission(perm))
				return true;
			else if(player.hasPermission(perm))
				return true;
			else
				return player.isOp();
		}else
			return true;
	}

	public void sendChunks(final CommandSender sender){
		int totalChunks = 0;
		final List<World> worlds = super.getServer().getWorlds();
		for(final World world: worlds){
			final String s = world.getName();
			final int i = super.getServer().getWorld(s).getLoadedChunks().length;
			totalChunks += i;
			if(i!=0)
				this.sendMessage(sender, 0, ChatColor.GOLD+"Chunks in world \""+s+"\": "+i);
		}
		this.sendMessage(sender, 0, ChatColor.GOLD+"Total chunks loaded on the server: "+totalChunks);
	}

	public void sendEntities(final CommandSender sender){
		int totalEntities = 0;
		final List<World> worlds = super.getServer().getWorlds();
		for(final World world: worlds){
			final String worldName = world.getName();
			final int i = super.getServer().getWorld(worldName).getEntities().size();
			totalEntities += i;
			if(i!=0)
				this.sendMessage(sender, 0, ChatColor.GOLD+"Entities in world \""+worldName+"\": "+i);
		}
		this.sendMessage(sender, 0, ChatColor.GOLD+"Total entities: "+totalEntities);
	}

	protected void sendLagMeter(final CommandSender sender){
		String lagMeter = "";
		final float tps;
		if(this.displayEntities)
			this.sendEntities(sender);
		if(this.sendChunks)
			this.sendChunks(sender);
		if(this.useAverage)
			tps = this.history.getAverage();
		else
			tps = this.ticksPerSecond;
		if(tps<21){
			int looped = 0;
			while(looped++<tps)
				lagMeter += "#";
			while(looped++<=20)
				lagMeter += "_";
		}else{
			this.sendMessage(sender, 1, "LagMeter just loaded, please wait for polling.");
			return;
		}
		this.sendMessage(sender, 0, ChatColor.GOLD+"["+(tps>=18 ? ChatColor.GREEN : tps>=15 ? ChatColor.YELLOW : ChatColor.RED)+lagMeter+ChatColor.GOLD+"] "+String.format("%3.2f", tps)+" TPS");
	}

	protected void sendMemMeter(final CommandSender sender){
		String bar = "";
		int looped = 0;
		this.updateMemoryStats();
		while(looped++<this.percentageFree/5)
			bar += '#';
		bar += ChatColor.WHITE;
		while(looped++<=20)
			bar += '_';
		this.sendMessage(sender, 0, ChatColor.GOLD+"["+(this.percentageFree>=60 ? ChatColor.GREEN : this.percentageFree>=35 ? ChatColor.YELLOW : ChatColor.RED)+bar+ChatColor.GOLD+"] "+String.format("%3.2f", this.memFree)+"MB/"+String.format("%3.2f", this.memMax)+"MB ("+String.format("%3.2f", this.percentageFree)+"%) free");
	}

	private void ping(final CommandSender sender, final String[] args){
		final List<String> processCmd = new ArrayList<String>();
		final String hops = this.getHops(sender, args);
		final String domain = this.pingDomain;
		processCmd.add("ping");
		processCmd.add(System.getProperty("os.name").startsWith("Windows") ? "-n" : "-c");
		processCmd.add(hops);
		processCmd.add(domain);
		final class SyncSendMessage extends BukkitRunnable{
			CommandSender sender;
			int severity;
			String message;

			SyncSendMessage(final CommandSender sender, final int severity, final String message){
				this.sender = sender;
				this.severity = severity;
				this.message = message;
			}

			@Override
			public void run(){
				LagMeter.this.sendMessage(this.sender, this.severity, this.message);
			}
		}
		this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable(){
			@Override
			public void run(){
				final BufferedReader result;
				final BufferedReader errorStream;
				Process p;
				String s;
				String output = null;
				final String windowsPingSummary = "Average = ";
				final String unixPingSummary = "rtt min/avg/max/mdev = ";
				try{
					p = new ProcessBuilder(processCmd).start();
					result = new BufferedReader(new InputStreamReader(p.getInputStream()));
					errorStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					while((s = result.readLine())!=null){
						if(s.trim().length()!=0)
							output = s;
						if(s.indexOf(windowsPingSummary)!=-1){
							output = s.substring(s.indexOf(windowsPingSummary)+windowsPingSummary.length());
							break;
						}
						if(s.indexOf(unixPingSummary)!=-1){
							output = s.substring(unixPingSummary.length()).split("/")[1];
							break;
						}
					}
					if(output!=null)
						new SyncSendMessage(sender, 0, "Average response time for the server for "+hops+" ping hop(s) to "+domain+": "+output).runTask(LagMeter.this);
					else
						new SyncSendMessage(sender, 0, "Error running ping command").runTask(LagMeter.this);
					while((s = errorStream.readLine())!=null)
						new SyncSendMessage(sender, 1, s).runTask(LagMeter.this);
					p.destroy();
				}catch(final IOException e){
					new SyncSendMessage(sender, 0, "Error running ping command").runTask(LagMeter.this);
					e.printStackTrace();
				}
			}
		});
	}

	public String getHops(final CommandSender sender, final String[] args){
		if(args.length>0)
			if(this.permit(sender, "lagmeter.commands.ping.unlimited"))
				try{
					if(Integer.parseInt(args[0])>10)
						this.sendMessage(sender, 1, "This might take a while...");
					return args[0];
				}catch(final NumberFormatException e){
					this.sendMessage(sender, 1, "You entered an invalid amount of hops; therefore, 1 will be used instead.");
					return "1";
				}
			else{
				this.sendMessage(sender, 1, "You don't have access to specifying ping hops!");
				return "1";
			}
		else
			return "1";
	}

	protected void sendMessage(final CommandSender sender, final int severity, final String message){
		if(sender!=null)
			switch(severity){
				case 0:
					sender.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.GREEN+message);
					break;
				case 1:
					sender.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.RED+message);
					break;
				case 2:
					sender.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.DARK_RED+message);
					break;
			}
		else
			switch(severity){
				case 0:
					this.info(message);
					break;
				case 1:
					this.warn(message);
					break;
				case 2:
					this.severe(message);
					break;
			}
	}

	protected void sendMessage(final Player player, final int severity, final String message){
		this.sendMessage((CommandSender) player, severity, message);
	}

	private String convertUptime(){
		int days, hours, minutes, seconds;
		long l = System.currentTimeMillis()-this.uptime;
		days = (int) (l/1000L/60L/60L/24L);
		l -= days*86400000L;
		hours = (int) (l/1000L/60L/60L);
		l -= hours*3600000;
		minutes = (int) (l/1000L/60L);
		l -= minutes*60000L;
		seconds = (int) (l/1000L);
		return days+" day(s), "+hours+" hour(s), "+minutes+" minute(s), and "+seconds+" second(s)";
	}

	public void severe(final String message){
		this.getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"[LagMeter "+this.getDescription().getVersion()+"] "+ChatColor.DARK_RED+message);
	}

	protected void updateMemoryStats(){
		this.memUsed = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576;
		this.memMax = Runtime.getRuntime().maxMemory()/1048576;
		this.memFree = this.memMax-this.memUsed;
		this.percentageFree = 100/this.memMax*this.memFree;
	}

	public void warn(final String message){
		this.getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"[LagMeter "+this.getDescription().getVersion()+"] "+ChatColor.RED+message);
	}

	/**
	 * 
	 * @return Amount of ticks which corresponds to this string of time.
	 * @throws InvalidTimeFormatException If the time format given is invalid
	 */
	protected long parseTime(final String timeString) throws InvalidTimeFormatException{
		long time = 0L;
		if(timeString.split(";").length==2){
			String x = timeString.split(";")[1].toLowerCase();
			String z = "";
			for(int i = 0; i<x.length(); i++){
				final String c = x.substring(i, i+1);
				if(c.matches("[^wdhms]"))
					z += c;
				else
					try{
						// switch(c){ // Non-java-6-compliant
						// case "w":
						if(c.equalsIgnoreCase("w"))
							time += 12096000L*Long.parseLong(z);
						// break;
						// case "d":
						else if(c.equalsIgnoreCase("d"))
							time += 1728000L*Long.parseLong(z);
						// break;
						// case "h":
						else if(c.equalsIgnoreCase("h"))
							time += 7200L*Long.parseLong(z);
						// break;
						// case "m":
						else if(c.equalsIgnoreCase("m"))
							time += 1200L*Long.parseLong(z);
						// break;
						// case "s":
						else if(c.equalsIgnoreCase("s"))
							time += 20L*Long.parseLong(z);
						// break;
						// }
						z = x = "";
					}catch(final NumberFormatException e){
						throw new InvalidTimeFormatException("The time for the uptime command "+timeString.split(";")[0]+" is invalid: the time string contains characters other than 0-9, w/d/h/m/s.");
					}
			}
		}else
			time = -1L;
		if(time<1)
			throw new InvalidTimeFormatException("The time or command for the uptime command string "+timeString+" is invalid.");
		return time;
	}

	final class LagWatcher implements Runnable{
		@Override
		public void run(){
			if(LagMeter.this.tpsNotificationThreshold>=LagMeter.this.getTPS()){
				final Player[] players = Bukkit.getServer().getOnlinePlayers();
				for(final Player p: players)
					if(LagMeter.this.permit(p, "lagmeter.notify.lag")||p.isOp())
						p.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.RED+"The server's TPS has dropped below "+LagMeter.this.tpsNotificationThreshold+"! If you configured a server command to execute at this time, it will run now.");
				LagMeter.this.severe("The server's TPS has dropped below "+LagMeter.this.tpsNotificationThreshold+"! Executing command (if configured).");
				if(LagMeter.this.highLagCommand.contains(";"))
					for(final String cmd: LagMeter.this.highLagCommand.split(";"))
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceFirst("/", ""));
				else
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LagMeter.this.highLagCommand.replaceFirst("/", ""));
			}
		}
	}

	final class MemoryWatcher implements Runnable{
		@Override
		public void run(){
			if(LagMeter.this.memoryNotificationThreshold>=LagMeter.this.getMemory()[3]){
				Player[] players;
				players = Bukkit.getServer().getOnlinePlayers();
				for(final Player p: players)
					if(LagMeter.this.permit(p, "lagmeter.notify.mem")||p.isOp())
						p.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.RED+"The server's free memory pool has dropped below "+LagMeter.this.memoryNotificationThreshold+"%! If you configured a server command to execute at this time, it will run now.");
				LagMeter.this.severe("The server's free memory pool has dropped below "+LagMeter.this.memoryNotificationThreshold+"! Executing command (if configured).");
				if(LagMeter.this.lowMemCommand.contains(";"))
					for(final String cmd: LagMeter.this.lowMemCommand.split(";"))
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceFirst("/", ""));
				else
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), LagMeter.this.lowMemCommand.replaceFirst("/", ""));
			}
		}
	}

	final class UptimeCommand implements Runnable{
		final String command;

		public UptimeCommand(final String command){
			this.command = command;
		}

		@Override
		public void run(){
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), this.command);
		}
	}
}