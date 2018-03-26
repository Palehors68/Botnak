package util;

import face.FaceManager;
import gui.forms.GUIMain;
import irc.account.OAuth;
import lib.JSON.JSONArray;
import lib.JSON.JSONObject;
import lib.pircbot.Channel;
import lib.pircbot.User;
import util.settings.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Nick on 5/22/2015.
 */
public class APIRequests {

    //Anything Twitch
    public static class Twitch {

        private static ConcurrentHashMap<String, String> usersIDMap = new ConcurrentHashMap<>();

        private static final String TWITCH_API = "https://api.twitch.tv/kraken";
        private static final String CLIENT_ID = "5xg0sgb6dymmmmbyqwt1zppij5xxpi";
        private static final String CLIENT_ID_PARAM = "client_id=" + CLIENT_ID;

        public enum TWITCH_API_REQUEST
        {
            CHAT_EMOTE_IMAGES_ALL("/chat/emoticon_images", "GET", false, false, false),
            CHAT_EMOTE_IMAGES_SET("/chat/emoticon_images?emotesets=%s", "GET", true, false, false),
            CHAT_GET_BADGES("/chat/%s/badges", "GET", true, false, false),
            CHANNELS_GET("/channels/%s", "GET", true, false, false),
            CHANNELS_GET_FOLLOWERS("/channels/%s/follows?limit=20", "GET", true, false, false),
            CHANNELS_PUT("/channels/%s", "PUT", true, true, true),
            CHANNELS_SHOW_COMMERCIAL("/channels/%s/commercial", "POST", true, true, true),
            CHANNELS_GET_COMMUNITIES("/channels/%s/communities", "GET", true, false, false),
            CHANNELS_SET_COMMUNITIES("/channels/%s/communities", "PUT", true, true, true),
            CHANNELS_DELETE_COMMUNITIES("/channels/%s/community", "DELETE", true, false, true),
            COMMUNITIES_GET_BY_NAME("/communities?name=%s", "GET", true, false, false),
            SEARCH_CHANNELS("/search/channels?limit=10&query=%s", "GET", true, false, false),
            STREAMS_GET("/streams/%s", "GET", true, false, false),
            STREAMS_GET_FOLLOWED("/streams/followed?limit=100", "GET", false, false, true),
            USERS_GET_BY_LOGIN("/users?login=%s", "GET", true, false, false),
            VIDEOS_GET("/videos/%s", "GET", true, false, false);

            String endpointURL, requestType;
            boolean hasURLInput, hasBodyData, needsAuthentication;

            TWITCH_API_REQUEST(String endpointURL, String requestType, boolean urlInput, boolean bodyData, boolean needsAuth)
            {
                this.endpointURL = endpointURL;
                this.requestType = requestType;
                this.hasURLInput = urlInput;
                this.hasBodyData = bodyData;
                this.needsAuthentication = needsAuth;
            }
        }

        private static class TwitchAPIRequestResponse
        {
            int responseCode = 404;
            String responseString = "";
        }

        private static TwitchAPIRequestResponse carryOutRequest(TWITCH_API_REQUEST request, String urlInput, String bodyData, String authKey)
        {
            TwitchAPIRequestResponse response = new TwitchAPIRequestResponse();
            try
            {
                String formattedURL = request.hasURLInput ? String.format(request.endpointURL, urlInput) : request.endpointURL;
                URL twitch = new URL(TWITCH_API + formattedURL);
                HttpURLConnection c = (HttpURLConnection) twitch.openConnection();
                c.setRequestMethod(request.requestType);

                c.setRequestProperty("Client-ID", CLIENT_ID); // Client ID
                c.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json"); // API Version
                c.setRequestProperty("Content-Type", "application/json");

                if (request.needsAuthentication)
                    c.setRequestProperty("Authorization", "OAuth " + authKey);

                if (request.hasBodyData)
                {
                    c.setDoOutput(true);
                    OutputStreamWriter op = new OutputStreamWriter(c.getOutputStream());
                    op.write(bodyData);
                    op.close();
                }

                response.responseString = Utils.createAndParseBufferedReader(c.getInputStream());
                response.responseCode = c.getResponseCode();

                c.disconnect();
            } catch (Exception e)
            {
                GUIMain.log("Failed to carry out Twitch API request " + request.name() + " due to Exception:");
                GUIMain.log(e);
            }

            return response;
        }

