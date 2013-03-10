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
	private final LagMeter plugin;

	public File getConfigFile(){
		return this.configFile;
	}

	protected LagMeterConfig(final LagMeter l){
		this.plugin = l;
	}

	@Override
	public YamlConfiguration getConfig(){
		if(!this.loaded)
			this.loadConfig();
		return this.config;
	}

	public void loadConfig(){
		this.configFile = new File(Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder(), "settings.yml");
		if(this.configFile.exists()){
			this.config = new YamlConfiguration();
			try{
				this.config.load(this.configFile);
				this.plugin.useAverage = this.config.getBoolean("useAverage", true);
				this.plugin.averageLength = this.config.getInt("averageLength", 10);
				this.plugin.interval = this.config.getInt("interval", 40);
				this.plugin.displayChunksOnLoad = this.config.getBoolean("LoadedChunksOnLoad", true);
				this.plugin.displayEntitiesOnLoad = this.config.getBoolean("displayEntitiesOnLoad", true);
				this.plugin.displayEntities = this.config.getBoolean("Commands.Lag.displayEntities", true);
				this.plugin.sendChunks = this.config.getBoolean("Commands.Lag.displayChunks", true);
				this.plugin.logInterval = this.config.getInt("log.interval", 150);
				this.plugin.enableLogging = this.config.getBoolean("log.enable", true);
				this.plugin.useLogsFolder = this.config.getBoolean("log.useLogsFolder", false);
				this.plugin.playerLoggingEnabled = this.config.getBoolean("log.logPlayersOnline", true);
				this.plugin.logChunks = this.config.getBoolean("log.logChunks", false);
				this.plugin.logTotalChunksOnly = this.config.getBoolean("log.logTotalChunksOnly", true);
				this.plugin.logEntities = this.config.getBoolean("log.logEntities", false);
				this.plugin.logTotalEntitiesOnly = this.config.getBoolean("log.logTotalEntitiesOnly", true);
				this.plugin.newBlockPerLog = this.config.getBoolean("log.newBlockPerLog", true);
				this.plugin.newLineForLogStats = this.config.getBoolean("log.newLinePerStatistic", true);
				this.plugin.AutomaticLagNotificationsEnabled = this.config.getBoolean("Notifications.Lag.Enabled", true);
				this.plugin.tpsNotificationThreshold = this.config.getInt("Notifications.Lag.Threshold", 15);
				this.plugin.lagNotifyInterval = this.config.getInt("Notifications.Lag.CheckInterval", 5);
				this.plugin.highLagCommand = this.config.getString("Notifications.Lag.ConsoleCommand", "/lag");
				this.plugin.AutomaticMemoryNotificationsEnabled = this.config.getBoolean("Notifications.Memory.Enabled", true);
				this.plugin.memoryNotificationThreshold = this.config.getInt("Notifications.Memory.Threshold", 25);
				this.plugin.memNotifyInterval = this.config.getInt("Notifications.Memory.CheckInterval", 10);
				this.plugin.lowMemCommand = this.config.getString("Notifications.Memory.ConsoleCommand", "/mem");
			}catch(final FileNotFoundException ex){
				ex.printStackTrace();
			}catch(final IOException ex){
				ex.printStackTrace();
			}catch(final InvalidConfigurationException ex){
				ex.printStackTrace();
			}
			this.loaded = true;
		}else
			try{
				Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder().mkdir();
				final InputStream jarURL = LagMeterConfig.class.getResourceAsStream("/main/resources/settings.yml");
				LagMeterConfig.copyFile(jarURL, this.configFile);
				this.config = new YamlConfiguration();
				this.config.load(this.configFile);
				try{
					this.config.load(this.configFile);
				}catch(final FileNotFoundException ex){
					ex.printStackTrace();
				}catch(final IOException ex){
					ex.printStackTrace();
				}catch(final InvalidConfigurationException ex){
					ex.printStackTrace();
				}
				this.loaded = true;
			}catch(final Exception e){
			}
	}

	private static void copyFile(final InputStream in, final File out) throws Exception{
		final InputStream fis = in;
		final FileOutputStream fos = new FileOutputStream(out);
		try{
			final byte[] buf = new byte[1024];
			int i = 0;
			while((i = fis.read(buf))!=-1)
				fos.write(buf, 0, i);
		}catch(final Exception e){
			throw e;
		}finally{
			if(fis!=null)
				fis.close();
			if(fos!=null)
				fos.close();
		}
	}
}