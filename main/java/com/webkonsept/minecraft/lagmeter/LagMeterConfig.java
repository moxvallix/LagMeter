package com.webkonsept.minecraft.lagmeter;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.omg.CORBA.DynAnyPackage.Invalid;

import java.awt.dnd.InvalidDnDOperationException;
import java.io.*;
import java.util.FormatFlagsConversionMismatchException;

public class LagMeterConfig{
	private YamlConfiguration config;

	public LagMeterConfig(){
		super();
		this.config = null;
	}

	private void copyFile(InputStream fis, File out) throws FileNotFoundException, IOException{
		FileOutputStream fos = new FileOutputStream(out);
		try{
			byte[] buffer = new byte[1024];
			int i;
			while((i = fis.read(buffer)) != -1){
				fos.write(buffer, 0, i);
			}
		}catch(IOException e){
			throw e;
		}finally{
			if(fis != null)
				fis.close();
			fos.close();
		}
	}

	public YamlConfiguration getConfig() throws InvalidConfigurationException, IOException{
		if(this.config == null)
			this.loadConfig();
		return this.config;
	}

	public void loadConfig() throws InvalidConfigurationException, IOException{
		File configFile = new File(Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder(), "settings.yml");
		if(configFile.exists()){
			this.config = new YamlConfiguration();
			this.config.load(configFile);
		}else{
			if(Bukkit.getPluginManager().getPlugin("LagMeter").getDataFolder().exists() || Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder().mkdir()){
				final InputStream jarURL = LagMeterConfig.class.getResourceAsStream("/resources/settings.yml");
				try{
					this.copyFile(jarURL, configFile);
					this.config = new YamlConfiguration();
					this.config.load(configFile);
				}catch(InvalidConfigurationException e){
					throw e;
				}catch(IOException e){
					throw e;
				}finally{
					if(jarURL != null)
						jarURL.close();
				}
			}else{
				LagMeter.getInstance().sendConsoleMessage(LagMeter.Severity.SEVERE, "Failed to create the directory for configuration.");
			}
		}
	}

	public String toString(){
		return "LagMeterConfig@"+hashCode()+"{\n\tconfig = "+this.config.toString()+"\n}";
	}
}