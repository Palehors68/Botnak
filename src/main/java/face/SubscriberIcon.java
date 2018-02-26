package face;

/**
 * Created by Nick on 12/30/13.
 * Represents a subscriber icon on Twitch.
 */
public class SubscriberIcon {

    private String fileLoc = null;
    private String channel = null;
    private int length;

    public SubscriberIcon(String channel, String file) {
        this(channel, file, 0);
    }
    
    public SubscriberIcon(String channel, String file, int length){
    	this.channel = channel;
        fileLoc = file;
        this.length = length;
    }

    public String getChannel() {
        return channel;
    }

    public String getFileLoc() {
        return fileLoc;
    }
    
    public int getLength() {
    	return length;
    }
}