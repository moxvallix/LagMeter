package main.java.com.webkonsept.minecraft.lagmeter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class LagMeter extends JavaPlugin implements ChatColourManager {
	private Logger log = Logger.getLogger("Minecraft");
	protected float ticksPerSecond = 20;
	public static PluginDescriptionFile pdfFile;

	private static final String fileSeparator = System.getProperty("file.separator");
	protected File logsFolder = new File("plugins"+fileSeparator+"LagMeter"+fileSeparator+"logs");

	protected LagMeterLogger logger = new LagMeterLogger(this);
	protected LagMeterPoller poller = new LagMeterPoller(this);
	protected static int averageLength = 10;
	protected LagMeterStack history = new LagMeterStack();

	protected boolean vault = false;
	protected Permission permission;

	double memUsed = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576;
	double memMax = Runtime.getRuntime().maxMemory()/1048576;
	double memFree = memMax-memUsed;
	double percentageFree = (100/memMax)*memFree;
	PluginDescriptionFile pdf;
	LagMeter plugin;

	//Configurable Values
	protected static int interval = 40;
	protected static float tpsNotificationThreshold, memoryNotificationThreshold;
	protected static boolean useAverage = true, enableLogging = true, useLogsFolder = true,
			AutomaticLagNotificationsEnabled, AutomaticMemoryNotificationsEnabled;
	protected static int logInterval = 150, lagNotifyInterval, memNotifyInterval;
	protected static boolean playerLoggingEnabled;
	protected static String highLagCommand, lowMemCommand; 
	protected static int lwTaskID, mwTaskID;

	@Override
	public void onEnable(){
		pdfFile = this.getDescription();
		LagMeterConfig.loadConfig();
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
		if((sender instanceof Player && this.permit((Player)sender, "lagmeter.command."+command.getName().toLowerCase())) || !(sender instanceof Player)){
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
			}else{
				success = true;
				sender.sendMessage(gold+"Sorry, permission lagmeter.command."+command.getName().toLowerCase()+" was denied.");
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
		String wrapColor = white.toString();
		if(sender instanceof Player){
			wrapColor = gold;
		}

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
			bar+= '#';
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
		List<World> worlds = getServer().getWorlds();
		for(World world: worlds){
			String worldName = world.getName();
			int i = getServer().getWorld(worldName).getEntities().size();
			sender.sendMessage(wrapColor+"Entities in world \""+worldName+"\": "+i);
		}
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
	private boolean setupPermissions(){
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if(permissionProvider != null){
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}
	class LagWatcher extends LagMeter implements Runnable{
		LagMeter plugin;
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
	class MemoryWatcher extends LagMeter implements Runnable{
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
