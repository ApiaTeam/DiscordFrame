package me.bertek41.discordframe.discord;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.vdurmont.emoji.EmojiParser;

import gui.ava.html.image.generator.HtmlImageGenerator;
import me.bertek41.discordframe.DiscordFrame;
import me.bertek41.discordframe.misc.Settings;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordIntegration extends ListenerAdapter{
	private JDA jda;
	private DiscordFrame instance;
	private Document start, author, text;
	private HashMap<String, LinkedList<Message>> lastMessages;
	private HashMap<String, String> latest;
	private HashMap<String, String> webhooks;
	
	public DiscordIntegration(DiscordFrame instance) {
		this.instance = instance;
		this.lastMessages = new LinkedHashMap<>();
		this.latest = new HashMap<>();
		this.webhooks = new HashMap<>();
		try {
			start = Jsoup.parse(instance.getResource("start.html"), "UTF-8", "");
			author = Jsoup.parse(instance.getResource("author.html"), "UTF-8", "");
			text = Jsoup.parse(instance.getResource("text.html"), "UTF-8", "");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public JDA login() {
		try {
			jda = JDABuilder.createLight(Settings.DISCORD_TOKEN.getString()).addEventListeners(this).build();
			jda.awaitReady();
		} catch (LoginException | InterruptedException e) {
			e.printStackTrace();
		}
		return jda;
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if((!event.isWebhookMessage() && event.getAuthor().isFake()) || !instance.getChannels().contains(event.getChannel().getId())) return;
		String channel = event.getChannel().getId();
		addMessage(channel, event.getMessage());
		instance.update(channel);
		if(!webhooks.containsKey(channel)) event.getChannel().createWebhook(channel).queue((webhook) -> {
			webhooks.put(channel, webhook.getId()+":"+webhook.getToken());
		});
	}
	
	public void parseMessages(String channel) {
		long previous = 0;
		Document cloneStart = start.clone();
		Document cloneAuthor = null;
		for(Message message : lastMessages.get(channel)) {
			if(previous != message.getAuthor().getIdLong()) {
				if(cloneAuthor != null) cloneStart.selectFirst("div.chatlog").append(cloneAuthor.toString().replace("<html>", "").replace("</html>", "")
						.replace("<head>", "").replace("</head>", "").replace("<body>", "").replace("</body>", ""));
				Elements divs = cloneStart.select("div.preamble__entry");
				divs.get(0).text(message.getGuild().getName());
				divs.get(1).text(message.getCategory().getName()+" / "+message.getChannel().getName());
				Color color = message.getMember() == null ? new Color(241, 196, 15) : message.getMember().getColor();
				cloneAuthor = author.clone();
				Elements span = cloneAuthor.select("span.chatlog__author-name");
				span.attr("title", message.getMember() == null ? message.getAuthor().getName() : message.getMember().getEffectiveName()+"#"+message.getAuthor().getDiscriminator()).attr("data-user-id", message.getAuthor().getId()).attr("style", "color: rgb("+color.getRed()+", "+color.getGreen()+", "+color.getBlue()+")");
				span.get(0).text(message.getMember() == null ? message.getAuthor().getName() : message.getMember().getEffectiveName());
				cloneAuthor.select("span.chatlog__timestamp").get(0).text(new SimpleDateFormat("dd-MMM-yy hh:mm aa").format(message.getTimeCreated().toInstant().toEpochMilli()));
			}
			Document cloneText = text.clone();
			Elements elements = cloneText.select("div.chatlog__message ");
			elements.attr("data-message-id", message.getId()).attr("id", "message-"+message.getId());
			String stripped = EmojiParser.parseToAliases(message.getContentStripped());
			if(stripped.length() > 40) {
				for(String line : splitEqually(stripped, 40)) {
					Document clone = cloneText.clone();
					clone.select("div.markdown").get(0).text(line);
					cloneAuthor.selectFirst("div.chatlog__messages").append(clone.toString().replace("<html>", "").replace("</html>", "")
							.replace("<head>", "").replace("</head>", "").replace("<body>", "").replace("</body>", ""));
				}
			}else {
				cloneText.select("div.markdown").get(0).text(stripped);
				cloneAuthor.selectFirst("div.chatlog__messages").append(cloneText.toString().replace("<html>", "").replace("</html>", "")
						.replace("<head>", "").replace("</head>", "").replace("<body>", "").replace("</body>", ""));
			}
			
			previous = message.getAuthor().getIdLong();
		}
		if(cloneAuthor != null) cloneStart.selectFirst("div.chatlog").append(cloneAuthor.toString().replace("<html>", "").replace("</html>", "")
				.replace("<head>", "").replace("</head>", "").replace("<body>", "").replace("</body>", ""));
		latest.put(channel, cloneStart.html().replace("<#root>", "").replace("</#root>", ""));
	}
	
	private List<String> splitEqually(String text, int size) {
		List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);
		for(int start = 0; start < text.length(); start += size) {
			ret.add(text.substring(start, Math.min(text.length(), start + size)));
		}
		return ret;
	}
	
	public void addMessage(String channel, Message message) {
		LinkedList<Message> messages = lastMessages.getOrDefault(channel, new LinkedList<>());
		messages.add(message);
		while(messages.size() > 5) messages.removeFirst();
		lastMessages.put(channel, messages);
		parseMessages(channel);
	}
	
	public BufferedImage getLatestImage(String channel) {
		if(latest.isEmpty() || !latest.containsKey(channel) || latest.get(channel).isEmpty()) {
			List<Message> history = jda.getGuilds().get(0).getTextChannelById(channel).getHistory().getRetrievedHistory();
			if(history.isEmpty()) history = jda.getGuilds().get(0).getTextChannelById(channel).getHistory().retrievePast(5).complete();
			if(history.isEmpty()) return null;
			history = history.subList(0, history.size() > 5 ? 5 : history.size());
			Collections.reverse(history);
			if(lastMessages.containsKey(channel)) lastMessages.get(channel).addAll(history);
			else lastMessages.put(channel, new LinkedList<>(history));
			parseMessages(channel);
		}
		HtmlImageGenerator hig = new HtmlImageGenerator();
		hig.loadHtml(latest.get(channel));
		return hig.getBufferedImage();
	}
	
	public JDA getJda() {
		return jda;
	}
	
	public HashMap<String, String> getWebhooks() {
		return webhooks;
	}
	
}
