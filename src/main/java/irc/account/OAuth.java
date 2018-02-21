package irc.account;

/**
 * Created by Nick on 7/16/2014.
 */
public class OAuth {

    private String key;
    private boolean canSetTitle = false;
    private boolean canPlayAd = false;

    public OAuth(String key, boolean canSetTitle, boolean canPlayAd) {
        this.key = key;
        this.canPlayAd = canPlayAd;
        this.canSetTitle = canSetTitle;
    }

    public String getKey() {
        return key;
    }

    public boolean canPlayAd() {
        return canPlayAd;
    }

    public boolean canSetTitle() {
        return canSetTitle;
    }
}