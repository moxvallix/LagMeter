package main.java.com.webkonsept.minecraft.lagmeter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
	double memFree = memMax - memUsed;
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
	
	@Override
	public void onEnable(){
		pdfFile = this.getDescription();
		LagMeterConfig.loadConfig();
		if(!logsFolder.exists() && useLogsFolder && enableLogging){
			info("Logs folder not found. Creating one for you.");
			logsFolder.mkdir();
			if(!logsFolder.exists()){
				severe("Error! Couldn't create the folder!");
			}else{
				info("Logs folder created.");
			}
			Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new LagWatcher(), lagNotifyInterval*1200, lagNotifyInterval*1200);
			Bukkit.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new MemoryWatcher(), memNotifyInterval*1200, memNotifyInterval*1200);
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
		String loggingMessage = "";
		if(enableLogging){
			loggingMessage = "  Logging to "+logger.getFilename();
		}
		info("Enabled! Polling every "+interval+" server ticks."+loggingMessage);
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
		getServer().getScheduler().cancelTasks(this);
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args){
		if(!this.isEnabled())
			return false;
		boolean success = false;
		if((sender instanceof Player && this.permit((Player)sender, "lagmeter.command."+command.getName().toLowerCase()))
		|| !(sender instanceof Player)){
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
			if((tpsNotificationThreshold <= plugin.getTPS()) && AutomaticLagNotificationsEnabled){
				Player[] players = Bukkit.getServer().getOnlinePlayers();
				for(Player p: players){
					if(permit(p, "lagmeter.notify.lag") || p.isOp())
						p.sendMessage(igt+red+"The server's TPS has dropped below "+tpsNotificationThreshold+"! If you configured a server command to execute at this time, it will run now.");
				}
				Bukkit.getServer().dispatchCommand(null, highLagCommand);
			}
		}
	}
	class MemoryWatcher extends LagMeter implements Runnable{
		public void run(){
			if((memoryNotificationThreshold <= plugin.memFree) && AutomaticMemoryNotificationsEnabled){
				Player[] players = Bukkit.getServer().getOnlinePlayers();
				for(Player p: players){
					if(permit(p, "lagmeter.notify.mem") || p.isOp()){
						p.sendMessage(igt+red+"The server's free memory pool has dropped below "+memFree+"%! If you configured a server command to execute at this time, it will run now.");
					}
				}
				Bukkit.getServer().dispatchCommand(null, lowMemCommand);
			}
		}
	}
}
