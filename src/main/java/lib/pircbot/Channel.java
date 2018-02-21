package lib.pircbot;

import gui.forms.GUIMain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;
import java.util.stream.Collectors;


import thread.ThreadEngine;
import util.APIRequests;
import util.Timer;
import util.Utils;
import util.misc.Race;
import util.misc.Raffle;
import util.misc.Vote;
import util.comm.Command;
import util.comm.ConsoleCommand;
import util.Response;

/**
 * Created by Nick on 12/22/13.
 */
public class Channel {

    private CopyOnWriteArraySet<String> mods;
    private ConcurrentHashMap<String, Integer> subscribers;
    private CopyOnWriteArraySet<Command> commandSet;
    private CopyOnWriteArraySet<ConsoleCommand> conCommands;
    private ConcurrentHashMap<String, Integer> cheers;
    private String name;
    private Race race;
	private ArrayList<Raffle> raffles;
	private ArrayList<String> winners;
	private Vote poll;
	private ArrayList<String> quotes;
	private File channelDir;
	private File quotesFile;
	private File commandsFile;
	private File ccommandsFile;
	private File localSettingsFile;
	private File badgeDir;
	private long messageDelay;
	private Timer globalTimer;
	private String gameID;
	private String gameCategory;
    
    
    
    

	/**
     * Constructs a channel object of the given name.
     *
     * @param name The name to assign to the channel (includes the hashtag).
     */
    public Channel(String name) {
        this.name = name;
        this.mods = new CopyOnWriteArraySet<>();
        this.subscribers = new ConcurrentHashMap<>();
        this.quotes = new ArrayList<String>();
        this.cheers = new ConcurrentHashMap<>();
        this.raffles = new ArrayList<>();
        this.winners = new ArrayList<>();
        this.conCommands = new CopyOnWriteArraySet<>();
        this.commandSet = new CopyOnWriteArraySet<>();
        globalTimer = new Timer(100);
        channelDir = new File(GUIMain.currentSettings.defaultDir + File.separator + "Channels" + File.separator + name.substring(1).toLowerCase());
		quotesFile = new File(channelDir + File.separator + "quotes.txt");
		commandsFile = new File(channelDir + File.separator + "commands.txt");
		ccommandsFile = new File(channelDir + File.separator + "chatcom.txt");
		localSettingsFile = new File(channelDir + File.separator + "settings.txt");
		badgeDir = new File(channelDir + File.separator + "badges");
		channelDir.mkdirs();
		badgeDir.mkdirs();
		setMessageDelay(1500); 
		load();
    }

    /**
     * Checks to see if a given user is a moderator of a channel.
     *
     * @param user The user to check.
     * @return True if the user is a mod, else false.
     */
    public boolean isMod(String user) {
        
    	if (user.equalsIgnoreCase(this.name.substring(1))){
    		return true; //Broadcaster
    	}
    	
    	for (String s : mods) {
            if (user.equalsIgnoreCase(s)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Checks to see if a given user is a subscriber to a channel.
     *
     * @param u The user to check.
     * @return True if the user is a subscriber, else false.
     */
    public int isSubscriber(User u) {
//        for (String s : subscribers) {
//            if (s.equals(u.getNick().toLowerCase())) {
//                return true;
//            }
//        }
//        return false;
    	int subLength = -1;
    	try {
    	subLength = subscribers.get(u.getNick().toLowerCase());
    	} catch (Exception e) {
//    	return subLength;
    	}
    	return subLength;
    	
    }

    /**
     * Gets the name of the channel.
     * INCLUDES THE HASHTAG!
     *
     * @return The name of the channel with the hashtag included.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the username of the channel.
     *
     * @return The name of the channel without the hashtag.
     */
    public String getUserName() {
        return name.substring(1);
    }
    
    public void setCheer(String user, int amount){
    	cheers.put(user, amount);
    }

    /**
     * Adds a multitude of mods to the mod list.
     *
     * @param mods The mod names to add.
     */
    public void addMods(String... mods) {
        Collections.addAll(this.mods, mods);
        if (isMod(GUIMain.bot.getBot().getNick())) setMessageDelay(100);
    }

    /**
     * Adds a subscriber name to the channel.
     *
     * @param sub The subscriber to add.
     */
    public void addSubscriber(String sub, Integer length) {
        subscribers.put(sub,length);
    }

    /**
     * Clear the channel of its mods and subscribers.
     */
    public void clear() {
        mods.clear();
        subscribers.clear();
        save();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Channel && ((Channel) obj).getName().equals(this.getName()));
    }
    
    private void load() {
		
		loadQuotes();
		loadCommands();
		loadConsoleCommands();
		loadSettings();
	}
	
	private void save() {
		
		Utils.saveQuotes(quotesFile, quotes);
		Utils.saveCommands(commandsFile, commandSet);
		Utils.saveConCommands(ccommandsFile, conCommands);
		saveSettings();
	}
	
	private void loadSettings(){
		if (Utils.areFilesGood(localSettingsFile.getAbsolutePath())){
			Properties p = new Properties();
			try {
				p.load(new FileInputStream(localSettingsFile));
				setGameCategory(p.getProperty("gameCategory").toLowerCase());
				gameID = p.getProperty("gameID");
				globalTimer = new Timer(Integer.parseInt(p.getProperty("globalTimer")));
			} catch (Exception e){
				GUIMain.log("Couldn't read settings for " + name + ": " + e.getMessage());
			}
		}
	}
	
	private void saveSettings() {
//		if (!Utils.areFilesGood(localSettingsFile.getAbsolutePath())) localSettingsFile.mkdirs();
		Properties p = new Properties();
		if (hasCategory()) p.put("gameCategory", gameCategory);
		if (hasGameID()) p.put("gameID", gameID);
		p.put("globalTimer", globalTimer.period + "");
		
		try {
			p.store(new FileWriter(localSettingsFile), "Local Settings");
		} catch (Exception e) {
			GUIMain.log(e);
		}
	}
	
	private void loadQuotes() {
		if (Utils.areFilesGood(quotesFile.getAbsolutePath())){
			try (BufferedReader br = new BufferedReader(new InputStreamReader(quotesFile.toURI().toURL().openStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					addQuote(line);
				}
			} catch (IOException e){
				GUIMain.log("Couldn't read quotes for " + name + ": " + e.getMessage());
			}
		}
	}
	
	
	public CopyOnWriteArraySet<Command> getCommandSet(){
		return this.commandSet;
	}
	
	private void loadCommands(){
		if (Utils.areFilesGood(commandsFile.getAbsolutePath())) {
            GUIMain.log("Loading " + name + "\'s text commands...");
            commandSet = Utils.loadCommands(commandsFile);
        }
		
	}
	
	private void loadConsoleCommands(){
		if (Utils.areFilesGood(ccommandsFile.getAbsolutePath())) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(ccommandsFile.toURI().toURL().openStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] split = line.split("\\[");
                    ConsoleCommand.Action a = Utils.getAction(split[1]);
                    int classPerm;
                    try {
                        classPerm = Integer.parseInt(split[2]);
                    } catch (Exception e) {
                        classPerm = -1;
                    }
                    String[] customUsers = null;
                    if (!split[3].equalsIgnoreCase("null")) {
                        customUsers = split[3].split(",");
                    }
                    conCommands.add(new ConsoleCommand(split[0], a, classPerm, customUsers));
                    
                }
                
                if (!conCommands.containsAll(GUIMain.conCommands)){
                	//This will add new hard coded console commands to the current channel commands
                	for (ConsoleCommand cc : GUIMain.conCommands){
                		if (!conCommands.contains(cc)) conCommands.add(cc);
                	}
                }
                
            } catch (Exception e) {
                GUIMain.log(e);
            }
        } else { //first time boot/reset/deleted file etc.
//            GUIMain.conCommands.addAll(hardcoded.stream().collect(Collectors.toList()));
        	this.conCommands.addAll(GUIMain.conCommands);
        }
	}
	