        /**
         * @param emotes The emoteset String that twitch provides in IRC.
         * @return The JSON string of the set of emotes, otherwise an empty string.
         */
        public static String getEmoteSet(String emotes)
        {
            return carryOutRequest(TWITCH_API_REQUEST.CHAT_EMOTE_IMAGES_SET, emotes, null, null).responseString;
        }

        /**
         * @return The JSON string of every Twitch emote.
         */
        public static String getAllEmotes()
        {
            return carryOutRequest(TWITCH_API_REQUEST.CHAT_EMOTE_IMAGES_ALL, null, null, null).responseString;
        }

        /**
         * Since Twitch is eventually going to deprecate v3, we need to store a user and their corresponding ID. This
         * method is in charge of obtaining said ID.
         *
         * @param channel The channel's name.
         * @return The String of the ID for the channel.
         */
        public static String getChannelID(String channel)
        {
            if (channel.contains("#"))
                channel = channel.replaceAll("#", "");

            if (usersIDMap.contains(channel))
                return usersIDMap.get(channel);


            String line = carryOutRequest(TWITCH_API_REQUEST.USERS_GET_BY_LOGIN, channel, null, null).responseString;
            if (!line.isEmpty())
            {
                JSONObject init = new JSONObject(line);
                if (init.getInt("_total") > 0)
                {
                    JSONArray users = init.getJSONArray("users");
                    JSONObject user = users.getJSONObject(0);
                    String ID = user.getString("_id");
                    usersIDMap.put(channel, ID);
                    return ID;
                }
            }

            return "";
        }

        /**
         * Fetches and downloads the given channel's subscriber icon.
         *
         * @param channel The channel to download the sub icon for.
         * @return The path to the downloaded sub icon if successful, otherwise null.
         */
        public static String getSubIcon(String channel)
        {
            String ID = getChannelID(channel);
            String line = carryOutRequest(TWITCH_API_REQUEST.CHAT_GET_BADGES, ID, null, null).responseString;
            if (!line.isEmpty())
            {
                JSONObject init = new JSONObject(line);
                if (init.has("subscriber"))
                {
                    JSONObject sub = init.getJSONObject("subscriber");
                    if (!sub.getString("image").equalsIgnoreCase("null"))
                    {
                        return FaceManager.downloadIcon(sub.getString("image"), channel);
                    }
                }
            }
            return null;
        }

