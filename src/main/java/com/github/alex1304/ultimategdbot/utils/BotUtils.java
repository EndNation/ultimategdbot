package com.github.alex1304.ultimategdbot.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;

import com.github.alex1304.ultimategdbot.core.Database;
import com.github.alex1304.ultimategdbot.core.UltimateGDBot;
import com.github.alex1304.ultimategdbot.dbentities.GlobalSettings;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IIDLinkedObject;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;

/**
 * Utilitary methods for the bot
 *
 * @author Alex1304
 */
public class BotUtils {
	
	/**
	 * Initializes the global settings by fetching database
	 * 
	 * @return GlobalSettings
	 */
	public static GlobalSettings initGlobalSettings() {
		Session s = Database.newSession();
		GlobalSettings g = null;
		
		try {
			g = s.load(GlobalSettings.class, 1);
		} finally {
			s.close();
		}
		
		return g;
	}
	
	/**
	 * Resolves a snowflake ID into a Discord object
	 * 
	 * @param type
	 *            - the type of Discord object (guild, channel, etc)
	 * @param snowflake
	 *            - the snowflake
	 * @return IIDLinkedObject
	 */
	public static IIDLinkedObject resolveSnowflake(SnowflakeType type, long snowflake) {
		return type.getFunc().apply(snowflake);
	}

