package main.java.com.webkonsept.minecraft.lagmeter;

import org.bukkit.Bukkit;

public final class UptimeCommand implements Runnable{
    private String command;

    public String getCommand(){
        return this.command;
    }

    @Override
    public void run(){
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), this.command);
    }

    public void setCommand(final String newCommand){
        this.command = newCommand;
    }

    public UptimeCommand(final String command){
        this.command = command;
    }
}