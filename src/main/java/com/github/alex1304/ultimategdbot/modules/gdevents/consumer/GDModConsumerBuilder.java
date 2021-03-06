package com.github.alex1304.ultimategdbot.modules.gdevents.consumer;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.github.alex1304.jdash.component.GDUser;
import com.github.alex1304.ultimategdbot.dbentities.GuildSettings;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.ChannelGDModeratorsSetting;
import com.github.alex1304.ultimategdbot.modules.commands.impl.setup.guildsettings.RoleGDModeratorsSetting;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.BroadcastableMessage;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.MessageBroadcaster;
import com.github.alex1304.ultimategdbot.modules.gdevents.broadcast.OptionalRoleTagMessage;
import com.github.alex1304.ultimategdbot.utils.BotUtils;
import com.github.alex1304.ultimategdbot.utils.GDUtils;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.api.internal.json.objects.EmbedObject.AuthorObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

/**
 * Builds consumer for GD mod
 *
 * @author Alex1304
 */
public class GDModConsumerBuilder extends GDEventConsumerBuilder<GDUser> {
	
	private AuthorObject embedAuthor;

	public GDModConsumerBuilder(String eventName, AuthorObject embedAuthor, Supplier<BroadcastableMessage> messageToBroadcast) {
		super(eventName, "channelGDModerators", messageToBroadcast, gs -> new ChannelGDModeratorsSetting(gs).getValue());
		this.embedAuthor = embedAuthor;
	}

	@Override
	protected void broadcastComponent(GDUser component, List<IChannel> channels,
			Map<Long, GuildSettings> channelToGS) {
		
		EmbedObject embed = GDUtils.buildEmbedForGDUser(embedAuthor, component);

		MessageBroadcaster mb = new MessageBroadcaster(channels, channel -> {
			GuildSettings gs = channelToGS.get(channel.getLongID());
			RoleGDModeratorsSetting rals = new RoleGDModeratorsSetting(gs);

			OptionalRoleTagMessage ortm = (OptionalRoleTagMessage) messageToBroadcast.get();
			ortm.setBaseEmbed(embed);
			ortm.setRoleToPing(rals.getValue());
			return ortm;
		});
		
		mb.setOnDone(onDone);
		mb.broadcast();
		onBroadcastDone.run();
		
		List<IUser> linkedUsers = GDUtils.getDiscordUsersLinkedToGDAccount(component.getAccountID());
		linkedUsers.forEach(u -> BotUtils.sendMessage(u.getOrCreatePMChannel(), ((OptionalRoleTagMessage) messageToBroadcast.get()).getPrivateContent(), embed));
	}

	@Override
	protected String componentToHumanReadableString(GDUser component) {
		return "user **" + component.getName() + "** (" + component.getAccountID() + ")";
	}

}
