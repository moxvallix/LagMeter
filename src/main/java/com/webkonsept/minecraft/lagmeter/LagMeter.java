package main.java.com.webkonsept.minecraft.lagmeter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class LagMeter extends JavaPlugin implements ChatColourManager {
	private Logger log = Logger.getLogger("Minecraft");
	protected float ticksPerSecond = 20;
	public PluginDescriptionFile pdfFile;

	private final String fileSeparator = System.getProperty("file.separator");
	protected File logsFolder = new File("plugins"+fileSeparator+"LagMeter"+fileSeparator+"logs");

	protected LagMeterLogger logger = new LagMeterLogger(this);
	protected LagMeterPoller poller = new LagMeterPoller(this);
	protected int averageLength = 10, sustainedLagTimer;
	protected LagMeterStack history = new LagMeterStack();

	protected boolean vault = false;
	protected Permission permission;

	double memUsed = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576;
	double memMax = Runtime.getRuntime().maxMemory()/1048576;
	double memFree = memMax-memUsed;
	double percentageFree = (100/memMax)*memFree;
	PluginDescriptionFile pdf;
	public static LagMeter p;

	//Configurable Values - mostly booleans
	protected int interval = 40, logInterval = 150, lagNotifyInterval,
			memNotifyInterval, lwTaskID, mwTaskID;
	protected float tpsNotificationThreshold, memoryNotificationThreshold;
	protected boolean useAverage = true, enableLogging = true, useLogsFolder = true,
			AutomaticLagNotificationsEnabled, AutomaticMemoryNotificationsEnabled, displayEntities,
			playerLoggingEnabled, displayChunksOnLoad, sendChunks, logChunks, logTotalChunksOnly,
			logEntities, logTotalEntitiesOnly, newBlockPerLog, displayEntitiesOnLoad, newLineForLogStats;
	protected String highLagCommand, lowMemCommand;
	private LagMeterConfig conf;

	@Override
	public void onEnable(){
		conf = new LagMeterConfig(this);
		p = this;
		pdfFile = this.getDescription();
		conf.loadConfig();
		vault = checkVault();
		
		if(!logsFolder.exists() && useLogsFolder && enableLogging){
			info("Logs folder not found. Creating one for you.");
			logsFolder.mkdir();
			
			if(!logsFolder.exists()){
				severe("Error! Couldn't create the folder!");
			}else{
				info("Logs folder created.");
			}
		}
		if(enableLogging){
			poller.setLogInterval(logInterval);
			if(!logger.enable()){
				severe("Logging is disabled because: "+logger.getError());
			}
		}
		history.setMaxSize(averageLength);
		getServer().getScheduler().scheduleSyncRepeatingTask(this,poller,0,interval);
		if(checkVault()){
			info("Vault hooked successfully.");
			vault = true;
			setupPermissions();
		}else{
			info("You don't have Vault. Defaulting to OP/Non-OP system.");
		}
		String loggingMessage = enableLogging ? "Logging to "+logger.getFilename() : "";
		info("Enabled! Polling every "+interval+" server ticks."+loggingMessage);
		if(AutomaticLagNotificationsEnabled)
			lwTaskID = Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new LagWatcher(), lagNotifyInterval*1200, lagNotifyInterval*1200);
		else
			lwTaskID = -1;
		if(AutomaticMemoryNotificationsEnabled)
			mwTaskID = Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new MemoryWatcher(), memNotifyInterval*1200, memNotifyInterval*1200);
		else
			mwTaskID = -1;
		
		if(displayChunksOnLoad){
			info("Chunks loaded:");
			int total = 0;
			for(World world: getServer().getWorlds()){
				int chunks=world.getLoadedChunks().length;
				info("World \""+world.getName()+"\": "+chunks+".");
				total+=chunks;
			}
			info("Total chunks loaded: "+total);
		}
		if(displayEntitiesOnLoad){
			info("Entities:");
			int total = 0;
			for(World world: getServer().getWorlds()){
				int entities=world.getEntities().size();
				info("World \""+world.getName()+"\": "+entities+".");
				total+=entities;
			}
			info("Total entities: "+total);
		}
	}
	@Override
	public void onDisable(){
		info("Disabled!");
		if(LagMeterLogger.enabled != false){
			try {
				logger.disable();
			}catch (FileNotFoundException e){
				e.printStackTrace();
			}catch (IOException e){
				e.printStackTrace();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		if(AutomaticLagNotificationsEnabled)
			getServer().getScheduler().cancelTask(lwTaskID);
		if(AutomaticMemoryNotificationsEnabled)
			getServer().getScheduler().cancelTask(mwTaskID);
		getServer().getScheduler().cancelTasks(this);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args){
		if(!this.isEnabled())
			return false;
		boolean success = false;
		if((sender instanceof Player && permit((Player)sender, "lagmeter.command."+command.getName().toLowerCase())) || !(sender instanceof Player)){
			if(command.getName().equalsIgnoreCase("lag")){
				success = true;
				sendLagMeter(sender);
			}else if(command.getName().equalsIgnoreCase("mem")){
				success = true;
				sendMemMeter(sender);
			}else if(command.getName().equalsIgnoreCase("lagmem")
					||command.getName().equalsIgnoreCase("lm")){
				success = true;
				sendLagMeter(sender);
				sendMemMeter(sender);
			}else if(command.getName().equalsIgnoreCase("lmp")){
				success = true;
				sendLagMeter(sender);
				sendMemMeter(sender);
				sender.sendMessage("Players online: "+gold+getServer().getOnlinePlayers().length);
			}else if(command.getName().equalsIgnoreCase("lchunks")){
				success = true;
				sendChunks(sender);
			}else if(command.getName().equalsIgnoreCase("lentities") || command.getName().equalsIgnoreCase("lmobs")){
				success = true;
				sendEntities(sender);
			}else if(command.getName().equalsIgnoreCase("LagMeter")){
				success = true;
				if(args.length == 0){
					sender.sendMessage(gold+"[LagMeter] Version: "+pdf.getVersion());
					sender.sendMessage(gold+"[LagMeter] Available sub-commands: </lagmeter <reload|r>|/lagmeter <help|?>>");
				}else if(args[0].equalsIgnoreCase("reload")){
					if(permit((Player)sender, "lagmeter.command.lagmeter.reload") || !(sender instanceof Player)){
						conf.loadConfig();
						sender.sendMessage("Configuration reloaded!");
					}
				}else if(args[0].equalsIgnoreCase("help")){
					sender.sendMessage(green+"*           *Help for LagMeter*           *");
					if(permit((Player)sender, "lagmeter.command.")){
						sender.sendMessage(gold+"[LagMeter] "+darkgreen+"/lag"+gold+" - Check the server's TPS. If configured, may also display chunks loaded and/or entities alive.");
					}
					if(permit((Player)sender, "lagmeter.command.")){
						sender.sendMessage(gold+"[LagMeter] "+darkgreen+"/mem"+gold+" - Displays how much memory the server currently has free.");
					}
					if(permit((Player)sender, "lagmeter.command.")){
						sender.sendMessage(gold+"[LagMeter] "+darkgreen+"/lagmem|/lm"+gold+" - A combination of both /lag and /mem.");
					}
					if(permit((Player)sender, "lagmeter.command.")){
						sender.sendMessage(gold+"[LagMeter] "+darkgreen+"/lchunks"+gold+" - Shows how many chunks are currently loaded in each world, then with a total.");
					}
					if(permit((Player)sender, "lagmeter.command.")){
						sender.sendMessage(gold+"[LagMeter] "+darkgreen+"/lmobs|/lentities"+gold+" - Shows how many entities are currently alive in each world, then with a total.");
					}
					if(permit((Player)sender, "lagmeter.command.")){
						sender.sendMessage(gold+"[LagMeter] "+darkgreen+"/lmp"+gold+" - Has the same function as /lagmem, but includes a player count.");
					}
				}else{
					sender.sendMessage(gold+"[LagMeter] Invalid sub-command. Try one of these:");
					sender.sendMessage(gold+"[LagMeter] Available sub-commands: </lagmeter <reload|r>|/lagmeter <help|?>>");
				}
			}
			return success;
		}else{
			success = true;
			sender.sendMessage(gold+"Sorry, permission lagmeter.command."+command.getName().toLowerCase()+" was denied.");
		}
		return success;
	}
	protected boolean permit(Player player, String perm){
		boolean permit = false;
		if(vault){
			permit = permission.has(player, perm);
		}else{
			permit = player.isOp();
		}
		return permit;
	}
	private boolean checkVault(){
		boolean usingVault = false;
		Plugin v = this.getServer().getPluginManager().getPlugin("Vault");
		if(v != null){
			usingVault = true;
		}
		return usingVault;
	}
	protected void updateMemoryStats (){
		memUsed = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576;
		memMax = Runtime.getRuntime().maxMemory()/1048576;
		memFree = memMax-memUsed;
		percentageFree = (100/memMax)*memFree;
	}
	protected void sendMemMeter(CommandSender sender){
		updateMemoryStats();
		String wrapColor = sender instanceof Player?ChatColor.GOLD.toString():ChatColor.WHITE.toString();
		String colour = gold.toString();
		
		if(percentageFree >= 60){
			colour = green.toString();
		}else if(percentageFree >= 35){
			colour = yellow.toString();
		}else{
			colour = red.toString();
		}

		String bar = "";
		int looped = 0;
		while(looped++< (percentageFree/5)){
			bar+='#';
		}
		//bar = String.format("%-20s",bar);
		bar+= white;
		while (looped++<= 20){
			bar+= '_';
		}
		sender.sendMessage(wrapColor+"["+colour+bar+wrapColor+"] "+memFree+"MB/"+memMax+"MB ("+(int)percentageFree+"%) free");
	}
	protected void sendLagMeter(CommandSender sender){
		String wrapColor = white.toString();
		
		if(displayEntities)
			sendEntities(sender);
		if(sendChunks)
			sendChunks(sender);
		
		if(sender instanceof Player)
			wrapColor = gold.toString();
		String lagMeter = "";
		float tps = 0f;
		if(useAverage){
			tps = history.getAverage();
		}else{
			tps = ticksPerSecond;
		}
		if(tps < 21){
			int looped = 0;
			while (looped++< tps){
				lagMeter+= "#";
			}
			//lagMeter = String.format("%-20s",lagMeter);
			lagMeter+= white;
			while (looped++<= 20){
				lagMeter+= "_";
			}
		}else{
			sender.sendMessage(wrapColor+"LagMeter just loaded, please wait for polling.");
			return;
		}
		String color = wrapColor;
		if(tps >= 20){
			color = green.toString();
		}else if(tps >= 18){
			color = green.toString();
		}else if(tps >= 15){
			color = yellow.toString();
		}else{
			color = red.toString();
		}
		sender.sendMessage(wrapColor+"["+color+lagMeter+wrapColor+"] "+tps+" TPS");
	}
	public void sendEntities(CommandSender sender){
		int totalEntities = 0;
		List<World> worlds = getServer().getWorlds();
		for(World world: worlds){
			String worldName = world.getName();
			int i = getServer().getWorld(worldName).getEntities().size();
			totalEntities += i;
			sender.sendMessage(gold+"Entities in world \""+worldName+"\": "+i);
		}
		sender.sendMessage(gold+"Total entities: "+totalEntities);
	}
	public void sendChunks(CommandSender sender){
		int totalChunks = 0;
		List<World> worlds = getServer().getWorlds();
		for(World world: worlds){
			String s = world.getName();
			int i = getServer().getWorld(s).getLoadedChunks().length;
			totalChunks += i;
			sender.sendMessage(gold+"Chunks in world \""+s+"\": "+i);
		}
		sender.sendMessage(gold+"Total chunks loaded on the server: "+totalChunks);
	}
	public void info(String message){
		log.info("["+pdfFile.getName()+" "+pdfFile.getVersion()+"] "+message);
	}
	public void warn(String message){
		log.warning("["+pdfFile.getName()+" "+pdfFile.getVersion()+"] "+message);
	}
	public void severe(String message){
		log.severe(pdfFile.getName()+""+pdfFile.getVersion()+"] "+message);
	}
	/**
	 * Gets the ticks per second.
	 * 
	 * @since 1.8
	 * @return ticksPerSecond
	 */
	public float getTPS(){
		if(useAverage)
			return history.getAverage();
		return ticksPerSecond;
	}
	/**
	 * Gets the current memory used, maxmimum memory, memory free, and percentage of memory free. Returned in a single array of doubles.
	 * 
	 * @since 1.11.0-SNAPSHOT
	 * @return memory[], which is a double array, containing four values, where:
	 * <br /><b><i>memory[0]</i></b> is the currently used memory;<br /><b><i>memory[1]</i></b> is the current maximum memory;<br /><b><i>memory[2]</i></b> is the current free memory;<br /><b><i>memory[3]</i></b> is the percentage memory free (note this may be an irrational number, so you might want to truncate it if you use this).
	 */
	public double[] getMemory(){
		double[] memory = {0D, 0D, 0D, 0D};
		updateMemoryStats();
		memory[0] = memUsed;
		memory[1] = memMax;
		memory[2] = memFree;
		memory[3] = percentageFree;
		return memory;
	}
	private boolean setupPermissions(){
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if(permissionProvider != null){
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}
	class LagWatcher implements Runnable{
		@Override
		public void run(){
			if(tpsNotificationThreshold >= getTPS()){
				Player[] players = Bukkit.getServer().getOnlinePlayers();
				for(Player p: players){
					if(permit(p, "lagmeter.notify.lag") || p.isOp())
						p.sendMessage(igt+red+"The server's TPS has dropped below "+tpsNotificationThreshold+"! If you configured a server command to execute at this time, it will run now.");
				}
				severe("The server's TPS has dropped below "+tpsNotificationThreshold+"! Executing command (if configured).");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), highLagCommand);
			}
		}
	}
	class MemoryWatcher implements Runnable{
		@Override
		public void run(){
			updateMemoryStats();
			if(memoryNotificationThreshold >= memFree){
				Player[] players;
				players = Bukkit.getServer().getOnlinePlayers();
				for(Player p: players){
					if(permit(p, "lagmeter.notify.mem") || p.isOp()){
						p.sendMessage(igt+red+"The server's free memory pool has dropped below "+memoryNotificationThreshold+"%! If you configured a server command to execute at this time, it will run now.");
					}
				}
				severe("The server's free memory pool has dropped below "+memoryNotificationThreshold+"! Executing command (if configured).");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), lowMemCommand);
			}
		}
	}
}
