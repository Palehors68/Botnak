package lib.pircbot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import gui.forms.GUIMain;
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
import util.settings.AbstractFileSave;
import util.settings.Settings;

/**
 * Created by Nick on 12/22/13.
 */
public class Channel {

    private CopyOnWriteArraySet<String> mods;
    private ConcurrentHashMap<Long, Integer> subscribers;
    private ConcurrentHashMap<Long, Integer> cheers;
    private CopyOnWriteArraySet<Command> commandSet;
    private CopyOnWriteArraySet<ConsoleCommand> conCommandSet;
    private ArrayList<String> quoteList;
    private Commands COMMANDS = new Commands();
    private ConCommands CONCOMMANDS = new ConCommands();
    private Quotes QUOTES = new Quotes();
    private ChannelSettings CHANNELSETTINGS = new ChannelSettings(); 
    private String name;
    private Race race;
	private ArrayList<Raffle> raffles;
	private ArrayList<String> winners;
	private Vote poll;
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
        this.cheers = new ConcurrentHashMap<>();
        this.quoteList = new ArrayList<String>();
        this.raffles = new ArrayList<>();
        this.winners = new ArrayList<>();
        this.conCommandSet = new CopyOnWriteArraySet<>();
        this.commandSet = new CopyOnWriteArraySet<>();
        globalTimer = new Timer(100);
        channelDir = new File(Settings.defaultDir + File.separator + "Channels" + File.separator + name.substring(1).toLowerCase());
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
//        return subscribers.contains(u.getUserID());
    	
    	int subLength = -1;
    	try {
    	subLength = subscribers.get(u.getUserID());
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
     * @param subID The subscriber to add.
     */
    public void addSubscriber(long subID, Integer length)
    {
        subscribers.put(subID, length);
    }

    /**
     *  Sets a user's cheer amount.
     * @param userID The user to set.
     * @param amount Their cheer amount.
     */
    public void setCheer(long userID, int amount)
    {
        cheers.put(userID, amount);
    }

    /**
     * Gets the cheer amount of bits this user has cheered, otherwise -1.
     *
     * @param userID The user in question.
     * @return The amount of bits this user has cheered, otherwise -1.
     */
    public int getCheer(long userID)
    {
        return cheers.getOrDefault(userID, -1);
    }

    /**
     * Clear the channel of its mods and subscribers.
     */
    public void clear() {
        mods.clear();
        subscribers.clear();
        cheers.clear();
        save();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Channel && ((Channel) obj).getName().equals(this.getName()));
    }
    
private void load() {
		
	QUOTES.load();
	COMMANDS.load();
	CONCOMMANDS.load();
	//TODO convert this to the abstract file writer, like used in Settings.java
		loadSettings();
	}
	
	private void save() {
		QUOTES.save();
		COMMANDS.save();
		CONCOMMANDS.save();
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
//		if (Utils.areFilesGood(commandsFile.getAbsolutePath())) {
//            GUIMain.log("Loading " + name + "\'s text commands...");
//            commandSet = Utils.loadCommands(commandsFile);
//        }
		
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
                    List<String> customUsers = null;
                    if (!split[3].equalsIgnoreCase("null")) {
                        customUsers = Arrays.asList(split[3].split(","));
                    }
                    conCommandSet.add(new ConsoleCommand(split[0], a, classPerm, customUsers));
                    
                }
                
                if (!conCommandSet.containsAll(GUIMain.conCommands)){
                	//This will add new hard coded console commands to the current channel commands
                	for (ConsoleCommand cc : GUIMain.conCommands){
                		if (!conCommandSet.contains(cc)) conCommandSet.add(cc);
                	}
                }
                
            } catch (Exception e) {
                GUIMain.log(e);
            }
        } else { //first time boot/reset/deleted file etc.
//            GUIMain.conCommands.addAll(hardcoded.stream().collect(Collectors.toList()));
        	this.conCommandSet.addAll(GUIMain.conCommands);
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
		return conCommandSet;
	}
	/**
	 * @param vote the vote to set
	 */
	public void setPoll(Vote poll) {
		this.poll = poll;
	}
	
	public void addQuote(String quote){
		quoteList.add(quote);
	}
	
	public boolean removeQuote(int index){
		
		if (index < 1 || index > quoteList.size()) return false;
		quoteList.remove(index - 1);
		save();
		return true;
	}
	
	public void removeAllQuotes(){
		quoteList.clear();
		save();
	}
	
	
	public String getQuote(int index){
		index--;
		String toReturn = "";
		if (index < 0) index = 0;
		if (index > quoteList.size()) index = quoteList.size();
		toReturn = quoteList.get(index) + " [" + ++index + "/" + quoteList.size() + "]";
		return toReturn;
	}
	
	public String getQuote(){
		if (quoteList.isEmpty()) return "There are no quotes for " + getName();
		return getQuote(Utils.random(1, quoteList.size() + 1));
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
    	
    	String newGameID = APIRequests.SpeedRun.getGameID(game.toLowerCase());
    	
    	if ( newGameID == null || "".equals(newGameID)){
    		toReturn.setResponseText("Unable to change game to " + game + "!");
    	} else if ( newGameID != gameID ) {
    		toReturn.setResponseText("Successfully changed game!");
    		gameID = newGameID;
    		toReturn.wasSuccessful();
    	}
    	
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
	
	private class Quotes extends AbstractFileSave {
		
		
		@Override
		public void handleLineLoad(String line) {
			quoteList.add(line);
		}

		@Override
		public void handleLineSave(PrintWriter pw) {
			quoteList.stream().forEach(pw::println);
		}

		@Override
		public File getFile() {
			return quotesFile;
		}
	}
	
private class Commands extends AbstractFileSave {
		

		
		@Override
		public void handleLineLoad(String line) {
//			commandSet.add(line);
			String[] split = line.split("\\[");
            String[] contents = split[2].split("\\]");
            int delay;
            try {
            	delay = Integer.parseInt(split[1]);
            	if (delay > 60000 || delay < 1000) delay = 5000;
            } catch (Exception e) {
            	delay = 5000;
            }
            Command c = new Command(split[0], delay, contents);
            if (split.length > 3) {
                c.addArguments(split[2].split(","));
            }
            commandSet.add(c);
		}

		@Override
		public void handleLineSave(PrintWriter pw) {
			//commandSet.stream().forEach(pw::println);
			for (Command next : commandSet){
				String name = next.getTrigger();
                String throttle = next.getDelayTimer().period + "";
                String[] contents = next.getMessage().data;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < contents.length; i++) {
                    sb.append(contents[i]);
                    if (i != (contents.length - 1)) sb.append("]");
                }
                pw.print(name + "[" + throttle + "[" + sb.toString());
                if (next.hasArguments()) {
                    pw.print("[");
                    for (int i = 0; i < next.countArguments(); i++) {
                        pw.print(next.getArguments().get(i));
                        if (i != (next.countArguments() - 1)) pw.print(",");
                    }
                }
                pw.println();
			}
		}

		@Override
		public File getFile() {
			return commandsFile;
		}
	}

private class ConCommands extends AbstractFileSave {
	
	
	@Override
	public void handleLineLoad(String line) {
//		conCommandSet.add(line);
	}

	@Override
	public void handleLineSave(PrintWriter pw) {
		conCommandSet.stream().forEach(pw::println);
	}

	@Override
	public File getFile() {
		return ccommandsFile;
	}
}

private class ChannelSettings extends AbstractFileSave {

	@Override
	public void handleLineLoad(String line) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleLineSave(PrintWriter pw) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public File getFile() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
    
}