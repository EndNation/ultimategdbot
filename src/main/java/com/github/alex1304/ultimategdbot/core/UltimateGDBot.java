package com.github.alex1304.ultimategdbot.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import com.github.alex1304.jdash.api.GDHttpClient;
import com.github.alex1304.jdash.exceptions.GDAPIException;
import com.github.alex1304.ultimategdbot.cache.Cache;
import com.github.alex1304.ultimategdbot.dbentities.GlobalSettings;
import com.github.alex1304.ultimategdbot.exceptions.ModuleUnavailableException;
import com.github.alex1304.ultimategdbot.modules.Module;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.Emojis;
import com.github.alex1304.ultimategdbot.utils.SnowflakeType;

import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IEmoji;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;

/**
 * Represents the bot itself. Encapsulates the client and provides other useful methods.
 *
 * @author Alex1304
 */
public class UltimateGDBot {
	
	private static UltimateGDBot instance = null;
	
	private IDiscordClient client;
	private Properties props;
	private Cache cache;
	private GlobalSettings globals;
	private Map<String, Module> modules;
	private Map<String, Boolean> startedModules;
	private GDHttpClient gdClient;
	private List<IGuild> emojiGuilds;
	
	/**
	 * Load properties and builds Discord client
	 * 
	 * @throws Exception if something goes wrong in building process
	 */
	private UltimateGDBot() throws Exception {
		this.props = new Properties();
		props.load(Main.class.getResourceAsStream("/ultimategdbot.properties"));

		if (props.values().stream().anyMatch(x -> x.toString().isEmpty())) {
			throw new Exception("Some properties are missing."
					+ "Make sure you defined all of them in ultimategdbot.properties");
		}
		
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(props.getProperty("ultimategdbot.client.token"));
		clientBuilder.withRecommendedShardCount();
		this.client = clientBuilder.build();
		this.cache = new Cache();
		this.globals = BotUtils.initGlobalSettings();
		this.modules = new HashMap<>();
		this.startedModules = new HashMap<>();
		this.gdClient = new GDHttpClient(
				Long.parseLong(props.getProperty("ultimategdbot.gd_client.id")),
				props.getProperty("ultimategdbot.gd_client.password"));
		String host = props.getProperty("ultimategdbot.gd_client.url");
		if (!host.equals("default"))
			gdClient.setHost(host);
		this.emojiGuilds = null;
	}
	
	public static void loadEmojiGuilds() {
		if (instance().emojiGuilds != null)
			return;
		
		instance().emojiGuilds = instance().props.keySet().stream()
				.map(x -> x.toString())
				.filter(x -> x.matches("ultimategdbot\\.misc\\.emoji_guild_id\\.[0-9A-Za-z]+"))
				.map(x -> client().getGuildByID(Long.parseLong(property(x))))
				.collect(Collectors.toList());
	}
	
	/**
	 * Initializes the instance of the bot
	 * 
	 * @throws Exception propagates any exception from the bot's constructor
	 */
	public static void init() throws Exception {
		instance = new UltimateGDBot();
	}
	
	/**
	 * Gets the instance of the bot
	 * 
	 * @return UltimateGDBot
	 * @throws IllegalStateException if the bot instance hasn't been initialized
	 */
	private static UltimateGDBot instance() {
		if (instance == null)
			throw new IllegalStateException("Bot not initialized");
		return instance;
	}

	public static IDiscordClient client() {
		return instance().client;
	}
	
	public static String property(String prop) {
		return instance().props.getProperty(prop);
	}
	
	public static Cache cache() {
		return instance().cache;
	}
	
	public static GDHttpClient gdClient() {
		return instance().gdClient;
	}
	
	public static IGuild officialGuild() {
		return (IGuild) BotUtils.resolveSnowflakeString(SnowflakeType.GUILD, property("ultimategdbot.hierarchy.official_guild_id"));
	}
	
	public static IUser owner() {
		return (IUser) BotUtils.resolveSnowflakeString(SnowflakeType.USER, property("ultimategdbot.hierarchy.owner_id"));
	}
	
	public static IRole moderatorRole() {
		return (IRole) BotUtils.resolveSnowflakeString(SnowflakeType.ROLE, property("ultimategdbot.hierarchy.moderator_role_id"));
	}
	
