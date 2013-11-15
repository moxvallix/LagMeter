package main.java.com.webkonsept.minecraft.lagmeter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

public class LagMeterConfig{
    private YamlConfiguration config;

    public LagMeterConfig(){
        this.config = null;
    }

    private void copyFile(InputStream fis, File out) throws Exception{
        FileOutputStream fos = new FileOutputStream(out);
        try{
            byte[] buffer = new byte[1024];
            int i;
            while((i = fis.read(buffer)) != -1){
                fos.write(buffer, 0, i);
            }
            fis.close();
            fos.close();
        }catch(Exception e){
            fis.close();
            fos.close();
            throw e;
        }
    }

    public YamlConfiguration getConfig() throws Exception{
        if(this.config == null)
            this.loadConfig();
        return this.config;
    }

    public void loadConfig() throws Exception{
        File configFile = new File(Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder(), "settings.yml");
        if(configFile.exists()){
            this.config = new YamlConfiguration();
            this.config.load(configFile);
        }else{
            if(Bukkit.getPluginManager().getPlugin("LagMeter").getDataFolder().exists() || Bukkit.getServer().getPluginManager().getPlugin("LagMeter").getDataFolder().mkdir()){
                final InputStream jarURL = LagMeterConfig.class.getResourceAsStream("/main/resources/settings.yml");
                this.copyFile(jarURL, configFile);
                this.config = new YamlConfiguration();
                this.config.load(configFile);
            }else{
                LagMeter.getInstance().sendConsoleMessage(LagMeter.Severity.SEVERE, "Failed to create the directory for configuration.");
            }
        }
    }
}
