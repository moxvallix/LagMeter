package main.java.com.webkonsept.minecraft.lagmeter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import main.java.com.webkonsept.minecraft.lagmeter.eventhandlers.DefaultHighLag;
import main.java.com.webkonsept.minecraft.lagmeter.eventhandlers.DefaultLowMemory;
import main.java.com.webkonsept.minecraft.lagmeter.eventhandlers.PlayerJoinListener;
import main.java.com.webkonsept.minecraft.lagmeter.eventhandlers.PlayerQuitListener;
import main.java.com.webkonsept.minecraft.lagmeter.events.HighLagEvent;
import main.java.com.webkonsept.minecraft.lagmeter.events.LowMemoryEvent;
import main.java.com.webkonsept.minecraft.lagmeter.exceptions.InvalidTimeFormatException;
import main.java.com.webkonsept.minecraft.lagmeter.listeners.LagListener;
import main.java.com.webkonsept.minecraft.lagmeter.listeners.MemoryListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class LagMeter extends JavaPlugin {
	private LagMeterLogger			logger;
	private LagMeterPoller			poller;
	private LagMeterStack			history;
	private float					ticksPerSecond	= 20;
	private long					uptime;
	private int						averageLength	= 10;
	private double					memUsed, memMax, memFree, percentageFree;
	private LagWatcher				lagWatcher;
	private MemoryWatcher			memWatcher;
	private List<LagListener>		syncLagListeners, asyncLagListeners;
	private List<MemoryListener>	syncMemListeners, asyncMemListeners;
	// Configurable Values - mostly booleans
	private int						interval		= 40;
	private int						logInterval		= 150;
	private int						lagNotifyInterval;
	private int						memNotifyInterval;
	private float					tpsNotificationThreshold, memoryNotificationThreshold;
	private boolean					useAverage		= true;
	private boolean					enableLogging	= true;
	private boolean					useLogsFolder	= true;
	private boolean					AutomaticLagNotificationsEnabled;
	private boolean					AutomaticMemoryNotificationsEnabled;
	private boolean					displayEntities;
	private boolean					playerLoggingEnabled;
	private boolean					displayChunksOnLoad;
	private boolean					displayChunks;
	private boolean					logChunks;
	private boolean					logTotalChunksOnly;
	private boolean					logEntities;
	private boolean					logTotalEntitiesOnly;
	private boolean					newBlockPerLog;
	private boolean					displayEntitiesOnLoad;
	private boolean					newLineForLogStats;
	private boolean					repeatingUptimeCommands;
	private List<String>			uptimeCommands;
	private String					highLagCommand, lowMemCommand;
	private static LagMeter			p;
	private HashMap<String, String>	pingDomains;

	/**
	 * This method gets the current instance of LagMeter.
	 * 
	 * @return The current instance of the plugin's main class.
	 */
	public static LagMeter getInstance() {
		return LagMeter.p;
	}

	/**
	 * This method is the main method for running this plugin as a java
	 * application. As Bukkit plugins are not intended to be run directly, all
	 * this does is give the user a long error message in a message box and then
	 * exits.
	 * 
	 * @param args
	 *            Command-line arguments, with which nothing is done.
	 */
	public static void main(final String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
		} catch (final InstantiationException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		} catch (final UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		} finally {
			JOptionPane.showMessageDialog(null, "Sorry, but LagMeter is a Bukkit plugin, and cannot be run directly like you've attempted.\nTo use the plugin, download and set up a Bukkit Minecraft server, and in the root directory, create a folder called\n\"plugins\" (no quotes, and assuming it hasn't already been created for you), and put this JAR file (LagMeter.jar) there.\nWhen you've done that, start the Bukkit server using the command line java -jar \"path to Bukkit.jar\",\nor if it's already running, type \"reload\" (no quotes) into the command-line.", "LagMeter", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}

	/**
	 * Add a TPS value to the plugin's history.
	 * 
	 * @param tps
	 *            The TPS value to add.
	 */
	public void addHistory(final float tps) {
		this.history.add(tps);
	}

	/**
	 * Not intended to be used by other plugins. <br />
	 * <br />
	 * Adds a specified player to a HashMap so that they can be pinged later
	 * without depending on {@code CraftPlayer}.
	 * 
	 * @param name
	 *            - The name of the player to add
	 * @param hostAddress
	 *            - The player's IP
	 */
	public void addPlayerIP(String name, String hostAddress) {
		this.pingDomains.put(name, hostAddress);
	}

	private void cancelAllLagListeners() {
		this.asyncLagListeners.clear();
		this.syncLagListeners.clear();
	}

	private void cancelAllMemoryListeners() {
		this.asyncMemListeners.clear();
		this.syncMemListeners.clear();
	}

	/**
	 * Uses an ID obtained from the registerLagListener(LagListener) method to
	 * cancel recurring notification of your observer object.
	 * 
	 * @param id
	 *            The id of the LagListener to stop notifying.
	 */
	public void cancelAsyncLagListener(final int id) {
		this.asyncLagListeners.set(id, null);
	}

	/**
	 * Uses an ID obtained from the registerMemoryListener(MemoryListener)
	 * method to cancel recurring notification of your observer object.
	 * 
	 * @param id
	 *            The id of the MemoryListener to stop notifying.
	 */
	public void cancelAsyncMemoryListener(final int id) {
		this.asyncMemListeners.set(id, null);
	}

	/**
	 * Uses an ID obtained from the registerLagListener(LagListener) method to
	 * cancel recurring notification of your observer object.
	 * 
	 * @param id
	 *            The id of the LagListener to stop notifying.
	 */
	public void cancelSyncLagListener(final int id) {
		this.syncLagListeners.set(id, null);
	}

	/**
	 * Uses an ID obtained from the registerMemoryListener(MemoryListener)
	 * method to cancel recurring notification of your observer object.
	 * 
	 * @param id
	 *            The id of the MemoryListener to stop notifying.
	 */
	public void cancelSyncMemoryListener(final int id) {
		this.syncMemListeners.set(id, null);
	}

	protected List<LagListener> getAsyncLagListeners() {
		return this.asyncLagListeners;
	}

	protected List<MemoryListener> getAsyncMemoryListeners() {
		return this.asyncMemListeners;
	}

	/**
	 * This is the getter for the upper limit of length of the history of the
	 * plugin's TPS averaging.
	 * 
	 * @return The max bound for the history.
	 */
	public int getAverageLength() {
		return this.averageLength;
	}

	/**
	 * This is the getter for the sleep interval of the TPS watcher's thread.
	 * 
	 * @return How often the plugin checks the server's TPS to notify its
	 *         observers.
	 */
	public long getCheckLagInterval() {
		return this.lagNotifyInterval;
	}

	/**
	 * This is the getter for the sleep interval of the memory watcher's thread.
	 * 
	 * @return How often the plugin checks the server's memory to notify its
	 *         observers.
	 */
	public long getCheckMemoryInterval() {
		return this.memNotifyInterval;
	}

	/**
	 * This method will return the time in days, hours, minutes, and seconds,
	 * since the server was <b>started</b> <i>OR</i> <b>since it was last
	 * reloaded</b>.
	 * 
	 * @return An array of <b>int</b>s, i, where:<br />
	 *         <ul>
	 *         <b>i[0]</b> is the seconds,<br />
	 *         <b>i[1]</b> is the minutes,<br />
	 *         <b>i[2]</b> is the hours,<br />
	 *         <b>i[3]</b> is the days,
	 *         </ul>
	 *         that the server has been online without reloading.
	 */
	public int[] getCurrentServerUptime() {
		final int[] i = new int[4];
		long l = System.currentTimeMillis() - this.uptime;
		i[3] = (int) (l / 86400000L);
		l -= i[3] * 86400000L;
		i[2] = (int) (l / 3600000L);
		l -= i[2] * 3600000;
		i[1] = (int) (l / 60000L);
		l -= i[1] * 60000L;
		i[0] = (int) (l / 1000L);
		return i;
	}

	/**
	 * Gets the LagMeterStack (the history) of the server TPS.
	 * 
	 * @return The LagMeterStack for however long the average upper bound
	 *         allows.
	 */
	public LagMeterStack getHistory() {
		return this.history;
	}

	private String getHops(final CommandSender sender, final String[] args) {
		if (args.length > 0) {
			if (this.permit(sender, "lagmeter.commands.ping.unlimited")) {
				try {
					if (Integer.parseInt(args[0]) > 10) {
						this.sendMessage(sender, 1, "This might take a while...");
					}
					return args[0];
				} catch (final NumberFormatException e) {
					this.sendMessage(sender, 1, "You entered an invalid amount of hops; therefore, 1 will be used instead.");
					return "1";
				}
			} else {
				this.sendMessage(sender, 1, "You don't have access to specifying ping hops!");
				return "1";
			}
		} else {
			return "1";
		}
	}

	/**
	 * This is a getter for how often the server is polled by LagMeter.
	 * 
	 * @return How often LagMeter polls the server's TPS.
	 */
	public int getInterval() {
		return this.interval;
	}

	/**
	 * This is the getter for the command the plugin runs when the default high
	 * lag listener is triggered.
	 * 
	 * @return The lag notification command.
	 */
	public String getLagCommand() {
		return this.highLagCommand;
	}

	/**
	 * Getter for the logger of tps and memory (and players if so configured).
	 * 
	 * @return The instance of LagMeter's logger for logging tps/memory/players.
	 */
	public LagMeterLogger getLMLogger() {
		return this.logger;
	}

	/**
	 * Gets the current memory used, maxmimum memory, memory free, and
	 * percentage of memory free. Returned in a single array of doubles.
	 * 
	 * @since 1.11.0-SNAPSHOT
	 * @return memory[], which is an array of doubles, containing four values,
	 *         where: <br />
	 *         <b><i>memory[0]</i></b> is the currently used memory;<br />
	 *         <b><i>memory[1]</i></b> is the current maximum memory;<br />
	 *         <b><i>memory[2]</i></b> is the current free memory;<br />
	 *         <b><i>memory[3]</i></b> is the percentage memory free (note this
	 *         may be an irrational number, so you might want to truncate it if
	 *         you use this).
	 */
	public synchronized double[] getMemory() {
		this.updateMemoryStats();
		return new double[] { this.memUsed, this.memMax, this.memFree, this.percentageFree };
	}

	/**
	 * This is the getter for the command the plugin runs when the default low
	 * memory listener is triggered.
	 * 
	 * @return The memory notification command.
	 */
	public String getMemoryCommand() {
		return this.lowMemCommand;
	}

	/**
	 * The memory notification theshold is the point at which LagMeter will
	 * notify its observers that the memory free has dropped below this amount.
	 * 
	 * @return The memory notification theshold.
	 */
	public float getMemoryNotificationThreshold() {
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
	 * @see #getPlayerIP(String)
	 * 
	 * @param p
	 *            - The player to get the IP of.
	 * @return The IP of the player specified, in String form.
	 */
	public String getPlayerIP(Player p) {
		return this.getPlayerIP(p.getName());
	}

	/**
	 * Although this is not really intended for use by other plugins, it may be
	 * useful to some. <br />
	 * <br />
	 * This method gets the IP of a player, which is stored in a HashMap when
	 * they log in, and removed when they log out. This is to avoid use of an
	 * NMS class, {@code CraftPlayer}. <br />
	 * <br />
	 * This method is invoked by {@link#getPlayerIP(Player)}.
	 * 
	 * @param player
	 *            - The player's name to get the IP of.
	 * @return The specified player's IP, in String form..
	 */
	public String getPlayerIP(String player) {
		return this.pingDomains.get(player);
	}

	protected List<LagListener> getSyncLagListeners() {
		return this.syncLagListeners;
	}

	protected List<MemoryListener> getSyncMemoryListeners() {
		return this.syncMemListeners;
	}

	/**
	 * Gets the ticks per second.
	 * 
	 * @since 1.8
	 * @return ticksPerSecond
	 */
	public float getTPS() {
		if (this.useAverage) {
			return this.history.getAverage();
		}
		return this.ticksPerSecond;
	}

	/**
	 * The TPS notification threshold is where the plugin will notify its
	 * observers when the TPS reaches or drops below this amount.
	 * 
	 * @return The tps notification theshold.
	 */
	public float getTpsNotificationThreshold() {
		return this.tpsNotificationThreshold;
	}

	protected void handleBaseCommand(final CommandSender sender, final String[] args) {
		if (args[0].equalsIgnoreCase("reload")) {
			if (this.permit(sender, "lagmeter.command.lagmeter.reload") || this.permit(sender, "lagmeter.reload")) {
				this.updateConfiguration();
				this.sendMessage(sender, 0, "Configuration reloaded!");
			}
		} else if (args[0].equalsIgnoreCase("help")) {
			if (this.permit(sender, "lagmeter.command.lagmeter.help") || this.permit(sender, "lagmeter.help")) {
				if ((args.length == 1) || args[1].trim().equals("0") || args[1].trim().equals("1")) {
					this.sendMessage(sender, 0, "*           *Help for LagMeter [1/2]*           *");
					if (this.permit(sender, "lagmeter.command.lag")) {
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/lag" + ChatColor.GOLD + " - Check the server's TPS. If configuChatColor.RED, may also display chunks loaded and/or entities alive.");
					}
					if (this.permit(sender, "lagmeter.command.mem")) {
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/mem" + ChatColor.GOLD + " - Displays how much memory the server currently has free.");
					}
					if (this.permit(sender, "lagmeter.command.lagmem") || this.permit(sender, "lagmeter.command.lm")) {
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/lagmem|/lm" + ChatColor.GOLD + " - A combination of both /lag and /mem.");
					}
					if (this.permit(sender, "lagmeter.command.lchunks")) {
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/lchunks" + ChatColor.GOLD + " - Shows how many chunks are currently loaded in each world, then with a total.");
					}
					if (this.permit(sender, "lagmeter.command.lmobs") || this.permit(sender, "lagmeter.command.lentities")) {
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/lmobs|/lentities" + ChatColor.GOLD + " - Shows how many entities are currently alive in each world, then with a total.");
					}
					if (this.permit(sender, "lagmeter.command.lmp")) {
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/lmp" + ChatColor.GOLD + " - Has the same function as /lagmem, but includes a player count.");
					}
					if (this.permit(sender, "lagmeter.command.lagmeter")) {
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/lagmeter|/lm" + ChatColor.GOLD + " - Shows the current version and gives sub-commands.");
					}
					if (this.permit(sender, "lagmeter.command.lagmeter.reload") || this.permit(sender, "lagmeter.reload")) {
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/lagmeter|/lm" + ChatColor.GREEN + " <reload|r> " + ChatColor.GOLD + " - Allows the player to reload the configuration.");
					}
				} else if ((args.length > 1) && args[1].trim().equals("2")) {
					this.sendMessage(sender, 0, "*           *Help for LagMeter [2/2]*           *");
					this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/lagmeter|/lm" + ChatColor.GREEN + " <help|?> [page]" + ChatColor.GOLD + " - This command. Gives the user a list of commands that they are able to use in this plugin.");
					if (this.permit(sender, "lagmeter.command.ping") || this.permit(sender, "lagmeter.command.lping")) {
						this.sendMessage(sender, 0, ChatColor.DARK_GREEN + "/ping|/lping" + ChatColor.GREEN + " [hops] " + ChatColor.GOLD + " - Pings google.com from the server. Specify an amount of hops to specify more packets." + ChatColor.RED + " Warning: server-intensive above 4 hops.");
					}
				} else {
					this.sendMessage(sender, 1, "Invalid page number.");
				}
			} else {
				this.sendMessage(sender, 1, "Sorry, but you don't have access to the help command.");
			}
		} else {
			this.sendMessage(sender, 1, "Invalid sub-command. " + ChatColor.GOLD + "Try one of these:");
			this.sendMessage(sender, 0, "Available sub-commands: /lagmeter|lm <reload|r>|/lagmeter|lm <help|?>");
		}
	}

	protected void info(final String message) {
		this.getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "[LagMeter " + this.getDescription().getVersion() + "] " + ChatColor.GREEN + message);
	}

	/**
	 * Whether or not the plugin is taking an average of TPS when outputting.
	 * 
	 * @return Whether or not the plugin is averaging TPS.
	 */
	public boolean isAveraging() {
		return this.useAverage;
	}

	/**
	 * Whether or not the plugin should display chunks with /lag
	 * 
	 * @return Displaying chunks with /lag or not
	 */
	public boolean isDisplayingChunks() {
		return this.displayChunks;
	}

	/**
	 * Whether or not the plugin should display entities with /lag
	 * 
	 * @return Displaying entities with /lag or not
	 */
	public boolean isDisplayingEntities() {
		return this.displayEntities;
	}

	/**
	 * Whether or not the plugin is logging chunks when it logs.
	 * 
	 * @return The plugin's setting for logging chunks.
	 */
	public boolean isLoggingChunks() {
		return this.isLoggingEnabled() ? this.logChunks : false;
	}

	/**
	 * This is a getter for whether or not the plugin is logging anything at
	 * all.
	 * 
	 * @return Whether or not the plugin is logging stuff.
	 */
	public boolean isLoggingEnabled() {
		return this.enableLogging;
	}

	/**
	 * Whether or not the plugin is logging entities when it logs.
	 * 
	 * @return The plugin's setting for logging entities.
	 */
	public boolean isLoggingEntities() {
		return this.isLoggingEnabled() ? this.logEntities : false;
	}

	/**
	 * Whether or not the plugin is logging total chunks only, not by world with
	 * a total.
	 * 
	 * @return Whether or not the plugin will log chunks per-world, or only a
	 *         total. Will return false if the setting is true, but the logging
	 *         chunks option is off.
	 */
	public boolean isLoggingTotalChunksOnly() {
		return this.isLoggingChunks() ? this.logTotalChunksOnly : false;
	}

	/**
	 * Whether or not the plugin is logging total entities only, not by world
	 * with a total.
	 * 
	 * @return Whether or not the plugin will log entities per-world, or only a
	 *         total. Will return false if the setting is true, but the logging
	 *         entities option is off.
	 */
	public boolean isLoggingTotalEntitiesOnly() {
		return this.isLoggingEntities() ? this.logTotalEntitiesOnly : false;
	}

	/**
	 * Whether or not the plugin will log players online.
	 * 
	 * @return If the plugin is logging players online or not.
	 */
	public boolean isPlayerLoggingEnabled() {
		return this.playerLoggingEnabled;
	}

	/**
	 * This is the getter for whether or not the plugin will put its logfile in
	 * a folder, with today as its date.
	 * 
	 * @return Whether or not the log will be separated from others, based on
	 *         the date it was created.
	 */
	public boolean isUsingLogFolder() {
		return this.useLogsFolder;
	}

	/**
	 * This method gets whether or not the plugin, when logging, will add an
	 * extra empty line between logging blocks.
	 * 
	 * @return If the plugin inserts an extra line feed between logging blocks.
	 */
	public boolean isUsingNewBlockEveryLog() {
		return this.newBlockPerLog;
	}

	/**
	 * This method gets whether or not the plugin, when logging, will add an
	 * extra empty line between logging entities and chunks, etc.
	 * 
	 * @return If the plugin inserts an extra line feed between logging chunks,
	 *         etc..
	 */
	public boolean isUsingNewLineForLogStats() {
		return this.newLineForLogStats;
	}

	protected void notifyAsyncLagListeners() {
		final HighLagEvent e = new HighLagEvent(this.getTPS());
		for (final LagListener l : this.getAsyncLagListeners()) {
			if (l != null) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						l.onHighLagEvent(e);
					}
				}).start();
			}
		}
	}

	protected void notifyAsyncMemoryListeners() {
		final LowMemoryEvent e = new LowMemoryEvent(this.getMemory(), this.getTPS());
		for (final MemoryListener m : this.getAsyncMemoryListeners()) {
			if (m != null) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						m.onLowMemoryEvent(e);
					}
				}).start();
			}
		}
	}

	protected void notifyLagListeners() {
		this.notifyAsyncLagListeners();
		this.notifySyncLagListeners();
	}

	protected void notifyMemoryListeners() {
		this.notifyAsyncMemoryListeners();
		this.notifySyncMemoryListeners();
	}

	protected void notifySyncLagListeners() {
		class C extends BukkitRunnable {
			@Override
			public void run() {
				final HighLagEvent e = new HighLagEvent(LagMeter.this.getTPS());
				for (final LagListener l : LagMeter.this.getSyncLagListeners()) {
					if (l != null) {
						l.onHighLagEvent(e);
					}
				}
			}
		}
		new C().runTask(this);
	}

	protected void notifySyncMemoryListeners() {
		class C extends BukkitRunnable {
			@Override
			public void run() {
				final LowMemoryEvent e = new LowMemoryEvent(LagMeter.this.getMemory(), LagMeter.this.getTPS());
				for (final MemoryListener m : LagMeter.this.getSyncMemoryListeners()) {
					if (m != null) {
						m.onLowMemoryEvent(e);
					}
				}
			}
		}
		new C().runTask(this);
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String commandLabel, final String[] args) {
		if (!this.isEnabled()) {
			return false;
		}
		boolean success = false;
		if (this.permit(sender, "lagmeter.command." + command.getName().toLowerCase()) || !(sender instanceof Player)) {
			if (command.getName().equalsIgnoreCase("lag")) {
				success = true;
				this.sendLagMeter(sender);
			} else if (command.getName().equalsIgnoreCase("mem")) {
				success = true;
				this.sendMemMeter(sender);
			} else if (command.getName().equalsIgnoreCase("lagmem")) {
				success = true;
				this.sendLagMeter(sender);
				this.sendMemMeter(sender);
			} else if (command.getName().equalsIgnoreCase("uptime")) {
				success = true;
				final int[] i = this.getCurrentServerUptime();
				this.sendMessage(sender, 0, "Current server uptime: " + i[3] + " day(s), " + i[2] + " hour(s), " + i[1] + " minute(s), and " + i[0] + " second(s)");
			} else if (command.getName().equalsIgnoreCase("lm")) {
				success = true;
				if (args.length == 0) {
					this.sendLagMeter(sender);
					this.sendMemMeter(sender);
				} else {
					this.handleBaseCommand(sender, args);
				}
			} else if (command.getName().equalsIgnoreCase("lmp")) {
				success = true;
				this.sendLagMeter(sender);
				this.sendMemMeter(sender);
				this.sendMessage(sender, 0, "Players online: " + ChatColor.GOLD + Bukkit.getServer().getOnlinePlayers().length);
			} else if (command.getName().equalsIgnoreCase("lchunks")) {
				success = true;
				this.sendChunks(sender);
			} else if (command.getName().equalsIgnoreCase("lentities") || command.getName().equalsIgnoreCase("lmobs")) {
				success = true;
				this.sendEntities(sender);
			} else if (command.getName().equalsIgnoreCase("ping")) {
				success = true;
				this.ping(sender, args);
			} else if (command.getName().equalsIgnoreCase("lping")) {
				success = true;
				this.ping(sender, args);
			} else if (command.getName().equalsIgnoreCase("LagMeter")) {
				success = true;
				if (args.length == 0) {
					this.sendMessage(sender, 0, "Version: " + this.getDescription().getVersion());
					this.sendMessage(sender, 0, "Available sub-commands: /lagmeter|lm <reload|r>|/lagmeter|lm <help|?>");
				} else {
					this.handleBaseCommand(sender, args);
				}
			}
			return success;
		} else {
			success = true;
			this.sendMessage(sender, 1, "Sorry, permission lagmeter.command." + command.getName().toLowerCase() + " was denied.");
		}
		return success;
	}

	@Override
	public void onDisable() {
		this.memWatcher.stop();
		this.lagWatcher.stop();
		this.cancelAllLagListeners();
		this.cancelAllMemoryListeners();
		if (this.logger.isEnabled()) {
			try {
				this.logger.disable();
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		Bukkit.getServer().getScheduler().cancelTasks(this);
		this.info("Disabled!");
	}

	@Override
	public void onEnable() {
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
		if (!logsFolder.exists() && this.useLogsFolder && this.enableLogging) {
			this.info("Logs folder not found. Attempting to create one for you.");
			if (!logsFolder.mkdir()) {
				this.severe("Error! Couldn't create the folder!");
			} else {
				this.info("Logs folder created.");
			}
		}
		if (this.enableLogging) {
			this.poller.setLogInterval(this.logInterval);
			if (!this.logger.enable()) {
				this.severe("Logging is disabled due to an error while attempting to enable it: " + this.logger.getError());
			}
		}
		this.history.setMaxSize(this.averageLength);
		this.info("Enabled! Polling every " + this.interval + " server ticks." + (this.isLoggingEnabled() ? " Logging to " + this.logger.getFilename() + "." : ""));
		this.registerTasks();
		if (this.displayChunksOnLoad) {
			this.info("Chunks loaded:");
			int total = 0;
			for (final World world : Bukkit.getServer().getWorlds()) {
				final int chunks = world.getLoadedChunks().length;
				this.info("World \"" + world.getName() + "\": " + chunks + ".");
				total += chunks;
			}
			this.info("Total chunks loaded: " + total);
		}
		if (this.displayEntitiesOnLoad) {
			this.info("Entities:");
			int total = 0;
			for (final World world : Bukkit.getServer().getWorlds()) {
				final int entities = world.getEntities().size();
				this.info("World \"" + world.getName() + "\": " + entities + ".");
				total += entities;
			}
			this.info("Total entities: " + total);
		}
		this.pingDomains = new HashMap<String, String>();
		Bukkit.getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
		Bukkit.getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
	}

	/**
	 * Parses a string to get the amount of ticks equal to what the string
	 * passed represents.
	 * 
	 * @param timeString
	 *            - The "human-readable" representation of time, where:
	 *            <ul>
	 *            <b>s</b> is seconds;<br>
	 *            <b>m</b> is minutes;<br>
	 *            <b>h</b> is hours;<br>
	 *            <b>d</b> is days; and finally,<br>
	 *            <b>w</b> is weeks.
	 *            </ul>
	 * 
	 * @return Amount of ticks which corresponds to this string of time.
	 * 
	 * @throws InvalidTimeFormatException
	 *             If the time format given is invalid or the tick amount which
	 *             results is less than 1
	 * 
	 * @see LagMeter#parseTimeMS(String)
	 */
	public long parseTime(String timeString) throws InvalidTimeFormatException {
		long time = 0L;
		if (timeString.split("<>").length == 2) {
			timeString = timeString.split("<>")[1].toLowerCase();
			String z = "";
			for (int i = 0; i < timeString.length(); i++) {
				final String c = timeString.substring(i, i + 1);
				if (c.matches("[^wdhms]")) {
					z += c;
				} else {
					try {
						if (c.equalsIgnoreCase("w")) {
							time += 12096000L * Long.parseLong(z);
						} else if (c.equalsIgnoreCase("d")) {
							time += 1728000L * Long.parseLong(z);
						} else if (c.equalsIgnoreCase("h")) {
							time += 7200L * Long.parseLong(z);
						} else if (c.equalsIgnoreCase("m")) {
							time += 1200L * Long.parseLong(z);
						} else if (c.equalsIgnoreCase("s")) {
							time += 20L * Long.parseLong(z);
						}
						z = "";
					} catch (final NumberFormatException e) {
						throw new InvalidTimeFormatException("The time for the uptime command " + timeString.split("<>")[0] + " is invalid: the time string contains characters other than 0-9, w/d/h/m/s.");
					}
				}
			}
		} else {
			time = -1L;
		}
		if (time < 1) {
			throw new InvalidTimeFormatException("The time \"" + timeString + "\" is invalid and couldn't be parsed.");
		}
		return time;
	}

	/**
	 * Parses the timeString given and returns milliseconds instead of ticks.
	 * Works in the same fashion as parseTime(String).
	 * 
	 * @see LagMeter#parseTime(String)
	 * 
	 * @param timeString
	 *            - The string of time, where:
	 *            <ul>
	 *            <b>s</b> is seconds;<br>
	 *            <b>m</b> is minutes;<br>
	 *            <b>h</b> is hours;<br>
	 *            <b>d</b> is days; and finally,<br>
	 *            <b>w</b> is weeks.
	 *            </ul>
	 * 
	 * @return The amount of milliseconds that would equate to the time string
	 *         given.
	 * 
	 * @throws InvalidTimeFormatException
	 *             If the timeString is in an invalid format (i.e. invalid
	 *             characters) or the result is less than 1.
	 */
	public long parseTimeMS(String timeString) throws InvalidTimeFormatException {
		return (this.parseTime(timeString) * 50L);
	}

	public boolean permit(final CommandSender sender, final String perm) {
		if (sender.hasPermission("lagmeter.*")) {
			return true;
		}
		return sender.hasPermission(perm);
	}

	public boolean permit(final Player player, final String perm) {
		return this.permit((CommandSender) player, perm);
	}

	/**
	 * This method pings google.com, telling the player what the result is.
	 * 
	 * @param sender
	 *            - The CommandSender object to output to.
	 * @param args
	 * <br />
	 *            <ul>
	 *            [0]: hops
	 *            </ul>
	 */
	public void ping(final CommandSender sender, final String[] args) {
		final List<String> processCmd = new ArrayList<String>();
		final String hops = this.getHops(sender, args);
		final String domain = (sender instanceof Player ? this.getPlayerIP(sender.getName()) : "google.com");
		if ((domain == null) || domain.isEmpty()) {
			return;
		}
		processCmd.add("ping");
		processCmd.add(System.getProperty("os.name").startsWith("Windows") ? "-n" : "-c");
		processCmd.add(hops);
		processCmd.add(domain);
		final class SyncSendMessage extends BukkitRunnable {
			CommandSender	sender;
			int				severity;
			String			message;

			@Override
			public void run() {
				LagMeter.this.sendMessage(this.sender, this.severity, this.message);
			}

			SyncSendMessage(final CommandSender sender, final int severity, final String message) {
				this.sender = sender;
				this.severity = severity;
				this.message = message;
			}
		}
		this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
			@Override
			public void run() {
				final BufferedReader result;
				final BufferedReader errorStream;
				Process p;
				String s;
				String output = null;
				final String windowsPingSummary = "Average = ";
				final String unixPingSummary = "rtt min/avg/max/mdev = ";
				try {
					p = new ProcessBuilder(processCmd).start();
					result = new BufferedReader(new InputStreamReader(p.getInputStream()));
					errorStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					while ((s = result.readLine()) != null) {
						if (s.trim().length() != 0) {
							output = s;
						}
						if (s.indexOf(windowsPingSummary) != -1) {
							output = s.substring(s.indexOf(windowsPingSummary) + windowsPingSummary.length());
							break;
						}
						if (s.indexOf(unixPingSummary) != -1) {
							output = s.substring(unixPingSummary.length()).split("/")[1];
							break;
						}
					}
					if (output != null) {
						new SyncSendMessage(sender, 0, "Average response time for the server for " + hops + " ping hop(s) to " + domain + ": " + output).runTask(LagMeter.this);
					} else {
						new SyncSendMessage(sender, 0, "Error running ping command").runTask(LagMeter.this);
					}
					while ((s = errorStream.readLine()) != null) {
						new SyncSendMessage(sender, 1, s).runTask(LagMeter.this);
					}
					errorStream.close();
					result.close();
					p.destroy();
				} catch (final IOException e) {
					new SyncSendMessage(sender, 0, "Error running ping command").runTask(LagMeter.this);
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Registers a listener for when LagMeter finds that the server's TPS has
	 * dropped below the user's specified threshold for the event to be fired.
	 * When this happens, the event method in the class which implements
	 * LagListener will be run. Code will be executed asynchronously in a new
	 * thread; therefore, only <b>thread-safe</b> code should be used.
	 * 
	 * @param listener
	 *            - The listener which implements LagListener which should be
	 *            notified of the event when (if) it happens.
	 * @return The ID of the listener in LagMeter's allocated memory. This is
	 *         used to cancel the registration of the listener, etc.
	 */
	public int registerAsyncLagListener(final LagListener listener) {
		if (!this.asyncLagListeners.contains(listener)) {
			this.asyncLagListeners.add(listener);
			return this.asyncLagListeners.indexOf(listener);
		} else {
			return -1;
		}
	}

	/**
	 * Registers a listener for when LagMeter finds that the free memory has
	 * dropped below the user's specified threshold for the evnt to be fired.
	 * When this happens, the event method in the class which implements
	 * MemoryListener will be run. Code will be executed asynchronously;
	 * therefore, <b>only thread-safe</b> code should be produced.
	 * 
	 * @param listener
	 *            The listener which implements MemoryListener which should be
	 *            notified of the event when (if) it happens.
	 * @return The ID of the listener in LagMeter's allocated memory. This is
	 *         used to cancel the registration of the listener, etc.
	 */
	public int registerAsyncMemoryListener(final MemoryListener listener) {
		if (!this.asyncMemListeners.contains(listener)) {
			this.asyncMemListeners.add(listener);
			return this.asyncMemListeners.indexOf(listener);
		} else {
			return -1;
		}
	}

	/**
	 * Registers a listener for when LagMeter finds that the server's TPS has
	 * dropped below the user's specified threshold for the event to be fired.
	 * When this happens, the event method in the class which implements
	 * LagListener will be run. Code will be executed synchronously, with the
	 * main server thread; therefore, thread-unsafe code may be used.
	 * 
	 * @param listener
	 *            - The listener which implements LagListener which should be
	 *            notified of the event when (if) it happens.
	 * @return The ID of the listener in LagMeter's allocated memory. This is
	 *         used to cancel the registration of the listener, etc.
	 */
	public int registerSyncLagListener(final LagListener listener) {
		if (!this.syncLagListeners.contains(listener)) {
			this.syncLagListeners.add(listener);
			return this.syncLagListeners.indexOf(listener);
		} else {
			return -1;
		}
	}

	/**
	 * Registers a listener for when LagMeter finds that the free memory has
	 * dropped below the user's specified threshold for the evnt to be fired.
	 * When this happens, the event method in the class which implements
	 * MemoryListener will be run. Code will be executed synchronously, with the
	 * main server thread; therefore, thread-unsafe methods and code may be
	 * used.
	 * 
	 * @param listener
	 *            The listener which implements MemoryListener which should be
	 *            notified of the event when (if) it happens.
	 * @return The ID of the listener in LagMeter's allocated memory. This is
	 *         used to cancel the registration of the listener, etc.
	 */
	public int registerSyncMemoryListener(final MemoryListener listener) {
		if (!this.syncMemListeners.contains(listener)) {
			this.syncMemListeners.add(listener);
			return this.syncMemListeners.indexOf(listener);
		} else {
			return -1;
		}
	}

	private void registerTasks() {
		Bukkit.getServer().getScheduler().cancelTasks(this);
		if (this.memWatcher != null) {
			this.memWatcher.stop();
		}
		if (this.lagWatcher != null) {
			this.lagWatcher.stop();
		}
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, this.poller, 1500L, this.interval);
		this.lagNotifyInterval *= 60000;
		this.memNotifyInterval *= 60000;
		new Thread(this.lagWatcher = new LagWatcher(this)).start();
		new Thread(this.memWatcher = new MemoryWatcher(this)).start();
		if (this.AutomaticLagNotificationsEnabled) {
			this.registerSyncLagListener(new DefaultHighLag(this));
		}
		if (this.AutomaticMemoryNotificationsEnabled) {
			this.registerSyncMemoryListener(new DefaultLowMemory(this));
		}
		if (this.uptimeCommands != null) {
			for (final String s : this.uptimeCommands) {
				long time;
				try {
					time = this.parseTime(s);
					if (this.repeatingUptimeCommands) {
						Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new UptimeCommand(s.split(";")[0]), time, time);
					} else {
						Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new UptimeCommand(s.split(";")[0]), time);
					}
				} catch (final InvalidTimeFormatException e) {
					this.sendMessage(Bukkit.getConsoleSender(), 2, e.getMessage());
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
	 * @param name
	 *            - The player to remove from the HashMap.
	 */
	public void removePlayerIP(String name) {
		this.pingDomains.remove(name);
	}

	/**
	 * This method sends the specified CommandSender a message, with the amount
	 * of chunks currently loaded on the server, for each world, followed by a
	 * grand total.
	 * 
	 * @param sender
	 *            - The CommandSender to send output to.
	 */
	public void sendChunks(final CommandSender sender) {
		int totalChunks = 0;
		final List<World> worlds = Bukkit.getServer().getWorlds();
		for (final World world : worlds) {
			final String s = world.getName();
			final int i = Bukkit.getServer().getWorld(s).getLoadedChunks().length;
			totalChunks += i;
			if (i != 0) {
				this.sendMessage(sender, 0, ChatColor.GOLD + "Chunks in world \"" + s + "\": " + i);
			}
		}
		this.sendMessage(sender, 0, ChatColor.GOLD + "Total chunks loaded on the server: " + totalChunks);
	}

	/**
	 * This method sends the specified CommandSender a message, with the amount
	 * of entities currently living on the server, for each world, followed by a
	 * grand total.
	 * 
	 * @param sender
	 *            - The CommandSender to send output to.
	 */
	public void sendEntities(final CommandSender sender) {
		int totalEntities = 0;
		final List<World> worlds = Bukkit.getServer().getWorlds();
		for (final World world : worlds) {
			final String worldName = world.getName();
			final int i = Bukkit.getServer().getWorld(worldName).getEntities().size();
			totalEntities += i;
			if (i != 0) {
				this.sendMessage(sender, 0, ChatColor.GOLD + "Entities in world \"" + worldName + "\": " + i);
			}
		}
		this.sendMessage(sender, 0, ChatColor.GOLD + "Total entities: " + totalEntities);
	}

	/**
	 * Sends the original purpose of this plugin, the Lag Meter, to the
	 * specified CommandSender.
	 * 
	 * @param sender
	 *            The CommandSender to send the LagMeter to.
	 */
	public void sendLagMeter(final CommandSender sender) {
		final StringBuilder lagMeter = new StringBuilder();
		final float tps;
		if (this.displayEntities) {
			this.sendEntities(sender);
		}
		if (this.displayChunks) {
			this.sendChunks(sender);
		}
		if (this.useAverage) {
			tps = this.history.getAverage();
		} else {
			tps = this.ticksPerSecond;
		}
		if ((tps < 21) && (tps >= 0)) {
			int looped = 0;
			while (looped++ < tps) {
				lagMeter.append("#");
			}
			while (looped++ <= 20) {
				lagMeter.append("_");
			}
		} else {
			this.sendMessage(sender, 1, "LagMeter has a 75 second delay before it begins polling. Please wait.");
			return;
		}
		this.sendMessage(sender, 0, ChatColor.GOLD + "[" + (tps >= 18 ? ChatColor.GREEN : tps >= 15 ? ChatColor.YELLOW : ChatColor.RED) + lagMeter.toString() + ChatColor.GOLD + "] " + String.format("%3.2f", tps) + " TPS");
	}

	/**
	 * This sends the other original function of this plugin, the memory meter,
	 * to the specified ConsoleSender.
	 * 
	 * @param sender
	 *            - The ConsoleSender object to send the memory meter to.
	 */
	public void sendMemMeter(final CommandSender sender) {
		final StringBuilder bar = new StringBuilder();
		int looped = 0;
		this.updateMemoryStats();
		while (looped++ < (this.percentageFree / 5)) {
			bar.append('#');
		}
		bar.append(ChatColor.WHITE);
		while (looped++ <= 20) {
			bar.append('_');
		}
		this.sendMessage(sender, 0, ChatColor.GOLD + "[" + (this.percentageFree >= 60 ? ChatColor.GREEN : this.percentageFree >= 35 ? ChatColor.YELLOW : ChatColor.RED) + bar.toString() + ChatColor.GOLD + "] " + String.format("%,.2f", this.memFree) + "MB/" + String.format("%,.2f", this.memMax) + "MB (" + String.format("%,.2f", this.percentageFree) + "%) free");
	}

	protected void sendMessage(final CommandSender sender, final int severity, final String message) {
		if (sender != null) {
			switch (severity) {
			case 0:
				sender.sendMessage(ChatColor.GOLD + "[LagMeter] " + ChatColor.GREEN + message);
				break;
			case 1:
				sender.sendMessage(ChatColor.GOLD + "[LagMeter] " + ChatColor.RED + message);
				break;
			case 2:
				sender.sendMessage(ChatColor.GOLD + "[LagMeter] " + ChatColor.DARK_RED + message);
				break;
			}
		} else {
			switch (severity) {
			default:
			case 0:
				this.info(message);
				break;
			case 1:
				this.warn(message);
				break;
			case 2:
				this.severe(message);
				break;
			}
		}
	}

	protected void sendMessage(final Player player, final int severity, final String message) {
		this.sendMessage((CommandSender) player, severity, message);
	}

	protected void setTicksPerSecond(final float f) {
		this.ticksPerSecond = f;
	}

	public void severe(final String message) {
		this.getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "[LagMeter " + this.getDescription().getVersion() + "] " + ChatColor.DARK_RED + message);
	}

	private void updateConfiguration() {
		final YamlConfiguration yml = new LagMeterConfig().loadConfig();
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
	}

	/**
	 * This method updates the plugin's stored statistics for memory.
	 */
	public synchronized void updateMemoryStats() {
		this.memUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576D;
		this.memMax = Runtime.getRuntime().maxMemory() / 1048576D;
		this.memFree = this.memMax - this.memUsed;
		this.percentageFree = (100D / this.memMax) * this.memFree;
	}

	private void warn(final String message) {
		this.getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "[LagMeter " + this.getDescription().getVersion() + "] " + ChatColor.RED + message);
	}

	/**
	 * Obviously, as this is a bukkit plugin, the constructor is not intended to
	 * be used by anything except for Bukkit.
	 * 
	 * Constructing a new LagMeter object will likely break a <i>lot</i> of
	 * stuff.
	 */
	public LagMeter() {
	}
}