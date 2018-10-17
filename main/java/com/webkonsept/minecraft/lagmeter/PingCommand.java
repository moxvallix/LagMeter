package com.webkonsept.minecraft.lagmeter;

import com.webkonsept.minecraft.lagmeter.util.SyncSendMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PingCommand implements CommandExecutor {
    private LagMeter plugin;
    private Plugin alternatePlugin;

    public PingCommand(LagMeter plugin, boolean isEnabled){
        this.plugin = plugin;

        if(!isEnabled) {
            for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
                if(p.getDescription().getMain().contains("com.webkonsept.minecraft.lagmeter")) continue;

                List<Command> commands = PluginCommandYamlParser.parse(p);
                for (Command command : commands) {
                    if (command.getLabel().equalsIgnoreCase("ping")) {
                        alternatePlugin = p;
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(this.alternatePlugin == null){
            this.ping(commandSender, strings);
        }else{
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for(int i = 2; i<stack.length; i++){
                if(stack[i].getClassName().equals(this.getClass().getName())){
                    plugin.sendMessage(commandSender, LagMeter.Severity.SEVERE, "Detected circular command failover. Can't run alternative ping command.");
                    return true;
                }
            }
            return this.alternatePlugin.onCommand(commandSender, command, s, strings);
        }
        return true;
    }

    /**
     * This method pings google.com, telling the player what the result is.
     *
     * @param sender The CommandSender object to output to.
     * @param args   <br />
     *               <ul>
     *               [0]: hops
     *               </ul>
     */
    public void ping(final CommandSender sender, final String[] args){
        final List<String> processCmd = new ArrayList<String>();
        final String hops = this.getHops(sender, args);
        final String domain = ((sender instanceof Player) ? plugin.getPlayerIP(sender.getName()) : "google.com");
        if((domain == null) || domain.isEmpty())
            return;
        processCmd.add("ping");
        processCmd.add(System.getProperty("os.name").startsWith("Windows") ? "-n" : "-c");
        processCmd.add(hops);
        processCmd.add(domain);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){
            @Override
            public void run(){
                StringBuilder fullOutput = new StringBuilder();
                try{
                    BufferedReader result;
                    BufferedReader errorStream;
                    Process p;
                    String s;
                    String output = null;
                    final String windowsPingSummary = "Average = ";
                    final String unixPingSummary = "rtt min/avg/max/mdev = ";
                    p = new ProcessBuilder(processCmd).start();
                    result = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    errorStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                    while((s = result.readLine()) != null){
                        fullOutput.append(s);
                        if(s.trim().length() != 0){
                            output = s;
                        }
                        if(s.contains(windowsPingSummary)){
                            output = s.substring(s.indexOf(windowsPingSummary) + windowsPingSummary.length());
                            break;
                        }else if(s.contains(unixPingSummary)){
                            String[] split = s.substring(unixPingSummary.length()).split("/");
                            if(split.length >= 2){
                                output = split[1]+"ms";
                            }else{
                                output = "Unexpected failure while pinging; result was: "+s;
                            }
                            break;
                        }
                    }
                    if(output != null){
                        new SyncSendMessage(sender, LagMeter.Severity.INFO, "Average response time for the server for " + hops + " ping hop(s) to " + domain + ": " + output).runTask(plugin);
                    }else{
                        new SyncSendMessage(sender, LagMeter.Severity.INFO, "Error running ping command.").runTask(plugin);
                    }
                    while((s = errorStream.readLine()) != null){
                        new SyncSendMessage(sender, LagMeter.Severity.WARNING, s).runTask(plugin);
                    }
                    errorStream.close();
                    result.close();
                    p.destroy();
                }catch(final IOException e){
                    new SyncSendMessage(sender, LagMeter.Severity.SEVERE, "Error running ping command.").runTask(plugin);
                    new SyncSendMessage(sender, LagMeter.Severity.INFO, "Full output was: "+fullOutput.toString()).runTask(plugin);
                    e.printStackTrace();
                }
            }
        });
    }
    private String getHops(final CommandSender sender, final String[] args){
        if(args.length > 0){
            if(plugin.permit(sender, "lagmeter.commands.ping.unlimited")){
                try{
                    if(Integer.parseInt(args[0]) > 10){
                        plugin.sendMessage(sender, LagMeter.Severity.WARNING, "This might take a while...");
                    }
                    return args[0];
                }catch(final NumberFormatException e){
                    plugin.sendMessage(sender, LagMeter.Severity.WARNING, "You entered an invalid amount of hops; therefore, 1 will be used instead. "+e.getMessage());
                    return "1";
                }
            }else{
                plugin.sendMessage(sender, LagMeter.Severity.WARNING, "You don't have access to specifying ping hops!");
                return "1";
            }
        }else
            return "1";
    }
}