	/**
	 * Resolves a snowflake ID into a Discord object. If the string isn't a
	 * valid snowflake, null is returned.
	 * 
	 * @param type
	 *            - the type of Discord object (guild, channel, etc)
	 * @param snowflake
	 *            - the snowflake as String
	 * @return IIDLinkedObject
	 */
	public static IIDLinkedObject resolveSnowflakeString(SnowflakeType type, String snowflake) {
		try {
			return resolveSnowflake(type, Long.parseLong(snowflake));
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	/**
	 * Gets the settings stored for the given guild. If no settings are stored,
	 * an empty instance will be saved and returned
	 * 
	 * @param guild - the guild to fetch settings for
	 * @return GuildSettings
	 */
	public static GuildSettings getSettingsForGuild(IGuild guild) {
		Session s = Database.newSession();
		GuildSettings gs = null;
		
		try {
			gs = s.load(GuildSettings.class, guild.getLongID());
			if (gs == null) {
				gs = new GuildSettings();
				gs.setGuildID(guild.getLongID());
				s.save(gs);
				s.flush();
			}
		} finally {
			s.close();
		}
		
		return gs;
	}
	
	/**
	 * Sends a message to a Discord channel
	 * 
	 * @param channel
	 *            - the channel to send the message in
	 * @param message
	 *            - the message content
	 * @param embed
	 *            - the message embed
	 * @return IMessage
	 */
	public static IMessage sendMessage(IChannel channel, String message, EmbedObject embed) {
		return RequestBuffer.request(() -> channel.sendMessage(truncate(message, 2000).trim(), embed)).get();
	}

	/**
	 * Sends a message to a Discord channel
	 * 
	 * @param channel
	 *            - the channel to send the message in
	 * @param message
	 *            - the message content
	 * @return IMessage
	 */
	public static IMessage sendMessage(IChannel channel, String message) {
		return RequestBuffer.request(() -> channel.sendMessage(truncate(message, 2000).trim())).get();
	}

	/**
	 * Sends a message to a Discord channel
	 * 
	 * @param channel
	 *            - the channel to send the message in
	 * @param embed
	 *            - the message embed
	 * @return IMessage
	 */
	public static IMessage sendMessage(IChannel channel, EmbedObject embed) {
		return RequestBuffer.request(() -> channel.sendMessage(embed)).get();
	}
	
	/**
	 * Toggles on/off typing status for a channel.
	 * 
	 * @param channel
	 * @param on
	 */
	public static void typing(IChannel channel, boolean on) {
		RequestBuffer.request(() -> channel.setTypingStatus(on));
	}

	/**
	 * Escapes characters used in Markdown syntax using a backslash
	 * 
	 * @param text
	 * @return String
	 */
	public static String escapeMarkdown(String text) {
		List<Character> resultList = new ArrayList<>();
		Character[] charsToEscape = { '\\', '_', '*', '~', '`', ':', '@', '#' };
		List<Character> charsToEscapeList = new ArrayList<>(Arrays.asList(charsToEscape));
		
		for (char c : text.toCharArray()) {
			if (charsToEscapeList.contains(c))
				resultList.add('\\');
			resultList.add(c);
		}
		
		char[] result = new char[resultList.size()];
		for (int i = 0 ; i < result.length ; i++)
			result[i] = resultList.get(i);
		
		return new String(result);
	}

	/**
	 * Escapes characters used in regex syntax using a backslash
	 * 
	 * @param text
	 * @return String
	 */
	public static String escapeRegex(String text) {
		List<Character> resultList = new ArrayList<>();
		Character[] charsToEscape = { '\\', '+', '*', '-', '[', ']', '.', '^', '$', '(', ')', '{', '}', '|', '?' };
		List<Character> charsToEscapeList = new ArrayList<>(Arrays.asList(charsToEscape));
		
		for (char c : text.toCharArray()) {
			if (charsToEscapeList.contains(c))
				resultList.add('\\');
			resultList.add(c);
		}
		
		char[] result = new char[resultList.size()];
		for (int i = 0 ; i < result.length ; i++)
			result[i] = resultList.get(i);
		
		return new String(result);
	}
	
	/**
	 * Gets the channel of a guild by the given String.
	 * 
	 * @param str
	 *            - The desired channel to look for. It can either be the name,
	 *            the ID or the mention of it.
	 * @param guild
	 *            - The guild in which the desired channel is supposed to be
	 * @return The desired channel, or null if the channel could not be found
	 */
	public static IChannel stringToChannel(String str, IGuild guild) {
		long channelID; 

		try {
			channelID = Long.parseLong(str);
		} catch (NumberFormatException e) {
			try {
				channelID = Long.parseLong(str.substring(2, str.length() - 1));
			} catch (NumberFormatException | IndexOutOfBoundsException e2) {
				try {
					channelID = guild.getChannelsByName(str).get(0).getLongID();
				} catch (IndexOutOfBoundsException e3) {
					return null;
				}
			}
		}
		return guild.getChannelByID(channelID);
	}

	/**
	 * Gets the role of a guild by the given String.
	 * 
	 * @param str
	 *            - The desired role to look for. It can either be the name, the
	 *            ID or the mention of it.
	 * @param guild
	 *            - The guild in which the desired role is supposed to be
	 * @return The desired role, or null if the role could not be found
	 */
	public static IRole stringToRole(String str, IGuild guild) {
		long roleID;

		try {
			roleID = Long.parseLong(str);
		} catch (NumberFormatException e) {
			try {
				roleID = Long.parseLong(str.substring(3, str.length() - 1));
			} catch (NumberFormatException | StringIndexOutOfBoundsException e2) {
				try {
					roleID = guild.getRolesByName(str).get(0).getLongID();
				} catch (IndexOutOfBoundsException e3) {
					return null;
				}
			}
		}
		return guild.getRoleByID(roleID);
	}
	
	/**
	 * Generates a random String made of alphanumeric characters. The length of
	 * the generated String is specified as an argument.
	 * 
	 * The following characters are excluded to avoid confusion between l and 1,
	 * O and 0, etc: <code>l, I, 1, 0, O</code>
	 * 
	 * @param n
	 *            - the length of the generated String
	 * @return the generated random String
	 */
	public static String generateAlphanumericToken(int n) {
		if (n < 1)
			return null;
		
		final String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
		char[] result = new char[n];
		
		for (int i = 0 ; i < result.length ; i++)
			result[i] = alphabet.charAt(new Random().nextInt(alphabet.length()));
		
		return new String(result);
	}
	
	/**
	 * Formats the username of the user specified as argument with the format username#discriminator
	 * @param user - The user whom username will be formatted
	 * @return The formatted username as String.
	 */
	public static String formatDiscordUsername(IUser user) {
		return escapeMarkdown(user.getName() + "#" + user.getDiscriminator());
	}
	
	/**
	 * Format a millisecond value to a human-readable format
	 * 
	 * @param millis
	 *            - the millisecond value to convert
	 * @return String
	 */
	public static String formatTimeMillis(long millis) {
		long tmp = millis;
		
		long days = TimeUnit.MILLISECONDS.toDays(tmp);
		tmp -= TimeUnit.DAYS.toMillis(days);
		long hours = TimeUnit.MILLISECONDS.toHours(tmp);
		tmp -= TimeUnit.HOURS.toMillis(hours);
		long minutes =  TimeUnit.MILLISECONDS.toMinutes(tmp);
		tmp -= TimeUnit.MINUTES.toMillis(minutes);
		long seconds =  TimeUnit.MILLISECONDS.toSeconds(tmp);
		tmp -= TimeUnit.SECONDS.toMillis(seconds);
		
		return String.format(" %d day%s %d hour%s %d minute%s %d second%s %d millisecond%s",
				days, days < 2 ? "" : "s",
				hours, hours < 2 ? "" : "s",
				minutes, minutes < 2 ? "" : "s",
				seconds, seconds < 2 ? "" : "s",
				millis % 1000, millis % 1000 < 2 ? "" : "s"
		).replaceAll(" 0 [^ ]+", "").trim();
	}
	
	/**
	 * Returns the prefix used in the message, null if none were used.
	 * 
	 * @param message
	 *            - The message
	 * @return String
	 */
	public static String prefixUsedInMessage(String message) {
		final String mentionPrefix = UltimateGDBot.client().getOurUser().mention(true);
		final String mentionPrefix2 = UltimateGDBot.client().getOurUser().mention(false);
		String prefixUsed = null;
		
		if (message.toLowerCase().startsWith(UltimateGDBot.property("ultimategdbot.prefix.full").toLowerCase()))
			prefixUsed = UltimateGDBot.property("ultimategdbot.prefix.full");
		else if (message.toLowerCase().startsWith(UltimateGDBot.property("ultimategdbot.prefix.canonical").toLowerCase()))
			prefixUsed = UltimateGDBot.property("ultimategdbot.prefix.canonical");
		else if (message.equals(mentionPrefix))
			prefixUsed = mentionPrefix;
		else if (message.equals(mentionPrefix2))
			prefixUsed = mentionPrefix2;
		
		return prefixUsed;
	}
	
	/**
	 * Tells whether the message starts with a mention prefix
	 * 
	 * @param message
	 *            - The message
	 * @return String
	 */
	public static boolean isMentionPrefix(String message) {
		String prefix = prefixUsedInMessage(message);
		final String mentionPrefix = UltimateGDBot.client().getOurUser().mention(true);
		final String mentionPrefix2 = UltimateGDBot.client().getOurUser().mention(false);
		
		return prefix != null && (prefix.equals(mentionPrefix) || prefix.equals(mentionPrefix2));
	}
	
	/**
	 * Truncates a string to have the desired length. If too long, it will be
	 * cut off with "...". If too short, it will be filled with spaces.
	 * 
	 * @param str
	 *            - the string to truncate
	 * @param n
	 *            - the number of characters the output should have
	 * @return String
	 * @throws IllegalArgumentException
	 *             if n is negative
	 */
	public static String truncate(String str, int n) {
		if (n < 0)
			throw new IllegalArgumentException("truncate: n cannot be a negative value");
		if (str.length() == n)
			return str;
		else if (str.length() > n)
			return str.substring(0, n - 3) + "...";
		else {
			StringBuffer sb = new StringBuffer(str);
			while (sb.length() < n)
				sb.append(" ");
			return sb.toString();
		}
	}

	/**
	 * Returns a String which is the concatenation of all the String elements
	 * from the list, each element seperated by a space.
	 * 
	 * @param args
	 *            - The list to concatenate
	 * @return the concatenated args
	 */
	public static String concatCommandArgs(List<String> args) {
		String result = "";
		for(String s : args)
			result += s + " ";
		
		return result.isEmpty() ? result : result.substring(0, result.length() - 1);
	}
	
	public static long extractIDFromMention(String mention) {
		IllegalArgumentException iae = new IllegalArgumentException("Not a mention");
		if (!mention.matches("<@!?[0-9]+>"))
			throw iae;
		
		StringBuffer sb = new StringBuffer(mention);
		sb.deleteCharAt(sb.length() - 1);
		sb.delete(0, 2);
		if (sb.charAt(0) == '!')
			sb.deleteCharAt(0);
		
		try {
			return Long.parseLong(sb.toString());
		} catch (NumberFormatException e) {
			throw iae;
		}
	}
	
	public static String commandWithoutArgs(String fullMessage, List<String> args) {
		StringBuffer sb = new StringBuffer(fullMessage);
		StringBuffer concatArgs = new StringBuffer(concatCommandArgs(args));
		
		sb.reverse();
		concatArgs.reverse();
		concatArgs.append(" ");
		
		
		return new StringBuffer(sb.toString().replaceFirst(escapeRegex(concatArgs.toString()), "")).reverse().toString();
	}

}
