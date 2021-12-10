package com.moxvallix;

import com.moxvallix.eventhandlers.DefaultHighLag;
import com.moxvallix.eventhandlers.DefaultLowMemory;
import com.moxvallix.eventhandlers.PlayerJoinListener;
import com.moxvallix.eventhandlers.PlayerQuitListener;
import com.moxvallix.events.HighLagEvent;
import com.moxvallix.events.LowMemoryEvent;
import com.moxvallix.exceptions.InvalidTimeFormatException;
import com.moxvallix.exceptions.NoActiveLagMapException;
import com.moxvallix.exceptions.NoAvailableTPSException;
import com.moxvallix.exceptions.NoMapHeldException;
import com.moxvallix.listeners.LagListener;
import com.moxvallix.listeners.MemoryListener;
import com.moxvallix.util.SyncSendMessage;
import com.moxvallix.util.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LagMeter extends JavaPlugin{
	private LagMeterLogger logger;
	private LagMeterPoller poller;
	private LagMeterStack history;
	private float ticksPerSecond;
	private long uptime;
	private int averageLength;
	private double memUsed, memMax, memFree, percentageFree;
	private LagWatcher lagWatcher;
	private MemoryWatcher memWatcher;
	private List<LagListener> syncLagListeners, asyncLagListeners;
	private List<MemoryListener> syncMemListeners, asyncMemListeners;
	// Configurable Values - mostly booleans
	private int interval;
	private int logInterval;
	private int lagNotifyInterval;
	private int memNotifyInterval;
	private int pollingDelay;
	private float tpsNotificationThreshold, memoryNotificationThreshold;
	private boolean useAverage;
	private boolean enableLogging;
	private boolean useLogsFolder;
	private boolean AutomaticLagNotificationsEnabled;
	private boolean AutomaticMemoryNotificationsEnabled;
	private boolean displayEntities;
	private boolean playerLoggingEnabled;
	private boolean displayChunksOnLoad;
	private boolean displayChunks;
	private boolean logChunks;
	private boolean logTotalChunksOnly;
	private boolean logEntities;
	private boolean logTotalEntitiesOnly;
	private boolean newBlockPerLog;
	private boolean displayEntitiesOnLoad;
	private boolean newLineForLogStats;
	private boolean repeatingUptimeCommands;
	private boolean lagmapsEnabled;
	private boolean stripConsoleColors;
	public boolean isPingEnabled;
	private List<String> uptimeCommands;
	private String highLagCommand, lowMemCommand;
	private static LagMeter p;
	private HashMap<String, String> pingDomains;
	private HashMap<String, MapView> maps;
	private HashMap<String, List<MapRenderer>> oldRenderers;
	private LagMapRenderer renderer;
	private Integer mapRenderInterval;

	/**
	 * This method gets the current instance of LagMeter.
	 *
	 * @return The current instance of the plugin's main class.
	 */
	public static LagMeter getInstance(){
		return LagMeter.p;
	}

	/**
	 * This method is the main method for running this plugin as a java
	 * application. As Bukkit plugins are not intended to be run directly, all
	 * this does is give the user a long error message in a message box and then
	 * exits.
	 *
	 * @param args Command-line arguments, with which nothing is done.
	 */
	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(final ClassNotFoundException e){
			e.printStackTrace();
		}catch(final InstantiationException e){
			e.printStackTrace();
		}catch(final IllegalAccessException e){
			e.printStackTrace();
		}catch(final UnsupportedLookAndFeelException e){
			e.printStackTrace();
		}finally{
			JOptionPane.showMessageDialog(null, "Sorry, but LagMeter is a Bukkit plugin, and cannot be run directly like you've attempted.\nTo use the plugin, download and set up a Bukkit Minecraft server, and in the root directory, create a folder called\n\"plugins\" (no quotes, and assuming it hasn't already been created for you), and put this JAR file (LagMeter.jar) there.\nWhen you've done that, start the Bukkit server using the command line java -jar \"path to Bukkit.jar\",\nor if it's already running, type \"reload\" (no quotes) into the command-line.", "LagMeter", JOptionPane.ERROR_MESSAGE);
		}
		System.exit(0);
	}

	/**
	 * Add a TPS value to the plugin's history.
	 *
	 * @param tps The TPS value to add.
	 */
	public void addHistory(final double tps){
		this.history.add(tps);
	}

	/**
	 * Not intended to be used by other plugins. <br />
	 * <br />
	 * Adds a specified player to a HashMap so that they can be pinged later
	 * without depending on {@code CraftPlayer}.
	 *
	 * @param name        The name of the player to add
	 * @param hostAddress The player's IP
	 */
	public void addPlayerIP(String name, String hostAddress){
		this.pingDomains.put(name, hostAddress);
	}

	private void cancelAllLagListeners(){
		this.asyncLagListeners.clear();
		this.syncLagListeners.clear();
	}

	private void cancelAllMemoryListeners(){
		this.asyncMemListeners.clear();
		this.syncMemListeners.clear();
	}

	@Override
	public YamlConfiguration getConfig(){
		try{
			return new LagMeterConfig().getConfig();
		}catch(Exception e){
			this.sendConsoleMessage(Severity.SEVERE, "An error occurred while loading the configuration: " + e.getMessage());
		}
		return new YamlConfiguration();
	}

	@Override
	public void reloadConfig(){
		super.reloadConfig();
		this.updateConfiguration();
	}

	/**
	 * Uses an ID obtained from the registerLagListener(LagListener) method to
	 * cancel recurring notification of your observer object.
	 *
	 * @param id The id of the LagListener to stop notifying.
	 */
	public void cancelAsyncLagListener(final int id){
		this.asyncLagListeners.set(id, null);
	}

	/**
	 * Uses an ID obtained from the registerMemoryListener(MemoryListener)
	 * method to cancel recurring notification of your observer object.
	 *
	 * @param id The id of the MemoryListener to stop notifying.
	 */
	public void cancelAsyncMemoryListener(final int id){
		this.asyncMemListeners.set(id, null);
	}

	/**
	 * Uses an ID obtained from the registerLagListener(LagListener) method to
	 * cancel recurring notification of your observer object.
	 *
	 * @param id The id of the LagListener to stop notifying.
	 */
	public void cancelSyncLagListener(final int id){
		this.syncLagListeners.set(id, null);
	}

	/**
	 * Uses an ID obtained from the registerMemoryListener(MemoryListener)
	 * method to cancel recurring notification of your observer object.
	 *
	 * @param id The id of the MemoryListener to stop notifying.
	 */
	public void cancelSyncMemoryListener(final int id){
		this.syncMemListeners.set(id, null);
	}

	private List<LagListener> getAsyncLagListeners(){
		return this.asyncLagListeners;
	}

	private List<MemoryListener> getAsyncMemoryListeners(){
		return this.asyncMemListeners;
	}

	/**
	 * This is the getter for the upper limit of length of the history of the
	 * plugin's TPS averaging. This value is from the configuration, even if
	 * averaging is disabled.
	 *
	 * @return The max bound for the history. Not necessarily 0 if averaging is disabled.
	 */
	public int getAverageLength(){
		return this.averageLength;
	}

	/**
	 * This is the getter for the sleep interval of the TPS watcher's thread.
	 *
	 * @return How often the plugin checks the server's TPS to notify its
	 * observers.
	 */
	public long getCheckLagInterval(){
		return this.lagNotifyInterval;
	}

	/**
	 * This is the getter for the sleep interval of the memory watcher's thread.
	 *
	 * @return How often the plugin checks the server's memory to notify its
	 * observers.
	 */
	public long getCheckMemoryInterval(){
		return this.memNotifyInterval;
	}

	/**
	 * This method will return the time in days, hours, minutes, and seconds,
	 * since the server was <b>started</b> <i>OR</i> <b>since it was last
	 * reloaded</b>.
	 *
	 * @return An array of <b>int</b>s, i, where:<br />
	 * <ul>
	 * <b>i[0]</b> is the seconds,<br />
	 * <b>i[1]</b> is the minutes,<br />
	 * <b>i[2]</b> is the hours,<br />
	 * <b>i[3]</b> is the days,
	 * </ul>
	 * that the server has been online without reloading.
	 */
	public int[] getCurrentServerUptime(){
		final int[] i = new int[4];
		long l = System.currentTimeMillis() - this.uptime;
		i[3] = (int)(l / 86400000L);
		l -= i[3] * 86400000L;
		i[2] = (int)(l / 3600000L);
		l -= i[2] * 3600000;
		i[1] = (int)(l / 60000L);
		l -= i[1] * 60000L;
		i[0] = (int)(l / 1000L);
		return i;
	}

	/**
	 * Gets the LagMeterStack (the history) of the server TPS.
	 *
	 * @return The LagMeterStack for however long the average upper bound
	 * allows.
	 */
	public LagMeterStack getHistory(){
		return this.history;
	}

	/**
	 * Gets the delay between the plugin enabling and the plugin starting to poll the server for its TPS. Always in seconds.
	 *
	 * @return The delay for the plugin to start polling, in seconds.
	 */
	public int getPollingDelay(){
		return this.pollingDelay;
	}

	/**
	 * This is a getter for how often the server is polled by LagMeter.
	 *
	 * @return How often LagMeter polls the server's TPS.
	 */
	public int getInterval(){
		return this.interval;
	}

	/**
	 * This is the getter for the command the plugin runs when the default high
	 * lag listener is triggered.
	 *
	 * @return The lag notification command.
	 */
	public String getLagCommand(){
		return this.highLagCommand;
	}

	/**
	 * Getter for the logger of tps and memory (and players if so configured).
	 *
	 * @return The instance of LagMeter's logger for logging tps/memory/players.
	 */
	public LagMeterLogger getLMLogger(){
		return this.logger;
	}

	/**
	 * Gets the current memory used, maxmimum memory, memory free, and
	 * percentage of memory free. Returned in a single array of doubles.
	 *
	 * @return memory[], which is an array of doubles, containing four values,
	 * where: <br />
	 * <b><i>memory[0]</i></b> is the currently used memory;<br />
	 * <b><i>memory[1]</i></b> is the current maximum memory;<br />
	 * <b><i>memory[2]</i></b> is the current free memory;<br />
	 * <b><i>memory[3]</i></b> is the percentage memory free (note this
	 * may be an irrational number, so you might want to truncate it if
	 * you use this).
	 *
	 * @since 1.11.0-SNAPSHOT
	 */
	public synchronized double[] getMemory(){
		this.updateMemoryStats();
		return new double[]{this.memUsed, this.memMax, this.memFree, this.percentageFree};
	}

	/**
	 * This is the getter for the command the plugin runs when the default low
	 * memory listener is triggered.
	 *
	 * @return The memory notification command.
	 */
	public String getMemoryCommand(){
		return this.lowMemCommand;
	}

	/**
	 * The memory notification theshold is the point at which LagMeter will
	 * notify its observers that the memory free has dropped below this amount.
	 *
	 * @return The memory notification theshold.
	 */
	public float getMemoryNotificationThreshold(){
		return this.memoryNotificationThreshold;
	}

	/**
	 * Although this is not really intended for use by other plugins, it may be
	 * useful to some. <br />
	 * <br />
	 * This method gets the IP of a player, which is stored in a HashMap when
	 * they log in, and removed when they log out. This is to avoid use of an
	 * NMS class, {@code CraftPlayer}. <br />
	 * <br />
	 * This method is an overloaded version of {@link #getPlayerIP(String)}.
	 * This method invokes that method, and returns its return, unmodified.
	 *
	 * @param p The player to get the IP of.
	 *
	 * @return The IP of the player specified, in String form.
	 *
	 * @see #getPlayerIP(String)
	 */
	public String getPlayerIP(Player p){
		return this.getPlayerIP(p.getName());
	}

	/**
	 * Although this is not really intended for use by other plugins, it may be
	 * useful to some. <br />
	 * <br />
	 * This method gets the IP of a player, which is stored in a HashMap when
	 * they log in, and removed when they log out. This is to avoid use of an
	 * NMS class, {@code CraftPlayer}.
	 *
	 * @param player The player's name to get the IP of.
	 *
	 * @return The specified player's IP, in String form.
	 */
	public String getPlayerIP(String player){
		return this.pingDomains.get(player);
	}

	private List<LagListener> getSyncLagListeners(){
		return this.syncLagListeners;
	}

	private List<MemoryListener> getSyncMemoryListeners(){
		return this.syncMemListeners;
	}

	/**
	 * Gets the ticks per second.
	 *
	 * @return The servers's ticks per second, out of 20 (20 being perfect, 0 being the server has reached an absolute halt [and for that reason, it will never be 0]), as of last poll. If the plugin is set to average the TPS, it will return the average instead.
	 *
	 * @throws NoAvailableTPSException If the TPS polling delay has not yet expired. Defaults to 75 seconds.
	 * @since 1.8
	 */
	public double getTPS() throws NoAvailableTPSException{
		if(this.useAverage){
			if(this.history.getAverage() < 0D)
				throw new NoAvailableTPSException("The TPS polling pollingDelay (" + this.pollingDelay + " seconds) has not yet expired.");
			return this.history.getAverage();
		}

		if(this.ticksPerSecond < 0D)
			throw new NoAvailableTPSException("The TPS polling pollingDelay (" + this.pollingDelay + " seconds) has not yet expired.");
		return this.ticksPerSecond;
	}

	/**
	 * The TPS notification threshold is where the plugin will notify its
	 * observers when the TPS reaches or drops below this amount.
	 *
	 * @return The tps notification theshold.
	 */
	public float getTpsNotificationThreshold(){
		return this.tpsNotificationThreshold;
	}

	private void handleBaseCommand(final CommandSender sender, final String[] args){
		if(args[0].equalsIgnoreCase("reload")){
			if(this.permit(sender, "lagmeter.command.lagmeter.reload") || this.permit(sender, "lagmeter.reload")){
				this.updateConfiguration();
				this.sendMessage(sender, Severity.INFO, "Configuration reloaded!");
			}
		}else if(args[0].equalsIgnoreCase("help")){
			if(this.permit(sender, "lagmeter.command.lagmeter.help") || this.permit(sender, "lagmeter.help")){
				if((args.length == 1) || args[1].trim().equals("0") || args[1].trim().equals("1")){
					this.sendMessage(sender, Severity.INFO, "*           *Help for LagMeter [1/2]*           *");
					if(this.permit(sender, "lagmeter.command.lag")){
						this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/lag" + ChatColor.GOLD + " - Check the server's TPS. If configuChatColor.RED, may also display chunks loaded and/or entities alive.");
					}
					if(this.permit(sender, "lagmeter.command.mem")){
						this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/mem" + ChatColor.GOLD + " - Displays how much memory the server currently has free.");
					}
					if(this.permit(sender, "lagmeter.command.lagmem") || this.permit(sender, "lagmeter.command.lm")){
						this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/lagmem|/lm" + ChatColor.GOLD + " - A combination of both /lag and /mem.");
					}
					if(this.permit(sender, "lagmeter.command.lchunks")){
						this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/lchunks" + ChatColor.GOLD + " - Shows how many chunks are currently loaded in each world, then with a total.");
					}
					if(this.permit(sender, "lagmeter.command.lmobs") || this.permit(sender, "lagmeter.command.lentities")){
						this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/lmobs|/lentities" + ChatColor.GOLD + " - Shows how many entities are currently alive in each world, then with a total.");
					}
					if(this.permit(sender, "lagmeter.command.lmp")){
						this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/lmp" + ChatColor.GOLD + " - Has the same function as /lagmem, but includes a player count.");
					}
					if(this.permit(sender, "lagmeter.command.lagmeter")){
						this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/lagmeter|/lm" + ChatColor.GOLD + " - Shows the current version and gives sub-commands.");
					}
					if(this.permit(sender, "lagmeter.command.lagmeter.reload") || this.permit(sender, "lagmeter.reload")){
						this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/lagmeter|/lm" + ChatColor.GREEN + " <reload|r> " + ChatColor.GOLD + " - Allows the player to reload the configuration.");
					}
				}else if((args.length > 1) && args[1].trim().equals("2")){
					this.sendMessage(sender, Severity.INFO, "*           *Help for LagMeter [2/2]*           *");
					this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/lagmeter|/lm" + ChatColor.GREEN + " <help|?> [page]" + ChatColor.GOLD + " - This command. Gives the user a list of commands that they are able to use in this plugin.");
					if(this.permit(sender, "lagmeter.command.ping") || this.permit(sender, "lagmeter.command.lping")){
						this.sendMessage(sender, Severity.INFO, ChatColor.DARK_GREEN + "/ping|/lping" + ChatColor.GREEN + " [hops] " + ChatColor.GOLD + " - Pings google.com from the server. Specify an amount of hops to specify more packets. Note: this can take a long time (especially if there is high latency), as it sends a ping to an external resource.");
					}
				}else{
					this.sendMessage(sender, Severity.WARNING, "Invalid page number.");
				}
			}else{
				this.sendMessage(sender, Severity.WARNING, "Sorry, but you don't have access to the help command.");
			}
		}else{
			this.sendMessage(sender, Severity.WARNING, "Invalid sub-command. " + ChatColor.GOLD + "Try one of these:");
			this.sendMessage(sender, Severity.INFO, "Available sub-commands: /lagmeter|lm <reload|r>|/lm short|lm <help|?>");
		}
	}

	/**
	 * Whether or not the plugin is taking an average of TPS when outputting.
	 *
	 * @return Whether or not the plugin is averaging TPS.
	 */
	public boolean isAveraging(){
		return this.useAverage;
	}

	/**
	 * Whether or not the plugin should display chunks with /lag
	 *
	 * @return Displaying chunks with /lag or not
	 */
	public boolean isDisplayingChunks(){
		return this.displayChunks;
	}

	/**
	 * Whether or not the plugin should display entities with /lag
	 *
	 * @return Displaying entities with /lag or not
	 */
	public boolean isDisplayingEntities(){
		return this.displayEntities;
	}

	/**
	 * Whether or not the plugin is logging chunks when it logs.
	 *
	 * @return The plugin's setting for logging chunks.
	 */
	public boolean isLoggingChunks(){
		return this.isLoggingEnabled() && this.logChunks;
	}

	/**
	 * This is a getter for whether or not the plugin is logging anything at
	 * all.
	 *
	 * @return Whether or not the plugin is logging stuff.
	 */
	public boolean isLoggingEnabled(){
		return this.enableLogging;
	}

	/**
	 * Whether or not the plugin is logging entities when it logs.
	 *
	 * @return The plugin's setting for logging entities.
	 */
	public boolean isLoggingEntities(){
		return this.isLoggingEnabled() && this.logEntities;
	}

	/**
	 * Whether or not the plugin is logging total chunks only, not by world with
	 * a total.
	 *
	 * @return Whether or not the plugin will log chunks per-world, or only a
	 * total. Will return false if the setting is true, but the logging
	 * chunks option is off.
	 */
	public boolean isLoggingTotalChunksOnly(){
		return this.isLoggingChunks() && this.logTotalChunksOnly;
	}

	/**
	 * Whether or not the plugin is logging total entities only, not by world
	 * with a total.
	 *
	 * @return Whether or not the plugin will log entities per-world, or only a
	 * total. Will return false if the setting is true, but the logging
	 * entities option is off.
	 */
	public boolean isLoggingTotalEntitiesOnly(){
		return this.isLoggingEntities() && this.logTotalEntitiesOnly;
	}

	/**
	 * Whether or not the plugin will log players online.
	 *
	 * @return If the plugin is logging players online or not.
	 */
	public boolean isPlayerLoggingEnabled(){
		return this.playerLoggingEnabled;
	}

	/**
	 * This is the getter for whether or not the plugin will put its logfile in
	 * a folder, with today as its date.
	 *
	 * @return Whether or not the log will be separated from others, based on
	 * the date it was created.
	 */
	public boolean isUsingLogFolder(){
		return this.useLogsFolder;
	}

	/**
	 * This method gets whether or not the plugin, when logging, will add an
	 * extra empty line between logging blocks.
	 *
	 * @return If the plugin inserts an extra line feed between logging blocks.
	 */
	public boolean isUsingNewBlockEveryLog(){
		return this.newBlockPerLog;
	}

	/**
	 * This method gets whether or not the plugin, when logging, will add an
	 * extra empty line between logging entities and chunks, etc.
	 *
	 * @return If the plugin inserts an extra line feed between logging chunks,
	 * etc..
	 */
	public boolean isUsingNewLineForLogStats(){
		return this.newLineForLogStats;
	}

	private void notifyAsyncLagListeners(){
		try{
			final HighLagEvent e = new HighLagEvent(this.getTPS());
			for(final LagListener l : this.getAsyncLagListeners()){
				if(l != null){
					new Thread(new Runnable(){
						@Override
						public void run(){
							l.onHighLagEvent(e);
						}
					}).start();
				}
			}
		}catch(NoAvailableTPSException e){
			//do nothing, shouldn't be checking anyways
		}
	}

	private void notifyAsyncMemoryListeners(){
		try{
			final LowMemoryEvent e = new LowMemoryEvent(this.getMemory(), this.getTPS());
			for(final MemoryListener m : this.getAsyncMemoryListeners()){
				if(m != null){
					new Thread(new Runnable(){
						@Override
						public void run(){
							m.onLowMemoryEvent(e);
						}
					}).start();
				}
			}
		}catch(NoAvailableTPSException e){
			//do nothing, shouldn't be checking anyways
		}
	}

	void notifyLagListeners(){
		this.notifyAsyncLagListeners();
		this.notifySyncLagListeners();
	}

	void notifyMemoryListeners(){
		this.notifyAsyncMemoryListeners();
		this.notifySyncMemoryListeners();
	}

	private void notifySyncLagListeners(){
		new BukkitRunnable(){
			@Override
			public void run(){
				try{
					final HighLagEvent e = new HighLagEvent(LagMeter.this.getTPS());
					for(final LagListener l : LagMeter.this.getSyncLagListeners()){
						if(l != null){
							l.onHighLagEvent(e);
						}
					}
				}catch(NoAvailableTPSException e){
					//do nothing, shouldn't be checking anyways
				}
			}
		}.runTask(this);
	}

	private void notifySyncMemoryListeners(){
		new BukkitRunnable(){
			@Override
			public void run(){
				try{
					final LowMemoryEvent e = new LowMemoryEvent(LagMeter.this.getMemory(), LagMeter.this.getTPS());
					for(final MemoryListener m : LagMeter.this.getSyncMemoryListeners()){
						if(m != null){
							m.onLowMemoryEvent(e);
						}
					}
				}catch(NoAvailableTPSException e){
					//do nothing, shouldn't be checking anyways
				}
			}
		}.runTask(this);
	}

	public void turnLagMapOff(Player sender) throws NoActiveLagMapException{
		if(!this.maps.containsKey(sender.getName()))
			throw new NoActiveLagMapException("You don't have an active LagMap to turn off!");
		MapView map = this.maps.get(sender.getName());
		map.getRenderers().clear();
		for(MapRenderer r : this.oldRenderers.get(sender.getName()))
			map.addRenderer(r);
		this.oldRenderers.remove(sender.getName());
		this.maps.remove(sender.getName());
	}

	public void turnLagMapOn(Player sender) throws NoMapHeldException{
		boolean hasMap = false;
		MapView map = null;

		if(sender.getInventory().getItemInMainHand().getType() == Material.MAP) {
			map = Bukkit.getMap(sender.getInventory().getItemInMainHand().getDurability());
		}else if(sender.getInventory().getItemInOffHand().getType() == Material.MAP){
			map = Bukkit.getMap(sender.getInventory().getItemInOffHand().getDurability());
		}

		if(hasMap){
			this.oldRenderers.put(sender.getName(), map.getRenderers());
			this.maps.put(sender.getName(), map);
			for(MapRenderer r : map.getRenderers())
				map.removeRenderer(r);
			map.addRenderer(this.renderer);
		}else{
			throw new NoMapHeldException("You don't have a map in your hand to turn into a LagMap!");
		}
	}

	public void returnAllOldMapRenderers(){
		for(String player : this.maps.keySet()){
			try{
				this.turnLagMapOff(Bukkit.getPlayer(player));
			}catch(NoActiveLagMapException e){
				//hopefully shouldn't execute, but in case
				if(this.oldRenderers.containsKey(player))
					this.oldRenderers.remove(player);
				this.maps.remove(player);
			}
		}
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String commandLabel, final String[] args){
		if(!this.isEnabled())
			return false;
		if(this.permit(sender, "lagmeter.command." + command.getName().toLowerCase()) || !(sender instanceof Player)){
			if(command.getName().equalsIgnoreCase("lag")){
				this.sendLagMeter(sender);
			}else if(command.getName().equalsIgnoreCase("lagmap")){
				if(!this.lagmapsEnabled){
					this.sendMessage(sender, Severity.WARNING, "LagMaps are not enabled in the LagMeter configuration, and thereby cannot be used!");
					return true;
				}
				if(sender instanceof Player){
					if(this.maps.containsKey(sender.getName())){
						try{
							this.turnLagMapOff((Player)sender);
							this.sendMessage(sender, Severity.INFO, "You've turned off LagMap and returned the old map view you had.");
						}catch(final NoActiveLagMapException e){
							this.sendMessage(sender, Severity.WARNING, e.getMessage());
						}
					}else{
						try{
							this.turnLagMapOn((Player)sender);
							this.sendMessage(sender, Severity.INFO, "You've turned on LagMap, replacing your current map's view. Toggle this off by using " + ChatColor.GRAY + "/lagmap" + ChatColor.GREEN + " again.");
						}catch(final NoMapHeldException e){
							this.sendMessage(sender, Severity.WARNING, e.getMessage());
						}
					}
				}else{
					this.sendMessage(sender, Severity.WARNING, "You must be a player to use a LagMap.");
				}
			}else if(command.getName().equalsIgnoreCase("mem")){
				this.sendMemMeter(sender);
			}else if(command.getName().equalsIgnoreCase("lagmem")){
				this.sendLagMeter(sender);
				this.sendMemMeter(sender);
			}else if(command.getName().equalsIgnoreCase("uptime")){
				final int[] i = this.getCurrentServerUptime();
				this.sendMessage(sender, Severity.INFO, "Current server uptime: " + i[3] + " day(s), " + i[2] + " hour(s), " + i[1] + " minute(s), and " + i[0] + " second(s)");
			}else if(command.getName().equalsIgnoreCase("LagMeter") || command.getName().equalsIgnoreCase("lm")){
				if(args.length == 0){
					this.sendLagMeter(sender);
					this.sendMemMeter(sender);
				}else{
					this.handleBaseCommand(sender, args);
				}
			}else if(command.getName().equalsIgnoreCase("lmp")){
				this.sendLagMeter(sender);
				this.sendMemMeter(sender);
				this.sendMessage(sender, Severity.INFO, "Players online: " + ChatColor.GOLD + Bukkit.getServer().getOnlinePlayers().size());
			}else if(command.getName().equalsIgnoreCase("lchunks")){
				this.sendChunks(sender);
			}else if(command.getName().equalsIgnoreCase("lentities") || command.getName().equalsIgnoreCase("lmobs")){
				this.sendEntities(sender);
//            }else if(command.getName().equalsIgnoreCase("ping") || command.getName().equalsIgnoreCase("lping")){
//                this.ping(sender, args);
			}else if(command.getName().equalsIgnoreCase("lms")) {
				try {
					this.sendMessage(sender, Severity.INFO, String.format("%.2f", this.getTPS()));
				} catch (NoAvailableTPSException e) {
					this.sendMessage(sender, Severity.INFO, "TPS not yet available");
				}

				double[] mem = this.getMemory();
				this.sendMessage(sender, Severity.INFO, String.format("%,.2f MB/%,.2f MB used (%.2f%% free)", mem[0], mem[1], mem[3]));

				this.sendMessage(sender, Severity.INFO, String.format("%,d players online", Bukkit.getServer().getOnlinePlayers().size()));

				if (this.displayChunks) {
					this.sendMessage(sender, Severity.INFO, String.format("%,d", this.getChunksLoaded()));
				}

				if (this.displayEntities) {
					this.sendMessage(sender, Severity.INFO, String.format("%,d", this.getEntitiesAlive()));
				}
			}else
				return false;
		}else{
			this.sendMessage(sender, Severity.WARNING, "Sorry, permission lagmeter.command." + command.getName().toLowerCase() + " was denied.");
		}
		return true;
	}

	@Override
	public void onDisable(){
		this.returnAllOldMapRenderers();
		this.memWatcher.stop();
		this.lagWatcher.stop();
		this.cancelAllLagListeners();
		this.cancelAllMemoryListeners();
		if(this.logger.isEnabled()){
			try{
				this.logger.disable();
			}catch(final FileNotFoundException e){
				e.printStackTrace();
			}catch(final IOException e){
				e.printStackTrace();
			}catch(final Exception e){
				e.printStackTrace();
			}
		}
		Bukkit.getServer().getScheduler().cancelTasks(this);
		this.sendConsoleMessage(Severity.INFO, "Disabled!");
		super.onDisable();
	}

	@Override
	public void onEnable(){
		this.uptime = System.currentTimeMillis();
		final File logsFolder = new File("plugins" + File.separator + "LagMeter" + File.separator + "logs");
		LagMeter.p = this;
		this.logger = new LagMeterLogger(this);
		this.poller = new LagMeterPoller(this);
		this.history = new LagMeterStack();
		this.asyncLagListeners = new ArrayList<LagListener>();
		this.syncLagListeners = new ArrayList<LagListener>();
		this.syncMemListeners = new ArrayList<MemoryListener>();
		this.asyncMemListeners = new ArrayList<MemoryListener>();
		this.updateConfiguration();
		if(!logsFolder.exists() && this.useLogsFolder && this.enableLogging){
			this.sendConsoleMessage(Severity.INFO, "Logs folder not found. Attempting to create one for you.");
			if(!logsFolder.mkdir()){
				this.sendConsoleMessage(Severity.SEVERE, "Error! Couldn't create the folder!");
			}else{
				this.sendConsoleMessage(Severity.INFO, "Logs folder created.");
			}
		}
		if(this.enableLogging){
			this.poller.setLogInterval(this.logInterval);
			if(!this.logger.enable()){
				this.sendConsoleMessage(Severity.SEVERE, "Logging is disabled due to an error while attempting to enable it: " + this.logger.getError());
			}
		}
		this.history.setMaxSize(this.averageLength);
		this.sendConsoleMessage(Severity.INFO, "Enabled! Polling every " + this.interval + " server ticks." + (this.isLoggingEnabled() ? (" Logging to " + this.logger.getFilename() + ".") : ""));
		this.registerTasks();
		if(this.displayChunksOnLoad){
			this.sendConsoleMessage(Severity.INFO, "Chunks loaded:");
			int total = 0;
			for(final World world : Bukkit.getServer().getWorlds()){
				final int chunks = world.getLoadedChunks().length;
				this.sendConsoleMessage(Severity.INFO, "World \"" + world.getName() + "\": " + chunks + ".");
				total += chunks;
			}
			this.sendConsoleMessage(Severity.INFO, "Total chunks loaded: " + total);
		}
		if(this.displayEntitiesOnLoad){
			this.sendConsoleMessage(Severity.INFO, "Entities:");
			int total = 0;
			for(final World world : Bukkit.getServer().getWorlds()){
				final int entities = world.getEntities().size();
				this.sendConsoleMessage(Severity.INFO, "World \"" + world.getName() + "\": " + entities + ".");
				total += entities;
			}
			this.sendConsoleMessage(Severity.INFO, "Total entities: " + total);
		}
		this.pingDomains = new HashMap<String, String>();
		Bukkit.getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
		Bukkit.getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
		this.maps = new HashMap<String, MapView>();
		this.oldRenderers = new HashMap<String, List<MapRenderer>>();
		this.renderer = new LagMapRenderer(this.mapRenderInterval);

        PingCommand pingExecutor = new PingCommand(this, this.isPingEnabled);
        getServer().getPluginCommand("ping").setExecutor(pingExecutor);
        if(this.isPingEnabled) {
            getServer().getPluginCommand("lping").setExecutor(pingExecutor);
        }else{
            getServer().getPluginCommand("lping").setExecutor(new PingCommand(this, true));
        }

		super.onEnable();
	}

	/**
	 * @deprecated Use {@link com.moxvallix.util.TimeUtils#parseTime(String)} instead.
	 * Parses a string to get the amount of ticks equal to what the string
	 * passed represents.
	 *
	 * @param timeString The "human-readable" representation of time, where:
	 *                   <ul>
	 *                   <b>s</b> is seconds;<br>
	 *                   <b>m</b> is minutes;<br>
	 *                   <b>h</b> is hours;<br>
	 *                   <b>d</b> is days; and finally,<br>
	 *                   <b>w</b> is weeks.
	 *                   </ul>
	 *
	 * @return Amount of ticks which corresponds to this string of time.
	 *
	 * @throws com.moxvallix.exceptions.InvalidTimeFormatException If the time format given is invalid (contains time delimiters other than s, m, h, d or w).
	 * @see com.moxvallix.LagMeter#parseTimeMS(String)
	 */
	@Deprecated
	public long parseTime(String timeString) throws InvalidTimeFormatException{
		long time = 0L;
		if(timeString.split("<>").length == 2){
			timeString = timeString.split("<>")[1].toLowerCase();
			StringBuilder z = new StringBuilder();
			for(int i = 0; i < timeString.length(); i++){
				final String c = timeString.substring(i, i + 1);
				if(c.matches("[^wdhms]")){
					z.append(c);
				}else{
					try{
						if(c.equalsIgnoreCase("w")){
							time += 12096000L * Long.parseLong(z.toString());
						}else if(c.equalsIgnoreCase("d")){
							time += 1728000L * Long.parseLong(z.toString());
						}else if(c.equalsIgnoreCase("h")){
							time += 72000L * Long.parseLong(z.toString());
						}else if(c.equalsIgnoreCase("m")){
							time += 1200L * Long.parseLong(z.toString());
						}else if(c.equalsIgnoreCase("s")){
							time += 20L * Long.parseLong(z.toString());
						}
						z = new StringBuilder();
					}catch(final NumberFormatException e){
						throw new InvalidTimeFormatException("The time for the uptime command " + timeString.split("<>")[0] + " is invalid: the time string contains characters other than 0-9, w/d/h/m/s. Top-level exception: "+e.getMessage());
					}
				}
			}
		}else{
			return 0L;
		}
		return time;
	}

	/**
	 * @deprecated Use {@link com.moxvallix.util.TimeUtils#parseTimeMS(String)} instead.
	 * Parses the timeString given and returns milliseconds instead of ticks.
	 * Works in the same fashion as parseTime(String).
	 *
	 * @param timeString The string of time, where:
	 *                   <ul>
	 *                   <b>s</b> is seconds;<br>
	 *                   <b>m</b> is minutes;<br>
	 *                   <b>h</b> is hours;<br>
	 *                   <b>d</b> is days; and finally,<br>
	 *                   <b>w</b> is weeks.
	 *                   </ul>
	 *
	 * @return The amount of milliseconds that would equate to the time string
	 * given.
	 *
	 * @throws com.moxvallix.exceptions.InvalidTimeFormatException If the timeString is in an invalid format (i.e. invalid
	 *                                                                                 characters) or the result is less than 1.
	 * @see com.moxvallix.LagMeter#parseTime(String)
	 */
	@Deprecated
	public long parseTimeMS(String timeString) throws InvalidTimeFormatException{
		return (this.parseTime(timeString) * 50L);
	}

	public boolean permit(final CommandSender sender, final String perm){
		return sender.hasPermission(perm);
	}

	public boolean permit(final Player player, final String perm){
		return this.permit((CommandSender)player, perm);
	}


	/**
	 * Registers a listener for when LagMeter finds that the server's TPS has
	 * dropped below the user's specified threshold for the event to be fired.
	 * When this happens, the event method in the class which implements
	 * LagListener will be run. Code will be executed asynchronously in a new
	 * thread; therefore, only <b>thread-safe</b> code should be used.
	 *
	 * @param listener The listener which implements LagListener which should be
	 *                 notified of the event when (if) it happens.
	 *
	 * @return The ID of the listener in LagMeter's allocated memory. This is
	 * used to cancel the registration of the listener, etc.
	 */
	public int registerAsyncLagListener(final LagListener listener){
		if(!this.asyncLagListeners.contains(listener)){
			this.asyncLagListeners.add(listener);
			return this.asyncLagListeners.indexOf(listener);
		}else
			return -1;
	}

	/**
	 * Registers a listener for when LagMeter finds that the free memory has
	 * dropped below the user's specified threshold for the evnt to be fired.
	 * When this happens, the event method in the class which implements
	 * MemoryListener will be run. Code will be executed asynchronously;
	 * therefore, <b>only thread-safe</b> code should be produced.
	 *
	 * @param listener The listener which implements MemoryListener which should be
	 *                 notified of the event when (if) it happens.
	 *
	 * @return The ID of the listener in LagMeter's allocated memory. This is
	 * used to cancel the registration of the listener, etc.
	 */
	public int registerAsyncMemoryListener(final MemoryListener listener){
		if(!this.asyncMemListeners.contains(listener)){
			this.asyncMemListeners.add(listener);
			return this.asyncMemListeners.indexOf(listener);
		}else
			return -1;
	}

	/**
	 * Registers a listener for when LagMeter finds that the server's TPS has
	 * dropped below the user's specified threshold for the event to be fired.
	 * When this happens, the event method in the class which implements
	 * LagListener will be run. Code will be executed synchronously, with the
	 * main server thread; therefore, thread-unsafe code may be used.
	 *
	 * @param listener The listener which implements LagListener which should be
	 *                 notified of the event when (if) it happens.
	 *
	 * @return The ID of the listener in LagMeter's allocated memory. This is
	 * used to cancel the registration of the listener, etc.
	 */
	public int registerSyncLagListener(final LagListener listener){
		if(!this.syncLagListeners.contains(listener)){
			this.syncLagListeners.add(listener);
			return this.syncLagListeners.indexOf(listener);
		}else
			return -1;
	}

	/**
	 * Registers a listener for when LagMeter finds that the free memory has
	 * dropped below the user's specified threshold for the evnt to be fired.
	 * When this happens, the event method in the class which implements
	 * MemoryListener will be run. Code will be executed synchronously, with the
	 * main server thread; therefore, thread-unsafe methods and code may be
	 * used.
	 *
	 * @param listener The listener which implements MemoryListener which should be
	 *                 notified of the event when (if) it happens.
	 *
	 * @return The ID of the listener in LagMeter's allocated memory. This is
	 * used to cancel the registration of the listener, etc.
	 */
	public int registerSyncMemoryListener(final MemoryListener listener){
		if(!this.syncMemListeners.contains(listener)){
			this.syncMemListeners.add(listener);
			return this.syncMemListeners.indexOf(listener);
		}else
			return -1;
	}

	private void registerTasks(){
		Bukkit.getServer().getScheduler().cancelTasks(this);
		if(this.memWatcher != null){
			this.memWatcher.stop();
		}
		if(this.lagWatcher != null){
			this.lagWatcher.stop();
		}
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, this.poller, (this.pollingDelay * 20L), this.interval);
		this.lagNotifyInterval *= 60000;
		this.memNotifyInterval *= 60000;
		new Thread(this.lagWatcher = new LagWatcher(this)).start();
		new Thread(this.memWatcher = new MemoryWatcher(this)).start();
		if(this.AutomaticLagNotificationsEnabled){
			this.registerSyncLagListener(new DefaultHighLag(this));
		}
		if(this.AutomaticMemoryNotificationsEnabled){
			this.registerSyncMemoryListener(new DefaultLowMemory(this));
		}
		if(this.uptimeCommands != null){
			for(final String s : this.uptimeCommands){
				try{
					long time;
					time = TimeUtils.parseTime(s.split("<>")[1]);
					if(this.repeatingUptimeCommands){
						Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new UptimeCommand(s.split("<>")[0]), time, time);
					}else{
						Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new UptimeCommand(s.split("<>>")[0]), time);
					}
				}catch(final InvalidTimeFormatException e){
					this.sendMessage(Bukkit.getConsoleSender(), Severity.SEVERE, e.getMessage());
				}
			}
		}
	}

	/**
	 * Not intended for use by other plugins. <br />
	 * <br />
	 * This method removes the specified player from the HashMap of players and
	 * their IPs. This only exists for the /ping command, where getting a
	 * player's IP other than on login requires the use of an NMS class,
	 * {@code CraftPlayer}.
	 *
	 * @param name The player to remove from the HashMap.
	 */
	public void removePlayerIP(String name){
		this.pingDomains.remove(name);
	}

	/**
	 * This method sends the specified CommandSender a message, with the amount
	 * of chunks currently loaded on the server, for each world, followed by a
	 * grand total.
	 *
	 * @param sender The CommandSender to send output to.
	 */
	public void sendChunks(final CommandSender sender){
		int totalChunks = 0;
		final List<World> worlds = Bukkit.getServer().getWorlds();
		for(final World world : worlds){
			final String s = world.getName();
			final int i = Bukkit.getServer().getWorld(s).getLoadedChunks().length;
			totalChunks += i;
			if(i != 0){
				this.sendMessage(sender, Severity.INFO, ChatColor.GOLD + "Chunks in world \"" + s + "\": " + i);
			}
		}
		this.sendMessage(sender, Severity.INFO, ChatColor.GOLD + "Total chunks loaded on the server: " + totalChunks);
	}

	public int getChunksLoadedInWorld(String world){
		return Bukkit.getServer().getWorld(world).getLoadedChunks().length;
	}

	public int getChunksLoaded(){
		int totalChunks = 0;
		final List<World> worlds = Bukkit.getServer().getWorlds();
		for(final World world : worlds){
			final String s = world.getName();
			final int i = Bukkit.getServer().getWorld(s).getLoadedChunks().length;
			totalChunks += i;
		}
		return totalChunks;
	}

	public void sendConsoleMessage(Severity severity, String message){
		this.sendMessage(Bukkit.getServer().getConsoleSender(), severity, message);
	}

	/**
	 * This method sends the specified CommandSender a message, with the amount
	 * of entities currently living on the server, for each world, followed by a
	 * grand total.
	 *
	 * @param sender The CommandSender to send output to.
	 */
	public void sendEntities(final CommandSender sender){
		int totalEntities = 0;
		final List<World> worlds = Bukkit.getServer().getWorlds();
		for(final World world : worlds){
			final String worldName = world.getName();
			final int i = Bukkit.getServer().getWorld(worldName).getEntities().size();
			totalEntities += i;
			if(i != 0){
				this.sendMessage(sender, Severity.INFO, ChatColor.GOLD + "Entities in world \"" + worldName + "\": " + i);
			}
		}
		this.sendMessage(sender, Severity.INFO, ChatColor.GOLD + "Total entities: " + totalEntities);
	}

	public int getEntitiesAlive(){
		int totalEntities = 0;
		final List<World> worlds = Bukkit.getServer().getWorlds();
		for(final World world : worlds){
			final String worldName = world.getName();
			final int i = Bukkit.getServer().getWorld(worldName).getEntities().size();
			totalEntities += i;
		}
		return totalEntities;
	}

	public int getEntitiesInWorld(String world){
		return Bukkit.getServer().getWorld(world).getEntities().size();
	}

	/**
	 * Sends the original purpose of this plugin, the Lag Meter, to the
	 * specified CommandSender.
	 *
	 * @param sender The CommandSender to send the LagMeter to.
	 */
	public void sendLagMeter(final CommandSender sender){
		try{
			final StringBuilder lagMeter = new StringBuilder();
			final double tps = this.getTPS();
			if(this.displayEntities){
				this.sendEntities(sender);
			}
			if(this.displayChunks){
				this.sendChunks(sender);
			}
			if(tps < 21){
				int looped = 0;
				while(looped++ < tps){
					lagMeter.append("#");
				}
				while(looped++ <= 20){
					lagMeter.append("_");
				}
				this.sendMessage(sender, Severity.INFO, ChatColor.GOLD + "[" + (tps >= 18 ? ChatColor.GREEN : tps >= 15 ? ChatColor.YELLOW : ChatColor.RED) + lagMeter.toString() + ChatColor.GOLD + "] " + String.format("%3.2f", tps) + " TPS");
			}
		}catch(NoAvailableTPSException e){
			this.sendMessage(sender, Severity.WARNING, e.getMessage());
		}
	}

	/**
	 * This sends the other original function of this plugin, the memory meter,
	 * to the specified ConsoleSender.
	 *
	 * @param sender The ConsoleSender object to send the memory meter to.
	 */
	public synchronized void sendMemMeter(final CommandSender sender){
		final StringBuilder bar = new StringBuilder();
		int looped = 0;
		this.updateMemoryStats();
		while(looped++ < (this.percentageFree / 5)){
			bar.append('#');
		}
		bar.append(ChatColor.WHITE);
		while(looped++ <= 20){
			bar.append('_');
		}
		this.sendMessage(sender, Severity.INFO, ChatColor.GOLD + "[" + (this.percentageFree >= 60 ? ChatColor.GREEN : this.percentageFree >= 35 ? ChatColor.YELLOW : ChatColor.RED) + bar.toString() + ChatColor.GOLD + "] " + String.format("%,.2f", this.memFree) + "MB/" + String.format("%,.2f", this.memMax) + "MB (" + String.format("%,.2f", this.percentageFree) + "%) free");
	}

	/**
	 * Sends a message to a CommandSender, using LagMeter's plugin tag/colour
	 * and severity colours. <br />
	 * <br />
	 * The severity identifiers are:
	 * <ul>
	 * <li>{@link LagMeter.Severity#INFO} is information;</li>
	 * <li>{@link LagMeter.Severity#WARNING} is a warning; and finally,</li>
	 * <li>{@link LagMeter.Severity#SEVERE} is an error.</li>
	 * </ul>
	 *
	 * @param sender   The CommandSender to send the message to.
	 * @param severity The Severity of the message.
	 * @param message  The message itself.
	 */
	public void sendMessage(final CommandSender sender, final Severity severity, String message){
		switch(severity) {
			case WARNING:
				message = ChatColor.GOLD + "[LagMeter] " + ChatColor.RED + message;
				break;
			case SEVERE:
				message = ChatColor.GOLD + "[LagMeter] " + ChatColor.DARK_RED + message;
				break;
			case INFO:
			default:
				message = ChatColor.GOLD + "[LagMeter] " + ChatColor.GREEN + message;
				break;
		}

		if(this.stripConsoleColors){
			if(!(sender instanceof Player)){
				message = ChatColor.stripColor(message);
			}
		}

		sender.sendMessage(message);
	}

	public void sendMessage(final Player player, final Severity severity, final String message){
		this.sendMessage((CommandSender)player, severity, message);
	}

	void setTicksPerSecond(final float f){
		this.ticksPerSecond = f;
	}

	private void updateConfiguration(){
		final YamlConfiguration yml = this.getConfig();
		this.useAverage = yml.getBoolean("useAverage", true);
		this.averageLength = yml.getInt("averageLength", 10);
		this.interval = yml.getInt("interval", 40);
		this.displayChunksOnLoad = yml.getBoolean("LoadedChunksOnLoad", true);
		this.displayEntitiesOnLoad = yml.getBoolean("displayEntitiesOnLoad", true);
		this.displayEntities = yml.getBoolean("Commands.Lag.displayEntities", true);
		this.displayChunks = yml.getBoolean("Commands.Lag.displayChunks", true);
		this.uptimeCommands = yml.getStringList("UptimeCommands.commandList");
		this.repeatingUptimeCommands = yml.getBoolean("UptimeCommands.repeatCommands", true);
		this.logInterval = yml.getInt("log.interval", 150);
		this.enableLogging = yml.getBoolean("log.enable", true);
		this.useLogsFolder = yml.getBoolean("log.useLogsFolder", false);
		this.playerLoggingEnabled = yml.getBoolean("log.logPlayersOnline", true);
		this.logChunks = yml.getBoolean("log.logChunks", false);
		this.logTotalChunksOnly = yml.getBoolean("log.logTotalChunksOnly", true);
		this.logEntities = yml.getBoolean("log.logEntities", false);
		this.logTotalEntitiesOnly = yml.getBoolean("log.logTotalEntitiesOnly", true);
		this.newBlockPerLog = yml.getBoolean("log.newBlockPerLog", true);
		this.newLineForLogStats = yml.getBoolean("log.newLinePerStatistic", true);
		this.AutomaticLagNotificationsEnabled = yml.getBoolean("Notifications.Lag.Enabled", true);
		this.tpsNotificationThreshold = yml.getInt("Notifications.Lag.Threshold", 15);
		this.lagNotifyInterval = yml.getInt("Notifications.Lag.CheckInterval", 5);
		this.highLagCommand = yml.getString("Notifications.Lag.ConsoleCommand", "/lag");
		this.AutomaticMemoryNotificationsEnabled = yml.getBoolean("Notifications.Memory.Enabled", true);
		this.memoryNotificationThreshold = yml.getInt("Notifications.Memory.Threshold", 25);
		this.memNotifyInterval = yml.getInt("Notifications.Memory.CheckInterval", 10);
		this.lowMemCommand = yml.getString("Notifications.Memory.ConsoleCommand", "/mem");
		this.lagmapsEnabled = yml.getBoolean("LagMaps.Enabled", true);
		this.mapRenderInterval = yml.getInt("LagMaps.Interval", 5);
		this.pollingDelay = yml.getInt("Commands.Lag.PollingDelay", 35);
		this.stripConsoleColors = yml.getBoolean("stripConsoleColors", true);
		this.isPingEnabled = yml.getBoolean("Commands.Ping.enabled", true);
	}

	/**
	 * This method updates the plugin's stored statistics for memory.
	 */
	public synchronized void updateMemoryStats(){
		this.memUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576D;
		this.memMax = Runtime.getRuntime().maxMemory() / 1048576D;
		this.memFree = this.memMax - this.memUsed;
		this.percentageFree = (100D / this.memMax) * this.memFree;
	}

	/**
	 * Obviously, as this is a bukkit plugin, the constructor is not intended to
	 * be used by anything except for Bukkit.
	 * <p/>
	 * Constructing a new LagMeter object will likely break a <i>lot</i> of
	 * stuff.
	 */
	public LagMeter(){
		super();
		this.logger = null;
		this.poller = null;
		this.history = null;
		this.uptime = Integer.MAX_VALUE;
		this.averageLength = 10;
		this.ticksPerSecond = 20;
		this.memUsed = this.memMax = this.memFree = this.percentageFree = 0D;
		this.lagWatcher = null;
		this.memWatcher = null;
		this.syncLagListeners = this.asyncLagListeners = null;
		this.syncMemListeners = this.asyncMemListeners = null;
		this.lagNotifyInterval = Integer.MAX_VALUE;
		this.memNotifyInterval = Integer.MAX_VALUE;
		this.pollingDelay = 300;
		this.interval = 40;
		this.logInterval = 150;
		this.tpsNotificationThreshold = this.memoryNotificationThreshold = 0F;
		this.useAverage = this.enableLogging = this.useLogsFolder = true;
		this.AutomaticLagNotificationsEnabled =
				this.AutomaticMemoryNotificationsEnabled =
				this.displayEntities = this.playerLoggingEnabled =
				this.displayChunksOnLoad = this.displayChunks = this.logChunks =
				this.logTotalChunksOnly = this.logEntities = this.logTotalEntitiesOnly =
				this.newBlockPerLog = this.displayEntitiesOnLoad =
				this.newLineForLogStats = this.repeatingUptimeCommands =
				this.lagmapsEnabled = false;
		this.uptimeCommands = null;
		this.highLagCommand = null;
		this.lowMemCommand = null;
		this.pingDomains = null;
		this.maps = null;
		this.oldRenderers = null;
		this.renderer = null;
		this.mapRenderInterval = null;
	}

	static{
		p = null;
	}

	public String toString(){
		return super.toString();
	}

	/**
	 * Represents severity of messages sent by LagMeter.
	 */
	public enum Severity{
		/**
		 * Represents an information message.
		 */
		INFO,
		/**
		 * Represents a warning message, where something went wrong (perhaps,
		 * the recipient typed something wrong, or the configuration is using
		 * default values).
		 */
		WARNING,
		/**
		 * Represents an error message, where something failed catastrophically.
		 * Should be used sparingly, i.e. in cases of complete and total
		 * inability to do something, such as create default configuration.
		 */
		SEVERE
	}
}