        /**
         * Gets stream uptime.
         *
         * @return the current stream uptime.
         */
        public static Response getUptimeString(String channelName)
        {
            Response toReturn = new Response();
            String line = carryOutRequest(TWITCH_API_REQUEST.STREAMS_GET, getChannelID(channelName), null, null).responseString;
            if (!line.isEmpty())
            {
                JSONObject outer = new JSONObject(line);
                if (!outer.isNull("stream"))
                {
                    JSONObject stream = outer.getJSONObject("stream");
                    Instant started = Instant.parse(stream.getString("created_at"));
                    Duration duration = Duration.between(started, Instant.now());
                    int hours = (int) duration.abs().getSeconds() / 3600;
                    int mins = ((int) duration.abs().getSeconds() % 3600) / 60;
                    toReturn.wasSuccessful();
                    toReturn.setResponseText("The stream has been live for " +
                            ((hours > 0 ? (hours + " hours ") : "") + mins + " minutes."));
                } else
                {
                    toReturn.setResponseText("The stream is not live!");
                }
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
            String line = carryOutRequest(TWITCH_API_REQUEST.STREAMS_GET, getChannelID(channelName), null, null).responseString;
            if (!line.isEmpty())
            {
                JSONObject jsonObject = new JSONObject(line);
                isLive = !jsonObject.isNull("stream") && !jsonObject.getJSONObject("stream").isNull("preview");
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
            String line = carryOutRequest(TWITCH_API_REQUEST.STREAMS_GET, getChannelID(channelName), null, null).responseString;
            if (!line.isEmpty())
            {
                Matcher m = Constants.PATTERN_VIEWER_COUNT.matcher(line);
                if (m.find())
                {
                    try
                    {
                        count = Integer.parseInt(m.group(1));
                    } catch (Exception ignored)
                    {
                    }//bad Int parsing
                }
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
            String[] toRet = {"", ""};
            String line = carryOutRequest(TWITCH_API_REQUEST.CHANNELS_GET, getChannelID(channel), null, null).responseString;
            if (!line.isEmpty())
            {
                JSONObject base = new JSONObject(line);
                //these are never null, just blank strings at worst
                toRet[0] = base.getString("status");
                toRet[1] = base.getString("game");
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
        public static Response setStreamStatus(OAuth key, String channel, String message, boolean isTitle) {
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

            JSONObject outer = new JSONObject();
            JSONObject channelObject = new JSONObject();
            channelObject.put("status", title);
            channelObject.put("game", game);
            outer.put("channel", channelObject);

            if (carryOutRequest(TWITCH_API_REQUEST.CHANNELS_PUT, getChannelID(channel), outer.toString(), key.split(":")[1]).responseCode == 200)
                toReturn.wasSuccessful();

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
            length = Utils.capNumber(30, 180, length);//can't be longer than 3 mins/shorter than 30 sec
            if ((length % 30) != 0) length = 30;//has to be divisible by 30 seconds

            JSONObject lengthObject = new JSONObject();
            lengthObject.put("length", length);

            return (carryOutRequest(TWITCH_API_REQUEST.CHANNELS_SHOW_COMMERCIAL, getChannelID(channel), lengthObject.toString(), key.split(":")[1]).responseCode == 200);
        }

        /**
         * Obtains the title and author of a video on Twitch.
         *
         * @param URL The URL to the video.
         * @return The appropriate response.
         */
        public static Response getTitleOfVOD(String URL) {
            Response toReturn = new Response();

            String ID = "";
            Matcher m = Constants.PATTERN_TWITCH_VIDEO_URL.matcher(URL);
            if (m.find())
            {
                ID = m.group(1);
            }

            String line = carryOutRequest(TWITCH_API_REQUEST.VIDEOS_GET, ID, null, null).responseString;
            if (!line.isEmpty())
            {
                JSONObject init = new JSONObject(line);
                String title = init.getString("title");
                JSONObject channel = init.getJSONObject("channel");
                String author = channel.getString("display_name");
                toReturn.wasSuccessful();
                toReturn.setResponseText("Linked Twitch VOD: \"" + title + "\" by " + author);
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

            String line = carryOutRequest(TWITCH_API_REQUEST.STREAMS_GET_FOLLOWED, null, null, key).responseString;
            if (!line.isEmpty())
            {
                JSONObject init = new JSONObject(line);
                JSONArray streams = init.getJSONArray("streams");
                for (int i = 0; i < streams.length(); i++)
                {
                    JSONObject stream = streams.getJSONObject(i);
                    JSONObject channel = stream.getJSONObject("channel");
                    toReturn.add(channel.getString("name").toLowerCase());
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
                partial = URLEncoder.encode(partial, "UTF-8");
                String line = carryOutRequest(TWITCH_API_REQUEST.SEARCH_CHANNELS, partial, null, null).responseString;
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
        public static List<String> getLast20Followers(String channel)
        {
            List<String> toReturn = new ArrayList<>();

            String line = carryOutRequest(TWITCH_API_REQUEST.CHANNELS_GET_FOLLOWERS, getChannelID(channel), null, null).responseString;
            if (!line.isEmpty())
            {
                JSONObject init = new JSONObject(line);
                JSONArray follows = init.getJSONArray("follows");
                for (int i = 0; i < follows.length(); i++)
                {
                    JSONObject person = follows.getJSONObject(i);
                    JSONObject user = person.getJSONObject("user");
                    toReturn.add(user.getString("name"));
                }
            }
            return toReturn;
        }

        public static class Community
        {
            public String ID, name;

            public Community(String id, String name)
            {
                this.ID = id;
                this.name = name;
            }

            public String getID()
            {
                return ID;
            }
        }

        /**
         * @param communityName The name of the community to get
         * @return The Community object of the community, or null
         */
        public static Community getCommunity(String communityName)
        {

            String response = carryOutRequest(TWITCH_API_REQUEST.COMMUNITIES_GET_BY_NAME, communityName, null, null).responseString;
            if (!response.isEmpty())
            {
                JSONObject community = new JSONObject(response);
                return new Community(community.getString("_id"), community.getString("display_name"));
            }
            return null;
        }

        /**
         * Gets the given channel's communities
         *
         * @param channel The channel to get the communities of
         */
        public static List<Community> getChannelCommunities(String channel)
        {
            List<Community> communities = new ArrayList<>();

            String response = carryOutRequest(TWITCH_API_REQUEST.CHANNELS_GET_COMMUNITIES, getChannelID(channel), null, null).responseString;
            if (!response.isEmpty())
            {
                JSONObject outer = new JSONObject(response);
                JSONArray comms = outer.getJSONArray("communities");
                for (int i = 0; i < comms.length(); i++)
                {
                    JSONObject community = comms.getJSONObject(i);
                    communities.add(new Community(community.getString("_id"), community.getString("display_name")));
                }
            }
            return communities;
        }

        /**
         * @param channel     The channel to set the communities for
         * @param key         The OAuth key to use
         * @param communities The communities to set
         * @return A response object
         */
        public static Response setChannelCommunities(String channel, String key, List<Community> communities)
        {
            Response response = new Response();

            // Setting to empty == removing all of them
            if (communities.isEmpty())
                return removeChannelFromCommunities(channel, key);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("community_ids", communities.stream().map(Community::getID).collect(Collectors.toList()));

            if (carryOutRequest(TWITCH_API_REQUEST.CHANNELS_SET_COMMUNITIES, getChannelID(channel), jsonObject.toString(), key.split(":")[1]).responseCode == 204)
                response.wasSuccessful();

            return response;
        }

        /**
         * @param channel The channel to remove from its communities
         * @param key     The OAuth key to use
         * @return A response object
         */
        public static Response removeChannelFromCommunities(String channel, String key)
        {
            Response toReturn = new Response();

            if (carryOutRequest(TWITCH_API_REQUEST.CHANNELS_DELETE_COMMUNITIES, getChannelID(channel), null, key.split(":")[1]).responseCode == 204)
                toReturn.wasSuccessful();

            return toReturn;
        }
        
        public static Response getFollowAge(Channel ch, User u){
        	Response toReturn = new Response();
			HttpURLConnection connection;
			try{
				URL request = new URL("https://api.twitch.tv/helix/users/follows?from_id=" 
						+ u.getUserID() 
						+ "&to_id=" + getTwitchUserIDByLoginName(ch.getUserName()));
				connection = (HttpURLConnection) request.openConnection();
				connection.setRequestProperty("Client-ID", CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(connection.getInputStream());
				connection.disconnect();

				if (!line.isEmpty()){
					JSONObject init = new JSONObject(line);
					if (init.getJSONArray("data").length() > 0){
						String followDate = init.getJSONArray("data").getJSONObject(0).getString("followed_at");
						Instant instant = Instant.parse(followDate);
						Date mydate = Date.from(instant);
						SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
						int diff = 1 + ((int)(Duration.between(instant, Instant.now()).getSeconds()) / 86400);
						toReturn.setResponseText("You have followed " + ch.getUserName() + 
								" since " + formatter.format(mydate) +
								" (" + diff + " days).");
					} else {
						toReturn.setResponseText("You don't follow " + ch.getUserName() + "!");
					}
				}

			} catch (Exception e) {
				toReturn.setResponseText("Unable to lookup followage.");
			}
			return toReturn;
        }
        
        public static String getTwitchUserIDByLoginName(String login){
			HttpURLConnection connection;
			login = login.replace("#", "");
			try{
				//				URL request = new URL("https://api.twitch.tv/helix/users?login=" + login);
				URL request = new URL("https://api.twitch.tv/helix/users?login=" + login);
				connection = (HttpURLConnection) request.openConnection();
				connection.setRequestProperty("Client-ID", CLIENT_ID);
				String line = Utils.createAndParseBufferedReader(connection.getInputStream());
				if (!line.isEmpty()){
					JSONObject init = new JSONObject(line);
					if (init.getJSONArray("data").length() > 0){
						return init.getJSONArray("data").getJSONObject(0).getString("id");
					} else {
						return null;
					}
				}
				connection.disconnect();
			} catch (Exception e) {
				return null;
			}
			return null;
		}
        
        public static Response getTwitchClipInfo(String URL){
			Response toReturn = new Response();
			HttpURLConnection connection;
			try {
				String slug = "";
				Pattern p  = Pattern.compile("clips.twitch.tv/([^&\\?/]+)");
				Matcher m = p.matcher(URL);
				if (m.find()) {
					slug = m.group().split("/")[1];
				}
				URL request = new URL(TWITCH_API + "/clips/" + slug + "?" + CLIENT_ID_PARAM);
				connection= (HttpURLConnection) request.openConnection();
				connection.setRequestProperty("Accept", "application/vnd.twitchtv.v5+json");
				String line = Utils.createAndParseBufferedReader(connection.getInputStream());
				connection.disconnect();
				if (!line.isEmpty()){
					JSONObject init = new JSONObject(line);
					String title = init.getString("title");
					//					JSONObject channel = init.getJSONObject("channel");
					String author = init.getJSONObject("broadcaster").getString("display_name");
					String game = init.getString("game");
					toReturn.wasSuccessful();
					toReturn.setResponseText("Linked Twitch Clip: \"" + title + "\" (" + author + " playing " + game + ")");
				}



			} catch (Exception e) {
				toReturn.setResponseText("Failed to parse Twitch clip due to an Exception!");
			}



			return toReturn;
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
            if ("".equals(Settings.lastFMAccount.getValue())) {
                toReturn.setResponseText("Failed to fetch current playing song, the user has no last.fm account set!");
                return toReturn;
            }
            //TODO check the song requests engine to see if that is currently playing
            String tracks_url = "http://www.last.fm/user/" + Settings.lastFMAccount.getValue() + "/now";
            try {
                URL request = new URL("http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=" +
                        Settings.lastFMAccount.getValue() + "&api_key=e0d3467ebb54bb110787dd3d77705e1a&format=json");
                String line = Utils.createAndParseBufferedReader(request.openStream());
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
                GUIMain.log(e);
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
                Pattern p = null;
                if (fullURL.contains("youtu.be/")) {
                    p = Pattern.compile("youtu\\.be/([^&?/]+)");
                } else if (fullURL.contains("v=")) {
                    p = Pattern.compile("v=([^&?/]+)");
                } else if (fullURL.contains("/embed/")) {
                    p = Pattern.compile("youtube\\.com/embed/([^&?/]+)");
                }
                if (p == null) {
                    toReturn.setResponseText("Could not read YouTube URL!");
                    return toReturn;
                }
                Matcher m = p.matcher(fullURL);
                if (m.find()) {
                    String ID = m.group(1);
                    URL request = new URL("https://www.googleapis.com/youtube/v3/videos?id=" + ID +
                            "&part=snippet,contentDetails&key=AIzaSyDVKqwiK_VGelKlNCHtEFWFbDfVuzl9Q8c" +
                            "&fields=items(snippet(title,channelTitle),contentDetails(duration))");
                    String line = Utils.createAndParseBufferedReader(request.openStream());
                    if (!line.isEmpty()) {
                        JSONObject initial = new JSONObject(line);
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
                    }
                }
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
  			String key = Settings.unshortenitKey.getValue();
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
  			key = Settings.twitterKey.getValue();
  			secret = Settings.twitterSecret.getValue();
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


		private static String[] getDetailsFromJSONData(JSONObject data){
			String[] toReturn = new String[10];
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
				toReturn[9] = data.getJSONObject("data").getJSONArray("runs").getJSONObject(0).getJSONObject("run").getString("date");
				toReturn[3] = getCategoryNameFromID(toReturn[4]);
			} else {
				for (int i = 0; i<data.getJSONObject("data").getJSONArray("players").length(); i++){
					sb.append(delim).append(getUsernameFromURL(data.getJSONObject("data").getJSONArray("players").getJSONObject(i).getString("uri")));
					delim = ", ";
				}
				toReturn[0] = getRuntimeFromDouble(data.getJSONObject("data").getJSONObject("times").getDouble("primary_t"));
				toReturn[1] = sb.toString();
				toReturn[4] = data.getJSONObject("data").getString("category");
				toReturn[9] = data.getJSONObject("data").getString("date");
				toReturn[3] = getCategoryNameFromID(toReturn[4]);
			}
			toReturn[2] = getGameNameFromID(data.getJSONObject("data").getString("game"));
			toReturn[5] = data.getJSONObject("data").getString("game");
			toReturn[8] = "";
			return toReturn;
		}

		private static String getGameNameFromID(String gameID){
			String URI = "https://www.speedrun.com/api/v1/games/" + gameID;
			JSONObject gameJ = getJSONFromURI(URI);
			if (gameJ != null) return gameJ.getJSONObject("data").getJSONObject("names").getString("international");
			return null;
		}

		private static String getCategoryNameFromID(String categoryID){
			String URI = "https://www.speedrun.com/api/v1/categories/" + categoryID;
			JSONObject categoryJ = getJSONFromURI(URI);
			if (categoryJ != null) return categoryJ.getJSONObject("data").getString("name");
			return null;
		}

		private static String[] getWorldRecord(String game, String category, String vars){
			String toReturn[] = null;
			if (category.contains("/")) vars = category.split("/")[1].trim();
			boolean multi = false;
			String apiBase = "https://www.speedrun.com/api/v1/";
			String apiLB = "leaderboards/%GAME%/category/%CAT%?top=1";
			String apiRecs = "https://www.speedrun.com/api_records.php?game=%GAME%";
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
								toReturn[6] = varsJ.getJSONArray("data").getJSONObject(i).getString("id");
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
					//				} else {
					//					name = getGameName(game);
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
				if (details[0].startsWith("00:")) details[0] = details[0].substring(3);
				toReturn.setResponseText("The WR for "  + details[2] + " (" + details[3] + details[8] + ") is " + details[0] + " by " + details[1] + " achieved on " + details[9] + ".");
				toReturn.wasSuccessful();
			} else {
				toReturn.setResponseText("Unable to find WR for " + game + "(" + cat + ").");
			}

			return toReturn;
		}

		//		public static String getGameID(String game){
		//			String gameID;
		//			String gameKey = aliasMap.get(game);
		//			gameID = gameIDMap.get(gameKey);
		//			return gameID;
		//		}

		//		public static String getGameName(String game){
		//			String gameName;
		//			String gameKey = aliasMap.get(game);
		//			gameName = gameNameMap.get(gameKey);
		//			return gameName;
		//
		//		}
	}
}