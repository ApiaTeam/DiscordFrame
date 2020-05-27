package me.bertek41.discordframe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import me.bertek41.discordframe.discord.DiscordChatCommand;
import me.bertek41.discordframe.discord.DiscordIntegration;
import me.bertek41.discordframe.misc.FrameWall;
import me.bertek41.discordframe.misc.Settings;
import net.dv8tion.jda.api.JDA;

public class DiscordFrame extends JavaPlugin implements Listener{
	private static TaskChainFactory taskChainFactory;
	private DiscordIntegration discordIntegration;
	private List<FrameWall> frameWalls = new ArrayList<>();
	private List<String> channels = new ArrayList<>();
	private HashMap<UUID, String> createModes = new HashMap<>();
	private File databaseFile;
	private FileConfiguration database;
	
	@Override
	public void onEnable() {
		taskChainFactory = BukkitTaskChainFactory.create(this);
		databaseFile = new File(getDataFolder(), "database.yml");
		if(!databaseFile.exists()) {
			databaseFile.mkdirs();
			saveResource("database.yml", true);
		}
		database = YamlConfiguration.loadConfiguration(databaseFile);
		saveDefaultConfig();
		Settings.setConfig(getConfig());
		getCommand("discordframe").setExecutor(this);
		Bukkit.getPluginManager().registerEvents(this, this);
		taskChainFactory.newChain()
			.asyncFirst(() -> {
				if(Settings.DISCORD_TOKEN.getString().isEmpty()) {
					getServer().getConsoleSender().sendMessage(color("[DiscordFrame] &cSet your discord token in config and restart your server."));
					setEnabled(false);
					return null;
				}
				discordIntegration = new DiscordIntegration(this);
				discordIntegration.login();
				return discordIntegration;
			}).abortIfNull()
			.syncLast((discordIntegration) -> {
				getCommand("discordchat").setExecutor(new DiscordChatCommand(discordIntegration, discordIntegration.getJda().getGuilds().get(0)));
				if(!database.isSet("data")) return;
				for(String index : database.getConfigurationSection("data").getKeys(false)) {
					FrameWall frameWall = new FrameWall(this, taskChainFactory, BlockFace.valueOf(database.getString("data."+index+".face")), getLocationFromString(database.getString("data."+index+".location")), database.getString("data."+index+".channel"));
					frameWalls.add(frameWall);
					channels.add(frameWall.getChannel());
				}
			})
		.execute();
	}
	
