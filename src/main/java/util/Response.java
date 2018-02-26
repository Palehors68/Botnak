package util;

/**
 * Created by Nick on 1/2/2015.
 * <p>
 * This class was created because a lot of the Utils classes need to be
 * a bit more in depth on the results of their methods than just returning "true"
 */
public class Response {

    private boolean isSuccessful = false; //defaults to failed
    private boolean isWhisperable = false;
    private String responseText = "";
    private boolean allowWhispers = true;

    public Response() {
        //default, blank response
    }
    
    public Response(String responseText){
    	// new Response with text and default false
    	this.responseText = responseText;
    }
    
    public Response(String responseText, boolean isSuccessful){
    	this.responseText = responseText;
    	this.isSuccessful = isSuccessful;
    }

    public void wasSuccessful() {
        this.isSuccessful = true;
    }

    public void canWhisper(){
    	if (allowWhispers)
    		this.isWhisperable = true;
    }
    
    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getResponseText() {
        return responseText;
    }

    /**
     * @return If the method was successfully completed or not.
     */
    public boolean isSuccessful() {
        return isSuccessful;
    }
    
    public boolean isWhisperable(){
    	return isWhisperable;
    }
    
    public void addSenderToResponseText(String sender){
    	if (!sender.contains("@")) sender = "@" + sender;
    	this.setResponseText(sender + ", " + getResponseText());
    	allowWhispers = false;
    }
}