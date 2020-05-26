package me.bertek41.discordframe.misc;

import java.awt.image.BufferedImage;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class Renderer extends MapRenderer{
	private BufferedImage image;
	private boolean rendered = false;
	
	public Renderer(BufferedImage image) {
		this.image = image;
	}
	
	@Override
	public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
		if(rendered) return;
		try {
			mapCanvas.drawImage(0, 0, image);
			rendered = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
