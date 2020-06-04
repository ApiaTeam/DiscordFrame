package me.bertek41.discordframe.discord;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import me.bertek41.discordframe.DiscordFrame;
import me.bertek41.discordframe.misc.Settings;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class DiscordChatCommand implements CommandExecutor{
	private DiscordFrame instance;
	private DiscordIntegration discordIntegration;
	private Guild guild;
	
	public DiscordChatCommand(DiscordFrame instance, DiscordIntegration discordIntegration, Guild guild) {
		this.instance = instance;
		this.discordIntegration = discordIntegration;
		this.guild = guild;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) return false;
		Player player = (Player) sender;
		if(args.length < 2) {
			player.sendMessage(color("&e/dc <channel> <message>"));
			return false;
		}
		instance.newChain()
			.async(() -> {
				String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				List<TextChannel> channels = guild.getTextChannelsByName(args[0], true);
				if(channels.isEmpty()) {
					player.sendMessage(color("&cNo channel found."));
					return;
				}
				TextChannel channel = channels.get(0);
				String webhook = discordIntegration.getWebhooks().get(channel.getId());
				if(webhook == null) {
					player.sendMessage(color("&cNo bridge found. Try to send message in Discord first."));
					return;
				}
				try(WebhookClient client = WebhookClient.withId(Long.parseLong(webhook.split(":")[0]), webhook.split(":")[1])) {
					WebhookMessageBuilder builder = new WebhookMessageBuilder();
					builder.setUsername(Settings.WEBHOOK_NAME.getString().replace("<player>", player.getName()));
					if(Settings.WEBHOOK_PLAYER_AVATAR.getBoolean()) builder.setAvatarUrl(Settings.WEBHOOK_AVATAR_API.getString().replace("<uuid>", player.getUniqueId().toString()).replace("<player>", player.getName()));
					else builder.setAvatarUrl(Settings.WEBHOOK_DEFAULT_AVATAR.getString());
					builder.setContent(text);
					client.send(builder.build());
				}
			})
		.execute();
		return true;
	}
	
	public String color(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}
	
}
