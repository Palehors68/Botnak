package irc;

import face.FaceManager;
import gui.forms.GUIMain;
import irc.account.Task;
import irc.message.Message;
import irc.message.MessageHandler;
import irc.message.MessageQueue;
import lib.pircbot.PircBot;
import lib.pircbot.User;
import thread.heartbeat.BanQueue;
import util.Utils;
import util.settings.Settings;

import java.util.Optional;


public class IRCViewer extends MessageHandler {

    public PircBot getViewer() {
        return GUIMain.currentSettings.accountManager.getViewer();
    }

    @Override
    public void onConnect() {
    	getViewer().log("Connecting..............");
        GUIMain.currentSettings.channelManager.addUser(new User(getViewer().getNick()));
        getViewer().log("Number of channels available: " + GUIMain.channelSet.size());
        GUIMain.channelSet.forEach(this::doConnect);
        GUIMain.updateTitle(null);
    }
    
    public void doConnect(String channel) {
    	getViewer().log("Trying to connect to " + channel);
        channel = channel.startsWith("#") ? channel : "#" + channel;
        GUIMain.currentSettings.accountManager.addTask(new Task(getViewer(), Task.Type.JOIN_CHANNEL, channel));
        if (GUIMain.currentSettings.logChat) Utils.logChat(null, channel, 0);
        if (!GUIMain.channelSet.contains(channel)) GUIMain.channelSet.add(channel);
        //TODO if currentSettings.FFZFacesEnable
        if (FaceManager.doneWithFrankerFaces)
            FaceManager.handleFFZChannel(channel.substring(1));
    }
    
    @Override
    public void onBanned(String line){
    	String[] parts = line.split(" ");
    	StringBuilder sb = new StringBuilder();
    	sb.append(getViewer().getNick()).append(" has been ");
    	for (int i = 6; i < parts.length; i++){
    		sb.append(parts[i]);
    		sb.append(" ");
    	}
    	MessageQueue.addMessage(new Message(sb.toString(), Message.MessageType.BAN_NOTIFY).setChannel(parts[3].substring(1)));
    }

    /**
     * Leaves a channel and if specified, removes the channel from the
     * channel list.
     *
     * @param channel The channel name to leave (# not included).
     */
    public void doLeave(String channel) {
        if (!channel.startsWith("#")) channel = "#" + channel;
        GUIMain.currentSettings.accountManager.addTask(new Task(getViewer(), Task.Type.LEAVE_CHANNEL, channel));
        GUIMain.channelSet.remove(channel);
    }

    /**
     * Disconnects from all chats and disposes of the bot.
     *
     * @param forget If true, will forget the user.
     */
    public synchronized void close(boolean forget) {
        GUIMain.log("Logging out of user: " + GUIMain.currentSettings.accountManager.getUserAccount().getName());
        GUIMain.currentSettings.accountManager.addTask(new Task(getViewer(), Task.Type.DISCONNECT, null));
        if (forget) {
            GUIMain.currentSettings.accountManager.setUserAccount(null);
        }
        GUIMain.viewer = null;
        GUIMain.currentSettings.channelManager.dispose();
    }

    @Override
    public void onMessage(final String channel, final String sender, final String message) {
    	if (message.startsWith("!asbot ")){
    		if (message.length() > 7) GUIMain.bot.getBot().sendMessage(channel, message.substring("!asbot ".length()));
    	} else if (sender.equalsIgnoreCase(GUIMain.currentSettings.accountManager.getViewer().getNick()) && message.equalsIgnoreCase("!recon")) {
    		GUIMain.log("Reconnecting " + GUIMain.currentSettings.accountManager.getBot().getNick() + " to " + channel);
    		GUIMain.currentSettings.accountManager.addTask(
    				new Task(GUIMain.currentSettings.accountManager.getBot(), Task.Type.LEAVE_CHANNEL, channel));
			GUIMain.currentSettings.accountManager.addTask(
					new Task(GUIMain.currentSettings.accountManager.getBot(), Task.Type.JOIN_CHANNEL, channel));
			GUIMain.log("Reconnecting " + GUIMain.currentSettings.accountManager.getViewer().getNick() + " to " + channel);
    		GUIMain.currentSettings.accountManager.addTask(
    				new Task(GUIMain.currentSettings.accountManager.getViewer(), Task.Type.LEAVE_CHANNEL, channel));
			GUIMain.currentSettings.accountManager.addTask(
					new Task(GUIMain.currentSettings.accountManager.getViewer(), Task.Type.JOIN_CHANNEL, channel));
			return;
    	} else {
    		MessageQueue.addMessage(new Message(channel, sender, message, false));
    	}
    }

    @Override
    public void onAction(final String sender, final String channel, final String action) {
        MessageQueue.addMessage(new Message(channel, sender, action, true));
    }

