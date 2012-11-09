package main.java.com.webkonsept.minecraft.lagmeter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public class LagMeterConfig extends LagMeter{
	private static YamlConfiguration configuration;
	private static File configFile;
	private static boolean loaded = false;
	
	@Override
	public YamlConfiguration getConfig(){
		if(!loaded)
			loadConfig();
		return configuration;
	}
	public static File getConfigFile(){
		return configFile;
	}
	public static void loadConfig(){
		configFile = new File(Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder(), "settings.yml");
		if(configFile.exists()){
			configuration = new YamlConfiguration();
			try{
				configuration.load(configFile);
				LagMeter.useAverage 						= configuration.getBoolean	("useAverage",							true);
				LagMeter.averageLength 						= configuration.getInt		("averageLength",						10);
				LagMeter.interval 							= configuration.getInt		("interval", 							40);
				LagMeter.displayChunksOnLoad				= configuration.getBoolean	("LoadedChunksOnLoad",					true);
				LagMeter.displayEntities					= configuration.getBoolean	("Commands.Lag.displayEntities",		true);
				LagMeter.displayEntities					= configuration.getBoolean	("Commands.Lag.displayChunks",			true);
				LagMeter.logInterval						= configuration.getInt		("log.interval",						150);
				LagMeter.enableLogging						= configuration.getBoolean	("log.enable",							true);
				LagMeter.useLogsFolder						= configuration.getBoolean	("log.useLogsFolder",					true);
				LagMeter.logChunks							= configuration.getBoolean	("log.logChunks",						true);
				LagMeter.logTotalChunksOnly					= configuration.getBoolean	("log.logTotalChunksOnly",				true);
				LagMeter.logEntities						= configuration.getBoolean	("log.logEntities",						true);
				LagMeter.logTotalEntitiesOnly				= configuration.getBoolean	("log.logTotalEntitiesOnly",			true);
				LagMeter.newBlockPerLog						= configuration.getBoolean	("log.newBlockPerLog",					true);
				LagMeter.AutomaticLagNotificationsEnabled 	= configuration.getBoolean	("Notifications.Lag.Enabled",			true);
				LagMeter.tpsNotificationThreshold 			= configuration.getInt		("Notifications.Lag.Threshold",			15);
				LagMeter.lagNotifyInterval					= configuration.getInt		("Notifications.Lag.CheckInterval",		5);
				LagMeter.highLagCommand 					= configuration.getString	("Notifications.Lag.ConsoleCommand",	"lag");
				LagMeter.AutomaticMemoryNotificationsEnabled= configuration.getBoolean	("Notifications.Memory.Enabled",		true);
				LagMeter.memoryNotificationThreshold		= configuration.getInt		("Notifications.Memory.Threshold",		25);
				LagMeter.memNotifyInterval					= configuration.getInt		("Notifications.Memory.CheckInterval",	10);
				LagMeter.lowMemCommand						= configuration.getString	("Notifications.Memory.ConsoleCommand",	"mem");
			}catch (FileNotFoundException ex){
				ex.printStackTrace();
			}catch (IOException ex){
				ex.printStackTrace();
			}catch (InvalidConfigurationException ex){
				ex.printStackTrace();
			}
			loaded = true;
		}else{
			try{
				Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder().mkdir();
				InputStream jarURL = LagMeterConfig.class.getResourceAsStream("/main/resources/settings.yml");
				copyFile(jarURL, configFile);
				configuration = new YamlConfiguration();
				configuration.load(configFile);
				try{
					configuration.load(configFile);
				}catch (FileNotFoundException ex){
					ex.printStackTrace();
				}catch (IOException ex){
					ex.printStackTrace();
				}catch (InvalidConfigurationException ex){
					ex.printStackTrace();
				}
				loaded = true;
			}catch(Exception e){
			}
		}
	}
	static private void copyFile(InputStream in, File out) throws Exception{
		InputStream fis = in;
		FileOutputStream fos = new FileOutputStream(out);
		try{
			byte[] buf = new byte[1024];
			int i = 0;
			while ((i = fis.read(buf)) != -1){
				fos.write(buf, 0, i);
			}
		} catch (Exception e){
			throw e;
		} finally{
			if(fis != null){
				fis.close();
			}
			if(fos != null){
				fos.close();
			}
		}
	}
	private LagMeterConfig(){
	}
}