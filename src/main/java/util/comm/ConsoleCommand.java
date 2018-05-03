package util.comm;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is the container for all of the default
 * Botnak commands, like !mod and !addface, etc.
 * <p>
 * This was created to change permissions of the aforementioned
 * commands to either full permission classes (all mods, or everyone)
 * or to specific people in the chat, regardless of status in the chat.
 * <p>
 * >> If you want more commands added, you will have to hard-code them! <<
 * <p>
 * The reason this is called *Console*Command is that these commands
 * directly change something about Botnak, or require specific code.
 * Text Commands are different.
 */
public class ConsoleCommand {

    public Action action;
    public List<String> certainPermission;
    public int classPermission;
    public String trigger;
    private final String helpText;

    public enum Action { //one for each
        ADD_FACE,
        CHANGE_FACE,
        REMOVE_FACE,
        TOGGLE_FACE,
        ADD_SOUND,
        CHANGE_SOUND,
        REMOVE_SOUND,
        SET_SOUND_DELAY,
        TOGGLE_SOUND,
        STOP_SOUND,
        STOP_ALL_SOUNDS,
        ADD_KEYWORD,
        REMOVE_KEYWORD,
        SET_USER_COL,
        SET_COMMAND_PERMISSION,
        ADD_TEXT_COMMAND,
        REMOVE_TEXT_COMMAND,
        ADD_DONATION,
        SET_SOUND_PERMISSION,
        SET_NAME_FACE,
        REMOVE_NAME_FACE,
        SET_STREAM_TITLE,
        SEE_STREAM_TITLE,
        SEE_STREAM_GAME,
        SET_STREAM_GAME,
        PLAY_ADVERT,
        START_RAFFLE,
        ADD_RAFFLE_WINNER,
        REMOVE_RAFFLE_WINNER,
        STOP_RAFFLE,
        SEE_WINNERS,
        START_POLL,
        VOTE_POLL,
        POLL_RESULT,
        CANCEL_POLL,
        NOW_PLAYING,
        SEE_SOUND_STATE,
        SHOW_UPTIME,
        SEE_PREV_SOUND_SUB,
        SEE_PREV_SOUND_DON,
        SEE_OR_SET_REPLY_TYPE,
        SEE_OR_SET_VOLUME,
        ADD_QUOTE,
        CAT,
        CLEAR_GAME,
        DAMPE_RACE,
        GET_QUOTE,
        HELP,
        HOST_USER,
        JUDGE_RACE,
        REMOVE_ALL_QUOTES,
        REMOVE_QUOTE,
        SET_GAME,
        SET_SUB_SOUND,
        TALK,
        THROTTLE,
        THROTTLEBOT,
        WHISPER,
        WR,
        FOLLOWAGE

    }

    /**
     * Creates a command that modifies Botnak directly. All of this is internal.
     *
     * @param trigger           The name of the command; what comes after the !
     * @param act               The action of the console command.
     * @param classPerm         The class permission (@see Constants.PERMISSION_ s)
     * @param certainPermission The certain users able to use the command.
     */
    public ConsoleCommand(String trigger, Action act, int classPerm, List<String> certainPermission, String helpText) {
        action = act;
        this.trigger = trigger;
        classPermission = classPerm;
        this.certainPermission = certainPermission;
        this.helpText = helpText;
    }
    
    public ConsoleCommand(String trigger, Action act, int classPerm, List<String> certainPermission){
    	this(trigger, act, classPerm, certainPermission, "");
    }

    public Action getAction() {
        return action;
    }

    public String getTrigger() {
        return trigger;
    }

    public int getClassPermission() {
        return classPermission;
    }

    public List<String> getCertainPermissions() {
        return certainPermission;
    }

    public void setClassPermission(int newInt) {
        classPermission = newInt;
    }

    public void setCertainPermission(String... newPerm) {
        certainPermission = Arrays.asList(newPerm);
    }
    
    public String getHelpText(){
    	return helpText;
    }

    @Override
    public String toString() {
        String certainPerm = certainPermission.isEmpty() ? "null" : certainPermission.stream().collect(Collectors.joining(","));
        return getHelpText().equals("") ? 
        		trigger + "[" + action.toString() + "[" + classPermission + "[" + certainPerm :
        			trigger + "[" + action.toString() + "[" + classPermission + "[" + certainPerm + "[" + getHelpText();
    }
    
    @Override
    public boolean equals(Object o){
    	if (!(o instanceof ConsoleCommand)) return false;
    	return trigger.equalsIgnoreCase( ((ConsoleCommand) o).getTrigger());
    }
}