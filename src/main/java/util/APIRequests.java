package util;

import face.FaceManager;
import face.SubscriberIcon;
import gui.GUIMain;
import irc.account.Oauth;
import lib.JSON.JSONArray;
import lib.JSON.JSONObject;
import lib.pircbot.org.jibble.pircbot.Channel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
//import java.net.URLDecoder;
//import java.security.cert.Certificate;
import java.text.DecimalFormat;

import javax.net.ssl.HttpsURLConnection;

import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Created by Nick on 5/22/2015.
 */
public class APIRequests {

	//Anything Twitch
	public static class Twitch {

		private static final String TWITCH_API = "https://api.twitch.tv/kraken";
		public static final String CLIENT_ID = "client_id=qw8d3ve921t0n6e3if07l664f1jn1y7";

		public static String getEmoteSet(String emotes)
		{
			return Utils.createAndParseBufferedReader(TWITCH_API + "/chat/emoticon_images?emotesets=" + emotes + "&" + CLIENT_ID);
		}

		/**
		 * @return The JSON string of every Twitch emote.
		 */
		public static String getAllEmotes()
		{
			return Utils.createAndParseBufferedReader(TWITCH_API + "/chat/emoticon_images?" + CLIENT_ID);
		}

		/**
		 * Fetches and downloads the given channel's subscriber icon.
		 *
		 * @param channel The channel to download the sub icon for.
		 * @return The path to the downloaded sub icon if successful, otherwise null.
		 */
		public static Boolean getSubIcon(String channel)
		{
			Boolean toReturn = false;
			Channel c = GUIMain.currentSettings.channelManager.getChannel(channel.replaceAll("#", ""));
			try
			{
				//                URL toRead = new URL(TWITCH_API + "/chat/" + channel.replaceAll("#", "") + "/badges?" + CLIENT_ID);
				//                String line = Utils.createAndParseBufferedReader(toRead.openStream());
				//                if (!line.isEmpty())
				//                {
				//                    JSONObject init = new JSONObject(line);
				//                    if (init.has("subscriber"))
				//                    {
				//                        JSONObject sub = init.getJSONObject("subscriber");
				//                        if (!sub.getString("image").equalsIgnoreCase("null"))
				//                        {
				//                            return FaceManager.downloadIcon(sub.getString("image"), channel);
				//                        }
				//                    }
				//                }
				URL toRead = new URL(TWITCH_API + "/channels/" + channel.replaceAll("#", "") + "?" + CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(toRead.openStream());
				if (!line.isEmpty())
				{
					JSONObject init = new JSONObject(line);
					if ( init.has("_id")) {
						String _id = init.getInt("_id") + "";
						toRead = new URL("https://badges.twitch.tv/v1/badges/channels/" + _id + "/display");
						line = Utils.createAndParseBufferedReader(toRead.openStream());
						if (!line.isEmpty()) {
							init = new JSONObject(line);
							if (init.getJSONObject("badge_sets").has("subscriber")){
								JSONObject versions = init.getJSONObject("badge_sets").getJSONObject("subscriber").getJSONObject("versions");
								if (versions.has("0")) {
									FaceManager.subIconSet.add(
											new SubscriberIcon(channel + "/0", FaceManager.downloadIcon(versions.getJSONObject("0").getString("image_url_1x"), channel + "_0"), 0)
											);
									toReturn = true;
								}


								if (versions.has("1")) {
									FaceManager.subIconSet.add(
											new SubscriberIcon(channel + "/1", FaceManager.downloadIcon(versions.getJSONObject("1").getString("image_url_1x"), channel + "_1"), 1)
											);
									toReturn = true;
								}

								if (versions.has("3")) {
									FaceManager.subIconSet.add(
											new SubscriberIcon(channel + "/3", FaceManager.downloadIcon(versions.getJSONObject("3").getString("image_url_1x"), channel + "_3"), 3)
											);
									toReturn = true;
								}

								if (versions.has("6")) {
									FaceManager.subIconSet.add(
											new SubscriberIcon(channel + "/6", FaceManager.downloadIcon(versions.getJSONObject("6").getString("image_url_1x"), channel + "_6"), 6)
											);
									toReturn = true;
								}

								if (versions.has("12")) {
									FaceManager.subIconSet.add(
											new SubscriberIcon(channel + "/12", FaceManager.downloadIcon(versions.getJSONObject("12").getString("image_url_1x"), channel + "_12"), 12)
											);
									toReturn = true;
								}

								if (versions.has("24")) {
									FaceManager.subIconSet.add(
											new SubscriberIcon(channel + "/24", FaceManager.downloadIcon(versions.getJSONObject("24").getString("image_url_1x"), channel + "_24"), 24)
											);
									toReturn = true;
								}
							}
						}

					}
				}
			} catch (Exception e)
			{
				GUIMain.log(e);
			}
			return toReturn;

		}

		/**
		 * Gets stream uptime.
		 *
		 * @return the current stream uptime.
		 */
		public static Response getUptimeString(String channelName) {
			if (channelName.contains("#")) channelName = channelName.replace("#", "");
			Response toReturn = new Response();
			try {
				URL twitch = new URL(TWITCH_API + "/streams/" + channelName	+ "?" + CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(twitch.openStream());
				if (line != null){
					JSONObject jsonObject = new JSONObject(line);
					if (jsonObject.isNull("stream")) { 
						toReturn.setResponseText("The stream is not live!");
					} else {
						JSONObject stream = jsonObject.getJSONObject("stream");
						String createdAt = stream.getString("created_at");
						Instant now = Instant.now();
						Instant then = Instant.parse(createdAt);
						long diff = Duration.between(then, now).getSeconds();
						int hours = (int) diff/3600;
						int minutes = (int) (diff - (3600 * hours)) / 60;
						int seconds = (int) diff - (3600 * hours) - (60 * minutes);
						toReturn.setResponseText("The stream has been live for " + hours + "h " + minutes + "m " + seconds + "s.");
					}
				}

			} catch (Exception ignored) {
				toReturn.setResponseText("Error checking uptime due to Exception!");
			}
			return toReturn;
		}

		/**
		 * Checks a channel to see if it's live (streaming).
		 *
		 * @param channelName The name of the channel to check.
		 * @return true if the specified channel is live and streaming, else false.
		 */
		public static boolean isChannelLive(String channelName) {
			boolean isLive = false;
			try {
				URL twitch = new URL(TWITCH_API + "/streams/" + channelName + "?" + CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(twitch.openStream());
				if (line != null) {
					JSONObject jsonObject = new JSONObject(line);
					isLive = !jsonObject.isNull("stream") && !jsonObject.getJSONObject("stream").isNull("preview");
				}
			} catch (Exception ignored) {
			}
			return isLive;
		}

		/**
		 * Gets the amount of viewers for a channel.
		 *
		 * @param channelName The name of the channel to check.
		 * @return The int amount of viewers watching the given channel.
		 */
		public static int countViewers(String channelName) {
			int count = -1;
			try {//this could be parsed with JSON, but patterns work, and if it ain't broke...
				URL twitch = new URL(TWITCH_API + "/streams/" + channelName + "?" + CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(twitch.openStream());
				if (line != null) {
					Matcher m = Constants.viewerTwitchPattern.matcher(line);
					if (m.find()) {
						try {
							count = Integer.parseInt(m.group(1));
						} catch (Exception ignored) {
						}//bad Int parsing
					}
				}
			} catch (Exception e) {
				count = -1;
			}
			return count;
		}

		/**
		 * Gets the status of a channel, which is the title and game of the stream.
		 *
		 * @param channel The channel to get the status of.
		 * @return A string array with the status as first index and game as second.
		 */
		public static String[] getStatusOfStream(String channel) {
			String[] toRet = new String[2];
			try {
				if (channel.contains("#")) channel = channel.replace("#", "");
				URL twitch = new URL(TWITCH_API + "/channels/" + channel + "?" + CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(twitch.openStream());
				if (line != null) {
					JSONObject base = new JSONObject(line);
					//these are never null, just blank strings at worst
					toRet[0] = base.getString("status");
					toRet[1] = base.getString("game");
				}
			} catch (Exception e) {
				GUIMain.log("Failed to get status of stream due to Exception: ");
				GUIMain.log(e);
			}
			return toRet;
		}

		/**
		 * Gets the title of a given channel.
		 *
		 * @param channel The channel to get the title of.
		 * @return The title of the stream.
		 */
		public static String getTitleOfStream(String channel) {
			String[] status = getStatusOfStream(channel);
			return status[0];
		}

		/**
		 * Gets the game of a given channel.
		 *
		 * @param channel The channel to get the game of.
		 * @return An empty string if not playing, otherwise the game being played.
		 */
		public static String getGameOfStream(String channel) {
			String[] status = getStatusOfStream(channel);
			return status[1];
		}

		/**
		 * Updates the stream's status to a given parameter.
		 *
		 * @param key     The oauth key which MUST be authorized to edit the status of the stream.
		 * @param channel The channel to edit.
		 * @param message The message containing the new title/game to update to.
		 * @param isTitle If the change is for the title or game.
		 * @return The response Botnak has for the method.
		 */
		public static Response setStreamStatus(Oauth key, String channel, String message, boolean isTitle) {
			Response toReturn = new Response();
			if (key.canSetTitle()) {
				String add = isTitle ? "title" : "game";
				if (message.split(" ").length > 1) {
					String toChangeTo = message.substring(message.indexOf(' ') + 1);
					if (toChangeTo.equals(" ") || toChangeTo.equals("null")) toChangeTo = "";
					if (toChangeTo.equalsIgnoreCase(isTitle ? getTitleOfStream(channel) : getGameOfStream(channel))) {
						toReturn.setResponseText("Failed to set " + add + ", the " + add + " is already set to that!");
					} else {
						Response status = setStatusOfStream(key.getKey(), channel,
								isTitle ? toChangeTo : getTitleOfStream(channel),
										isTitle ? getGameOfStream(channel) : toChangeTo);
						if (status.isSuccessful()) {
							toReturn.wasSuccessful();
							toChangeTo = "".equals(toChangeTo) ? (isTitle ? "(untitled broadcast)" : "(not playing a game)") : toChangeTo;
							toReturn.setResponseText("Successfully set " + add + " to: \"" + toChangeTo + "\" !");
						} else {
							toReturn.setResponseText(status.getResponseText());
						}
					}
				} else {
					toReturn.setResponseText("Failed to set status status of the " + add + ", usage: !set" + add + " (new " + add + ") or \"null\"");
				}
			} else {
				toReturn.setResponseText("This OAuth key cannot update the status of the stream! Try re-authenticating in the Settings GUI!");
			}
			return toReturn;
		}

		/**
		 * Sets the status of a stream.
		 *
		 * @param key     The oauth key which MUST be authorized to edit the status of a stream.
		 * @param channel The channel to edit.
		 * @param title   The title to set.
		 * @param game    The game to set.
		 * @return The response Botnak has for the method.
		 */
		public static Response setStatusOfStream(String key, String channel, String title, String game) {
			Response toReturn = new Response();
			try {
				if (channel.contains("#")) channel = channel.replace("#", "");
				String request = TWITCH_API + "/channels/" + channel +
						"?channel[status]=" + URLEncoder.encode(title, "UTF-8") +
						"&channel[game]=" + URLEncoder.encode(game, "UTF-8") +
						"&oauth_token=" + key.split(":")[1] + "&_method=put&" + CLIENT_ID;
				URL twitch = new URL(request);
				String line = Utils.createAndParseBufferedReader(twitch.openStream());
				if (!line.isEmpty() && line.contains(title) && line.contains(game)) {
					toReturn.wasSuccessful();
				}
			} catch (Exception e) {
				String error = e.getMessage().length() > 20 ? (e.getMessage().substring(0, e.getMessage().length() / 2) + "...") : e.getMessage();
				toReturn.setResponseText("Failed to update status due to Exception: " + error);
			}
			return toReturn;
		}

		/**
		 * Plays an ad on stream.
		 *
		 * @param key     The oauth key which MUST be authorized to play a commercial on a stream.
		 * @param channel The channel to play the ad for.
		 * @param length  How long
		 * @return true if the commercial played, else false.
		 */
		public static boolean playAdvert(String key, String channel, int length) {
			boolean toReturn = false;
			try {
				if ((length % 30) != 0 || length < 30 || length > 180) length = 30;//has to be divisible by 30 seconds
				if (channel.contains("#")) channel = channel.replace("#", "");
				String request = TWITCH_API + "/channels/" + channel + "/commercial";
				URL twitch = new URL(request);
				HttpURLConnection c = (HttpURLConnection) twitch.openConnection();
				c.setRequestMethod("POST");
				c.setDoOutput(true);
				String toWrite = "length=" + length;
				c.setRequestProperty("Client-ID", CLIENT_ID.split("=")[1]);
				c.setRequestProperty("Authorization", "OAuth " + key.split(":")[1]);
				c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				c.setRequestProperty("Content-Length", String.valueOf(toWrite.length()));
				OutputStreamWriter op = new OutputStreamWriter(c.getOutputStream());
				op.write(toWrite);
				op.close();
				try {
					int response = c.getResponseCode();
					toReturn = (response == 204);
				} catch (Exception e) {
					GUIMain.log(e);
				}
				c.disconnect();
			} catch (Exception e) {
				GUIMain.log(e);
			}
			return toReturn;
		}

		/**
		 * Obtains the title and author of a video on Twitch.
		 *
		 * @param URL The URL to the video.
		 * @return The appropriate response.
		 */
		public static Response getTitleOfVOD(String URL) {
			Response toReturn = new Response();
			try {
				String ID = "";
				Pattern p = Pattern.compile("/[vcb]/([^&\\?/]+)");
				Matcher m = p.matcher(URL);
				if (m.find()) {
					ID = m.group().replaceAll("/", "");
				}
				URL request = new URL(TWITCH_API + "/videos/" + ID + "?" + CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(request.openStream());
				if (!line.isEmpty()){
					JSONObject init = new JSONObject(line);
					String title = init.getString("title");
					//					JSONObject channel = init.getJSONObject("channel");
					String author = init.getJSONObject("channel").getString("display_name");
					toReturn.wasSuccessful();
					toReturn.setResponseText("Linked Twitch VOD: \"" + title + "\" by " + author);
				}
			} catch (Exception e) {
				toReturn.setResponseText("Failed to parse Twitch VOD due to an Exception!");
			}
			return toReturn;
		}


		public static JSONObject getUserSubs(String oath, int passesCompleted){
			JSONObject toReturn = null;
			try {
				URL request = new URL("http://api.twitch.tv/api/users/palehors68/tickets?limit=20&offset=0&unended=true&on_site=1&oauth_token=" + oath);
				BufferedReader br = new BufferedReader(new InputStreamReader(request.openStream()));
				String line = br.readLine();
				br.close();
				toReturn = new JSONObject(line);
			} catch (Exception e){
				GUIMain.log(e);
			}


			return toReturn;
		}

		public static JSONObject getEmoteXref() {
			JSONObject toReturn = null;
			try {
				URL request = new URL("https://twitchemotes.com/api_cache/v2/sets.json");
				BufferedReader br = new BufferedReader(new InputStreamReader(request.openStream()));
				String line = br.readLine();
				br.close();
				toReturn = new JSONObject(line);
			} catch (Exception e){
				GUIMain.log(e);
			}

			return toReturn;
		}

		/**
		 * Uses the main account's OAuth to check for your followed channels, if enabled.
		 *
		 * @param key The oauth key to use.
		 * @return An array of live streams (empty if no one is live).
		 */
		public static ArrayList<String> getLiveFollowedChannels(String key) {
			ArrayList<String> toReturn = new ArrayList<>();
			try
			{
				URL request = new URL(TWITCH_API + "/streams/followed?oauth_token=" + key + "&limit=100&" + CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(request.openStream());
				if (!line.isEmpty()) {
					JSONObject init = new JSONObject(line);
					JSONArray streams = init.getJSONArray("streams");
					for (int i = 0; i < streams.length(); i++) {
						JSONObject stream = streams.getJSONObject(i);
						JSONObject channel = stream.getJSONObject("channel");
						toReturn.add(channel.getString("name").toLowerCase());
					}
				}
			} catch (Exception e) {
				if (!e.getMessage().contains("401") && !e.getMessage().contains("503")) {
					GUIMain.log("Failed to get live followed channels due to exception:");
					GUIMain.log(e);
				}
			}
			return toReturn;
		}

		/**
		 * Gets username suggestions when adding a stream.
		 *
		 * @param partial The partially typed text to prompt a suggestion.
		 * @return An array of suggested stream names.
		 */
		public static String[] getUsernameSuggestions(String partial) {
			ArrayList<String> toReturn = new ArrayList<>();
			try
			{
				URL request = new URL(TWITCH_API + "/search/channels?limit=10&q=" + partial + "&" + CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(request.openStream());
				if (!line.isEmpty()) {
					JSONObject init = new JSONObject(line);
					JSONArray channels = init.getJSONArray("channels");
					for (int i = 0; i < channels.length(); i++) {
						JSONObject channel = channels.getJSONObject(i);
						toReturn.add(channel.getString("name"));
					}
				}
			} catch (Exception e) {
				GUIMain.log(e);
			}
			return toReturn.toArray(new String[toReturn.size()]);
		}

		/**
		 * Gets the last 20 followers of a channel.
		 *
		 * @param channel The channel to check.
		 * @return A string array of (up to) the last 20 followers.
		 */
		public static String[] getLast20Followers(String channel) {
			ArrayList<String> toReturn = new ArrayList<>();
			try
			{
				URL request = new URL(TWITCH_API + "/channels/" + channel + "/follows?limit=20&" + CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(request.openStream());
				if (!line.isEmpty()) {
					JSONObject init = new JSONObject(line);
					JSONArray follows = init.getJSONArray("follows");
					for (int i = 0; i < follows.length(); i++) {
						JSONObject person = follows.getJSONObject(i);
						JSONObject user = person.getJSONObject("user");
						toReturn.add(user.getString("name"));
					}
				}

			} catch (Exception e) {
				GUIMain.log(e);
			}
			return toReturn.toArray(new String[toReturn.size()]);
		}


	}

	//Current playing song
	public static class LastFM {
		/**
		 * Gets the currently playing song from LastFM, assuming the LastFM account was set up correctly.
		 *
		 * @return The name of the song, else an empty string.
		 */
		public static Response getCurrentlyPlaying() {
			Response toReturn = new Response();
			if ("".equals(GUIMain.currentSettings.lastFMAccount)) {
				toReturn.setResponseText("Failed to fetch current playing song, the user has no last.fm account set!");
				return toReturn;
			}
			//TODO check the song requests engine to see if that is currently playing
			String tracks_url = "http://www.last.fm/user/" + GUIMain.currentSettings.lastFMAccount + "/now";
			try {
				URL request = new URL("http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=" +
						GUIMain.currentSettings.lastFMAccount + "&api_key=e0d3467ebb54bb110787dd3d77705e1a&format=json");
				BufferedReader br = new BufferedReader(new InputStreamReader(request.openStream()));
				String line = br.readLine();
				br.close();
				JSONObject outermost = new JSONObject(line);
				JSONObject recentTracks = outermost.getJSONObject("recenttracks");
				JSONArray songsArray = recentTracks.getJSONArray("track");
				if (songsArray.length() > 0) {
					JSONObject mostRecent = songsArray.getJSONObject(0);
					JSONObject artist = mostRecent.getJSONObject("artist");
					String artistOfSong = artist.getString("#text");
					String nameOfSong = mostRecent.getString("name");
					if (mostRecent.has("@attr")) {//it's the current song
						toReturn.setResponseText("The current song is: " + artistOfSong + " - " + nameOfSong + " || " + tracks_url);
					} else {
						toReturn.setResponseText("The most recent song was: " + artistOfSong + " - " + nameOfSong + " || " + tracks_url);
					}
					toReturn.wasSuccessful();
				} else {
					toReturn.setResponseText("Failed to fetch current song; last.fm shows no recent tracks!");
				}
			} catch (Exception e) {
				toReturn.setResponseText("Failed to fetch current playing song due to an Exception!");
			}
			return toReturn;
		}
	}


	//Youtube video data
	public static class YouTube {
		/**
		 * Fetches the title, author, and duration of a linked YouTube video.
		 *
		 * @param fullURL The URL to the video.
		 * @return The appropriate response.
		 */
		public static Response getVideoData(String fullURL) {
			Response toReturn = new Response();
			try {
				String ID = "";
				Pattern p = null;
				if (fullURL.contains("youtu.be/")) {
					p = Pattern.compile("youtu\\.be/([^&\\?/]+)");
				} else if (fullURL.contains("watch?v=")) {
					p = Pattern.compile("v=([^&\\?/]+)");
				} else if (fullURL.contains("/embed/")) {
					p = Pattern.compile("youtube\\.com/embed/([^&\\?/]+)");
				}
				if (p == null) {
					toReturn.setResponseText("Could not read YouTube URL!");
					return toReturn;
				}
				Matcher m = p.matcher(fullURL);
				if (m.find()) {
					ID = m.group(1);
				}
				URL request = new URL("https://www.googleapis.com/youtube/v3/videos?id=" + ID +
						"&part=snippet,contentDetails&key=" + GUIMain.currentSettings.youTubeKey +
						"&fields=items(snippet(title,channelTitle),contentDetails(duration))");
				BufferedReader br = new BufferedReader(new InputStreamReader(request.openStream()));
				StringBuilder sb = new StringBuilder();
				Utils.parseBufferedReader(br, sb, false);
				JSONObject initial = new JSONObject(sb.toString());
				JSONArray items = initial.getJSONArray("items");
				if (items.length() < 1) {
					toReturn.setResponseText("Failed to parse YouTube video! Perhaps a bad ID?");
					return toReturn;
				}
				JSONObject juicyDetails = items.getJSONObject(0);
				JSONObject titleAndChannel = juicyDetails.getJSONObject("snippet");
				JSONObject duration = juicyDetails.getJSONObject("contentDetails");
				String title = titleAndChannel.getString("title");
				String channelName = titleAndChannel.getString("channelTitle");
				Duration d = Duration.parse(duration.getString("duration"));
				String time = getTimeString(d);
				toReturn.setResponseText("Linked YouTube Video: \"" + title + "\" by " + channelName + " [" + time + "]");
				toReturn.wasSuccessful();
			} catch (Exception e) {
				toReturn.setResponseText("Failed to parse YouTube video due to an Exception!");
			}
			return toReturn;
		}

		private static String getTimeString(Duration d) {
			int s = (int) d.getSeconds();
			int hours = s / 3600;
			int minutes = (s % 3600) / 60;
			int seconds = (s % 60);
			if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
			else return String.format("%02d:%02d", minutes, seconds);
		}
	}

	//URL Un-shortening
	public static class UnshortenIt {

		/**
		 * Fetches the domain of the shortened URL's un-shortened destination.
		 *
		 * @param shortenedURL The shortened URL string.
		 * @return The appropriate response.
		 */
		public static Response getUnshortened(String shortenedURL) {
			String key = GUIMain.currentSettings.unshortenitKey;
			Response toReturn = new Response();
			if (key.equals("")) return toReturn;
			toReturn.setResponseText("Failed to un-shorten URL! Click with caution!");
			try {
				//                URL request = new URL("https://therealurl.appspot.com/?url=" + shortenedURL);
				URL request = new URL("http://api.unshorten.it/?shortURL=" + shortenedURL + "&apiKey=" + key);
				URLConnection connection = request.openConnection();
				connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
				connection.connect();
				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = br.readLine();
				br.close();
				if (line != null) {
					if (line.startsWith("error (")){
						String error = line.substring(7, 8);
						switch (error) {
						case "0":
							GUIMain.log("unshorten.it API Error: URL passed incorrectly");
							break;
						case "1":
							GUIMain.log("unshorten.it API Error: invalid responseFormat parameter in API call");
							break;
						case "2":
							GUIMain.log("unshorten.it API Error: invalid return parameter in API call");
							break;
							// case 3 basically means the service couldn't unshorten the link. we just pass back the default message
						case "4":
							GUIMain.log("unshorten.it API Error: Invalid API Key");
							break;
						}
						return toReturn;
					}
					if (!line.equals(shortenedURL)) {
						String host = getHost(line);
						toReturn.setResponseText("Linked Shortened URL directs to: " + host + " !" + line);
						toReturn.wasSuccessful();
					} else {
						toReturn.setResponseText("Invalid shortened URL!");
					}
				}
			} catch (Exception ignored) {
				GUIMain.log(ignored.getMessage());
			}
			return toReturn;
		}

		private static String getHost(String webURL) {
			String toReturn = webURL;
			try {
				URL url = new URL(webURL);
				toReturn = url.getHost();
			} catch (Exception ignored) {
			}
			return toReturn;
		}
	}

	//Twitter tweet text
	public static class Twitter{

		/**
		 * Credit to Martyr2
		 * http://www.coderslexicon.com/demo-of-twitter-application-only-oauth-authentication-using-java/
		 */
		private static String key;
		private static String secret;
		private static final String oathEndpointURL = "https://api.twitter.com/oauth2/token";
		private static final String tweetEndpointURL = "https://api.twitter.com/1.1/statuses/show.json";
		private static String bearerToken;
		// https://twitter.com/Jodenstone/status/740016789648605184


		static {
			key = GUIMain.currentSettings.twitterKey;
			secret = GUIMain.currentSettings.twitterSecret;
		}
		/**
		 * Fetches the text of a tweet. Read only implementation for now
		 * 
		 * @param tweetLink the link to the tweet
		 * @return The text of the tweet
		 */
		public static Response getTweetText(String tweetLink){

			Response toReturn = new Response();
			toReturn.setResponseText("Failed to load tweet");

			if (key.equals("") || secret.equals("")) return toReturn;

			String ID = "";
			Pattern p = null;
			if (tweetLink.contains("/status/")){
				p = Pattern.compile("/status/([^&\\?/]+)");
			}
			if (p == null){
				toReturn.setResponseText("Could not read twitter URL!");
				return toReturn;
			}

			Matcher m = p.matcher(tweetLink);
			if (m.find()){
				ID = m.group(1);
			}
			try{
				String s1 = tweetEndpointURL + "?id=" + ID;
				String s2 = fetchTimelineTweet(s1);
				JSONObject obj = new JSONObject(s2);
				if (obj != null){
					String tweet = obj.getString("text");
					JSONObject user = obj.getJSONObject("user");
					String userName = user.getString("screen_name");
					toReturn.setResponseText("Link to tweet: \"" + tweet + "\" by " + userName);
					toReturn.wasSuccessful();
				}

			} catch (Exception e){
				GUIMain.log(e);
			}

			return toReturn;
		}

		private static String encodeKeys(String key, String secret){
			try{
				String encodedKey = URLEncoder.encode(key, "UTF-8");
				String encodedSecret = URLEncoder.encode(secret, "UTF-8");

				String fullKey = encodedKey + ":" + encodedSecret;
				byte[] encodedBytes = Base64.getEncoder().encode(fullKey.getBytes());
				return new String(encodedBytes);
			} catch (Exception e) {
				return new String();
			}

		}

		private static String requestBearerToken(String endpointURL) throws IOException{
			HttpsURLConnection connection = null;
			String encodedCredentials = encodeKeys(key, secret);

			try{
				URL url = new URL(endpointURL);
				connection = (HttpsURLConnection) url.openConnection();
				connection.setDoOutput(true);
				connection.setDoInput(true); 
				connection.setRequestMethod("POST"); 
				connection.setRequestProperty("Host", "api.twitter.com");
				connection.setRequestProperty("User-Agent", "palehorsbot");
				connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
				connection.setRequestProperty("Content-Length", "29");
				//				connection.setRequestProperty("Accept-Encoding", "gzip");
				connection.setUseCaches(false);

				writeRequest(connection, "grant_type=client_credentials");

				String s3 = readResponse(connection);

				JSONObject obj = new JSONObject(s3); 

				if (obj != null){
					String tokenType = (String) obj.get("token_type");
					String token = (String)obj.get("access_token");

					return ((tokenType.equals("bearer")) && (token != null)) ? token : "";
				} 
				return new String();


			} catch (Exception e){
				throw new IOException("Invalid endpoint URL specified.", e);
			}
			finally{
				if (connection != null){
					connection.disconnect();
				}
			}
		}

		private static String fetchTimelineTweet(String endpointURL) throws IOException {
			HttpsURLConnection connection = null;


			try{
				URL url = new URL(endpointURL);
				if (bearerToken == null || "".equals(bearerToken)){
					bearerToken = requestBearerToken(oathEndpointURL);
				}
				connection = (HttpsURLConnection) url.openConnection();
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setRequestMethod("GET");
				connection.setRequestProperty("Host", "api.twitter.com");
				connection.setRequestProperty("User-Agent", "palehorsbot");
				connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
				connection.setUseCaches(false);

				return readResponse(connection);

				//    			return new String();
			} catch (Exception e) {
				throw new IOException("Invalid endpoint URL specified.", e);
			}
			finally {
				if (connection != null){
					connection.disconnect();
				}
			}
		}

		private static boolean writeRequest(HttpsURLConnection connection, String textBody) {
			try {
				BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
				wr.write(textBody);
				wr.flush();
				wr.close();

				return true;
			}
			catch (IOException e) { return false; }
		}

		private static String readResponse(HttpsURLConnection connection) {
			try {
				StringBuilder str = new StringBuilder();

				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = "";
				while((line = br.readLine()) != null) {
					str.append(line + System.getProperty("line.separator"));
				}
				return str.toString();
			}
			catch (IOException e) { return new String(); }
		}

	}

	public static class SpeedRun{
		//OOT Game ID j1l9qz1g
		//SMB1 Game ID om1m3625

		private static HashMap<String, String> aliasMap = new HashMap<String, String>() {
			{
				put("oot", "oot");
				put("ocarina", "oot");
				put("ocarina of time", "oot");
				put("the legend of zelda: ocarina of time", "oot");
				put("the legend of zelda ocarina of time", "oot");
				put("j1l9qz1g", "oot");

				put("smb1", "smb1");
				put("mario 1", "smb1");
				put("super mario 1", "smb1");
				put("super mario brothers 1", "smb1");
				put("super mario bros 1", "smb1");
				put("super mario bros. 1", "smb1");
				put("super mario bros", "smb1");
				put("om1m3625", "smb1");

				put("codbo2", "blops2");
				put("call of duty black ops 2", "blops2");
				put("cod black ops 2", "blops2");
			};
		};
		private static HashMap<String, String> gameIDMap = new HashMap<String, String>() {
			{
				put("oot", "j1l9qz1g");
				put("smb1", "om1m3625");
				put("blops2", "m1mx2462");
			};
		};

		private static HashMap<String, String> gameNameMap = new HashMap<String, String>() {
			{
				put("oot", "Ocarina of Time");
				put("smb1", "Super Mario Bros. 1");
				put("blops2", "Call of Duty Black Ops II");

			};
		};

		public enum Category {
			ANY ("oot", "z275w5k0", "&ANY&"),
			HUNDO ("oot", "q255jw2o", "&100&"),
			MST ("oot", "jdrwr0k6", "&MST&"),
			AD ("oot", "zdnoz72q", "&AD&"),
			NOWW ("oot", "xd1wj828", "&NOWW&"),
			GLITCHLESS("oot", "zd35jnkn", "&GLITCHLESS&"),
			NOIMWW ("oot", "9d85yqdn", "&NOIMWW&"),
			SMB1 ("smb1", "w20p0zkn","&SMB1&");

			private String gameID;
			private String catID;
			private final String trigger;

			Category(String gameID, String catID, String trigger){
				this.gameID = gameID;
				this.catID = catID;
				this.trigger = trigger;
			}

			private String getID(){return catID; }
			private String getGameKey(){return gameID; }
			public String getTrigger(){return trigger;}
		}

		public static String getWorldRecordByCategory(Category category){
			String toReturn = "";
			String URI = "http://www.speedrun.com/api/v1/leaderboards/" + gameIDMap.get(category.getGameKey()) + "/category/" + category.getID() + "?top=1";
			String[] details = getDetailsFromJSONData(getJSONFromURI(URI));
			toReturn = "The WR is " + details[0] + " by " + details[1] + ".";
			return toReturn;
		}

		private static JSONObject getJSONFromURI(String URI){
			try{
				URL url = new URL(URI);
				String line = Utils.createAndParseBufferedReader(url.openStream());
				if (line != null){
					return new JSONObject(line);
				}
			} catch (Exception e){

			}
			return null;
		}

		private static String getRuntimeFromDouble(double d){
			String toReturn = "";
			DecimalFormat df = new DecimalFormat(".###");
			int primarySec = (int) d;
			double remainder = d - primarySec;
			LocalTime duration = LocalTime.ofSecondOfDay(primarySec);
			toReturn = duration.toString();
			if (remainder > 0) toReturn = toReturn.concat(df.format(remainder));
			return toReturn;
		}

		private static String getUsernameFromURL(String URI){
			String toReturn = "";
			if (URI.contains("/guests/")) {
				String userName = URI.substring(URI.indexOf("/guests/") + 8);
				if (userName.contains("%28"))
					userName = userName.substring(userName.indexOf("%28") + 3, userName.indexOf("%29"));
				return userName;
			}
			try{
				URL playerURL = new URL(URI);
				String line = Utils.createAndParseBufferedReader(playerURL.openStream());
				JSONObject player = new JSONObject(line);
				toReturn = player.getJSONObject("data").getJSONObject("names").getString("international");
			} catch (Exception e) {
				GUIMain.log("Unable to get WR player from " + URI);
				GUIMain.log(e);
			}

			return toReturn;
		}

		public static StringArray replaceWithWR(StringArray source){
			//Here we assume that the message part that needs replacing is at index 0 only
			StringArray toReturn = new StringArray(source.data);
			for (Category cat : Category.values()){
				if (source.data[0].contains(cat.getTrigger())){
					toReturn.data[0] = source.data[0].replace(cat.getTrigger(), getWorldRecordByCategory(cat));
				}
			}

			return toReturn;
		}

		private static String[] getDetailsFromJSONData(JSONObject data){
			String[] toReturn = new String[9];
			String delim = "";
			StringBuilder sb = new StringBuilder();
			if (data.getJSONObject("data").has("runs")){

				for (int i = 0; i<data.getJSONObject("data").getJSONArray("runs").length(); i++){
					sb.append(delim).append(getUsernameFromURL(data.getJSONObject("data").getJSONArray("runs").getJSONObject(i).getJSONObject("run").getJSONArray("players").getJSONObject(0).getString("uri")));
					delim = ", ";
				}
				toReturn[1] = sb.toString();
				toReturn[0] = getRuntimeFromDouble(data.getJSONObject("data").getJSONArray("runs").getJSONObject(0).getJSONObject("run").getJSONObject("times").getDouble("primary_t"));
				toReturn[4] = data.getJSONObject("data").getJSONArray("runs").getJSONObject(0).getJSONObject("run").getString("category");
				toReturn[3] = getCategoryNameFromID(toReturn[4]);
			} else {
				for (int i = 0; i<data.getJSONObject("data").getJSONArray("players").length(); i++){
					sb.append(delim).append(getUsernameFromURL(data.getJSONObject("data").getJSONArray("players").getJSONObject(i).getString("uri")));
					delim = ", ";
				}
				toReturn[0] = getRuntimeFromDouble(data.getJSONObject("data").getJSONObject("times").getDouble("primary_t"));
				toReturn[1] = sb.toString();
				toReturn[4] = data.getJSONObject("data").getString("category");
				toReturn[3] = getCategoryNameFromID(toReturn[4]);
			}
			toReturn[2] = getGameNameFromID(data.getJSONObject("data").getString("game"));
			toReturn[5] = data.getJSONObject("data").getString("game");
			toReturn[8] = "";
			return toReturn;
		}

		private static String getGameNameFromID(String gameID){
			String URI = "http://www.speedrun.com/api/v1/games/" + gameID;
			JSONObject gameJ = getJSONFromURI(URI);
			if (gameJ != null) return gameJ.getJSONObject("data").getJSONObject("names").getString("international");
			return null;
		}

		private static String getCategoryNameFromID(String categoryID){
			String URI = "http://www.speedrun.com/api/v1/categories/" + categoryID;
			JSONObject categoryJ = getJSONFromURI(URI);
			if (categoryJ != null) return categoryJ.getJSONObject("data").getString("name");
			return null;
		}

		private static String[] getWorldRecord(String game, String category, String vars){
			String toReturn[] = null;
			
			boolean multi = false;
			String apiBase = "http://www.speedrun.com/api/v1/";
			String apiLB = "leaderboards/%GAME%/category/%CAT%?top=1";
			String apiRecs = "http://www.speedrun.com/api_records.php?game=%GAME%";
			String apiVars = "categories/%CAT%/variables";
			String apiLBVarsAppend = "&var-%ID%=%VAR%";
			try {
				URL url = new URL(apiBase + apiLB.replace("%GAME%", game).replace("%CAT%", category));
				BufferedReader br;
				try{
					br = new BufferedReader(new InputStreamReader(url.openStream()));
				} catch (Exception e) {
					url = new URL(apiRecs.replace("%GAME%", game));
					br = new BufferedReader(new InputStreamReader(url.openStream()));
					multi = true;
				}
				StringBuilder sb = new StringBuilder();
				Utils.parseBufferedReader(br, sb, false);
				if (sb.toString().equals("{}")) return null;
				if (!multi){
					toReturn =  getDetailsFromJSONData(new JSONObject(sb.toString()));
				} else {
					JSONObject first = new JSONObject(sb.toString());
					JSONObject multiJ = first.getJSONObject(first.keys().next());
					Iterator<?> keys = multiJ.keys();
					String URI = "";
					double high = 0.0, current = 0.0;
					while (keys.hasNext()){
						String key = (String) keys.next();
						current = Utils.compareStrings(key, category);//Utils.fuzzyScore(key, category);
						if (current > high){
							high = current;
							URI = multiJ.getJSONObject(key).getJSONObject("links").getString("api");
						}
					}
					if (!"".equals(URI)){
						toReturn = getDetailsFromJSONData(getJSONFromURI(URI));
					}
				}

				if (vars != null){
					String label = "";
					toReturn[6] = toReturn[7]= toReturn[8] = "";
					try{
						String line = Utils.createAndParseBufferedReader(apiBase + apiVars.replace("%CAT%", toReturn[4]));
						JSONObject varsJ = new JSONObject(line);
						
						for (int i = 0; i < varsJ.getJSONArray("data").length(); i++) {
							if (varsJ.getJSONArray("data").getJSONObject(i).getString("category").equalsIgnoreCase(toReturn[4])) {
								toReturn[6] = varsJ.getJSONArray("data").getJSONObject(0).getString("id");
							} else {
								continue;
							}
							
							double high = 0.0, current = 0.0;
							
							JSONArray valsA = varsJ.getJSONArray("data").getJSONObject(i).getJSONObject("values").getJSONObject("values").names();
							for (int j = 0; j < valsA.length(); j++){
								label = varsJ.getJSONArray("data").getJSONObject(i).getJSONObject("values").getJSONObject("values").getJSONObject(valsA.getString(j)).getString("label");
								current = Utils.compareStrings(label, vars);
								if (current > high) {
									high = current;
									toReturn[7] = valsA.getString(j);
									toReturn[8] = " - " + label;
								}
							}
							break;
						}
						
						if ( !toReturn[6].equals("") && !toReturn[7].equals("")){							
							try{
								label = toReturn[8];
								url = new URL(apiBase + apiLB.replace("%GAME%", toReturn[5]).replace("%CAT%",toReturn[4]) + apiLBVarsAppend.replace("%ID%", toReturn[6]).replace("%VAR%", toReturn[7]));
								br = new BufferedReader(new InputStreamReader(url.openStream()));
								sb = new StringBuilder();
								Utils.parseBufferedReader(br, sb, false);
								if (sb.toString().equals("{}")) return toReturn;
								toReturn = getDetailsFromJSONData(new JSONObject(sb.toString()));
								toReturn[8] = label;
							} catch (Exception e) {
								GUIMain.log(e);
							}
							
							
						}
					} catch (Exception e) {
						GUIMain.log(e);
					}
				}

			} catch (Exception e) {
				GUIMain.log(e);
			}

			return toReturn;
		}

		public static Response processWRRequest(Channel ch, String request){
			Response toReturn = new Response();
			String game, cat, name, vars = null;

			if (request.trim().length() <= 3){
				game = ch.getGameID();
				if (game == null) {
					name = game = Twitch.getGameOfStream(ch.getName());
					if ("".equals(game)) {
						toReturn.setResponseText("Cannot fetch WR. Set your game in Twitch, or try !play <game>!");
						return toReturn;
					}
				} else {
					name = getGameName(game);
				}
				cat = ch.hasCategory() ? ch.getGameCategory() : "any";
			} else {
				String params = request.trim().substring(request.indexOf(' '));
				int slash = params.indexOf('/');
				if (params.contains("/")) {
					//if (slash > 0){
					String split[] = params.split("/");
					//					game = params.substring(0, slash).trim().toLowerCase();
					game = split[0].trim().toLowerCase();
					//					cat = params.substring(slash + 1).trim().toLowerCase();
					cat = split[1].trim().toLowerCase();
					if (split.length > 2) vars = split[2].trim().toLowerCase();

				} else {
					game = params.trim().toLowerCase();
					cat = ch.hasCategory() ? ch.getGameCategory() : "any";
				}
				name = game;
			}

			toReturn.setResponseText("Usage: !wr || !wr <game> / <Optional:category>");

			String details[] = getWorldRecord(game, cat, vars);
			if (details != null){
				toReturn.setResponseText("The WR for "  + details[2] + " (" + details[3] + details[8] + ") is " + details[0] + " by " + details[1] + ".");
				toReturn.wasSuccessful();
			} else {
				toReturn.setResponseText("Unable to find WR for " + game + "(" + cat + ").");
			}

			return toReturn;
		}

		public static String getGameID(String game){
			String gameID;
			String gameKey = aliasMap.get(game);
			gameID = gameIDMap.get(gameKey);
			return gameID;
		}

		public static String getGameName(String game){
			String gameName;
			String gameKey = aliasMap.get(game);
			gameName = gameNameMap.get(gameKey);
			return gameName;

		}
	}
}