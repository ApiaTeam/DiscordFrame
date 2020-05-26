package me.bertek41.discordframe.misc;

import org.bukkit.entity.ItemFrame;

public class FrameWallSection {
	private int x, y;
	private ItemFrame itemFrame;
	
	public FrameWallSection(int x, int y, ItemFrame itemFrame) {
		this.x = x;
		this.y = y;
		this.itemFrame = itemFrame;
	}
	
	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public ItemFrame getItemFrame() {
		return itemFrame;
	}
	
	public void setItemFrame(ItemFrame itemFrame) {
		this.itemFrame = itemFrame;
	}
	
}
