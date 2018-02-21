package irc;

import face.Face;
import face.FaceManager;
import gui.forms.GUIMain;
import irc.account.OAuth;
import irc.account.Task;
import irc.message.Message;
import irc.message.MessageHandler;
import irc.message.MessageQueue;
import lib.pircbot.Channel;
import lib.pircbot.PircBot;
import lib.pircbot.User;
import sound.Sound;
import sound.SoundEngine;
import thread.ThreadEngine;
import util.APIRequests;
import util.Response;
import util.StringArray;
import util.Utils;
import util.comm.Command;
import util.comm.ConsoleCommand;
import util.misc.Raffle;
import util.misc.Vote;
import util.misc.Race;
import util.settings.Settings;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class IRCBot extends MessageHandler {

    public PircBot getBot() {
        return GUIMain.currentSettings.accountManager.getBot();
    }

    public ArrayList<String> winners;
    public ArrayList<Raffle> raffles;

    private static Vote poll;
    private static Race race;
    private long lastAd;
    private int initialConnections = 0;

    public IRCBot() {
        raffles = new ArrayList<>();
        winners = new ArrayList<>();
        poll = null;
        lastAd = -1;
    }

    @Override
    public void onConnect() {
    	getBot().log("Connecting to " + GUIMain.channelSet.size() + " channels");
        GUIMain.channelSet.forEach(this::doConnect);
        GUIMain.updateTitle(null);
        GUIMain.log(getBot().getNick() + " connected to " + initialConnections + " channels");
    }
    
    @Override
    public void onBanned(String line){
    	String[] parts = line.split(" ");
    	StringBuilder sb = new StringBuilder();
    	sb.append(getBot().getNick()).append(" has been ");
    	for (int i = 6; i < parts.length; i++){
    		sb.append(parts[i]);
    		sb.append(" ");
    	}
    	MessageQueue.addMessage(new Message(sb.toString(), Message.MessageType.BAN_NOTIFY).setChannel(parts[3].substring(1)));
    }

    public void doConnect(String channel) {
    	getBot().log("Trying to connect to " + channel);
    	String channelName = channel.startsWith("#") ? channel : "#" + channel;
        GUIMain.currentSettings.accountManager.addTask(
                new Task(GUIMain.currentSettings.accountManager.getBot(), Task.Type.JOIN_CHANNEL, channelName));
        initialConnections++;
    }

    /**
     * Leaves a channel and if specified, removes the channel from the
     * channel list.
     *
     * @param channel The channel name to leave (# not included).
     */
    public void doLeave(String channel) {
        if (!channel.startsWith("#")) channel = "#" + channel;
        GUIMain.currentSettings.accountManager.addTask(new Task(getBot(), Task.Type.LEAVE_CHANNEL, channel));
    }

    /**
     * Disconnects from all chats and disposes of the bot.
     *
     * @param forget True if you are logging out, false if shutting down.
     */
    public void close(boolean forget) {
        GUIMain.log("Logging out of bot: " + GUIMain.currentSettings.accountManager.getBotAccount().getName());
        GUIMain.currentSettings.accountManager.addTask(new Task(getBot(), Task.Type.DISCONNECT, null));
        if (forget) {
            GUIMain.currentSettings.accountManager.setBotAccount(null);
        }
        GUIMain.bot = null;
    }

    public void onDisconnect() {
    	if (!GUIMain.shutDown && getBot() != null) {
            if (GUIMain.currentSettings.autoReconnectAccounts)
                GUIMain.currentSettings.accountManager.createReconnectThread(getBot().getConnection());
            else {
            	GUIMain.logCurrent("Auto-reconnects disabled, please check Preferences -> Auto-Reconnect!");
            }
        }
    }

    @Override
    public void onMessage(String channel, String sender, String message) {
    	
        if (message != null && channel != null && sender != null && GUIMain.currentSettings.accountManager.getViewer() != null) {
            String botnakUserName = GUIMain.currentSettings.accountManager.getUserAccount().getName();
            sender = sender.toLowerCase();
            Channel ch = GUIMain.currentSettings.channelManager.getChannel(channel);
            //Races - have to grab users' messages even if bot not set to reply to them
            if (ch.getRace() != null && !ch.getRace().votingDone()){
            	ch.getRace().addGuess(sender, message);
            }
            if (!channel.contains(botnakUserName.toLowerCase())) {//in other channels
                int replyType = GUIMain.currentSettings.botReplyType;
                if (replyType == 0) return;
                //0 = reply to nobody (just spectate), 1 = reply to just the Botnak user, 2 = reply to everyone
                if (replyType == 1 && !sender.equalsIgnoreCase(botnakUserName)) return;
            }

            boolean senderIsBot = sender.equalsIgnoreCase(getBot().getNick());
            boolean userIsBot = botnakUserName.equalsIgnoreCase(GUIMain.currentSettings.accountManager.getBotAccount().getName());
            //if the sender of the message is the bot, but
            //the user account is NOT the bot, just return, we don't want the bot to trigger anything
            if (senderIsBot && !userIsBot) return;

            //raffles
            User u = GUIMain.currentSettings.channelManager.getUser(sender, true);
            if (!ch.getRaffles().isEmpty()) {
                if (!ch.getWinners().contains(u.getNick().toLowerCase())) {
                    for (Raffle r : ch.getRaffles()) {
                        if (r.isDone()) {
                            continue;
                        }
                        String key = r.getKeyword();
                        if (message.contains(key)) {
                            int permBase = r.getPermission();
                            int permission = Utils.getUserPermission(u, channel);
                            if (permission >= permBase) {
                                r.addUser(u.getLowerNick());
                            }
                        }
                    }
                }
                ArrayList<Raffle> toRemove = new ArrayList<>();
                ch.getRaffles().stream().filter(Raffle::isDone).forEach(r -> {
                    ch.getWinners().add(r.getWinner());
                    toRemove.add(r);
                });
                if (!toRemove.isEmpty()) {
                    toRemove.forEach(ch.getRaffles()::remove);
                    toRemove.clear();
                }
            }

            OAuth key = GUIMain.currentSettings.accountManager.getUserAccount().getKey();
            String[] split = message.split(" ");

            //URL Checking
            ThreadEngine.submit(() -> {
                int count = 0;
                ArrayList<String> splitList = new ArrayList<String>(Arrays.asList(split));
                Response linkResponse = null;
                for (int i = 0; i < splitList.size(); i++){
                	String part = splitList.get(i);
                    if (count > 1) break;//only allowing 2 requests here; don't want spam
                    if (part.startsWith("http") || part.startsWith("www") || part.startsWith("https")) {
                        if (part.contains("youtu.be") || part.contains("youtube.com/watch")
                                || part.contains("youtube.com/v") || part.contains("youtube.com/embed/")) {
                        	linkResponse = APIRequests.YouTube.getVideoData(part);
                            count++;
                        } else if (part.contains("bit.ly") || part.contains("tinyurl") || part.contains("goo.gl") || part.contains("t.co/")) {
                        	linkResponse = APIRequests.UnshortenIt.getUnshortened(part);
                        	if (linkResponse.isSuccessful()){
                        		String fullURL = linkResponse.getResponseText().substring(linkResponse.getResponseText().indexOf('!') + 1);
                        		linkResponse.setResponseText(linkResponse.getResponseText().substring(0, linkResponse.getResponseText().indexOf(fullURL)));
                        		splitList.add(fullURL);
                        	}
                            count++;
                        } else if (part.contains("twitch.tv/")) {
                            if (part.contains("/v/") || part.contains("/c/") || part.contains("/b/") || part.contains("/videos/")) {
                                linkResponse = APIRequests.Twitch.getTitleOfVOD(part);
                                count++;
                            }
                            if (part.contains("clips.twitch.tv/")){
                            	linkResponse = APIRequests.Twitch.getTwitchClipInfo(part);
                            	count++;
                            }
                        } else if (part.contains("twitter.com")) {
                        	if (part.contains("/status/")){
                        		linkResponse = APIRequests.Twitter.getTweetText(part);
                        		count++;
                        	}
                        }
                        
                    }
                }
                if (linkResponse != null && !"".equals(linkResponse.getResponseText())){
                	if (GUIMain.currentSettings.botWhisperMode && linkResponse.isWhisperable()){
                		linkResponse.setResponseText("/w " + "Palehors68" + " " + linkResponse.getResponseText());
                	}
                    getBot().sendMessage(channel, linkResponse.getResponseText());
                }
            });
            
            if (message.equals("!test") && sender.equalsIgnoreCase("palehors68")){
            	Response testResponse = APIRequests.Twitch.getFollowAge("torje", sender);
            	getBot().sendMessage(channel, testResponse.getResponseText());
            	
            }
            
            String first = "";
            if (split.length > 1) first = split[1];
            //commands
            //TODO turn all sendMessages into commandResponses
            if (message.startsWith("!")) {
                String trigger = message.substring(1).split(" ")[0].toLowerCase();
                String mess = message.substring(1);
                //sound
                if (SoundEngine.getEngine().soundTrigger(trigger, sender, channel)) {
                    SoundEngine.getEngine().playSound(new Sound(SoundEngine.getEngine().getSoundMap().get(trigger)));
                }
                ConsoleCommand consoleCommand = Utils.getConsoleCommand(trigger, channel, u);
                if (consoleCommand != null) {
                    Response commandResponse = null;
                    switch (consoleCommand.getAction()) {
                        case ADD_FACE:
                            commandResponse = FaceManager.handleFace(mess);
                            if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveFaces();
                            commandResponse.canWhisper();
                            break;
                        case CHANGE_FACE:
                            commandResponse = FaceManager.handleFace(mess);
                            if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveFaces();
                            commandResponse.canWhisper();
                            break;
                        case REMOVE_FACE:
                            commandResponse = FaceManager.removeFace(first);
                            if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveFaces();
                            commandResponse.canWhisper();
                            break;
                        case TOGGLE_FACE:
                            commandResponse = FaceManager.toggleFace(first);
                            break;
                        case ADD_SOUND:
                            commandResponse = SoundEngine.getEngine().handleSound(mess, false);
                            break;
                        case CHANGE_SOUND:
                            commandResponse = SoundEngine.getEngine().handleSound(mess, true);
                            break;
                        case REMOVE_SOUND:
                            commandResponse = SoundEngine.getEngine().removeSound(first);
                            break;
                        case SET_SOUND_DELAY:
                            commandResponse = SoundEngine.getEngine().setSoundDelay(first);
                            break;
                        case TOGGLE_SOUND:
                            boolean individualSound = split.length > 1;
                            commandResponse = SoundEngine.getEngine().toggleSound(individualSound ? first : null, individualSound);
                            break;
                        case STOP_SOUND:
                            commandResponse = SoundEngine.getEngine().stopSound(false);
                            break;
                        case STOP_ALL_SOUNDS:
                            commandResponse = SoundEngine.getEngine().stopSound(true);
                            break;
                        case SEE_SOUND_STATE:
                            commandResponse = SoundEngine.getEngine().getSoundState(first);
                            break;
                        case ADD_KEYWORD:
                            commandResponse = Utils.handleKeyword(mess);
                            if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveKeywords();
                            break;
                        case REMOVE_KEYWORD:
                            commandResponse = Utils.handleKeyword(mess);
                            if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveKeywords();
                            break;
                        case ADD_QUOTE:
                        	commandResponse = Utils.handleQuote(ch, mess);
//                        	if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveQuotes();
                        	break;   
                        case REMOVE_QUOTE:
                        	commandResponse = Utils.handleQuote(ch, mess);
                        	break;
                        case REMOVE_ALL_QUOTES:
                        	commandResponse = Utils.handleQuote(ch, mess);
//                        	if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveQuotes();
                        	break; 
                        case GET_QUOTE:
                        	commandResponse = Utils.handleQuote(ch, mess);
                        	break;
                        case SET_USER_COL:
                            commandResponse = Utils.handleColor(sender, mess, u.getColor());
                            if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveUserColors();
                            commandResponse.canWhisper();
                            break;
                        case SET_COMMAND_PERMISSION:
                            commandResponse = Utils.setCommandPermission(mess, ch);
                            break;
                        case ADD_TEXT_COMMAND:
                        	if (trigger.contains("edit")) {
                        		commandResponse = Utils.addCommands(mess, true, ch);
                        	} else {
                        		commandResponse = Utils.addCommands(mess, false, ch);
                        	}
//                            if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveCommands();
                            break;
                        case REMOVE_TEXT_COMMAND:
                            commandResponse = Utils.removeCommands(first, ch);
//                            if (commandResponse.isSuccessful()) GUIMain.currentSettings.saveCommands();
                            break;
                        case THROTTLE:
                        	commandResponse = new Response("Usage: !throttle {textCommand} {time(1-60s)}");
                        	if (split.length < 3) break;
                        	String commTrigger = first.startsWith("!") ? first.substring(1) : first;
                        	Command comm = Utils.getCommand(commTrigger, ch);
                        	if (comm != null){
                        		int seconds;
                        		try{
                        			seconds = Integer.parseInt(split[2]);
                        		} catch (Exception e){
                        			
                        			break;
                        		}
                        		
                        		comm.setDelayTimer(seconds);
                        		commandResponse.setResponseText("Successfully throttled " + commTrigger + " to " + seconds + " seconds.");
                        		
                        		
                        	}
                        	
                        	break;
                        case THROTTLEBOT:
                        	commandResponse = new Response("Bot is currently throttled to " + (ch.getChannelTimer().period / 1000) + " seconds.");
                        	int seconds;
                        	try{
                    			seconds = Integer.parseInt(first);
                    		} catch (Exception e){
                    			
                    			break;
                    		}
                        	if (seconds < 0) break;
                        	ch.setChannelTimer(seconds);
                        	if (seconds > 1000) seconds = seconds / 1000;
                        	commandResponse.setResponseText("Successfully throttled bot to " + seconds + " seconds.");
                        	break;
                        case ADD_DONATION:
                            commandResponse = GUIMain.currentSettings.donationManager.parseDonation(split);
                            break;
                        case SET_SUB_SOUND:
                            if (GUIMain.currentSettings.loadSubSounds()) {
//                                getBot().sendMessage(channel, "Reloaded sub sounds!");
                            	commandResponse = new Response("Reloaded sub sounds!", true);
                            }
                            break;
                        case SET_SOUND_PERMISSION:
                            commandResponse = SoundEngine.getEngine().setSoundPermission(first);
                            break; 
                        case SET_NAME_FACE:
                            if (first.startsWith("http")) {
                                commandResponse = FaceManager.downloadFace(first,
                                        GUIMain.currentSettings.nameFaceDir.getAbsolutePath(),
                                        Utils.setExtension(sender, ".png"), sender, FaceManager.FACE_TYPE.NAME_FACE);
                            }
                            break;
                        case REMOVE_NAME_FACE:
                            if (FaceManager.nameFaceMap.containsKey(sender)) {
                                try {
                                    Face f = FaceManager.nameFaceMap.remove(sender);
                                    if (f != null && new File(f.getFilePath()).delete())
//                                        getBot().sendMessage(channel, "Removed face for user: " + sender + " !");
                                    	commandResponse = new Response("Removed face for user: " + sender + " !", true);
                                } catch (Exception e) {
//                                    getBot().sendMessage(channel, "Name face for user " + sender +
//                                            " could not be removed due to an exception!");
                                	commandResponse = new Response("Name face for user " + sender + " could not be removed due to an exception!");
                                }
                            } else {
//                                getBot().sendMessage(channel, "The user " + sender + " has no name face!");
                            	commandResponse = new Response("The user " + sender + " has no name face!", true);
                            }
                            break;
                        case SET_STREAM_TITLE:
                            commandResponse = APIRequests.Twitch.setStreamStatus(key, channel, message, true);
                            break;
                        case SEE_STREAM_TITLE:
                            String title = APIRequests.Twitch.getTitleOfStream(channel);
                            if (!"".equals(title)) {
//                                getBot().sendMessage(channel, "The title of the stream is: " + title);
                                commandResponse = new Response("The title of the stream is: " + title,true);
                                commandResponse.addSenderToResponseText(sender);
                                commandResponse.canWhisper();
                            }
                            break;
                        case SEE_STREAM_GAME:
                            String game = APIRequests.Twitch.getGameOfStream(channel);
                            if ("".equals(game)) {
//                                getBot().sendMessage(channel, "The streamer is currently not playing a game!");
                                commandResponse = new Response("The streamer is currently not playing a game!", true);
                            } else {
//                                getBot().sendMessage(channel, "The current game is: " + game);
                                commandResponse = new Response("The current game is: " + game, true);
                                commandResponse.addSenderToResponseText(sender);
                            }
                            commandResponse.canWhisper();
                            break;
                        case SET_STREAM_GAME:
                            commandResponse = APIRequests.Twitch.setStreamStatus(key, channel, message, false);
                            break;
                        case PLAY_ADVERT:
                            if (key != null) {
                                if (key.canPlayAd()) {
                                    int length = Utils.getTime(first);
                                    if (length == -1) length = 30;
                                    if (APIRequests.Twitch.playAdvert(key.getKey(), channel, length)) {
//                                        getBot().sendMessage(channel, "Playing an ad for " + length + " seconds!");
                                        commandResponse = new Response("Playing an ad for " + length + " seconds!", true);
                                        lastAd = System.currentTimeMillis();
                                    } else {
//                                        getBot().sendMessage(channel, "Error playing an ad!");
                                        commandResponse = new Response("Error playing an ad!");
                                        long diff = System.currentTimeMillis() - lastAd;
                                        if (lastAd > 0 && (diff < 480000)) {
                                            SimpleDateFormat sdf = new SimpleDateFormat("m:ss");
                                            Date d = new Date(diff);
                                            Date toPlay = new Date(480000 - diff);
//                                            getBot().sendMessage(channel, "Last ad was was only " + sdf.format(d)
//                                                    + " ago! You must wait " + sdf.format(toPlay) + " to play another ad!");
                                            commandResponse.setResponseText(commandResponse.getResponseText() + " Last ad was was only " + sdf.format(d)
                                            		+ " ago! You must wait " + sdf.format(toPlay) + " to play another ad!");
                                        }
                                    }
                                } else {
//                                    getBot().sendMessage(channel, "This OAuth key cannot play an advertisement!");
                                    commandResponse = new Response("This OAuth key cannot play an advertisement!");
                                }
                            }
                            break;
                        case START_RAFFLE:
                            if (split.length > 2) {
                                String timeString = split[2];
                                int time = Utils.getTime(timeString);
                                if (time < 1) {
//                                    getBot().sendMessage(channel, "Failed to start raffle, usage: !startraffle (key) (time) (permission?)");
                                    commandResponse = new Response("Failed to start raffle, usage: !startraffle (key) (time) (permission?)");
                                    break;
                                }
                                int perm = 0;//TODO select a parameter in Settings GUI that defines the default raffle
                                if (split.length == 4) {
                                    //because right now it's just "Everyone" unless specified with the int param
                                    try {
                                        perm = Integer.parseInt(split[3]);
                                    } catch (Exception ignored) {//default to the specified value
                                    }
                                }
                                Raffle r = new Raffle(getBot(), first, time, channel, perm);
                                r.start();
                                ch.getRaffles().add(r);
                                //print the blarb
                                getBot().sendMessage(channel, r.getStartMessage());
                                getBot().sendMessage(channel, "NOTE: This is a promotion from " + channel.substring(1) +
                                        ". Twitch does not sponsor or endorse broadcaster promotions and is not responsible for them.");
                            } else {
//                                getBot().sendMessage(channel, "Failed to start raffle, usage: !startraffle (key) (time) (permission?)");
                            	commandResponse = new Response("Failed to start raffle, usage: !startraffle (key) (time) (permission?)");
                            }
                            break;
                        case ADD_RAFFLE_WINNER:
                            if (!ch.getWinners().contains(first)) {
                                ch.getWinners().add(first);
//                                getBot().sendMessage(channel, "The user " + first + " has been added to the winners pool!");
                                commandResponse = new Response("The user " + first + " has been added to the winners pool!", true);
                            } else {
//                                getBot().sendMessage(channel, "The user " + first + " is already in the winners pool!");
                                commandResponse = new Response("The user " + first + " is already in the winners pool!", true);
                            }
                            break;
                        case STOP_RAFFLE:
                            Raffle toRemove = null;
                            for (Raffle r : ch.getRaffles()) {
                                if (r.getKeyword().equalsIgnoreCase(first)) {
                                    r.setDone(true);
                                    r.interrupt();
                                    toRemove = r;
//                                    getBot().sendMessage(channel, "The raffle with key " + first + " has been stopped!");
                                    commandResponse = new Response("The raffle with key " + first + " has been stopped!",true);
                                    break;
                                }
                            }
                            if (toRemove != null) {
                                ch.getRaffles().remove(toRemove);
                            } else {
//                                getBot().sendMessage(channel, "There is no such raffle \"" + first + "\" !");
                                commandResponse = new Response("There is no such raffle \"" + first + "\" !");
                            }
                            break;
                        case REMOVE_RAFFLE_WINNER:
                            if (ch.getWinners().contains(first)) {
                                if (ch.getWinners().remove(first)) {
//                                    getBot().sendMessage(channel, "The user " + first + " was removed from the winners pool!");
                                    commandResponse = new Response("The user " + first + " was removed from the winners pool!",true);
                                }
                            } else {
//                                getBot().sendMessage(channel, "The user " + first + " is not in the winners pool!");
                                commandResponse = new Response("The user " + first + " is not in the winners pool!",true);
                            }
                            break;
                        case SEE_WINNERS:
                            if (!ch.getWinners().isEmpty() && ch.getWinners().size() > 0) {
                                StringBuilder stanSB = new StringBuilder();
                                stanSB.append("The current raffle winners are: ");
                                for (String name : ch.getWinners()) {
                                    stanSB.append(name);
                                    stanSB.append(", ");
                                }
//                                getBot().sendMessage(channel, stanSB.toString().substring(0, stanSB.length() - 2) + " .");
                                commandResponse = new Response(stanSB.toString().substring(0, stanSB.length() - 2) + " .", true);
                            } else {
//                                getBot().sendMessage(channel, "There are no recorded winners!");
                                commandResponse = new Response("There are no recorded winners!", true);
                            }
                            break;
                        case START_POLL:
                            if (ch.getPoll() != null) {
                                if (ch.getPoll().isDone()) {
                                    createPoll(channel, message);
                                } else {
//                                    getBot().sendMessage(channel, "Cannot start a poll with one currently running!");
                                    commandResponse = new Response("Cannot start a poll with one currently running!");
                                }
                            } else {
                                createPoll(channel, message);
                            }
                            break;
                        case POLL_RESULT:
                            if (ch.getPoll() != null) {
                            	ch.getPoll().printResults();
                            } else {
//                                getBot().sendMessage(channel, "There never was a poll!");
                                commandResponse = new Response("There never was a poll!", true);
                            }
                            break;
                        case CANCEL_POLL:
                            if (ch.getPoll() != null) {
                                if (ch.getPoll().isDone()) {
//                                    getBot().sendMessage(channel, "The poll is already finished!");
                                    commandResponse = new Response("The poll is already finished!", true);
                                } else {
                                	ch.getPoll().interrupt();
//                                    getBot().sendMessage(channel, "The poll has been stopped.");
                                    commandResponse = new Response("The poll has been stopped.", true);
                                }
                            } else {
//                                getBot().sendMessage(channel, "There is no current poll!");
                                commandResponse = new Response("There is no current poll!", true);
                            }
                            break;
                        case VOTE_POLL:
                            if (ch.getPoll() != null) {
                                if (!ch.getPoll().isDone()) {
                                    int option;
                                    try {
                                        option = Integer.parseInt(first);
                                    } catch (Exception e) {
                                        break;
                                    }
                                    ch.getPoll().addVote(sender, option);
                                }
                            }
                            break;
                        case DAMPE_RACE:
                        	if (ch.getRace() != null) {
                        		if (ch.getRace().isDone()){
                        			startRace(channel, message);
                        		} else {
//                        			getBot().sendMessage(channel, "The race is currently underway!");
                        			commandResponse = new Response("The race is currently underway!", true);
                        		}
                        	} else {
                        		startRace(channel, message);
                        	}
                        	break;
                        case JUDGE_RACE:
                        	commandResponse = new Response();
                        	if (ch.getRace() != null){
                        		if (ch.getRace().votingDone()) {
	                        		if (first != ""){
	                        			int result;
	                        			try {
	                        				result = Integer.parseInt(first);
	                        			} catch (Exception e) {
//	                        				getBot().sendMessage(channel, "You must enter the final race time. Ex: !judgerace 47");
	                        				commandResponse.setResponseText("You must enter the final race time. Ex: !judgerace 47");
	                        				break;
	                        			}
	                        			ch.getRace().interrupt();
	                        			ch.getRace().judgeRace(first);
//	                        			race = null;
	                        		} else {
//	                        			getBot().sendMessage(channel, "You must enter the final race time. Ex: !judgerace 47");
	                        			commandResponse.setResponseText("You must enter the final race time. Ex: !judgerace 47");
	                        		}
                        		} else {
//                        			getBot().sendMessage(channel, "Please wait until voting has finished before judging the race!");
                        			commandResponse.setResponseText("Please wait until voting has finished before judging the race!");
                        		}
                        	} else {
//                        		getBot().sendMessage(channel, "There isn't currently a race to judge. Come back later!");
                        		commandResponse.setResponseText("There isn't currently a race to judge. Come back later!");
                        	}
                        	break;
                        case NOW_PLAYING:
                            commandResponse = APIRequests.LastFM.getCurrentlyPlaying();
                            commandResponse.canWhisper();
                            break;
                        case SHOW_UPTIME:
                            commandResponse = APIRequests.Twitch.getUptimeString(channel.substring(1));
                            commandResponse.addSenderToResponseText(sender);
//                            commandResponse.canWhisper();
                            break;
                        case SEE_PREV_SOUND_DON:
                            //TODO if currentSettings.seePreviousDonEnable
                            if (GUIMain.currentSettings.loadedDonationSounds){
                                commandResponse = SoundEngine.getEngine().getLastDonationSound();
                                commandResponse.canWhisper();
                            }
                            break;
                        case SEE_PREV_SOUND_SUB:
                            //TODO if currentSettings.seePreviousSubEnable
                            if (GUIMain.currentSettings.loadedSubSounds) {
                                commandResponse = SoundEngine.getEngine().getLastSubSound();
                                commandResponse.canWhisper();
                            }
                            break;
                        case SEE_OR_SET_REPLY_TYPE:
                            commandResponse = parseReplyType(first, botnakUserName);
                            commandResponse.canWhisper();
                            break;
                        case SEE_OR_SET_VOLUME:
                        	commandResponse = new Response();
                            if (first == null || first.equals("")) {
//                                getBot().sendMessage(channel, "Volume is " + String.format("%.1f", GUIMain.currentSettings.soundVolumeGain));
                                commandResponse.setResponseText("Volume is " + String.format("%.1f", GUIMain.currentSettings.soundVolumeGain));
                                commandResponse.wasSuccessful();
                                commandResponse.canWhisper();
                            } else {
                                Float volume = Float.parseFloat(first);
                                if (volume > 100F)
                                    volume = 100F;
                                else if (volume < 0F)
                                    volume = 0F;
                                GUIMain.currentSettings.soundVolumeGain = volume;
//                                getBot().sendMessage(channel, "Volume set to " + String.format("%.1f", GUIMain.currentSettings.soundVolumeGain));
                                commandResponse.setResponseText("Volume set to " + String.format("%.1f", GUIMain.currentSettings.soundVolumeGain));
                                commandResponse.wasSuccessful();
                                commandResponse.canWhisper();
                            }
                            break;
                        case WHISPER:
                        	GUIMain.currentSettings.botWhisperMode = true;
                        	GUIMain.instance.setWhisperModeToggle();
                        	commandResponse = new Response("Bot set to Whisper reply mode", true);
                        	break;
                        case TALK:
                        	GUIMain.currentSettings.botWhisperMode = false;
                        	GUIMain.instance.setWhisperModeToggle();
                        	commandResponse = new Response("Bot set to Out Loud reply mode", true);
                        	break;
                        case WR:
                        	commandResponse = APIRequests.SpeedRun.processWRRequest(ch, message);
                        	commandResponse.addSenderToResponseText(sender);
                        	break;
                        case HOST_USER:
                        	if (split.length < 2){
                        		commandResponse = new Response("Usage: !host <twitch user> <optional:raid message>");
                        		break;
                        	}
                        	String twitchUser = split[1];
                        	if (!APIRequests.Twitch.isChannelLive(twitchUser)) {
                        		commandResponse = new Response(twitchUser + " is not live right now!");
                        		break;
                        	}
                        	String raid = "";
                        	if (split.length > 2){ //There is a raid message
                        		raid = mess.substring(mess.indexOf(twitchUser) + twitchUser.length()).trim();
                        	}
                        	
                        	int messageCount = 1;
                        	if (IAmAModOf(channel)) { 
                        		messageCount = 10;
                        		getBot().setMessageDelay(100);
                        	}else {
                        		getBot().setMessageDelay(1500);
                        	}
                        	for (int i = 1; i <= messageCount; i++){
                        		String hostMessage = "http://www.twitch.tv/" + twitchUser;
                        		if (!"".equals(raid)) hostMessage = hostMessage + " RAID: " + raid;
                        		getBot().sendMessage(channel, hostMessage);
                        	}
                        	
                        	break;
                        case SET_GAME:
                        	if (!"".equals(first)){
                        		commandResponse = ch.setGame(mess);
                        	}
                        	break;
                        case CLEAR_GAME:
                        	commandResponse = ch.clearGame();
                        	break;
                        case CAT:
                        	commandResponse = new Response();
                        	if ("".equals(first) || first == null){
                        		if(ch.hasCategory()){
                        			commandResponse.setResponseText("The current category is " + ch.getGameCategory() + "!");
                        		} else {
                        			commandResponse.setResponseText("No category is currently set!");
                        		}
                        	} else {
                        		String cat = message.substring(5);
                        		if (cat.equalsIgnoreCase("none")){
                        			ch.clearCategory();
                        		} else {
                        			ch.setGameCategory(cat);
                        		}
                        		commandResponse.setResponseText("The current category has been changed to " + cat + "!");
                        	}
                        	break;
                        case HELP:
                        	commandResponse = new Response("Use \"!help <command>\" for command usage! Parameters: {} are mandatory, <> are optional.");
                        	if (!"".equals(first)){
                        		if (first.startsWith("!")) first = first.substring(1);
                        		try{
                        		  commandResponse.setResponseText("@" + u.getDisplayName() + ": " + Utils.getConsoleCommand(first, channel, u).getHelpText());
                        		} catch (NullPointerException e) {
                        			commandResponse.setResponseText("Command !" + first + " does not exist!");
                        		}
                        	}
                        	
                        	break;
                        case FOLLOWAGE:
                        	commandResponse = APIRequests.Twitch.getFollowAge(channel, sender);
                        	commandResponse.addSenderToResponseText(sender);
                        	break;
                        default:
                            break;

                    }

                    if (commandResponse != null && !"".equals(commandResponse.getResponseText())
                    		&& (!ch.getChannelTimer().isRunning() || trigger.contains("throttle"))){
                    	if (GUIMain.currentSettings.botWhisperMode && commandResponse.isWhisperable()){
                    		commandResponse.setResponseText("/w " + sender + " " + commandResponse.getResponseText());
                    	}
                        getBot().sendMessage(channel, commandResponse.getResponseText());
                    }
                }
//                if (message.equals("!wwtest")){
//                	Response commandResponse = new Response(APIRequests.SpeedRun.getWorldRecordByCategory(APIRequests.SpeedRun.Category.ANY));
//                	commandResponse.setResponseText(commandResponse.getResponseText() + '\t');
//                	commandResponse.setResponseText(commandResponse.getResponseText() + APIRequests.SpeedRun.getWorldRecordByCategory(APIRequests.SpeedRun.Category.NOWW));
//                	getBot().sendMessage(channel, commandResponse.getResponseText());
//                }
                //text command
                Command c = Utils.getCommand(trigger, ch);
                //we check the senderIsBot here because we want to be able to call console commands,
                //but we don't want the bot to trigger its own text commands, which
                //could infinite loop (two commands calling each other over and over)
                if (c != null && !senderIsBot && c.getMessage().data.length > 0 && !c.getDelayTimer().isRunning() && !ch.getChannelTimer().isRunning()) {
                    StringArray sa = c.getMessage();
                    if (c.hasArguments()) {
                        //build arguments if it has any
                        int argAmount = c.countArguments();
                        if ((split.length - 1) < argAmount) {
                            getBot().sendMessage(channel, "Missing command arguments! Command format: " + c.printCommand());
                            return;
                        }
                        String[] definedArguments = new String[argAmount];
                        System.arraycopy(split, 1, definedArguments, 0, argAmount);
                        sa = c.buildMessage(sa, definedArguments);
                    }
                    //check for WR replacement
//                    sa = APIRequests.SpeedRun.replaceWithWR(sa);
                    
                    //send the message
                    for (String s : sa.data) {
                        getBot().sendMessage(channel, s);
                    }
                    c.getDelayTimer().reset();
                    ch.getChannelTimer().reset();
                } 
            }
        }
    }
    
    @Override
    public void onAction(final String sender, final String channel, final String action) {
    	
    }

    //!startpoll time options
    private void createPoll(String channel, String message) {
        if (message.contains("]")) {//because what's the point of a poll with one option?
        	Channel ch = GUIMain.currentSettings.channelManager.getChannel(channel);
        	if (ch == null) return;
            int first = message.indexOf(" ") + 1;
            int second = message.indexOf(" ", first) + 1;
            String[] split = message.split(" ");
            int time = Utils.getTime(split[1]);
            if (time > 0) {
                ch.setPoll(new Vote(channel, time, message.substring(second).split("\\]")));
                ch.getPoll().start();
            }
        }
    }
    
    //!damperace time
    private void startRace(String channel, String message){
    	int time = 20;
    	int first = message.indexOf(" ") + 1;
    	Channel ch = GUIMain.currentSettings.channelManager.getChannel(channel);
    	if (ch == null) return;
    	String[] split = message.split(" ");
    	if (first > 0 && first <= 60){
    		time = Utils.getTime(split[1]);    		
    	}
    	
    	if (time > 0) {
    		ch.setRace(new Race(channel, time));
    		ch.getRace().start();
    	}
    }

    private Response parseReplyType(String first, String botnakUser) {
        Response toReturn = new Response();
        try {
            if (!"".equals(first)) {
                int perm = Integer.parseInt(first);
                if (perm > 2) perm = 2;
                else if (perm < 0) perm = 0;
                GUIMain.currentSettings.botReplyType = perm;
                GUIMain.instance.setBotReplyRadioButton();
                toReturn.setResponseText("Successfully changed the bot reply type (for other channels) to: " + getReplyType(perm, botnakUser));
            } else {
                toReturn.setResponseText("Current bot reply type for other channels is: " +
                        getReplyType(GUIMain.currentSettings.botReplyType, botnakUser));
            }
        } catch (Exception ignored) {
            toReturn.setResponseText("Failed to set bot reply type due to an exception!");
        }
        return toReturn;
    }

    private String getReplyType(int perm, String botnakUser) {
        if (perm > 1) {
            return "Reply to everybody (" + perm + ")";
        } else if (perm > 0) {
            return "Reply to just " + botnakUser + " (" + perm + ")";
        } else {
            return "Reply to nobody (" + perm + ")";
        }
    }
    
    private boolean IAmAModOf(String channel){
    	return GUIMain.currentSettings.channelManager.getChannel(channel).isMod(getBot().getNick());
    }
}