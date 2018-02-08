package util.misc;

import gui.GUIMain;
import util.Timer;
import util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by Palehorse68 on 2/22/2016.
 */
public class Race extends Thread {

    private int time;
    private Timer raceTime, voteTime;
    private boolean isDone, votingDone;
    private String channel;

    public boolean isDone() {
        return isDone;
    }
    
    public boolean votingDone() {
    	return votingDone;
    }

    private ArrayList<Guess> guesses;

    public Race(String channel, int time) {
        this.channel = channel;
        isDone = votingDone = false;
        this.time = time;
        this.guesses = new ArrayList<Guess>();

    }


    public synchronized void addGuess(String user, String message) {
    	String messageGuess = findGuess(message);
    	if (messageGuess.equals("") || messageGuess == null) return;
    	
    	if (userHasVoted(user)){
    		Guess oldGuess = getUserGuess(user);
    		oldGuess.decrease(user);
    	}
    	
    	Guess guess = getGuess(messageGuess);
    	if (guess != null){
    		//That guess already exists, just add the user
    		guess.increment(user);
    	} else {
    		//This is the first person to guess the number, so create it and add the user
	    	Guess newGuess = new Guess(messageGuess, Integer.parseInt(messageGuess));
	    	newGuess.increment(user);
	    	guesses.add(newGuess);
    	}
    }


    @Override
    public synchronized void start() {
        raceTime = new Timer(Utils.handleInt(60));
        voteTime = new Timer(Utils.handleInt(time));
        printStart();
        super.start();
    }

    @Override
    public void run() {
    	
    	while (!GUIMain.shutDown && raceTime.isRunning()) {
            try {
                Thread.sleep(20);
            } catch (Exception ignored) {
            }
            if (!voteTime.isRunning() && !votingDone()){
            	votingDone = true;
                printVoteOver();
            }
        }
        if (!isDone) {
            isDone = true;
        }
    }

    @Override
    public void interrupt() {
        isDone = true;
        super.interrupt();
    }

    /**
     * Gets an Option that the user is in, otherwise null.
     *
     * @param guess The number to check.
     * @return The option they voted for, otherwise null.
     */
    private synchronized Guess getGuess(String guess) {
        for (Guess o : guesses) {
            if (o.name.equals(guess)) return o;
        }
        return null;
    }
    
    private synchronized Guess getUserGuess(String user) {
    	for (Guess o : guesses){
    		for (String guessUser : o.voters){
    			if (guessUser.equalsIgnoreCase(user)) return o;
    		}
    	}
    	
    	return null;
    }
    
    private synchronized boolean userHasVoted(String user){
    	for (Guess o : guesses) {
    		for (String voter : o.voters) {
                if (voter.equalsIgnoreCase(user)) {
                    return true;
                }
            }
    	}
    	
    	return false;
    }

    class Guess implements Comparable<Guess> {
        String name;
        ArrayList<String> voters;
        int count = 0;
        int compare;

        Guess(String name, int compareIndex) {
            this.name = name;
            compare = compareIndex;
            voters = new ArrayList<>();
        }

        void increment(String name) {
            count++;
            voters.add(name);
        }

        void decrease(String name) {
            count--;
            voters.remove(name);
        }

        @Override
        public int compareTo(Guess o) {
            if (o.count > this.count) {
                return -1;
            } else if (o.count == this.count) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    public void printStart() {
        GUIMain.currentSettings.accountManager.getBot().sendMessage(channel, "/me A wild Dampe race has appeared! Everybody has " + 
         time + " seconds to enter a guess! Any 2-digit number you type will be registered!");
    }

    public synchronized void printVoteOver() {
    	ArrayList<Guess> results = getSortedOptions();
    	if (results.isEmpty()){
    		GUIMain.currentSettings.accountManager.getBot().sendMessage(channel, "/me Voting is now closed! Nobody guessed. Dampe wins. WinWaker");
    	} else {
	    	Guess most = results.get(0);
	    	GUIMain.currentSettings.accountManager.getBot().sendMessage(channel, "/me Voting is now closed! The most common guess was " + 
	    	most.name + " with " + most.count + (most.count > 1 ? " guesses." : " guess."));
    	}
    }

    public void judgeRace(String result){
    	if (!votingDone()) {
    		votingDone = false;
    		printVoteOver();
    	}
    	
    	Guess correctGuess = getGuess(result);
    	String verb = "were", noun = "winners";
    	
    	if (correctGuess == null || correctGuess.count == 0) {
    		GUIMain.currentSettings.accountManager.getBot().sendMessage(channel, "/me The race finished in " + result + " seconds. Nobody guessed correctly. BibleThump");
    	} else {
    		StringBuilder sb = new StringBuilder();
    		if (correctGuess.count == 1) {
    			verb = "was";
    			noun = "winner";
    		}
    		sb.append("/me The race finished in " + result + " seconds. There " + verb +" " + correctGuess.count + " " + noun + ". " + correctGuess.voters.toString());
    		GUIMain.currentSettings.accountManager.getBot().sendMessage(channel, sb.toString());
    	}

    }

    private ArrayList<Guess> getSortedOptions() {
        ArrayList<Guess> results = new ArrayList<>();
        guesses.forEach(results::add);
        Collections.sort(results);//sort into ascending based on votes
        Collections.reverse(results);//make it descending
        return results;
    }
    
    private String findGuess(String message){
    	String toReturn = "";
    	if (message == null || message.isEmpty()) return toReturn;
    	//This pattern attempts to capture 2 digit numbers that fall at the beginning of a line,
    	//the end of a line, or surround by space on either side.
    	//Matches: 33, My guess is 33, My guess is 33 and I'm going to win
    	//Fails: 123, 12:45, 33.17, My guess is 33., My guess is 33.75
    	Pattern p = Pattern.compile("(?:^| )(\\d{2})(?:$| )");
    	Matcher m = p.matcher(message);
    	if (m.find()){
    		toReturn = m.group();
    	}
    	
    	return toReturn.trim();

    }
}