	public static IChannel channelDebugLogs() {
		return (IChannel) BotUtils.resolveSnowflake(SnowflakeType.CHANNEL, instance().globals.getChannelDebugLogs());
	}
	
	public static void log(String text) {
		System.out.println(text);
		
		RequestBuffer.request(() -> {
			channelDebugLogs().sendMessage(text);
		});
	}
	
	public static void log(String tag, String text) {
		log("[" + tag + "] " + text);
	}
	
	public static void logInfo(String text) {
		log(Emojis.INFO + " " + text);
	}
	
	public static void logWarning(String text) {
		log(":warning: " + text);
	}
	
	public static void logSuccess(String text) {
		log(Emojis.SUCCESS + " " + text);
	}
	
	public static void logError(String text) {
		log(Emojis.CROSS + " " + text);
	}
	
	public static void logException(Exception e) {
		if (e instanceof GDAPIException) {
			GDAPIException ge = (GDAPIException) e;
			StringBuilder sb = new StringBuilder("A problem occured when fetching data from Geometry Dash servers.\n");
			if (ge.getResponse().isEmpty())
				sb.append("The following request failed:\n");
			else
				sb.append("Request sent:\n");
			
			sb.append("```\n" + ge.getRequest() + "\n```\n");
			
			if (!ge.getResponse().isEmpty()) {
				sb.append("Response returned by the server, that the bot was unable to parse:\n");
				sb.append("```\n" + BotUtils.truncate(ge.getResponse(), 1400).trim() + "\n```\n");
			}
			
			sb.append("Exception thrown: `" + ge.getUnderlyingException().getClass().getName() + ": " + ge.getUnderlyingException().getMessage() + "`");
			
			logError(sb.toString());
			System.err.print("[GDAPIException] ");
			ge.getUnderlyingException().printStackTrace();
		} else {
			logError("Exception thrown: `" + e.getClass().getName() + ": " + e.getMessage() + "`");
			e.printStackTrace();
		}
	}
	
	public static void addModule(String key, Module module) {
		instance().modules.put(key, module);
		instance().startedModules.put(key, false);
		instance().client.getDispatcher().registerListener(module);
	}
	
	public static void startModules() {
		for (Entry<String, Module> m : instance().modules.entrySet())
			startModule(m.getKey());
	}
	
	public static void stopModules() {
		for (Entry<String, Module> m : instance().modules.entrySet())
			stopModule(m.getKey());
	}
	
	public static void restartModules() {
		for (Entry<String, Module> m : instance().modules.entrySet())
			restartModule(m.getKey());
	}
	
	public static void startModule(String key) {
		if (instance().modules.containsKey(key)) {
			instance().modules.get(key).start();
			instance().startedModules.put(key, true);
			logSuccess("Started module: `" + key + "`");
		}
	}
	
	public static void stopModule(String key) {
		if (instance().modules.containsKey(key)) {
			instance().modules.get(key).stop();
			instance().startedModules.put(key, false);
			logWarning("Stopped module: `" + key + "`");
		}
	}
	
	public static void restartModule(String key) {
		stopModule(key);
		startModule(key);
	}
	
	public static Module getModule(String key) throws ModuleUnavailableException {
		if (!instance().startedModules.containsKey(key) || !instance.startedModules.get(key))
			throw new ModuleUnavailableException();
		return instance().modules.get(key);
	}

	/**
	 * Gets a copy of the startedModules
	 *
	 * @return Map&lt;String,Boolean&gt;
	 */
	public static Map<String, Boolean> getStartedModules() {
		return new HashMap<>(instance().startedModules);
	}
	
	/**
	 * Tests if the module with the given name is available
	 * 
	 * @param key
	 *            - the module name
	 * @return boolean
	 */
	public static boolean isModuleAvailable(String key) {
		try {
			return getModule(key) != null;
		} catch (ModuleUnavailableException e) {
			return false;
		}
	}
	
	/**
	 * Gets an emoji by its name from the emoji server
	 * 
	 * @param name
	 * @return IEmoji
	 */
	public static IEmoji emoji(String name) {
		if (instance().emojiGuilds == null)
			return null;
		
		IEmoji emoji = null;
		
		for (IGuild g : instance().emojiGuilds) {
			emoji = g.getEmojiByName(name);
			if (emoji != null)
				return emoji;
		}
		
		return null;
	}

}
