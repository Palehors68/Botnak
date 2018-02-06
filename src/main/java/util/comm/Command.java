package util.comm;

import util.StringArray;
import util.Timer;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created to cleanup the horrendous HashMap that was the commandMap in GUIMain.
 */
public class Command {
    private Timer delayTimer;
    private String trigger;
    private StringArray contents;
    private ArrayList<String> arguments;

    
    public Command(String name, int delay, String... contents) {
        arguments = new ArrayList<>();
        this.contents = new StringArray(contents);
        trigger = name;
        delayTimer = new Timer(delay);
    }
    
    public Command(String name, String... contents){
    	this(name, 5000, contents);
    }

    public String getTrigger() {
        return trigger;
    }

    public ArrayList<String> getArguments() {
        return arguments;
    }

    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    public int countArguments() {
        return arguments.size();
    }

    public void addArguments(String... argumentsToAdd) {
        Collections.addAll(arguments, argumentsToAdd);
    }
    
    public void setContents(String... contents){
    	this.contents = new StringArray(contents);
    }

    public StringArray buildMessage(StringArray source, String[] definedArguments) {
        StringArray toReturn = new StringArray(source.data);
        for (int messageIndex = 0; messageIndex < source.data.length; messageIndex++) {
            String messagePart = toReturn.data[messageIndex];
            for (int argIndex = 0; argIndex < definedArguments.length; argIndex++) {
                //because arguments.get(i) will be replaced by definedArguments[i]
                if (messagePart.contains(arguments.get(argIndex))) {
                    toReturn.data[messageIndex] = messagePart.replace(arguments.get(argIndex), definedArguments[argIndex]);
                }
            }
        }
        return toReturn;
    }

    public String printCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("!");
        sb.append(trigger);
        if (hasArguments()) {
            for (String arg : arguments) {
                sb.append(" ");
                sb.append(arg);
            }
        }
        return sb.toString();
    }

    public StringArray getMessage() {
        return contents;
    }

    public Timer getDelayTimer() {
        return delayTimer;
    }
    
    public void setDelayTimer(int seconds){
    	this.delayTimer = new Timer(seconds * 1000);
    	this.delayTimer.setEndIn(1);
    }

    @Override
    public boolean equals(Object other) {
        if (other != null) {
            if (other instanceof Command) {
                return ((Command) other).contents.equals(contents) &&
                        ((Command) other).trigger.equals(trigger);
            }
        }
        return false;
    }
    
    
}