    @Override
    public void onBeingHosted(final String line) {
        MessageQueue.addMessage(new Message(line, Message.MessageType.HOSTED_NOTIFY)
                .setChannel(GUIMain.currentSettings.accountManager.getUserAccount().getName()));
    }

    @Override
    public void onHosting(final String channel, final String target, String viewers) {
        Message m = new Message().setChannel(channel).setType(Message.MessageType.HOSTING_NOTIFY);
        if ("-".equals(target)) m.setContent("Exited host mode.");
        else {
            String content = channel + " is now hosting " + target;
            String viewCount;
            if ("-".equals(viewers)) {
                viewCount = ".";
            } else if (viewers.compareTo("1") > 0) {
                viewCount = " for " + viewers + " viewers.";
            } else {
                viewCount = " for " + viewers + " viewer.";
            }
            m.setContent(content + viewCount);
        }
        MessageQueue.addMessage(m);
    }
    
    @Override
    public void onWhisper(String user, String receiver, String contents) {
        MessageQueue.addMessage(new Message().setType(Message.MessageType.WHISPER_MESSAGE).setSender(user).setContent(contents)
                .setExtra(receiver));
    }

    @Override
    public void onNewSubscriber(String channel, String line, String newSub) {
        Message m = new Message().setChannel(channel).setType(Message.MessageType.SUB_NOTIFY).setContent(line);
        if (channel.substring(1).equalsIgnoreCase(GUIMain.currentSettings.accountManager.getUserAccount().getName())) {
            if (line.endsWith("subscribed!")) {//new sub
                if (GUIMain.currentSettings.subscriberManager.addNewSubscriber(newSub, channel)) return;
            } else {
                //it's the (blah blah has subbed for more than 1 month!)
                //Botnak already handles this, so we can construct this message again since the user feels entitled
                //to tell us they've remained subbed... again
                //the catch is the message they send isn't automatic, so there's a chance it won't be sent (ex: on an IRC client, shy, etc)
                //HOWEVER, we will make sure Botnak does not increment the sub counter for this
                Optional<Subscriber> s = GUIMain.currentSettings.subscriberManager.getSubscriber(newSub);
                if (s.isPresent()) {
                    if (!s.get().isActive()) {
                        s.get().setActive(true);//fixes issue #87 (I hope)
                    }
                }
                m.setExtra(false);//anything other than "null" works
            }
        } 
        	//else it's someone else's channel, just print the message
        	
        MessageQueue.addMessage(m);
        String mess;
    	int months;
    	mess = line;
		try{
			months = Integer.parseInt(mess.substring(mess.indexOf("for ")).replaceAll("[\\D]", ""));
		} catch (Exception e) {
			months = 1;
		}
		
//		mess = "";
//    	if (channel.equalsIgnoreCase("#jodenstone")){
//    		mess = "Welcome to the Tea Party!";
////    		for (int i = 0; i < months; i++){
////    			mess += " jodenTeaGasm";
////    		}
//    		getViewer().sendMessage(channel, mess);
//    		
//    	} else if (channel.equalsIgnoreCase("#360chrism")){
//    		return;
//    	} 
//    	mess = "YO! @" + newSub + " with that " + months + " month subscription! Hub SYPE!";
//		getViewer().sendMessage(channel, mess);
//    	mess = "";
    }

    public void onDisconnect() {
        if (!GUIMain.shutDown && getViewer() != null) {
//            if (!whisper) GUIMain.logCurrent("Detected a disconnection for the account: " + getViewer().getNick());
            if (GUIMain.currentSettings.autoReconnectAccounts)
                GUIMain.currentSettings.accountManager.createReconnectThread(getViewer().getConnection());
            else {
//                if (!whisper) GUIMain.logCurrent("Auto-reconnects disabled, please check Preferences -> Auto-Reconnect!");
            	GUIMain.logCurrent("Auto-reconnects disabled, please check Preferences -> Auto-Reconnect!");
            }
        }
    }

    public synchronized void onClearChat(String channel, String name) {
        if (name != null) {
            BanQueue.addToMap(channel, name);
        } else {
            //TODO perhaps add the option to actually clear the chat based on user setting?
            MessageQueue.addMessage(new Message().setChannel(channel).setType(Message.MessageType.BAN_NOTIFY)
                    .setContent("The chat was cleared by a moderator. (Prevented by Botnak)"));
        }
    }

    @Override
    public void onJTVMessage(String channel, String line, String tags) {
//        MessageQueue.addMessage(new Message().setChannel(channel).setType(Message.MessageType.JTV_NOTIFY).setContent(line));
    	MessageQueue.addMessage(new Message(channel, line, Message.MessageType.JTV_NOTIFY));
    }


}