	/**
	 * @return the race
	 */
	public Race getRace() {
		return race;
	}
	/**
	 * @param race the race to set
	 */
	public void setRace(Race race) {
		this.race = race;
	}
	/**
	 * @return the raffles list
	 */
	public ArrayList<Raffle> getRaffles() {
		return raffles;
	}
	/**
	 * @return the raffles list
	 */
	public ArrayList<String> getWinners() {
		return winners;
	}

	/**
	 * @return the poll
	 */
	public Vote getPoll() {
		return poll;
	}
	public CopyOnWriteArraySet<ConsoleCommand> getConCommandSet(){
		return conCommands;
	}
	/**
	 * @param vote the vote to set
	 */
	public void setPoll(Vote poll) {
		this.poll = poll;
	}
	
	public void addQuote(String quote){
		quotes.add(quote);
	}
	
	public boolean removeQuote(int index){
		
		if (index < 1 || index > quotes.size()) return false;
		quotes.remove(index - 1);
		save();
		return true;
	}
	
	public void removeAllQuotes(){
		quotes.clear();
		save();
	}
	
	
	public String getQuote(int index){
		index--;
		String toReturn = "";
		if (index < 0) index = 0;
		if (index > quotes.size()) index = quotes.size();
		toReturn = quotes.get(index) + " [" + ++index + "/" + quotes.size() + "]";
		return toReturn;
	}
	
	public String getQuote(){
		if (quotes.isEmpty()) return "There are no quotes for " + getName();
		return getQuote(Utils.random(1, quotes.size() + 1));
	}

	/**
	 * @return the messageDelay
	 */
	public final long getMessageDelay() {
		return messageDelay;
	}

	/**
	 * @param messageDelay the messageDelay to set
	 */
	public void setMessageDelay(long messageDelay) {
		this.messageDelay = messageDelay;
	}
	
	public Timer getChannelTimer(){
    	return globalTimer;
    }
    
    public void setChannelTimer(int delay){
    	if (delay < 1000) delay = delay * 1000; //turn seconds into milliseconds
    	globalTimer = new Timer(delay);
    }
    
    public String getGameID(){
    	return gameID;
    }
    
    public boolean hasGameID(){
    	return getGameID() != null;
    }
    
    public Response setGame(String message){
    	Response toReturn = new Response();
    	String game = message.substring(5);
    	
//    	String newGameID = APIRequests.SpeedRun.getGameID(game.toLowerCase());
    	
//    	if ( newGameID == null || "".equals(newGameID)){
//    		toReturn.setResponseText("Unable to change game to " + game + "!");
//    	} else if ( newGameID != gameID ) {
//    		toReturn.setResponseText("Successfully changed game to " + APIRequests.SpeedRun.getGameName(game) + "!");
//    		gameID = newGameID;
//    		toReturn.wasSuccessful();
//    	}
    	
    	return toReturn;
    	
    }
    
    public Response clearGame() {
    	Response toReturn = new Response();
    	gameID = null;
    	toReturn.setResponseText("Successfully set the game to nothing!");
    	toReturn.wasSuccessful();
    	return toReturn;
    }
    
    public String getGameCategory() {
		return gameCategory;
	}

	public void setGameCategory(String gameCategory) {
		this.gameCategory = gameCategory;
	}
	
	public void clearCategory() {
		setGameCategory(null);
	}
	
	public boolean hasCategory()
	{
		return this.gameCategory != null;
	}
}