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

    public LagMeterConfig(){
        this.config = null;
    }

    private static void copyFile(InputStream fis, File out) throws Exception{
        FileOutputStream fos = new FileOutputStream(out);
        try{
            byte[] buffer = new byte[1024];
            int i = 0;
            while((i = fis.read(buffer)) != -1){
                fos.write(buffer, 0, i);
            }
        }catch(Exception e){
            throw e;
        }finally{
            if(fis != null){
                fis.close();
            }
            if(fos != null){
                fos.close();
            }
        }
    }

    @Override
    public YamlConfiguration getConfig() throws Exception{
        if(this.config == null)
            return this.loadConfig();
        return this.config;
    }

    public void loadConfig() throws Exception{
        File configFile = new File(Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder(), "settings.yml");
        if(configFile.exists()){
            this.config = new YamlConfiguration();
            this.config.load(configFile);
            return;
        }else{
            if(Bukkit.getPluginManager().getPlugin("LagMeter").getDataFolder().exists() || Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder().mkdir()){
                final InputStream jarURL = LagMeterConfig.class.getResourceAsStream("/main/resources/settings.yml");
                LagMeterConfig.copyFile(jarURL, configFile);
                this.config = new YamlConfiguration();
                this.config.load(configFile);
                return;
            }else{
                this.sendConsoleMessage(Severity.SEVERE, "Failed to create the directory for configuration.");
            }
        }
    }
}
