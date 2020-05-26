package me.bertek41.discordframe.misc;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public enum Settings {
	DISCORD_TOKEN("discord_token"),
	MAX_MESSAGES("max_messages"),
	MESSAGE_LENGTH("message_length"),
	WEBHOOK_PLAYER_AVATAR("webhook_player_avatar"),
	WEBHOOK_DEFAULT_AVATAR("webhook_default_avatar"),
	WEBHOOK_AVATAR_API("webhook_avatar_api"),
	WEBHOOK_NAME("webhook_name");
	
	private static FileConfiguration config;
	private final String path;
	
	private Settings(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}
	
	public static void setConfig(FileConfiguration config) {
		Settings.config = config;
	}
	
	public static void sendMessage(CommandSender sender, String message) {
		sender.sendMessage(message);
	}
	
	public static void sendMessages(CommandSender sender, List<String> messages) {
		messages.forEach(message -> sender.sendMessage(message));
	}
	
	public boolean getBoolean() {
		return config == null ? null : config.getBoolean(path);
	}
	
	public double getDouble() {
		return config == null ? null : config.getDouble(path);
	}
	
	public int getInt() {
		return config == null ? null : config.getInt(path);
	}
	
	public String getString() {
		return config == null ? null : ChatColor.translateAlternateColorCodes('&', config.getString(path));
	}
	
	public List<String> getStringList() {
		return config == null ? null : config.getStringList(path).stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
	}
	
	public Material getAsMaterial() {
		return config == null ? null : Material.valueOf(config.getString(path));
	}
	
	public String toString() {
		return config == null ? null : config.getString(path);
	}
	
}