	@Override
	public void onDisable() {
		if(discordIntegration != null && discordIntegration.getJda() != null) {
			JDA jda = discordIntegration.getJda();
			discordIntegration.getWebhooks().forEach((channel, webhook) -> jda.getGuilds().get(0).getTextChannelById(channel).deleteWebhookById(webhook.split(":")[0]).complete());
			jda.shutdownNow();
		}
		database.set("data", null);
		int index = 0;
		for(FrameWall frameWall : frameWalls) {
			database.set("data."+index+".location", getStringFromLocation(frameWall.getLocation()));
			database.set("data."+index+".face", frameWall.getFace().toString());
			database.set("data."+index+".channel", frameWall.getChannel());
			frameWall.kill();
			index++;
		}
		try {
			database.save(databaseFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) return false;
		Player player = (Player) sender;
		if(!player.isOp()) {
			player.sendMessage(color("&cYou don't have permission."));
			return false;
		}
		if(args.length == 0) {
			player.sendMessage(color("&e/df create <channel_id>"));
			player.sendMessage(color("&e/df reload"));
			return false;
		}
		if(args[0].equalsIgnoreCase("reload")) {
			reloadConfig();
			Settings.setConfig(getConfig());
			player.sendMessage(color("&eReloaded."));
			return true;
		}
		if(args[0].equalsIgnoreCase("create")) {
			if(args.length == 1) {
				player.sendMessage(color("&cNeed channel id."));
				return false;
			}
			if(createModes.containsKey(player.getUniqueId())) createModes.remove(player.getUniqueId());
			else createModes.put(player.getUniqueId(), args[1]);
			player.sendMessage(color("&eCreate mode: &7"+createModes.containsKey(player.getUniqueId())));
			return true;
		}
		player.sendMessage(color("&e/df create <channel_id>"));
		player.sendMessage(color("&e/df reload"));
		return false;
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if(!createModes.containsKey(event.getPlayer().getUniqueId()) || event.getHand() != EquipmentSlot.HAND) return;
		Player player = event.getPlayer();
		String channel = createModes.remove(player.getUniqueId());
		event.setCancelled(true);
		BlockFace face = getBlockFace(player);
		Location playerLocation = player.getLocation();
		Location location;
		if(event.getClickedBlock() != null) {
		    location = event.getClickedBlock().getLocation();
		    location.setYaw(playerLocation.getYaw());
		    location.setPitch(playerLocation.getPitch());
		}else location = playerLocation;
		if(face == null) return;
		FrameWall frameWall = new FrameWall(this, taskChainFactory, face, location, channel);
		frameWall.init();
		frameWalls.add(frameWall);
		channels.add(channel);
		player.sendMessage(color("&eCreated for channel: &7"+channel));
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBreak(BlockBreakEvent event) {
		if(event.getBlock().getType() != Material.OAK_WALL_SIGN) return;
		for(FrameWall frameWall : new ArrayList<>(frameWalls)) {
			if(!frameWall.getSigns().contains(event.getBlock().getLocation())) continue;
			if(!event.getPlayer().isOp()) event.setCancelled(true);
			else {
				frameWall.kill();
				frameWalls.remove(frameWall);
			}
			return;
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onBreakFrame(HangingBreakByEntityEvent event) {
		if(!(event.getEntity() instanceof ItemFrame)) return;
		for(FrameWall frameWall : new ArrayList<>(frameWalls)) {
			if(!frameWall.hasFrame(event.getEntity())) continue;
			if(event.getRemover() == null || !event.getRemover().isOp()) event.setCancelled(true);
			else {
				frameWall.kill();
				frameWalls.remove(frameWall);
			}
			return;
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
	public void onTakeItem(EntityDamageByEntityEvent event) {
		if(!(event.getEntity() instanceof ItemFrame)) return;
		for(FrameWall frameWall : new ArrayList<>(frameWalls)) {
			if(!frameWall.hasFrame(event.getEntity())) continue;
			if(event.getDamager() == null || !event.getDamager().isOp()) event.setCancelled(true);
			else {
				frameWall.kill();
				frameWalls.remove(frameWall);
			}
			return;
		}
	}
	
	@EventHandler()
	public void onChunkLoad(ChunkLoadEvent event) {
		for(FrameWall frameWall : frameWalls) {
			if(!frameWall.isInit() && frameWall.getLocation().getChunk().equals(event.getChunk())) frameWall.init();
		}
	}
	
	public void update(String channel) {
		for(FrameWall frameWall : frameWalls) {
			if(!frameWall.isInit() || !frameWall.getChannel().equals(channel)) continue;
			frameWall.update();
			return;
		}
	}
	
	public BlockFace getBlockFace(Player player) {
	    List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, 100);
	    if(lastTwoTargetBlocks.size() != 2) return null;
	    return lastTwoTargetBlocks.get(1).getFace(lastTwoTargetBlocks.get(0));
	}
	
	public String color(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}
	
	public String getStringFromLocation(final Location location) {
		String loc = new String();
		loc = location == null ? "" : location.getWorld().getName() + ":" + location.getX() + ":" + location.getY() + ":" + location.getZ();
		loc = location.getYaw() != 0.0 ? loc + ":yaw=" + location.getYaw() : loc;
		loc = location.getPitch() != 0.0 ? loc + ":pitch=" + location.getPitch() : loc;
		return loc;
	}
	
	public Location getLocationFromString(final String location) {
		if(location == null || location.trim() == "") return null;
		final String[] split = location.split(":");
		final World world = Bukkit.getWorld(split[0]);
		if(world == null) return null;
		final double x = Double.parseDouble(split[1]);
		final double y = Double.parseDouble(split[2]);
		final double z = Double.parseDouble(split[3]);
		if(split.length == 4){
			return new Location(world, x, y, z);
		} else if(split.length > 4){
			final float yaw = (float) (split[4].contains("yaw") ? Float.parseFloat(split[4].replace("yaw=", "")) : 0.0);
			final float pitch = (float) (split[5] != null && split[5].contains("pitch") ? Float.parseFloat(split[5].replace("pitch=", "")) : split[4].contains("pitch") ? Float.parseFloat(split[5].replace("pitch=", "")) : 0.0);
			return new Location(world, x, y, z, yaw, pitch);
		}
		return null;
	}
	
	public DiscordIntegration getDiscordIntegration() {
		return discordIntegration;
	}
	
	public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }
	
    public static <T> TaskChain<T> newSharedChain(String name) {
        return taskChainFactory.newSharedChain(name);
    }
    
	public List<String> getChannels() {
		return channels;
	}
	
}
