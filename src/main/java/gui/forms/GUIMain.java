package gui.forms;

import face.FaceManager;
import face.Icons;
import gui.ChatPane;
import gui.CombinedChatPane;
import gui.DraggableTabbedPane;
import gui.listeners.ListenerName;
import gui.listeners.ListenerURL;
import gui.listeners.ListenerUserChat;
import gui.listeners.NewTabListener;
import irc.IRCBot;
import irc.IRCViewer;
import irc.message.Message;
import irc.message.MessageQueue;
import lib.pircbot.Channel;
import sound.SoundEngine;
import thread.TabPulse;
import thread.ThreadEngine;
import thread.heartbeat.*;
import util.Constants;
import util.Utils;
import util.comm.Command;
import util.comm.ConsoleCommand;
import util.settings.Settings;
import face.TwitchFace;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class GUIMain extends JFrame {

	public static boolean _debug = false;
	
    public static ConcurrentHashMap<String, Color> userColMap;
    public static CopyOnWriteArraySet<Command> commandSet;
    public static CopyOnWriteArraySet<String> channelSet;
    public static ConcurrentHashMap<String, ChatPane> chatPanes;
    public static CopyOnWriteArraySet<CombinedChatPane> combinedChatPanes;
    public static ConcurrentHashMap<String, Color> keywordMap;

    public static ConcurrentHashMap<String, GUIViewerList> viewerLists;

    public static int userResponsesIndex = 0;
    public static ArrayList<String> userResponses;
    public static ArrayList<String> quotes;

    public static CopyOnWriteArraySet<ConsoleCommand> conCommands;

    public static IRCBot bot;
    public static IRCViewer viewer;
    public static GUISettings settings = null;
    public static GUIStreams streams = null;
    public static GUIEmotes emotes = null;
    public static GUIVotePHB voteGUI = null;
    public static GUIAbout aboutGUI = null;
    public static GUIStatus statusGUI = null;
    public static GUITextCommandEditor textEditorGUI = null;
    public static GUIAuthorizeAccount accountGUI = null;

    public static boolean shutDown = false;
    

    public static SimpleAttributeSet norm = new SimpleAttributeSet();

    public static GUIMain instance;

    public static Settings currentSettings;

    public static CopyOnWriteArraySet<TabPulse> tabPulses;

    public static Heartbeat heartbeat;

    private static ChatPane systemLogsPane;

    public GUIMain(boolean debug) {
    	_debug = debug;
        new MessageQueue().start();
        instance = this;
        channelSet = new CopyOnWriteArraySet<>();
        userColMap = new ConcurrentHashMap<>();
        commandSet = new CopyOnWriteArraySet<>();
        conCommands = new CopyOnWriteArraySet<>();
        keywordMap = new ConcurrentHashMap<>();
        tabPulses = new CopyOnWriteArraySet<>();
        combinedChatPanes = new CopyOnWriteArraySet<>();
        viewerLists = new ConcurrentHashMap<>();
        userResponses = new ArrayList<>();

        ThreadEngine.init();

        quotes = new ArrayList<>();
        FaceManager.init();
        SoundEngine.init();
        StyleConstants.setForeground(norm, Color.white);
        initComponents();
        chatPanes = new ConcurrentHashMap<>();
        systemLogsPane = new ChatPane("System Logs", allChatsScroll, allChats, 0);
        chatPanes.put("System Logs", systemLogsPane);
        currentSettings = new Settings();
        currentSettings.load();
        heartbeat = new Heartbeat();
        heartbeat.addHeartbeatThread(new ViewerCount());
        heartbeat.addHeartbeatThread(new UserManager());
        heartbeat.addHeartbeatThread(new BanQueue());
        //TODO if (GUISettings.trackDonations)
        heartbeat.addHeartbeatThread(new DonationCheck());
        heartbeat.start();
    }
    

    public static ChatPane getCurrentPane() {
        ChatPane toReturn;
        int index = channelPane.getSelectedIndex();
        if (index == 0) return getSystemLogsPane();
        toReturn = Utils.getChatPane(index);
        if (toReturn == null) {
            toReturn = Utils.getCombinedChatPane(index);
        }
        return toReturn == null ? getSystemLogsPane() : toReturn;
    }
    
    public static boolean loadedSettingsUser() {
        return currentSettings != null && currentSettings.accountManager.getUserAccount() != null;
    }

    public static boolean loadedSettingsBot() {
        return currentSettings != null && currentSettings.accountManager.getBotAccount() != null;
    }

    public static boolean loadedCommands() {
        return !commandSet.isEmpty();
    }

    public void setBotReplyRadioButton(){
    	switch (currentSettings.botReplyType) {
    	case 0:
    		radioButtonMenuItem2.setSelected(true);
    		break;
    	case 1:
    		radioButtonMenuItem3.setSelected(true);
    		break;
    	case 2:
    		radioButtonMenuItem1.setSelected(true);
    		break;
    	default:
    		break;
    	}
    }
    
    public void setEmoteSwitches(){
    	ffzEmotesToggle.setSelected(currentSettings.ffzEmotes);
    	subEmotesToggle.setSelected(currentSettings.subEmotes);
    	
    	GUIEmotes.setFfzEmoteToggle(currentSettings.ffzEmotes);
    	GUIEmotes.setSubEmoteToggle(currentSettings.subEmotes);
    }
    
    public void chatButtonActionPerformed() {
        userResponsesIndex = 0;
        String channel = channelPane.getTitleAt(channelPane.getSelectedIndex());
        if (GUIMain.currentSettings.accountManager.getViewer() == null) return;
        if (!GUIMain.currentSettings.accountManager.getViewer().isConnected()) {
            logCurrent("Failed to send message, currently trying to reconnect!");
            return;
        }
        String userInput = userChat.getText().replaceAll("\n", "");
        if (channel != null && !channel.equalsIgnoreCase("system logs")) {
            CombinedChatPane ccp = Utils.getCombinedChatPane(channelPane.getSelectedIndex());
            boolean comboExists = ccp != null;
            if (comboExists) {
                String[] channels;
                if (!ccp.getActiveChannel().equalsIgnoreCase("All")) {
                    channels = new String[]{ccp.getActiveChannel()};
                } else {
                    channels = ccp.getChannels();
                }
                if (!Utils.checkText(userInput).equals("")) {
                    for (String c : channels) {
                        GUIMain.currentSettings.accountManager.getViewer().sendMessage("#" + c, userInput);
                    }
                    if (!userResponses.contains(userInput)) userResponses.add(userInput);
                }
                userChat.setText("");
            } else {
                if (!Utils.checkText(userInput).equals("")) {
                    GUIMain.currentSettings.accountManager.getViewer().sendMessage("#" + channel, userInput);
                    if (!userResponses.contains(userInput)) userResponses.add(userInput);
                }
                userChat.setText("");
            }
        }
    }

    /**
     * Wrapper for ensuring no null chat pane is produced due to hash tags.
     *
     * @param channel The channel, either inclusive of the hash tag or not.
     * @return The chat pane if existent, otherwise to System Logs to prevent null pointers.
     * (Botnak will just print out to System Logs the message that was eaten)
     */
    public static ChatPane getChatPane(String channel) {
        ChatPane toReturn = chatPanes.get(channel.replaceAll("#", ""));
        return toReturn == null ? getSystemLogsPane() : toReturn;
    }

    /**
     * @return The System Logs chat pane.
     */
    public static ChatPane getSystemLogsPane() {
        return systemLogsPane;
    }

    /**
     * Logs a message to the current chat pane.
     *
     * @param message The message to log.
     */
    public static void logCurrent(Object message) {
        String channel = channelPane.getTitleAt(channelPane.getSelectedIndex());
        if (message != null && GUIMain.chatPanes != null && !GUIMain.chatPanes.isEmpty())
            MessageQueue.addMessage(new Message().setChannel("#" + channel)
                    .setContent(message.toString()).setType(Message.MessageType.LOG_MESSAGE));
    }

    /**
     * Logs a message to the chat console under all white, SYS username.
     * This should only be used for serious reports, like exception reporting and
     * other status updates.
     *
     * @param message The message to log.
     */
    public static void log(Object message) {
        String toPrint;
        Message.MessageType type = Message.MessageType.LOG_MESSAGE; // Moved here to allow for changing message type to something like error for throwables
        if (message instanceof Throwable) {
            Throwable t = (Throwable) message;
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            toPrint = sw.toString(); // stack trace as a string
        } else {
            // Not a throwable.. Darn strings
            toPrint = message.toString();
        }
        if (toPrint != null && GUIMain.chatPanes != null && !GUIMain.chatPanes.isEmpty())
            MessageQueue.addMessage(new Message(toPrint, type));
    }

    public static void updateTitle(String viewerCount) {
        StringBuilder stanSB = new StringBuilder();
        stanSB.append("PaleHorsBot ");
        if (viewerCount != null) {
            stanSB.append("| ");
            stanSB.append(viewerCount);
            stanSB.append(" ");
        }
        if (currentSettings != null) {
            if (currentSettings.accountManager.getUserAccount() != null) {
                stanSB.append("| User: ");
                stanSB.append(currentSettings.accountManager.getUserAccount().getName());
            }
            if (currentSettings.accountManager.getBotAccount() != null) {
                stanSB.append(" | Bot: ");
                stanSB.append(currentSettings.accountManager.getBotAccount().getName());
            }
        }
        instance.setTitle(stanSB.toString());
    }

    public void exitButtonActionPerformed() {
        shutDown = true;
        for (String c : channelSet){
        	Channel ch = currentSettings.channelManager.getChannel(c);
        	if (ch != null){
        		ch.clear();
        	}
        	
        }
        if (viewer != null) {
            viewer.close(false);
        }
        if (bot != null) {
            bot.close(false);
        }
        if (!tabPulses.isEmpty()) {
            tabPulses.forEach(TabPulse::interrupt);
            tabPulses.clear();
        }
        SoundEngine.getEngine().close();
        
        currentSettings.save();
        heartbeat.interrupt();
        if (currentSettings.logChat) {
            String[] keys = chatPanes.keySet().toArray(new String[chatPanes.keySet().size()]);
            for (String s : keys) {
                ChatPane cp = chatPanes.get(s);
                Utils.logChat(cp.getText().split("\\n"), s, 2);
            }
        }
        
        
        dispose();
        System.exit(0);
    }
    
    
    public void emoteButtonActionPerformed() {
     	if (emotes == null) {
            emotes = new GUIEmotes(userChat, this);
         }
     	//emotes.scanUserSubscriptions();
    	Point p = getLocation();
    	Dimension d = getSize();
    	GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    	GraphicsDevice[] gds = ge.getScreenDevices();

    	int currentMonitor = -1, currentPosition = (p.x < 0) ? 0 : p.x, offset = 0;
    	while (currentPosition >= 0){
    		try{
    			offset += gds[(currentMonitor)].getDisplayMode().getWidth();
    		} catch (Exception e) {
    			//ignore
    		}
    		currentPosition -= gds[++currentMonitor].getDisplayMode().getWidth();
    	} 
    	
    	if (p.x + d.width + emotes.getSize().width > (offset + gds[currentMonitor].getDisplayMode().getWidth())){
    		emotes.setLocation((p.x - emotes.getSize().width),p.y);
    	} else {
    		emotes.setLocation((p.x + d.width), p.y);
    	}
         if (!emotes.isVisible()) {
        	emotes.refreshEmotes();
             emotes.setVisible(true);
         }
    }
    
    public void tabChanged(){
    	if (emotes != null)
    		emotes.refreshEmotes();
    	
    	if (textEditorGUI != null)
    		textEditorGUI.refreshList();
    }
    
    public void setWhisperModeToggle(){
    	whisperModeToggle.setSelected(currentSettings.botWhisperMode);
    }

    public synchronized void pulseTab(ChatPane cp) {
        if (shutDown) return;
        if (cp.isPulsing()) return;
        TabPulse tp = new TabPulse(cp);
        tp.start();
        tabPulses.add(tp);
    }

    private void openBotnakFolderOptionActionPerformed() {
        Utils.openWebPage(Settings.defaultDir.toURI().toString());
    }

    private void openLogViewerOptionActionPerformed() {
        // TODO add your code here
    }

    private void openSoundsOptionActionPerformed() {
        Utils.openWebPage(new File(currentSettings.defaultSoundDir).toURI().toString());
    }

    private void autoReconnectToggleItemStateChanged(ItemEvent e) {
        // TODO check login status of both accounts to determine if they need relogging in, upon enable
    }
    
    private void setBotReplyToAll(){
    	currentSettings.botReplyType = 2;
    }
    
    private void setBotReplyToUser(){
    	currentSettings.botReplyType = 1;
    }
    
    private void setBotReplyToNone(){
    	currentSettings.botReplyType = 0;
    }

    private void whisperModeToggleStateChanged(){
    	currentSettings.botWhisperMode = whisperModeToggle.isSelected();
    }
    
    private void alwaysOnTopToggleItemStateChanged(ItemEvent e) {
        Window[] windows = getWindows();
        for (Window w : windows) {
            w.setAlwaysOnTop(e.getStateChange() == ItemEvent.SELECTED);
        }
    }

    private void settingsOptionActionPerformed() {
        if (settings == null) {
            settings = new GUISettings();
        }
        if (!settings.isVisible()) {
            settings.setVisible(true);
        }
    }

    private void startRaffleOptionActionPerformed() {
        // TODO add your code here
    }

    private void startVoteOptionActionPerformed() {
    		voteGUI = new GUIVotePHB();
    		voteGUI.setVisible(true);	
    }

    private void soundsToggleItemStateChanged(ItemEvent e) {
        // TODO add your code here
    	currentSettings.soundsEnabled = soundsToggle.isSelected();
    }

    public void manageTextCommandsOptionActionPerformed(Channel ch) {
        
        textEditorGUI = new GUITextCommandEditor(ch);
                
        if (!textEditorGUI.isVisible()){
        	textEditorGUI.setVisible(true);
        }
    }

    private void updateStatusOptionActionPerformed() {
        if (statusGUI == null) {
            statusGUI = new GUIStatus();
        }
        if (!statusGUI.isVisible()) {
            statusGUI.setVisible(true);
        }
    }

    private void subOnlyToggleItemStateChanged(ItemEvent e) {
        //TODO viewer.getViewer().sendRawMessage();
    }

    private void ffzEmotesToggleItemStateChanged() {
    	GUIEmotes.setFfzEmoteToggle(ffzEmotesToggle.isSelected());
    	if (emotes != null){
    		emotes.refreshEmotes();
    	}
    }
    
    private void subEmotesToggleItemStateChanged() {
    	GUIEmotes.setSubEmoteToggle(subEmotesToggle.isSelected());
    	if (emotes != null){
    		emotes.refreshEmotes();
    	}
    }
    
    private void projectGithubOptionActionPerformed() {
        Utils.openWebPage("https://github.com/Gocnak/Botnak/");
    }

    private void projectWikiOptionActionPerformed() {
        Utils.openWebPage("https://github.com/Gocnak/Botnak/wiki");
    }

    private void projectDetailsOptionActionPerformed() {
        if (aboutGUI == null) {
            aboutGUI = new GUIAbout();
        }
        if (!aboutGUI.isVisible())
            aboutGUI.setVisible(true);
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Nick K
        menuBar1 = new JMenuBar();
        fileMenu = new JMenu();
        openBotnakFolderOption = new JMenuItem();
        openLogViewerOption = new JMenuItem();
        openSoundsOption = new JMenuItem();
        exitOption = new JMenuItem();
        preferencesMenu = new JMenu();
        botReplyMenu = new JMenu();
        radioButtonMenuItem1 = new JRadioButtonMenuItem();
        radioButtonMenuItem3 = new JRadioButtonMenuItem();
        radioButtonMenuItem2 = new JRadioButtonMenuItem();
        autoReconnectToggle = new JCheckBoxMenuItem();
        ffzEmotesToggle = new JCheckBoxMenuItem();
        subEmotesToggle = new JCheckBoxMenuItem();
        alwaysOnTopToggle = new JCheckBoxMenuItem();
        whisperModeToggle = new JCheckBoxMenuItem();
        settingsOption = new JMenuItem();
        toolsMenu = new JMenu();
        startRaffleOption = new JMenuItem();
        startVoteOption = new JMenuItem();
        soundsToggle = new JCheckBoxMenuItem();
        soundDelayMenu = new JMenu();
        soundDelayOffOption = new JRadioButtonMenuItem();
        soundDelay5secOption = new JRadioButtonMenuItem();
        soundDelay10secOption = new JRadioButtonMenuItem();
        soundDelay20secOption = new JRadioButtonMenuItem();
        soundDelayCustomOption = new JRadioButtonMenuItem();
        soundPermissionMenu = new JMenu();
        soundPermEveryoneOption = new JRadioButtonMenuItem();
        soundPermSDMBOption = new JRadioButtonMenuItem();
        soundPermDMBOption = new JRadioButtonMenuItem();
        soundPermModAndBroadOption = new JRadioButtonMenuItem();
        soundPermBroadOption = new JRadioButtonMenuItem();
        manageTextCommandsOption = new JMenuItem();
        runAdMenu = new JMenu();
        timeOption30sec = new JMenuItem();
        timeOption60sec = new JMenuItem();
        timeOption90sec = new JMenuItem();
        timeOption120sec = new JMenuItem();
        timeOption150sec = new JMenuItem();
        timeOption180sec = new JMenuItem();
        updateStatusOption = new JMenuItem();
        subOnlyToggle = new JCheckBoxMenuItem();
        slowModeMenu = new JMenu();
        slowModeOffOption = new JRadioButtonMenuItem();
        slowMode5secOption = new JRadioButtonMenuItem();
        slowMode10secOption = new JRadioButtonMenuItem();
        slowMode15secOption = new JRadioButtonMenuItem();
        slowMode30secOption = new JRadioButtonMenuItem();
        slowModeCustomOption = new JRadioButtonMenuItem();
        helpMenu = new JMenu();
        projectGithubOption = new JMenuItem();
        projectWikiOption = new JMenuItem();
        projectDetailsOption = new JMenuItem();
        channelPane = new DraggableTabbedPane();
        allChatsScroll = new JScrollPane();
        allChats = new JTextPane();
        dankLabel = new JLabel();
        scrollPane1 = new JScrollPane();
        userChat = new JTextArea();
        emoteButton = new JButton();

        //======== Botnak ========
        {
            setMinimumSize(new Dimension(680, 504));
            setName("PaleHorsBot Control Panel");
            setTitle("PaleHorsBot | Please go to Preferences->Settings!");
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setIconImage(new ImageIcon(getClass().getResource("/image/icon.png")).getImage());
            Container BotnakContentPane = getContentPane();

            //======== menuBar1 ========
            {

                //======== fileMenu ========
                {
                    fileMenu.setText("File");

                    //---- openBotnakFolderOption ----
                    openBotnakFolderOption.setText("Open PalehorsBot Folder");
                    openBotnakFolderOption.addActionListener(e -> openBotnakFolderOptionActionPerformed());
                    fileMenu.add(openBotnakFolderOption);

                    //---- openLogViewerOption ----
                    openLogViewerOption.setText("Open Log Viewer");
                    openLogViewerOption.addActionListener(e -> openLogViewerOptionActionPerformed());
                    fileMenu.add(openLogViewerOption);

                    //---- openSoundsOption ----
                    openSoundsOption.setText("Open Sound Directory");
                    openSoundsOption.addActionListener(e -> openSoundsOptionActionPerformed());
                    fileMenu.add(openSoundsOption);
                    fileMenu.addSeparator();

                    //---- exitOption ----
                    exitOption.setText("Save and Exit");
                    exitOption.addActionListener(e -> exitButtonActionPerformed());
                    fileMenu.add(exitOption);
                }
                menuBar1.add(fileMenu);

                //======== preferencesMenu ========
                {
                    preferencesMenu.setText("Preferences");

                    //======== botReplyMenu ========
                    {
                        botReplyMenu.setText("Bot Reply");

                        //---- radioButtonMenuItem1 ----
                        radioButtonMenuItem1.setText("Reply to all");
                        radioButtonMenuItem1.addActionListener(e -> setBotReplyToAll());
                        botReplyMenu.add(radioButtonMenuItem1);

                        //---- radioButtonMenuItem3 ----
                        radioButtonMenuItem3.setText("Reply to you");
                        radioButtonMenuItem3.addActionListener(e -> setBotReplyToUser());
                        botReplyMenu.add(radioButtonMenuItem3);

                        //---- radioButtonMenuItem2 ----
                        radioButtonMenuItem2.setText("Reply to none");
                        radioButtonMenuItem2.addActionListener(e -> setBotReplyToNone());
                        botReplyMenu.add(radioButtonMenuItem2);
                        botReplyMenu.addSeparator();
                        
                      //---- whisperModeToggle ----
                        whisperModeToggle.setText("Whisper Mode");
                        whisperModeToggle.setSelected(false);
                        whisperModeToggle.addActionListener(e -> whisperModeToggleStateChanged());
                        botReplyMenu.add(whisperModeToggle);
                        
                    }
                    preferencesMenu.add(botReplyMenu);

                    //---- autoReconnectToggle ----
                    autoReconnectToggle.setText("Auto-Reconnect");
                    autoReconnectToggle.setSelected(true);
                    autoReconnectToggle.addItemListener(e -> autoReconnectToggleItemStateChanged(e));
                    preferencesMenu.add(autoReconnectToggle);
                    
                  //---- subEmotesToggle ----
                    subEmotesToggle.setText("Enable Sub Emotes");
                    subEmotesToggle.setSelected(true);
                    subEmotesToggle.addItemListener(e -> subEmotesToggleItemStateChanged());
                    preferencesMenu.add(subEmotesToggle);
                    
                    //---- ffzEmotesToggle ----
                    ffzEmotesToggle.setText("Enable FFZ Emotes");
                    ffzEmotesToggle.setSelected(true);
                    ffzEmotesToggle.addItemListener(e -> ffzEmotesToggleItemStateChanged());
                    preferencesMenu.add(ffzEmotesToggle);

                    //---- alwaysOnTopToggle ----
                    alwaysOnTopToggle.setText("Always On Top");
                    alwaysOnTopToggle.setSelected(false);
                    alwaysOnTopToggle.addItemListener(e -> alwaysOnTopToggleItemStateChanged(e));
                    preferencesMenu.add(alwaysOnTopToggle);
                    preferencesMenu.addSeparator();

                    //---- settingsOption ----
                    settingsOption.setText("Settings...");
                    settingsOption.addActionListener(e -> settingsOptionActionPerformed());
                    preferencesMenu.add(settingsOption);
                }
                menuBar1.add(preferencesMenu);

                //======== toolsMenu ========
                {
                    toolsMenu.setText("Tools");

                    //---- startRaffleOption ----
                    startRaffleOption.setText("Create Raffle...");
                    startRaffleOption.addActionListener(e -> startRaffleOptionActionPerformed());
                    toolsMenu.add(startRaffleOption);

                    //---- startVoteOption ----
                    startVoteOption.setText("Create Vote...");
                    startVoteOption.addActionListener(e -> startVoteOptionActionPerformed());
                    toolsMenu.add(startVoteOption);

                    //---- soundsToggle ----
                    soundsToggle.setText("Enable Sounds");
                    soundsToggle.setSelected(true);
                    soundsToggle.addItemListener(e -> soundsToggleItemStateChanged(e));
                    toolsMenu.add(soundsToggle);

                    //======== soundDelayMenu ========
                    {
                        soundDelayMenu.setText("Sound Delay");

                        //---- soundDelayOffOption ----
                        soundDelayOffOption.setText("None (Off)");
                        soundDelayOffOption.addActionListener(e -> {

                        });
                        soundDelayMenu.add(soundDelayOffOption);

                        //---- soundDelay5secOption ----
                        soundDelay5secOption.setText("5 seconds");
                        soundDelayMenu.add(soundDelay5secOption);

                        //---- soundDelay10secOption ----
                        soundDelay10secOption.setText("10 seconds");
                        soundDelay10secOption.setSelected(true);
                        soundDelayMenu.add(soundDelay10secOption);

                        //---- soundDelay20secOption ----
                        soundDelay20secOption.setText("20 seconds");
                        soundDelayMenu.add(soundDelay20secOption);

                        //---- soundDelayCustomOption ----
                        soundDelayCustomOption.setText("Custom (Use chat)");
                        soundDelayCustomOption.setEnabled(false);
                        soundDelayMenu.add(soundDelayCustomOption);
                    }
                    toolsMenu.add(soundDelayMenu);

                    //======== soundPermissionMenu ========
                    {
                        soundPermissionMenu.setText("Sound Permission");

                        //---- soundPermEveryoneOption ----
                        soundPermEveryoneOption.setText("Everyone");
                        soundPermissionMenu.add(soundPermEveryoneOption);

                        //---- soundPermSDMBOption ----
                        soundPermSDMBOption.setText("Subs, Donors, Mods, Broadcaster");
                        soundPermSDMBOption.setSelected(true);
                        soundPermissionMenu.add(soundPermSDMBOption);

                        //---- soundPermDMBOption ----
                        soundPermDMBOption.setText("Donors, Mods, Broadcaster");
                        soundPermissionMenu.add(soundPermDMBOption);

                        //---- soundPermModAndBroadOption ----
                        soundPermModAndBroadOption.setText("Mods and Broadcaster Only");
                        soundPermissionMenu.add(soundPermModAndBroadOption);

                        //---- soundPermBroadOption ----
                        soundPermBroadOption.setText("Broadcaster Only");
                        soundPermissionMenu.add(soundPermBroadOption);
                    }
                    toolsMenu.add(soundPermissionMenu);

                    //---- manageTextCommandsOption ----
                    manageTextCommandsOption.setText("Manage Text Commands...");
                    manageTextCommandsOption.addActionListener(e -> manageTextCommandsOptionActionPerformed(null));
                    toolsMenu.add(manageTextCommandsOption);
                    toolsMenu.addSeparator();

                    //======== runAdMenu ========
                    {
                        runAdMenu.setText("Run Ad");

                        //---- timeOption30sec ----
                        timeOption30sec.setText("30 sec");
                        runAdMenu.add(timeOption30sec);

                        //---- timeOption60sec ----
                        timeOption60sec.setText("1 min");
                        runAdMenu.add(timeOption60sec);

                        //---- timeOption90sec ----
                        timeOption90sec.setText("1 min 30 sec");
                        runAdMenu.add(timeOption90sec);

                        //---- timeOption120sec ----
                        timeOption120sec.setText("2 min");
                        runAdMenu.add(timeOption120sec);

                        //---- timeOption150sec ----
                        timeOption150sec.setText("2 min 30 sec");
                        runAdMenu.add(timeOption150sec);

                        //---- timeOption180sec ----
                        timeOption180sec.setText("3 min");
                        runAdMenu.add(timeOption180sec);
                    }
                    toolsMenu.add(runAdMenu);

                    //---- updateStatusOption ----
                    updateStatusOption.setText("Update Status...");
                    updateStatusOption.addActionListener(e -> updateStatusOptionActionPerformed());
                    toolsMenu.add(updateStatusOption);

                    //---- subOnlyToggle ----
                    subOnlyToggle.setText("Sub-only Chat");
                    subOnlyToggle.addItemListener(e -> subOnlyToggleItemStateChanged(e));
                    toolsMenu.add(subOnlyToggle);

                    //======== slowModeMenu ========
                    {
                        slowModeMenu.setText("Slow Mode");

                        //---- slowModeOffOption ----
                        slowModeOffOption.setText("Off");
                        slowModeOffOption.setSelected(true);
                        slowModeMenu.add(slowModeOffOption);

                        //---- slowMode5secOption ----
                        slowMode5secOption.setText("5 seconds");
                        slowModeMenu.add(slowMode5secOption);

                        //---- slowMode10secOption ----
                        slowMode10secOption.setText("10 seconds");
                        slowModeMenu.add(slowMode10secOption);

                        //---- slowMode15secOption ----
                        slowMode15secOption.setText("15 seconds");
                        slowModeMenu.add(slowMode15secOption);

                        //---- slowMode30secOption ----
                        slowMode30secOption.setText("30 seconds");
                        slowModeMenu.add(slowMode30secOption);

                        //---- slowModeCustomOption ----
                        slowModeCustomOption.setText("Custom (use chat)");
                        slowModeCustomOption.setEnabled(false);
                        slowModeMenu.add(slowModeCustomOption);
                    }
                    toolsMenu.add(slowModeMenu);
                }
                menuBar1.add(toolsMenu);

                //======== helpMenu ========
                {
                    helpMenu.setText("Help");

                    //---- projectGithubOption ----
                    projectGithubOption.setText("Botnak Github");
                    projectGithubOption.addActionListener(e -> projectGithubOptionActionPerformed());
                    helpMenu.add(projectGithubOption);

                    //---- projectWikiOption ----
                    projectWikiOption.setText("Wiki");
                    projectWikiOption.addActionListener(e -> projectWikiOptionActionPerformed());
                    helpMenu.add(projectWikiOption);
                    helpMenu.addSeparator();

                    //---- projectDetailsOption ----
                    projectDetailsOption.setText("About...");
                    projectDetailsOption.addActionListener(e -> projectDetailsOptionActionPerformed());
                    helpMenu.add(projectDetailsOption);
                }
                menuBar1.add(helpMenu);
            }
            setJMenuBar(menuBar1);

            //======== channelPane ========
            {
                channelPane.setFocusable(false);
                channelPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
                channelPane.setAutoscrolls(true);
                channelPane.addChangeListener(Constants.tabListener);
                channelPane.addMouseListener(Constants.tabListener);
//                channelPane.add

                //======== allChatsScroll ========
                {
                    allChatsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    allChats.setForeground(Color.white);
                    allChats.setBackground(Color.black);
                    allChats.setFont(new Font("Calibri", Font.PLAIN, 16));
                    allChats.setMargin(new Insets(0, 0, 0, 0));
                    allChats.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
                    allChats.addMouseListener(new ListenerURL());
                    allChats.addMouseListener(new ListenerName());
                    allChatsScroll.setViewportView(allChats);
                }
                channelPane.addTab("System Logs", allChatsScroll);

                //---- dankLabel ----
                dankLabel.setText("Dank memes");
                channelPane.addTab("+", dankLabel);
                channelPane.setEnabledAt(channelPane.getTabCount() - 1, false);
                channelPane.addMouseListener(new NewTabListener());
            }

            //======== scrollPane1 ========
            {
                scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

                //---- userChat ----
                userChat.setFont(new Font("Consolas", Font.PLAIN, 12));
                userChat.setLineWrap(true);
                userChat.addKeyListener(new ListenerUserChat(userChat));
                scrollPane1.setViewportView(userChat);
            }
            
          //---- emoteButton ----
            emoteButton.setText("Wait...");
            setChatButtonIcon();
            emoteButton.setFocusable(false);
            emoteButton.setToolTipText("Popup window with available emotes.");
            emoteButton.setSize(5, userChat.getHeight());
            emoteButton.setEnabled(false);
            emoteButton.addActionListener(e -> emoteButtonActionPerformed());

            GroupLayout BotnakContentPaneLayout = new GroupLayout(BotnakContentPane);
            BotnakContentPaneLayout.setHorizontalGroup(
            	BotnakContentPaneLayout.createParallelGroup(Alignment.LEADING)
            		.addGroup(BotnakContentPaneLayout.createSequentialGroup()
            			.addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 615, GroupLayout.PREFERRED_SIZE)
            			.addPreferredGap(ComponentPlacement.UNRELATED)
            			.addComponent(emoteButton, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
            			.addGap(6))
            		.addComponent(channelPane, GroupLayout.DEFAULT_SIZE, 664, Short.MAX_VALUE)
            );
            BotnakContentPaneLayout.setVerticalGroup(
            	BotnakContentPaneLayout.createParallelGroup(Alignment.LEADING)
            		.addGroup(BotnakContentPaneLayout.createSequentialGroup()
            			.addComponent(channelPane, GroupLayout.PREFERRED_SIZE, 393, GroupLayout.PREFERRED_SIZE)
            			.addPreferredGap(ComponentPlacement.UNRELATED)
            			.addGroup(BotnakContentPaneLayout.createParallelGroup(Alignment.TRAILING)
            				.addGroup(BotnakContentPaneLayout.createSequentialGroup()
            					.addComponent(emoteButton)
            					.addGap(32))
            				.addGroup(BotnakContentPaneLayout.createSequentialGroup()
            					.addComponent(scrollPane1, GroupLayout.PREFERRED_SIZE, 51, GroupLayout.PREFERRED_SIZE)
            					.addContainerGap())
            				.addGap(25)))
            );
            BotnakContentPane.setLayout(BotnakContentPaneLayout);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (channelPane != null) {
                        channelPane.scrollDownPanes();
                    }
                }
            });
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    exitButtonActionPerformed();
                }
            });
            pack();
            setLocationRelativeTo(getOwner());
        }

        //---- botReplyGroup ----
        ButtonGroup botReplyGroup = new ButtonGroup();
        botReplyGroup.add(radioButtonMenuItem1);
        botReplyGroup.add(radioButtonMenuItem3);
        botReplyGroup.add(radioButtonMenuItem2);

        //---- soundDelayGroup ----
        ButtonGroup soundDelayGroup = new ButtonGroup();
        soundDelayGroup.add(soundDelayOffOption);
        soundDelayGroup.add(soundDelay5secOption);
        soundDelayGroup.add(soundDelay10secOption);
        soundDelayGroup.add(soundDelay20secOption);
        soundDelayGroup.add(soundDelayCustomOption);

        //---- soundPermissionGroup ----
        ButtonGroup soundPermissionGroup = new ButtonGroup();
        soundPermissionGroup.add(soundPermEveryoneOption);
        soundPermissionGroup.add(soundPermSDMBOption);
        soundPermissionGroup.add(soundPermDMBOption);
        soundPermissionGroup.add(soundPermModAndBroadOption);
        soundPermissionGroup.add(soundPermBroadOption);

        //---- slowModeGroup ----
        ButtonGroup slowModeGroup = new ButtonGroup();
        slowModeGroup.add(slowModeOffOption);
        slowModeGroup.add(slowMode5secOption);
        slowModeGroup.add(slowMode10secOption);
        slowModeGroup.add(slowMode15secOption);
        slowModeGroup.add(slowMode30secOption);
        slowModeGroup.add(slowModeCustomOption);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    private void setChatButtonIcon() {
    	Thread runner = new Thread(new Runnable(){
    	     public void run() {
    	    	 try{
    	     		while (!(FaceManager.doneWithTwitchFaces && FaceManager.doneWithFrankerFaces)){
    	     			try{
    	     				Thread.sleep(1000);
    	     			} catch (InterruptedException e) {
    	     				GUIMain.log(e);
    	     			}
    	     		}
    	     		emoteButton.setIcon(Icons.sizeIcon(new File(FaceManager.twitchFaceMap.get(1).getFilePath()).toURI().toURL()));
    	     		emoteButton.setText("");
    	     		
    	     	} catch (Exception e){//MalformedURLException e) {
    	     		GUIMain.log(e);
    	     	}
    	    	 emoteButton.setEnabled(true);
    	     }
    	});  
    	runner.start();
    }
    
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Nick K
    private JMenuBar menuBar1;
    private JMenu fileMenu;
    private JMenuItem openBotnakFolderOption;
    private JMenuItem openLogViewerOption;
    private JMenuItem openSoundsOption;
    private JMenuItem exitOption;
    private JMenu preferencesMenu;
    private JMenu botReplyMenu;
    private JRadioButtonMenuItem radioButtonMenuItem1;
    private JRadioButtonMenuItem radioButtonMenuItem3;
    private JRadioButtonMenuItem radioButtonMenuItem2;
    private JCheckBoxMenuItem autoReconnectToggle;
    private JCheckBoxMenuItem ffzEmotesToggle;
    private JCheckBoxMenuItem subEmotesToggle;
    private JCheckBoxMenuItem alwaysOnTopToggle;
    private JCheckBoxMenuItem whisperModeToggle;
    private JMenuItem settingsOption;
    private JMenu toolsMenu;
    private JMenuItem startRaffleOption;
    private JMenuItem startVoteOption;
    private JCheckBoxMenuItem soundsToggle;
    private JMenu soundDelayMenu;
    private JRadioButtonMenuItem soundDelayOffOption;
    private JRadioButtonMenuItem soundDelay5secOption;
    private JRadioButtonMenuItem soundDelay10secOption;
    private JRadioButtonMenuItem soundDelay20secOption;
    private JRadioButtonMenuItem soundDelayCustomOption;
    private JMenu soundPermissionMenu;
    private JRadioButtonMenuItem soundPermEveryoneOption;
    private JRadioButtonMenuItem soundPermSDMBOption;
    private JRadioButtonMenuItem soundPermDMBOption;
    private JRadioButtonMenuItem soundPermModAndBroadOption;
    private JRadioButtonMenuItem soundPermBroadOption;
    private JMenuItem manageTextCommandsOption;
    private JMenu runAdMenu;
    private JMenuItem timeOption30sec;
    private JMenuItem timeOption60sec;
    private JMenuItem timeOption90sec;
    private JMenuItem timeOption120sec;
    private JMenuItem timeOption150sec;
    private JMenuItem timeOption180sec;
    private JMenuItem updateStatusOption;
    private JCheckBoxMenuItem subOnlyToggle;
    private JMenu slowModeMenu;
    private JRadioButtonMenuItem slowModeOffOption;
    private JRadioButtonMenuItem slowMode5secOption;
    private JRadioButtonMenuItem slowMode10secOption;
    private JRadioButtonMenuItem slowMode15secOption;
    private JRadioButtonMenuItem slowMode30secOption;
    private JRadioButtonMenuItem slowModeCustomOption;
    private JMenu helpMenu;
    private JMenuItem projectGithubOption;
    private JMenuItem projectWikiOption;
    private JMenuItem projectDetailsOption;
    public static DraggableTabbedPane channelPane;
    private JScrollPane allChatsScroll;
    private JTextPane allChats;
    private JLabel dankLabel;
    private JScrollPane scrollPane1;
    private JButton emoteButton;
    public static JTextArea userChat;
}