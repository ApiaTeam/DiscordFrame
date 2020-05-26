package me.bertek41.discordframe.misc;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;

import co.aikar.taskchain.TaskChainFactory;
import me.bertek41.discordframe.DiscordFrame;

public class FrameWall {
	private DiscordFrame instance;
	private TaskChainFactory factory;
	private int xSections, ySections;
	private List<Location> signs;
	private List<FrameWallSection> sections;
	private BlockFace face;
	private Location location;
	private String channel;
	private boolean init;
	
	public FrameWall(DiscordFrame instance, TaskChainFactory factory, BlockFace face, Location location, String channel) {
		this.instance = instance;
		this.factory = factory;
		this.face = face;
		this.location = location;
		this.channel = channel;
		signs = new ArrayList<>();
		sections = new ArrayList<>();
	}
	
	public void init() {
		init = true;
		factory.newChain()
			.asyncFirst(() -> instance.getDiscordIntegration().getLatestImage(channel))
			.abortIfNull()
			.sync((source) -> {
				int localXSections = (source.getWidth() + 128 - 1) / 128;
				int localYSections = (source.getHeight() + 128 - 1) / 128;
				if(localXSections != xSections || localYSections != ySections) {
					for(Location loc : signs) loc.getBlock().setType(Material.AIR);
					for(FrameWallSection section : sections) {
						section.getItemFrame().remove();
					}
					sections.clear();
					signs.clear();
				}
				xSections = localXSections;
				ySections = localYSections;
				return source;
			}).async((source) -> resize(source, xSections, ySections))
			.syncLast((resized) -> {
				for(int x = 0; x < xSections; x++) {
					for(int y = 0; y < ySections; y++) {
						Location clone = location.clone();
						switch (face) {
							case UP:
								clone.add(x, 0, y);
								break;
							case DOWN:
								clone.add(x, 0, -y);
								break;
							case SOUTH:
								clone.add(x, -y, 0);
								break;
							case NORTH:
								clone.add(-x, -y, 0);
								break;
							case WEST:
								clone.add(0, -y, x);
								break;
							case EAST:
								clone.add(0, -y, -x);
								break;
							default:
								break;
						}
						Block behind = clone.getBlock().getRelative(face.getOppositeFace());
						if(behind.getType() == Material.AIR) behind.setType(Material.OAK_WALL_SIGN);
						if(face != BlockFace.UP && face != BlockFace.DOWN && behind.getBlockData() instanceof WallSign) {
							WallSign data = (WallSign) behind.getBlockData();
							data.setFacing(face.getOppositeFace());
							behind.setBlockData(data);
						}
						if(clone.getBlock().getType() != Material.AIR) clone.getBlock().setType(Material.AIR);
						ItemFrame frame = (ItemFrame) clone.getWorld().spawnEntity(clone, EntityType.ITEM_FRAME);
						frame.setFacingDirection(face);
						frame.setItem(createMap(resized.getSubimage(x * 128, y * 128, 128, 128), clone.getWorld()));
						sections.add(new FrameWallSection(x, y, frame));
						signs.add(behind.getLocation());
					}
				}
			})
		.execute();
	}
	
	public void update() {
		factory.newChain()
			.asyncFirst(() -> instance.getDiscordIntegration().getLatestImage(channel))
			.abortIfNull()
			.sync((source) -> {
				int localXSections = (source.getWidth() + 128 - 1) / 128;
				int localYSections = (source.getHeight() + 128 - 1) / 128;
				if(localXSections != xSections || localYSections != ySections) {
					init();
					return null;
				}
				return source;
			}).abortIfNull()
			.async((source) -> resize(source, xSections, ySections))
			.syncLast((resized) -> {
				for(int x = 0; x < xSections; x++) {
					for(int y = 0; y < ySections; y++) {
						FrameWallSection section = getSuitableSection(x, y);
						ItemFrame frame = section.getItemFrame();
						if(!frame.isValid()) {
							Location loc = frame.getLocation();
							Block behind = loc.getBlock().getRelative(face.getOppositeFace());
							if(behind.getType() == Material.AIR) behind.setType(Material.OAK_WALL_SIGN);
							if(face != BlockFace.UP && face != BlockFace.DOWN && behind.getBlockData() instanceof WallSign) {
								WallSign data = (WallSign) behind.getBlockData();
								data.setFacing(face.getOppositeFace());
								behind.setBlockData(data);
							}
							if(loc.getBlock().getType() != Material.AIR) loc.getBlock().setType(Material.AIR);
							frame = (ItemFrame) loc.getWorld().spawnEntity(loc, EntityType.ITEM_FRAME);
							frame.setFacingDirection(face);
						}
						frame.setItem(createMap(resized.getSubimage(x * 128, y * 128, 128, 128), frame.getWorld()));
					}
				}
			})
		.execute();
	}
	
	private FrameWallSection getSuitableSection(int x, int y) {
		for(FrameWallSection section : sections) if(section.getX() == x && section.getY() == y) return section;
		return null;
	}
	
	public boolean hasFrame(Entity hanging) {
		for(FrameWallSection section : sections) {
			if(section.getItemFrame().equals(hanging)) return true;
		}
		return false;
	}
	
	public void kill() {
		for(Location loc : signs) loc.getBlock().setType(Material.AIR);
		for(FrameWallSection section : sections) {
			section.getItemFrame().remove();
		}
		sections.clear();
		signs.clear();
		instance.getChannels().remove(channel);
	}
	
	public String getChannel() {
		return channel;
	}
	
	public BlockFace getFace() {
		return face;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public List<Location> getSigns() {
		return signs;
	}
	
	public boolean isInit() {
		return init;
	}
	
	private static BufferedImage resize(BufferedImage image, int xSections, int ySections) {
		if(image.getWidth() % 128 == 0 && image.getHeight() % 128 == 0) {
			return image;
		}
		Image img = image.getScaledInstance(xSections * 128, ySections * 128, Image.SCALE_DEFAULT);
		image = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2D = image.createGraphics();
		g2D.drawImage(img, 0, 0, null);
		g2D.dispose();
		return image;
	}
	
	private static ItemStack createMap(BufferedImage image, World world) {
		ItemStack item = new ItemStack(Material.FILLED_MAP);
		MapMeta meta = (MapMeta) item.getItemMeta();
		meta.setMapView(Bukkit.createMap(world));
		MapView view = meta.getMapView();
		view.setScale(Scale.FARTHEST);
		view.getRenderers().forEach(view::removeRenderer);
		view.addRenderer(new Renderer(image));
		item.setItemMeta(meta);
		return item;
	}
	
}
