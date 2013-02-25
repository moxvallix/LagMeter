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
	private YamlConfiguration config;
	private File configFile;
	private boolean loaded = false;
	private LagMeter plugin;

	public File getConfigFile(){
		return this.configFile;
	}

	protected LagMeterConfig(LagMeter l){
		this.plugin = l;
	}

	@Override
	public YamlConfiguration getConfig(){
		if(!loaded)
			loadConfig();
		return config;
	}

	public void loadConfig(){
		configFile = new File(Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder(), "settings.yml");
		if(configFile.exists()){
			config = new YamlConfiguration();
			try{
				config.load(configFile);
				plugin.useAverage							= config.getBoolean	("useAverage",							true);
				plugin.averageLength						= config.getInt		("averageLength",						10);
				plugin.interval								= config.getInt		("interval",							40);
				plugin.displayChunksOnLoad					= config.getBoolean	("LoadedChunksOnLoad",					true);
				plugin.displayEntitiesOnLoad				= config.getBoolean	("displayEntitiesOnLoad",				true);
				plugin.displayEntities						= config.getBoolean	("Commands.Lag.displayEntities",		true);
				plugin.sendChunks							= config.getBoolean	("Commands.Lag.displayChunks",			true);
				plugin.logInterval							= config.getInt		("log.interval",						150);
				plugin.enableLogging						= config.getBoolean	("log.enable",							true);
				plugin.useLogsFolder						= config.getBoolean	("log.useLogsFolder",					false);
				plugin.playerLoggingEnabled					= config.getBoolean	("log.logPlayersOnline",				true);
				plugin.logChunks							= config.getBoolean	("log.logChunks",						false);
				plugin.logTotalChunksOnly					= config.getBoolean	("log.logTotalChunksOnly",				true);
				plugin.logEntities							= config.getBoolean	("log.logEntities",						false);
				plugin.logTotalEntitiesOnly					= config.getBoolean	("log.logTotalEntitiesOnly",			true);
				plugin.newBlockPerLog						= config.getBoolean	("log.newBlockPerLog",					true);
				plugin.newLineForLogStats					= config.getBoolean	("log.newLinePerStatistic",				true);
				plugin.AutomaticLagNotificationsEnabled		= config.getBoolean	("Notifications.Lag.Enabled",			true);
				plugin.tpsNotificationThreshold				= config.getInt		("Notifications.Lag.Threshold",			15);
				plugin.lagNotifyInterval					= config.getInt		("Notifications.Lag.CheckInterval",		5);
				plugin.highLagCommand						= config.getString	("Notifications.Lag.ConsoleCommand",	"/lag");
				plugin.AutomaticMemoryNotificationsEnabled	= config.getBoolean	("Notifications.Memory.Enabled",		true);
				plugin.memoryNotificationThreshold			= config.getInt		("Notifications.Memory.Threshold",		25);
				plugin.memNotifyInterval					= config.getInt		("Notifications.Memory.CheckInterval",	10);
				plugin.lowMemCommand						= config.getString	("Notifications.Memory.ConsoleCommand",	"/mem");
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
				config = new YamlConfiguration();
				config.load(configFile);
				try{
					config.load(configFile);
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

	private static void copyFile(InputStream in, File out) throws Exception{
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
}