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
	@Override
	public YamlConfiguration getConfig(){
		return this.loadConfig();
	}

	public YamlConfiguration loadConfig(){
		final YamlConfiguration config;
		final File configFile;
		configFile = new File(Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder(), "settings.yml");
		if(configFile.exists()){
			config = new YamlConfiguration();
			try{
				config.load(configFile);
				return config;
			}catch(final FileNotFoundException ex){
				ex.printStackTrace();
			}catch(final IOException ex){
				ex.printStackTrace();
			}catch(final InvalidConfigurationException ex){
				ex.printStackTrace();
			}
			return new YamlConfiguration();
		}else
			try{
				Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder().mkdir();
				final InputStream jarURL = LagMeterConfig.class.getResourceAsStream("/main/resources/settings.yml");
				LagMeterConfig.copyFile(jarURL, configFile);
				config = new YamlConfiguration();
				config.load(configFile);
				return config;
			}catch(final Exception e){
				e.printStackTrace();
			}
		return new YamlConfiguration();
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