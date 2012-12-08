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
	private LagMeter plugin;
	
	@Override
	public YamlConfiguration getConfig(){
		if(!loaded)
			loadConfig();
		return configuration;
	}
	public static File getConfigFile(){
		return configFile;
	}
	public void loadConfig(){
		configFile = new File(Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder(), "settings.yml");
		if(configFile.exists()){
			configuration = new YamlConfiguration();
			try{
				configuration.load(configFile);
				plugin.useAverage							= configuration.getBoolean	("useAverage",							true);
				plugin.averageLength						= configuration.getInt		("averageLength",						10);
				plugin.interval								= configuration.getInt		("interval",							40);
				plugin.displayChunksOnLoad					= configuration.getBoolean	("LoadedChunksOnLoad",					true);
				plugin.displayEntitiesOnLoad				= configuration.getBoolean	("displayEntitiesOnLoad",				true);
				plugin.displayEntities						= configuration.getBoolean	("Commands.Lag.displayEntities",		true);
				plugin.sendChunks							= configuration.getBoolean	("Commands.Lag.displayChunks",			true);
				plugin.logInterval							= configuration.getInt		("log.interval",						150);
				plugin.enableLogging						= configuration.getBoolean	("log.enable",							true);
				plugin.useLogsFolder						= configuration.getBoolean	("log.useLogsFolder",					false);
				plugin.logChunks							= configuration.getBoolean	("log.logChunks",						false);
				plugin.logTotalChunksOnly					= configuration.getBoolean	("log.logTotalChunksOnly",				true);
				plugin.logEntities							= configuration.getBoolean	("log.logEntities",						false);
				plugin.logTotalEntitiesOnly					= configuration.getBoolean	("log.logTotalEntitiesOnly",			true);
				plugin.newBlockPerLog						= configuration.getBoolean	("log.newBlockPerLog",					true);
				plugin.newLineForLogStats					= configuration.getBoolean	("log.NewLinePerStatistic",				true);
				plugin.AutomaticLagNotificationsEnabled		= configuration.getBoolean	("Notifications.Lag.Enabled",			true);
				plugin.tpsNotificationThreshold				= configuration.getInt		("Notifications.Lag.Threshold",			15);
				plugin.lagNotifyInterval					= configuration.getInt		("Notifications.Lag.CheckInterval",		5);
				plugin.highLagCommand						= configuration.getString	("Notifications.Lag.ConsoleCommand",	"lag");
				plugin.AutomaticMemoryNotificationsEnabled	= configuration.getBoolean	("Notifications.Memory.Enabled",		true);
				plugin.memoryNotificationThreshold			= configuration.getInt		("Notifications.Memory.Threshold",		25);
				plugin.memNotifyInterval					= configuration.getInt		("Notifications.Memory.CheckInterval",	10);
				plugin.lowMemCommand						= configuration.getString	("Notifications.Memory.ConsoleCommand",	"mem");
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
	protected LagMeterConfig(LagMeter l){
		this.plugin = l;
	}
}