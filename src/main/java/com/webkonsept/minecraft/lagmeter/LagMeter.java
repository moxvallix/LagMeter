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

public class LagMeter extends JavaPlugin {
	protected LagMeterLogger logger = new LagMeterLogger(this);
	protected LagMeterPoller poller = new LagMeterPoller(this);
	protected LagMeterStack history = new LagMeterStack();
	private LagMeterConfig conf;
	
	private Logger log = Logger.getLogger("Minecraft");

	protected float ticksPerSecond = 20;
	public PluginDescriptionFile pdfFile;
	private final String fileSeparator = System.getProperty("file.separator");
	protected File logsFolder = new File("plugins"+fileSeparator+"LagMeter"+fileSeparator+"logs");


	protected int averageLength = 10, sustainedLagTimer;

	protected boolean vault = false;
	protected Permission permission;
	double memUsed = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576;
	double memMax = Runtime.getRuntime().maxMemory()/1048576;
	double memFree = memMax-memUsed;

	double percentageFree = (100/memMax)*memFree;
	//Configurable Values - mostly booleans
	protected int interval = 40, logInterval = 150, lagNotifyInterval,
			memNotifyInterval, lwTaskID, mwTaskID;
	protected float tpsNotificationThreshold, memoryNotificationThreshold;
	protected boolean useAverage = true, enableLogging = true, useLogsFolder = true,
			AutomaticLagNotificationsEnabled, AutomaticMemoryNotificationsEnabled, displayEntities,
			playerLoggingEnabled, displayChunksOnLoad, sendChunks, logChunks, logTotalChunksOnly,
			logEntities, logTotalEntitiesOnly, newBlockPerLog, displayEntitiesOnLoad, newLineForLogStats;

	protected String highLagCommand, lowMemCommand;
	
	//Accessor
	public static LagMeter p;
	
