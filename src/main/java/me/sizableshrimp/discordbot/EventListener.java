package me.sizableshrimp.discordbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.sizableshrimp.discordbot.music.GuildMusicManager;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.MissingPermissionsException;

public class EventListener {
	@EventSubscriber
	public void onMessageEvent(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		String message = event.getMessage().getContent();
		IChannel channel = event.getChannel();
		if (message.toLowerCase().startsWith(XTBot.prefix+"help") || (!message.contains("@everyone") && !message.contains("@here") && event.getMessage().getMentions().contains(XTBot.client.getOurUser()))) {
			sendMessage("Hello! I am XT Bot. My commands are:\n```"+XTBot.prefix+"hey\n"+XTBot.prefix+"info\n"+XTBot.prefix+"settings\n"+XTBot.prefix+"music```", channel);
			return;
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"info")) {
			EmbedBuilder embed = new EmbedBuilder();
			embed.withAuthorName("Information");
			embed.appendDesc("This bot is built with [Spring Boot 2.0.3](https://spring.io/projects/spring-boot) and hosted on [Heroku](https://dashboard.heroku.com). It is coded in Java using the [Discord4J](https://github.com/Discord4J/Discord4J) library.");
			embed.appendField("Author", "SizableShrimp", true);
			embed.appendField("Discord4J Version", "2.10.1", true);
			embed.appendField("Prefix", XTBot.prefix, false);
			embed.appendField("Uptime", getUptime(), false);
			new MessageBuilder(XTBot.client).appendContent("To find out my commands, use `"+XTBot.prefix+"help`").withEmbed(embed.build()).withChannel(channel).build();
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"fortnite") || message.toLowerCase().startsWith(XTBot.prefix+"ftn")) {
			if (message.split(" ").length == 3) {
				String platform = message.split(" ")[1];
				String embedPlatform;
				if (platform.equals("pc")) {
					embedPlatform = "PC";
				} else if (platform.equals("ps4")) {
					embedPlatform = "PS4";
				} else if (platform.equals("xbox")) {
					embedPlatform = "Xbox One";
				} else {
					sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"fortnite [pc|ps4|xbox] [username]```", channel);
					return;
				}
				String username = message.split(" ")[2];
				try {
					HttpsURLConnection conn = (HttpsURLConnection) new URL("https://api.fortnitetracker.com/v1/profile/"+platform+"/"+username).openConnection();
					conn.setRequestMethod("GET");
					conn.setRequestProperty("TRN-Api-Key", System.getenv("FORTNITE_KEY"));
					conn.connect();
					if (conn.getResponseCode() == HttpsURLConnection.HTTP_OK) {
						BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						String inputLine;
						StringBuffer response = new StringBuffer();
						while ((inputLine = reader.readLine()) != null) response.append(inputLine);
						reader.close();
						JSONObject json = new JSONObject(response.toString());
						EmbedBuilder embed = new EmbedBuilder();
						embed.withAuthorName(username+" | "+embedPlatform);
						embed.appendField("Solos", getSolos(json), true);
						embed.appendField("Duos", getDuos(json), true);
						embed.appendField("Squads", getSquads(json), true);
						embed.appendField("Lifetime", getLifetime(json), false);
						embed.withFooterText("fortnitetracker.com");
						sendEmbed(embed, channel);
						return;
					}
					sendMessage("The user specified could not be found. Please try a different name or platform.", channel);
					return;
				} catch (IOException e) {
					e.printStackTrace();
					sendMessage("An error occured when trying to fetch the stats. Please try again later.", channel);
					return;
				} catch (JSONException e) {
					e.printStackTrace();
					sendMessage("An error occured when trying to parse the stats. Please try again later.", channel);
					return;
				}
			} else {
				sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"fortnite [pc|ps4|xbox] [username]```", channel);
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"hey")) {
			sendMessage("Hello! :smile:", channel);
			return;
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"settings prefix")) {
			if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_SERVER)) {
				if (message.split(" ").length != 3) {
					sendMessage("Incorrect usage. Please use: ```"+XTBot.prefix+"settings prefix [new prefix]```", channel);
					return;
				} else {
					String newPrefix = message.split(" ")[2];
					if (newPrefix.length() != 1) {
						sendMessage(":x: A prefix can only be 1 character long.", channel);
						return;
					}
					if (newPrefix.toUpperCase() != newPrefix) {
						sendMessage(":x: A prefix cannot be a letter.", channel);
						return;
					}
					XTBot.prefix = newPrefix;
					sendMessage(":white_check_mark: Prefix successfully changed to `"+XTBot.prefix+"`", channel);
					return;
				}
			} else {
				sendMessage(":x: Insufficient permission.", channel);
				return;
			}
		} else if (message.toLowerCase().startsWith(XTBot.prefix+"settings") && message.split(" ").length == 1) {
			if (channel.getModifiedPermissions(event.getAuthor()).contains(Permissions.MANAGE_SERVER)) {
				EmbedBuilder embed = new EmbedBuilder();
				embed.withAuthorName("XT Bot Settings");
				embed.appendField(":exclamation: **Prefix**", "`"+XTBot.prefix+"settings prefix [new prefix]`", true);
				sendEmbed(embed, channel);
				return;
			} else {
				sendMessage(":x: Insufficient permission.", channel);
				return;
			}
		}
	}

	@EventSubscriber
	public void onUserVoiceLeave(UserVoiceChannelLeaveEvent event) {
		if (event.getUser() == XTBot.client.getOurUser()) {
			GuildMusicManager manager = XTBot.music.getGuildAudioPlayer(event.getGuild());
			manager.player.setVolume(XTBot.music.DEFAULT_VOLUME);
			return;
		} else {
			IVoiceChannel channel = event.getVoiceChannel();
			if (event.getGuild().getConnectedVoiceChannel() == channel) {
				if (channel.getConnectedUsers().size() == 1) {
					channel.leave();
					GuildMusicManager manager = XTBot.music.getGuildAudioPlayer(event.getGuild());
					manager.scheduler.queue.clear();
					manager.player.startTrack(null, false);
				}
			}
			return;
		}
	}

	@EventSubscriber
	public void onReady(ReadyEvent event) {
		XTBot.client.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "a random thing");
	}

	public static void sendMessage(String message, IChannel channel) throws DiscordException, MissingPermissionsException {
		channel.sendMessage("\u200B"+message);
	}

	public static void sendEmbed(EmbedBuilder embed, IChannel channel) throws DiscordException, MissingPermissionsException {
		channel.sendMessage("\u200B", embed.build());
	}

	private String getUptime() {
		Long uptime = System.currentTimeMillis()-XTBot.firstOnline;
		Long days = TimeUnit.MILLISECONDS.toDays(uptime);
		Long hours = TimeUnit.MILLISECONDS.toHours(uptime) - TimeUnit.DAYS.toHours(days);
		Long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) - TimeUnit.HOURS.toMinutes(hours) - TimeUnit.DAYS.toMinutes(days);
		Long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) - TimeUnit.MINUTES.toSeconds(minutes) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.DAYS.toSeconds(days);
		List<String> formats = new ArrayList<String>();
		if (days > 0) {if (days == 1) {formats.add(days.toString()+" day");} else {formats.add(days.toString()+" days");}}
		if (hours > 0) {if (hours == 1) {formats.add(hours.toString()+" hour");} else {formats.add(hours.toString()+" hours");}}
		if (minutes > 0) {if (minutes == 1) {formats.add(minutes.toString()+" minute");} else {formats.add(minutes.toString()+" minutes");}}
		if (seconds > 0) {if (seconds == 1) {formats.add(seconds.toString()+" second");} else {formats.add(seconds.toString()+" seconds");}}
		if (formats.size() == 0) return "Less than a second";
		String result = formats.get(0);
		if (formats.size() == 2) result = formats.get(0)+" and "+formats.get(1);
		if (formats.size() == 3) result = formats.get(0)+", "+formats.get(1)+", and "+formats.get(2);
		if (formats.size() == 4) result = formats.get(0)+", "+formats.get(1)+", "+formats.get(2)+", and "+formats.get(3);
		return result;
	}

	private String getSolos(JSONObject json) {
		StringBuffer main = new StringBuffer();
		try {
			main.append("**Matches:** "+json.getJSONObject("stats").getJSONObject("p2").getJSONObject("matches").getString("displayValue"));
			main.append("\n**Wins:** "+json.getJSONObject("stats").getJSONObject("p2").getJSONObject("top1").getString("displayValue"));
			main.append("\n**Win Ratio:** "+json.getJSONObject("stats").getJSONObject("p2").getJSONObject("winRatio").getString("displayValue")+"%");
			main.append("\n**Top 10:** "+json.getJSONObject("stats").getJSONObject("p2").getJSONObject("top10").getString("displayValue"));
			main.append("\n**Top 25:** "+json.getJSONObject("stats").getJSONObject("p2").getJSONObject("top25").getString("displayValue"));
			main.append("\n**Kills:** "+json.getJSONObject("stats").getJSONObject("p2").getJSONObject("kills").getString("displayValue"));
			main.append("\n**K/D:** "+json.getJSONObject("stats").getJSONObject("p2").getJSONObject("kd").getString("displayValue"));
			return main.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	private String getDuos(JSONObject json) {
		StringBuffer main = new StringBuffer();
		try {
			main.append("**Matches:** "+json.getJSONObject("stats").getJSONObject("p10").getJSONObject("matches").getString("displayValue"));
			main.append("\n**Wins:** "+json.getJSONObject("stats").getJSONObject("p10").getJSONObject("top1").getString("displayValue"));
			main.append("\n**Win Ratio:** "+json.getJSONObject("stats").getJSONObject("p10").getJSONObject("winRatio").getString("displayValue")+"%");
			main.append("\n**Top 10:** "+json.getJSONObject("stats").getJSONObject("p10").getJSONObject("top10").getString("displayValue"));
			main.append("\n**Top 25:** "+json.getJSONObject("stats").getJSONObject("p10").getJSONObject("top25").getString("displayValue"));
			main.append("\n**Kills:** "+json.getJSONObject("stats").getJSONObject("p10").getJSONObject("kills").getString("displayValue"));
			main.append("\n**K/D:** "+json.getJSONObject("stats").getJSONObject("p10").getJSONObject("kd").getString("displayValue"));
			return main.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	private String getSquads(JSONObject json) {
		StringBuffer main = new StringBuffer();
		try {
			main.append("**Matches:** "+json.getJSONObject("stats").getJSONObject("p9").getJSONObject("matches").getString("displayValue"));
			main.append("\n**Wins:** "+json.getJSONObject("stats").getJSONObject("p9").getJSONObject("top1").getString("displayValue"));
			main.append("\n**Win Ratio:** "+json.getJSONObject("stats").getJSONObject("p9").getJSONObject("winRatio").getString("displayValue")+"%");
			main.append("\n**Top 10:** "+json.getJSONObject("stats").getJSONObject("p9").getJSONObject("top10").getString("displayValue"));
			main.append("\n**Top 25:** "+json.getJSONObject("stats").getJSONObject("p9").getJSONObject("top25").getString("displayValue"));
			main.append("\n**Kills:** "+json.getJSONObject("stats").getJSONObject("p9").getJSONObject("kills").getString("displayValue"));
			main.append("\n**K/D:** "+json.getJSONObject("stats").getJSONObject("p9").getJSONObject("kd").getString("displayValue"));
			return main.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	private String getLifetime(JSONObject json) {
		StringBuffer main = new StringBuffer();
		try {
			JSONArray stats = json.getJSONArray("lifeTimeStats");
			main.append("**Matches:** "+stats.getString(7));
			main.append("\n**Wins:** "+stats.getString(8));
			main.append("\n**Win Ratio:** "+stats.getString(9));
			main.append("\n**Top 10:** "+stats.getString(3));
			main.append("\n**Top 25:** "+stats.getString(5));
			main.append("\n**Kills:** "+stats.getString(10));
			main.append("\n**K/D:** "+stats.getString(11));
			return main.toString();
		} catch (JSONException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static void newVideo(String payload) {
		String formatted = payload.substring(7, payload.length()-2);
		EventListener.sendMessage("@everyone "+formatted, XTBot.client.getChannelByID(341028279584817163L));
	}
}
