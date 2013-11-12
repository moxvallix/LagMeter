package main.java.com.webkonsept.minecraft.lagmeter;

import main.java.com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import main.java.com.webkonsept.minecraft.lagmeter.util.SyncCommand;

public class TimedCommand implements Runnable{
    private final String command;
    private final LagMeter plugin;

    public void process(String s){
        try{
            Thread.sleep(this.plugin.parseTimeMS(s));
        }catch(final InvalidTimeFormatException e){
            e.printStackTrace();
        }catch(final InterruptedException e){
        }
        new SyncCommand(s.split("<>")[0]).runTask(this.plugin);
    }

    @Override
    public void run(){
        if(this.command.contains(";")){
            for(final String cmd : this.command.split(";")){
                this.process(cmd);
            }
        }else{
            this.process(this.command);
        }
    }

    public TimedCommand(String command, LagMeter plugin){
        this.command = command;
        this.plugin = plugin;
    }
}