	@Override
	public void onEnable(){
		conf = new LagMeterConfig(this);
		p = this;
		pdfFile = this.getDescription();
		conf.loadConfig();
		vault = checkVault();
		logger = new LagMeterLogger(this);
		poller = new LagMeterPoller(this);

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
		super.getServer().getScheduler().scheduleSyncRepeatingTask(this, poller, 0, interval);
		if(checkVault()){
			info("Vault hooked successfully.");
			vault = true;
			setupPermissions();
		}else{
			info("You don't have Vault. Defaulting to OP/Non-OP system.");
		}
		String loggingMessage = enableLogging ? " Logging to "+logger.getFilename()+"." : "";
		info("Enabled! Polling every "+interval+" server ticks."+loggingMessage);
		if(AutomaticLagNotificationsEnabled)
			lwTaskID = super.getServer().getScheduler().scheduleSyncRepeatingTask(this, new LagWatcher(), lagNotifyInterval*1200, lagNotifyInterval*1200);
		else
			lwTaskID = -1;
		if(AutomaticMemoryNotificationsEnabled)
			mwTaskID = super.getServer().getScheduler().scheduleSyncRepeatingTask(this, new MemoryWatcher(), memNotifyInterval*1200, memNotifyInterval*1200);
		else
			mwTaskID = -1;

		if(displayChunksOnLoad){
			info("Chunks loaded:");
			int total = 0;
			for(World world: super.getServer().getWorlds()){
				int chunks=world.getLoadedChunks().length;
				info("World \""+world.getName()+"\": "+chunks+".");
				total+=chunks;
			}
			info("Total chunks loaded: "+total);
		}
		if(displayEntitiesOnLoad){
			info("Entities:");
			int total = 0;
			for(World world: super.getServer().getWorlds()){
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
			super.getServer().getScheduler().cancelTask(lwTaskID);
		if(AutomaticMemoryNotificationsEnabled)
			super.getServer().getScheduler().cancelTask(mwTaskID);
		getServer().getScheduler().cancelTasks(this);
	}
	private boolean checkVault(){
		boolean usingVault = false;
		Plugin v = super.getServer().getPluginManager().getPlugin("Vault");
		if(v != null){
			usingVault = true;
		}
		return usingVault;
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
	protected void handleBaseCommand(CommandSender sender, String[] args){
		if(args[0].equalsIgnoreCase("reload")){
			if(permit(sender, "lagmeter.command.lagmeter.reload") || permit(sender, "lagmeter.reload")){
				conf.loadConfig();
				sendMessage(sender, 0, "Configuration reloaded!");
			}
		}else if(args[0].equalsIgnoreCase("help")){
			if(permit(sender, "lagmeter.command.lagmeter.help") || permit(sender, "lagmeter.help")){
				int doesntHave = 0;
				sendMessage(sender, 0, "*           *Help for LagMeter*           *");
				if(permit(sender, "lagmeter.command.lag")){
					sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lag"+ChatColor.GOLD+" - Check the server's TPS. If configuChatColor.RED, may also display chunks loaded and/or entities alive.");
				}else doesntHave++;
				if(permit(sender, "lagmeter.command.mem")){
					sendMessage(sender, 0, ChatColor.DARK_GREEN+"/mem"+ChatColor.GOLD+" - Displays how much memory the server currently has free.");
				}else doesntHave++;
				if(permit(sender, "lagmeter.command.lagmem") || permit(sender, "lagmeter.command.lm")){
					sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lagmem|/lm"+ChatColor.GOLD+" - A combination of both /lag and /mem.");
				}else doesntHave++;
				if(permit(sender, "lagmeter.command.lchunks")){
					sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lchunks"+ChatColor.GOLD+" - Shows how many chunks are currently loaded in each world, then with a total.");
				}else doesntHave++;
				if(permit(sender, "lagmeter.command.lmobs") || permit(sender, "lagmeter.command.lentities")){
					sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lmobs|/lentities"+ChatColor.GOLD+" - Shows how many entities are currently alive in each world, then with a total.");
				}else doesntHave++;
				if(permit(sender, "lagmeter.command.lmp")){
					sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lmp"+ChatColor.GOLD+" - Has the same function as /lagmem, but includes a player count.");
				}else doesntHave++;
				if(permit(sender, "lagmeter.command.lagmeter")){
					sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lagmeter|/lm"+ChatColor.GOLD+" - Shows the current version and gives sub-commands.");
				}else ++doesntHave;
				if(permit(sender, "lagmeter.command.lagmeter.reload") || permit(sender, "lagmeter.reload")){
					sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lagmeter|/lm"+ChatColor.GREEN+" <reload|r> "+ChatColor.GOLD+" - Allows the player to reload the configuration.");
				}else ++doesntHave;
				sendMessage(sender, 0, ChatColor.DARK_GREEN+"/lagmeter|/lm"+ChatColor.GREEN+" <help|?> "+ChatColor.GOLD+" - This command. Gives the user a list of commands that they are able to use in this plugin.");
				if(doesntHave == 8)
					sendMessage(sender, 1, "You don't have permission for any of the commands (besides this one)!");
			}else
				sendMessage(sender, 1, "Sorry, but you don't have access to the help command.");
		}else{
			sendMessage(sender, 1, ChatColor.GOLD+"[LagMeter] "+ChatColor.RED+"Invalid sub-command. "+ChatColor.GOLD+"Try one of these:");
			sendMessage(sender, 0, ChatColor.GOLD+"[LagMeter] Available sub-commands: /lagmeter|lm <reload|r>|/lagmeter|lm <help|?>");
		}
	}
	public void info(String message){
		log.info("["+pdfFile.getName()+" "+pdfFile.getVersion()+"] "+message);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args){
		if(!this.isEnabled())
			return false;
		boolean success = false;
		if(permit(sender, "lagmeter.command."+command.getName().toLowerCase()) || !(sender instanceof Player)){
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
			}else if(command.getName().equalsIgnoreCase("lm")){
				success = true;
				if(args.length == 0){
					this.sendLagMeter(sender);
					this.sendMemMeter(sender);
				}else
					handleBaseCommand(sender, args);
			}else if(command.getName().equalsIgnoreCase("lmp")){
				success = true;
				this.sendLagMeter(sender);
				this.sendMemMeter(sender);
				sendMessage(sender, 0, "Players online: "+ChatColor.GOLD+super.getServer().getOnlinePlayers().length);
			}else if(command.getName().equalsIgnoreCase("lchunks")){
				success = true;
				sendChunks(sender);
			}else if(command.getName().equalsIgnoreCase("lentities") || command.getName().equalsIgnoreCase("lmobs")){
				success = true;
				sendEntities(sender);
			}else if(command.getName().equalsIgnoreCase("LagMeter")){
				success = true;
				if(args.length == 0){
					sendMessage(sender, 0, ChatColor.GOLD+"[LagMeter] Version: "+pdfFile.getVersion());
					sendMessage(sender, 0, ChatColor.GOLD+"[LagMeter] Available sub-commands: /lagmeter|lm <reload|r>|/lagmeter|lm <help|?>");
				}else
					handleBaseCommand(sender, args);
			}
			return success;
		}else{
			success = true;
			sendMessage(sender, 1, "Sorry, permission lagmeter.command."+command.getName().toLowerCase()+" was denied.");
		}
		return success;
	}
	protected boolean permit(CommandSender sender, String perm){
		boolean permit = false;
		if(sender instanceof Player){
			if(vault)
				permit = permission.has(sender, perm);
			else
				permit = sender.isOp();
		}else
			permit = true;
		return permit;
	}
	protected boolean permit(Player player, String perm){
		boolean permit = false;
		if(player != null && player instanceof Player){
			if(vault)
				permit = permission.has(player, perm);
			else
				permit = player.isOp();
		}else
			permit = true;
		return permit;
	}
	public void sendChunks(CommandSender sender){
		int totalChunks = 0;
		List<World> worlds = super.getServer().getWorlds();
		for(World world: worlds){
			String s = world.getName();
			int i = super.getServer().getWorld(s).getLoadedChunks().length;
			totalChunks += i;
			sendMessage(sender, 0, ChatColor.GOLD+"Chunks in world \""+s+"\": "+i);
		}
		sendMessage(sender, 0, ChatColor.GOLD+"Total chunks loaded on the server: "+totalChunks);
	}
	public void sendEntities(CommandSender sender){
		int totalEntities = 0;
		List<World> worlds = super.getServer().getWorlds();
		for(World world: worlds){
			String worldName = world.getName();
			int i = super.getServer().getWorld(worldName).getEntities().size();
			totalEntities += i;
			sendMessage(sender, 0, ChatColor.GOLD+"Entities in world \""+worldName+"\": "+i);
		}
		sendMessage(sender, 0, ChatColor.GOLD+"Total entities: "+totalEntities);
	}
	protected void sendLagMeter(CommandSender sender){
		String wrapColor = ChatColor.WHITE.toString();

		if(displayEntities)
			this.sendEntities(sender);
		if(sendChunks)
			this.sendChunks(sender);

		if(sender instanceof Player)
			wrapColor = ChatColor.GOLD.toString();
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
			lagMeter+= ChatColor.WHITE;
			while (looped++<= 20){
				lagMeter+= "_";
			}
		}else{
			sendMessage(sender, 1, "LagMeter just loaded, please wait for polling.");
			return;
		}
		String color = wrapColor;
		if(tps >= 20){
			color = ChatColor.GREEN.toString();
		}else if(tps >= 18){
			color = ChatColor.GREEN.toString();
		}else if(tps >= 15){
			color = ChatColor.YELLOW.toString();
		}else{
			color = ChatColor.RED.toString();
		}
		sendMessage(sender, 0, wrapColor+"["+color+lagMeter+wrapColor+"] "+tps+" TPS");
	}
	protected void sendMemMeter(CommandSender sender){
		updateMemoryStats();
		String wrapColor = sender instanceof Player?ChatColor.GOLD.toString():ChatColor.WHITE.toString();
		String colour = ChatColor.GOLD.toString();

		if(percentageFree >= 60){
			colour = ChatColor.GREEN.toString();
		}else if(percentageFree >= 35){
			colour = ChatColor.YELLOW.toString();
		}else{
			colour = ChatColor.RED.toString();
		}

		String bar = "";
		int looped = 0;
		while(looped++< (percentageFree/5)){
			bar+='#';
		}
		//bar = String.format("%-20s",bar);
		bar+= ChatColor.WHITE;
		while (looped++<= 20){
			bar+= '_';
		}
		sendMessage(sender, 0, wrapColor+"["+colour+bar+wrapColor+"] "+memFree+"MB/"+memMax+"MB ("+(int)percentageFree+"%) free");
	}
	protected void sendMessage(CommandSender sender, int severity, String message){
		if(sender instanceof Player){
			switch(severity){
			case 0:
				sender.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.GREEN+message);
				break;
			case 1:
				sender.sendMessage(ChatColor.GOLD+"[LagMeter]"+ChatColor.RED+message);
				break;
			case 2:
				sender.sendMessage(ChatColor.GOLD+"[LagMeter]"+ChatColor.DARK_RED+message);
				break;
			}
		}else{
			switch(severity){
			case 0:
				info(ChatColor.GREEN+message);
				break;
			case 1:
				warn(ChatColor.RED+message);
				break;
			case 2:
				severe(ChatColor.DARK_RED+message);
				break;
			}
		}
	}
	protected void sendMessage(Player player, int severity, String message){
		if(player != null){
			switch(severity){
			case 0:
				player.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.GREEN+message);
				break;
			case 1:
				player.sendMessage(ChatColor.GOLD+"[LagMeter]"+ChatColor.RED+message);
				break;
			case 2:
				player.sendMessage(ChatColor.GOLD+"[LagMeter]"+ChatColor.DARK_RED+message);
				break;
			}
		}else{
			switch(severity){
			case 0:
				info(ChatColor.GREEN+message);
				break;
			case 1:
				warn(ChatColor.RED+message);
				break;
			case 2:
				severe(ChatColor.DARK_RED+message);
				break;
			}
		}
	}
	private boolean setupPermissions(){
		RegisteredServiceProvider<Permission> permissionProvider = super.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if(permissionProvider != null){
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}
	public void severe(String message){
		log.severe("["+pdfFile.getName()+" "+pdfFile.getVersion()+"] "+message);
	}
	protected void updateMemoryStats(){
		memUsed = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576;
		memMax = Runtime.getRuntime().maxMemory()/1048576;
		memFree = memMax-memUsed;
		percentageFree = (100/memMax)*memFree;
	}
	
	public void warn(String message){
		log.warning("["+pdfFile.getName()+" "+pdfFile.getVersion()+"] "+message);
	}	class LagWatcher implements Runnable{
		@Override
		public void run(){
			if(tpsNotificationThreshold >= getTPS()){
				Player[] players = Bukkit.getServer().getOnlinePlayers();
				for(Player p: players){
					if(permit(p, "lagmeter.notify.lag") || p.isOp())
						p.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.RED+"The server's TPS has dropped below "+tpsNotificationThreshold+"! If you configured a server command to execute at this time, it will run now.");
				}
				severe("The server's TPS has dropped below "+tpsNotificationThreshold+"! Executing command (if configured).");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), highLagCommand.replaceFirst("/", ""));
			}
		}
	}
	class MemoryWatcher implements Runnable{
		@Override
		public void run(){
			if(memoryNotificationThreshold >= getMemory()[3]){
				Player[] players;
				players = Bukkit.getServer().getOnlinePlayers();
				for(Player p: players){
					if(permit(p, "lagmeter.notify.mem") || p.isOp()){
						p.sendMessage(ChatColor.GOLD+"[LagMeter] "+ChatColor.RED+"The server's free memory pool has dropped below "+memoryNotificationThreshold+"%! If you configured a server command to execute at this time, it will run now.");
					}
				}
				severe("The server's free memory pool has dropped below "+memoryNotificationThreshold+"! Executing command (if configured).");
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), lowMemCommand.replaceFirst("/", ""));
			}
		}
	}
}