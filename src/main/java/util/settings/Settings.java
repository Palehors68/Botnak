package util.settings;

import face.*;
import gui.ChatPane;
import gui.CombinedChatPane;
import gui.forms.GUIEmotes;
import gui.forms.GUIMain;
import irc.Donor;
import irc.Subscriber;
import irc.account.Account;
import irc.account.AccountManager;
import irc.account.OAuth;
import irc.account.Task;
import lib.pircbot.Channel;
import lib.pircbot.ChannelManager;
import sound.Sound;
import sound.SoundEngine;
import thread.ThreadEngine;
import util.Constants;
import util.Utils;
import util.comm.Command;
import util.comm.ConsoleCommand;
import util.misc.Donation;

import javax.swing.filechooser.FileSystemView;
import javax.swing.text.StyleConstants;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * This class is the container for every setting Botnak has.
 * There's accounts, booleans of all sorts, and ints, you name it.
 * <p/>
 * What is unique about this is you can define an "account" which
 * may be in the future to prevent unnecessary logging out. For now
 * we can continue to use a "default" account.
 */
public class Settings {

    //accounts
    public AccountManager accountManager = null;
    public ChannelManager channelManager = null;
    public String lastFMAccount = "";
    public int botReplyType = 0; //0 = none, 1 = botnak user only, 2 = everyone
    public boolean botWhisperMode = false;
    public boolean ffzEmotes = false, subEmotes = false, soundsEnabled = false, autoReconnectAccounts = true;
    

    //donations
    public DonationManager donationManager = null;
    public boolean loadedDonationSounds = false;
    public SubscriberManager subscriberManager = null;
    public boolean loadedSubSounds = false;

    //custom directories
    public String defaultSoundDir = "";
    public String defaultFaceDir = "";

    //icons
    public URL modIcon;
    public URL broadIcon;
    public URL adminIcon;
    public URL staffIcon;
    public URL turboIcon;
    public URL primeIcon;
    public boolean useMod = false;//"should use a custom mod icon"
    public boolean useBroad = false;
    public boolean useAdmin = false;
    public boolean useStaff = false;

    //API Keys
    public String youTubeKey = "";
    public String twitterKey = "";
    public String twitterSecret = "";
    public String unshortenitKey = "";
    public String twitchClientID = "";
    
    //font
    public Font font;

    //directories
    public static File defaultDir = new File(FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath()
            + File.separator + "Palehorsbot");
    public File faceDir = new File(defaultDir + File.separator + "Faces");
    public File nameFaceDir = new File(defaultDir + File.separator + "NameFaces");
    public File twitchFaceDir = new File(defaultDir + File.separator + "TwitchFaces");
    public File frankerFaceZDir = new File(defaultDir + File.separator + "FrankerFaceZ");
    public File subIconsDir = new File(defaultDir + File.separator + "SubIcons");
    public File subSoundDir = new File(defaultDir + File.separator + "SubSounds");
    public File donationSoundDir = new File(defaultDir + File.separator + "DonationSounds");
    public File logDir = new File(defaultDir + File.separator + "Logs");
    //files
    public File accountsFile = new File(defaultDir + File.separator + "acc.ini");
    public File APIKeysFile = new File(defaultDir + File.separator + "APIKeys.ini");
    public File tabsFile = new File(defaultDir + File.separator + "tabs.txt");
    public File soundsFile = new File(defaultDir + File.separator + "sounds.txt");
    public File faceFile = new File(defaultDir + File.separator + "faces.txt");
    public File twitchFaceFile = new File(defaultDir + File.separator + "twitchfaces.txt");
    public File userColFile = new File(defaultDir + File.separator + "usercols.txt");
    public File commandsFile = new File(defaultDir + File.separator + "commands.txt");
    public File ccommandsFile = new File(defaultDir + File.separator + "chatcom.txt");
    public File defaultsFile = new File(defaultDir + File.separator + "defaults.ini");
    public static File lafFile = new File(defaultDir + File.separator + "laf.txt");
    public File windowFile = new File(defaultDir + File.separator + "window.txt");
    public File keywordsFile = new File(defaultDir + File.separator + "keywords.txt");
    public File subIconsFile = new File(defaultDir + File.separator + "subIcons.txt");
    public File donatorsFile = new File(defaultDir + File.separator + "donators.txt");
    public File donationsFile = new File(defaultDir + File.separator + "donations.txt");
    public File subsFile = new File(defaultDir + File.separator + "subs.txt");
    public File quotesFile = new File(defaultDir + File.separator + "quotes.txt");

    //appearance
    public boolean logChat = false;
    public int chatMax = 100;
    public boolean cleanupChat = true;
    public static String lookAndFeel = "lib.jtattoo.com.jtattoo.plaf.hifi.HiFiLookAndFeel";
    public int faceMaxHeight = 20;
    //Graphite = "lib.jtattoo.com.jtattoo.plaf.graphite.GraphiteLookAndFeel"

    public String date;
    public float soundVolumeGain = 100;
    public String welcomeMessage = "";

    public Settings() {//default account
        modIcon = Settings.class.getResource("/image/mod.png");
        broadIcon = Settings.class.getResource("/image/broad.png");
        adminIcon = Settings.class.getResource("/image/admin.png");
        staffIcon = Settings.class.getResource("/image/staff.png");
        turboIcon = Settings.class.getResource("/image/turbo.png");
        primeIcon = Settings.class.getResource("/image/prime.png");
        long time = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy");
        date = sdf.format(new Date(time));
        logDir.mkdirs();
        font = new Font("Calibri", Font.PLAIN, 18);
        defaultDir.mkdirs();
        faceDir.mkdirs();
        nameFaceDir.mkdirs();
        twitchFaceDir.mkdirs();
        subIconsDir.mkdirs();
        subSoundDir.mkdirs();
        donationSoundDir.mkdirs();
        //TODO if frankerFaceZEnable
        frankerFaceZDir.mkdirs();
    }

    /**
     * This void loads everything Botnak will use, and sets the appropriate settings.
     */
    public void load() {
        ThreadEngine.submit(() -> {
            loadWindow();
            accountManager = new AccountManager();
            channelManager = new ChannelManager();
            accountManager.start();
            donationManager = new DonationManager();
            subscriberManager = new SubscriberManager();
            if (Utils.areFilesGood(accountsFile.getAbsolutePath())) {
                GUIMain.log("Loading accounts...");
                loadPropData(0);
            }
            if (Utils.areFilesGood(defaultsFile.getAbsolutePath())) {
                GUIMain.log("Loading defaults...");
                loadPropData(1);
            }
            if (Utils.areFilesGood(APIKeysFile.getAbsolutePath())){
            	GUIMain.log("Loading API Keys...");
            	loadPropData(2);
            }
            if (Utils.areFilesGood(tabsFile.getAbsolutePath()) && accountManager.getUserAccount() != null) {
                GUIMain.log("Loading tabs...");
                loadTabState();
            }
            if (Utils.areFilesGood(soundsFile.getAbsolutePath())) {
                GUIMain.log("Loading sounds...");
                loadSounds();
            }
            if (subSoundDir.exists() && subSoundDir.list().length > 0) {
                GUIMain.log("Loading sub sounds...");
                doLoadSubSounds();
            }
            if (donationSoundDir.exists() && donationSoundDir.list().length > 0) {
                GUIMain.log("Loading donation sounds...");
                doLoadDonationSounds();
                loadDonationSounds();
            }
            if (Utils.areFilesGood(quotesFile.getAbsolutePath())) {
            	GUIMain.log("Loading quotes...");
            	loadQuotes();
            }
            if (Utils.areFilesGood(userColFile.getAbsolutePath())) {
                GUIMain.log("Loading user colors...");
                loadUserColors();
            }
            if (Utils.areFilesGood(donatorsFile.getAbsolutePath())) {
                GUIMain.log("Loading donors...");
                loadDonors();
            }
            if (Utils.areFilesGood(donationsFile.getAbsolutePath())) {
                GUIMain.log("Loading donations...");
                loadDonations();//these are stored locally
            }
            //checks online for offline donations and adds them
            if (donationManager.canCheck()) {
                donationManager.checkDonations(false);
                donationManager.ranFirstCheck = true;
            }
            //TODO implement #canCheckSubs()
            if (!subscriberManager.ranInitialCheck && accountManager.getUserAccount() != null) {
                subscriberManager.scanInitialSubscribers(accountManager.getUserAccount().getName(),
                        accountManager.getUserAccount().getKey().getKey().split(":")[1], 0, new HashSet<>());
            }
            if (Utils.areFilesGood(subsFile.getAbsolutePath())) {
                GUIMain.log("Loading subscribers...");
                loadSubscribers();
            }
            if (Utils.areFilesGood(commandsFile.getAbsolutePath())) {
                GUIMain.log("Loading text commands...");
//                loadCommands();
                GUIMain.commandSet = Utils.loadCommands(commandsFile);
            }
            if (Utils.areFilesGood(subIconsFile.getAbsolutePath())) {
                GUIMain.log("Loading subscriber icons...");
                loadSubIcons();
            }
            File[] nameFaces = nameFaceDir.listFiles();
            if (nameFaces != null && nameFaces.length > 0) {
                GUIMain.log("Loading name faces...");
                loadNameFaces();
            }
            //TODO if frankerFaceZEnable
            if (frankerFaceZDir.exists() && frankerFaceZDir.length() > 0) {
                GUIMain.log("Loading FrankerFaceZ faces...");
                loadFFZFaces();
            }
            GUIMain.log("Loading keywords...");
            loadKeywords();//first time boot adds the username
            GUIMain.log("Loading console commands...");
            loadConsoleCommands();//has to be out of the check for files for first time boot
            if (Utils.areFilesGood(faceFile.getAbsolutePath())) {
                GUIMain.log("Loading custom faces...");
                loadFaces();
            }
            GUIMain.log("Loading default Twitch faces...");
            if (Utils.areFilesGood(twitchFaceFile.getAbsolutePath())) {
                loadDefaultTwitchFaces();
            }
            FaceManager.loadDefaultFaces();
        });
    }

    /**
     * This handles saving all the settings that need saved.
     */
    public void save() {
        saveLAF();
        saveWindow();
        if (accountManager.getUserAccount() != null || accountManager.getBotAccount() != null) savePropData(0);
        savePropData(1);
        savePropData(2);
        if (!SoundEngine.getEngine().getSoundMap().isEmpty()) saveSounds();
        if (!FaceManager.faceMap.isEmpty()) saveFaces();
        if (!FaceManager.twitchFaceMap.isEmpty()) saveTwitchFaces();
        saveTabState();
        if (!GUIMain.userColMap.isEmpty()) saveUserColors();
        if (GUIMain.loadedCommands()) Utils.saveCommands(commandsFile, GUIMain.commandSet);
        if (!GUIMain.keywordMap.isEmpty()) saveKeywords();
        if (!FaceManager.subIconSet.isEmpty()) saveSubIcons();
        if (!donationManager.getDonors().isEmpty()) saveDonors();
        if (!donationManager.getDonations().isEmpty()) saveDonations();
        if (!subscriberManager.getSubscribers().isEmpty()) saveSubscribers();
        Utils.saveConCommands(ccommandsFile, GUIMain.conCommands);
    }
    
//    public void save(String channel){
//    	Channel ch = channelManager.getChannel(channel);
//    	if (ch == null) return;
//    	channel = channel.startsWith("#") ? channel.substring(1).toLowerCase() : channel.toLowerCase();
//    	File chanDir = new File(defaultDir + File.separator + "Channels" + File.separator + channel);
////    	chanDir.mkdirs();
//    	
//    	//Commands
//    	String oldCommFile = commandsFile.getAbsolutePath();
//    	commandsFile = new File(chanDir + File.separator + "commands.txt");
////    	if (ch.g)
//    }

    /**
     * *********VOIDS*************
     */

    //TODO make this load from just one file, but separating the setters based on the int type
    public void loadPropData(int type) {
        Properties p = new Properties();
        if (type == 0) {//accounts
            try {
                p.load(new FileInputStream(accountsFile));
                String userNorm = p.getProperty("UserNorm", "").toLowerCase();
                String userNormPass = p.getProperty("UserNormPass", "");
                String status = p.getProperty("CanStatus", "false");
                String commercial = p.getProperty("CanCommercial", "false");
                if (!userNorm.equals("") && !userNormPass.equals("") && userNormPass.contains("oauth")) {
                    boolean stat = Boolean.parseBoolean(status);
                    boolean ad = Boolean.parseBoolean(commercial);
                    accountManager.setUserAccount(new Account(userNorm, new OAuth(userNormPass, stat, ad)));
                }
                String userBot = p.getProperty("UserBot", "").toLowerCase();
                String userBotPass = p.getProperty("UserBotPass", "");
                if (!userBot.equals("") && !userBotPass.equals("") && userBotPass.contains("oauth")) {
                    accountManager.setBotAccount(new Account(userBot, new OAuth(userBotPass, false, false)));
                }
                if (accountManager.getUserAccount() != null) {
                    accountManager.addTask(new Task(null, Task.Type.CREATE_VIEWER_ACCOUNT, null));
                }
                if (accountManager.getBotAccount() != null) {
                    accountManager.addTask(new Task(null, Task.Type.CREATE_BOT_ACCOUNT, null));
                }
            } catch (Exception e) {
                GUIMain.log(e);
            }
        }
        if (type == 1) {//defaults
            try {
                p.load(new FileInputStream(defaultsFile));
                subscriberManager.ranInitialCheck = Boolean.parseBoolean(p.getProperty("RanInitSub", "false"));
                lastFMAccount = p.getProperty("LastFMAccount", "");
                String donation_client_id = p.getProperty("DCID", "");
                String donation_client_oauth = p.getProperty("DCOAUTH", "");
                if (!"".equals(donation_client_id)) donationManager.setClientID(donation_client_id);
                if (!"".equals(donation_client_oauth)) donationManager.setAccessCode(donation_client_oauth);
                defaultFaceDir = p.getProperty("FaceDir", "");
                defaultSoundDir = p.getProperty("SoundDir", "");
                useMod = Boolean.parseBoolean(p.getProperty("UseMod", "false"));
                welcomeMessage = p.getProperty("WelcomeMessage");
                try {
                    modIcon = new URL(p.getProperty("CustomMod", modIcon.toString()));
                } catch (Exception e) {
                    GUIMain.log(e);
                }
                useBroad = Boolean.parseBoolean(p.getProperty("UseBroad", "false"));
                try {
                    broadIcon = new URL(p.getProperty("CustomBroad", broadIcon.toString()));
                } catch (Exception e) {
                    GUIMain.log(e);
                }
                useAdmin = Boolean.parseBoolean(p.getProperty("UseAdmin", "false"));
                try {
                    adminIcon = new URL(p.getProperty("CustomAdmin", adminIcon.toString()));
                } catch (Exception e) {
                    GUIMain.log(e);
                }
                useStaff = Boolean.parseBoolean(p.getProperty("UseStaff", "false"));
                try {
                    staffIcon = new URL(p.getProperty("CustomStaff", staffIcon.toString()));
                } catch (Exception e) {
                    GUIMain.log(e);
                }
                cleanupChat = Boolean.parseBoolean(p.getProperty("ClearChat", "true"));
                logChat = Boolean.parseBoolean(p.getProperty("LogChat", "false"));
                chatMax = Integer.parseInt(p.getProperty("MaxChat", "100"));
                faceMaxHeight = Integer.parseInt(p.getProperty("FaceMaxHeight", "20"));
                font = Utils.stringToFont(p.getProperty("Font").split(","));
                StyleConstants.setFontFamily(GUIMain.norm, font.getFamily());
                StyleConstants.setFontSize(GUIMain.norm, font.getSize());
                SoundEngine.getEngine().setPermission(Integer.parseInt(p.getProperty("SoundEnginePerm", "1")));
                SoundEngine.getEngine().setDelay(Integer.parseInt(p.getProperty("SoundEngineDelay", "10000")));
                botReplyType = Integer.parseInt(p.getProperty("BotReplyType", "0"));
                botWhisperMode = Boolean.parseBoolean(p.getProperty("BotWhisperMode", "false"));
                GUIMain.instance.setBotReplyRadioButton();
                subEmotes = Boolean.parseBoolean(p.getProperty("subEmotesToggle", "false"));
                ffzEmotes = Boolean.parseBoolean(p.getProperty("ffzEmotesToggle", "false"));
                GUIMain.instance.setEmoteSwitches();
                GUIMain.log("Loaded defaults!");
            } catch (Exception e) {
                GUIMain.log(e);
            }
        }
        if (type == 2){//APIKeys
        	try {
                p.load(new FileInputStream(APIKeysFile));
                
                unshortenitKey = p.getProperty("unshortenitAPIKey","");
                youTubeKey = p.getProperty("youTubeAPIKey","");
                twitterKey = p.getProperty("twitterAPIKey","");
                twitterSecret = p.getProperty("twitterAPISecret");
                twitchClientID = p.getProperty("twitchClientID");
                GUIMain.log("Loaded API Keys!");
        	} catch (Exception e) {
                GUIMain.log(e);
            }
        }
    }

    public void savePropData(int type) {
        Properties p = new Properties();
        if (type == 0) {//account data
            Account user = accountManager.getUserAccount();
            Account bot = accountManager.getBotAccount();
            if (user != null) {
                OAuth key = user.getKey();
                p.put("UserNorm", user.getName());
                p.put("UserNormPass", key.getKey());
                p.put("CanStatus", String.valueOf(key.canSetTitle()));
                p.put("CanCommercial", String.valueOf(key.canPlayAd()));
            }
            if (bot != null) {
                OAuth key = bot.getKey();
                p.put("UserBot", bot.getName());
                p.put("UserBotPass", key.getKey());
            }
            try {
                p.store(new FileWriter(accountsFile), "Account Info");
            } catch (IOException e) {
                GUIMain.log(e);
            }
        }
        if (type == 1) {//deaults data
            p.put("RanInitSub", String.valueOf(subscriberManager.ranInitialCheck));
            p.put("LastFMAccount", lastFMAccount);
            p.put("DCID", donationManager.getClientID());
            p.put("DCOAUTH", donationManager.getAccessCode());
            if (defaultFaceDir != null && !defaultFaceDir.equals("")) {
                p.put("FaceDir", defaultFaceDir);
            }
            if (defaultSoundDir != null && !defaultSoundDir.equals("")) {
                p.put("SoundDir", defaultSoundDir);
            }
            p.put("UseMod", String.valueOf(useMod));
            p.put("CustomMod", modIcon.toString());
            p.put("UseBroad", String.valueOf(useBroad));
            p.put("CustomBroad", broadIcon.toString());
            p.put("UseAdmin", String.valueOf(useAdmin));
            p.put("CustomAdmin", adminIcon.toString());
            p.put("UseStaff", String.valueOf(useStaff));
            p.put("CustomStaff", staffIcon.toString());
            p.put("MaxChat", String.valueOf(chatMax));
            p.put("FaceMaxHeight", String.valueOf(faceMaxHeight));
            p.put("ClearChat", String.valueOf(cleanupChat));
            p.put("LogChat", String.valueOf(logChat));
            p.put("Font", Utils.fontToString(font));
            p.put("SoundEnginePerm", String.valueOf(SoundEngine.getEngine().getPermission()));
            p.put("SoundEngineDelay", String.valueOf(SoundEngine.getEngine().getDelay()));
            p.put("BotReplyType", String.valueOf(botReplyType));
            p.put("BotWhisperMode", String.valueOf(botWhisperMode));
            p.put("ffzEmotesToggle", String.valueOf(GUIEmotes.getFfzEmoteToggle()));
            p.put("subEmotesToggle", String.valueOf(GUIEmotes.getSubEmoteToggle()));
            p.put("WelcomeMessage", welcomeMessage);
            try {
                p.store(new FileWriter(defaultsFile), "Default Settings");
            } catch (IOException e) {
                GUIMain.log(e);
            }
        }
        if (type == 2){//APIKey data
        	p.put("unshortenitAPIKey", unshortenitKey);
        	p.put("youTubeAPIKey", youTubeKey);
        	p.put("twitterAPIKey", twitterKey);
        	p.put("twitterAPISecret", twitterSecret);
        	p.put("twitchClientID", twitchClientID);
        	try{
        		p.store(new FileWriter(APIKeysFile), "API Keys");
        		
        	} catch (IOException e){
        		GUIMain.log(e);
        	}
        }
    }

    /**
     * Quotes
     */
    public void loadQuotes() {
    	try (BufferedReader br = new BufferedReader(new InputStreamReader(quotesFile.toURI().toURL().openStream()))) {
    		String line;
    		while ((line = br.readLine()) != null) {
    			GUIMain.quotes.add(line);
    		}
    		GUIMain.log("Loaded quotes!");
    	} catch (Exception e) {
    		GUIMain.log(e.getMessage());
    	}
    }
    
    public void saveQuotes() {
    	try (PrintWriter br = new PrintWriter(quotesFile)) {
                for (String next : GUIMain.quotes) {
                    if (next != null) {
                        br.print(next);
                        br.println();
                    }
                }
    	} catch (Exception e) {
    		GUIMain.log(e.getMessage());
    	}
    }
    /**
     * Sounds
     */
    public void loadSounds() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(soundsFile.toURI().toURL().openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(",");
                int startIdx = line.indexOf(",", line.indexOf(",", 0) + 1);//name,0,<- bingo
                String[] split2add = line.substring(startIdx + 1).split(",");//files
                int perm = 0;
                try {
                    perm = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    GUIMain.log(split[0] + " has a problem. Making it public.");
                }
                SoundEngine.getEngine().getSoundMap().put(split[0], new Sound(perm, split2add));
            }
            GUIMain.log("Loaded sounds!");
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    public void saveSounds() {
        try (PrintWriter br = new PrintWriter(soundsFile)) {
            Set<String> keys = SoundEngine.getEngine().getSoundMap().keySet();
            keys.stream().filter(s -> s != null && SoundEngine.getEngine().getSoundMap().get(s) != null).forEach(s -> {
                Sound boii = SoundEngine.getEngine().getSoundMap().get(s);//you're too young to play that sound, boy
                StringBuilder sb = new StringBuilder();
                sb.append(s);
                sb.append(",");
                sb.append(boii.getPermission());
                for (String soundboy : boii.getSounds().data) {
                    sb.append(",");
                    sb.append(soundboy);
                }
                br.println(sb.toString());
            });
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    public boolean doLoadSubSounds() {
        if (loadSubSounds()) {
            GUIMain.log("Loaded sub sounds!");
            loadedSubSounds = true;
            return true;
        } else return false;
    }

    public boolean loadSubSounds() {
        boolean toReturn = false;
        try {
            File[] files = subSoundDir.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    SoundEngine.getEngine().getSubStack().add(new Sound(5, f.getAbsolutePath()));
                }
                Collections.shuffle(SoundEngine.getEngine().getSubStack());
                toReturn = true;
            }
        } catch (Exception e) {
            GUIMain.log(e);
        }
        return toReturn;
    }

    private void doLoadDonationSounds() {
        loadDonationSounds();
        loadedDonationSounds = true;
        GUIMain.log("Loaded donation sounds!");
    }

    public void loadDonationSounds() {
        try {
            File[] files = donationSoundDir.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    SoundEngine.getEngine().getDonationStack().add(new Sound(5, f.getAbsolutePath()));
                }
                Collections.shuffle(SoundEngine.getEngine().getDonationStack());
            }
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }


    /**
     * User Colors
     */
    public void loadUserColors() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(userColFile.toURI().toURL().openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(",");
                GUIMain.userColMap.put(split[0], new Color(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3])));
            }
            GUIMain.log("Loaded user colors!");
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    public void saveUserColors() {
        try (PrintWriter br = new PrintWriter(userColFile)) {
            GUIMain.userColMap.keySet().stream().filter(
                    s -> s != null && GUIMain.userColMap.get(s) != null).forEach(
                    s -> br.println(s + "," +
                            GUIMain.userColMap.get(s).getRed() + "," +
                            GUIMain.userColMap.get(s).getGreen() + "," +
                            GUIMain.userColMap.get(s).getBlue()));
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }


    /**
     * Saves the faces to the given text file.
     * The map is unique, as the key is the name of the face, which could be the same as the regex
     * if it was added via !addface and no regex was specified.
     */
    public void saveFaces() {
        try (PrintWriter br = new PrintWriter(faceFile)) {
            FaceManager.faceMap.keySet().stream().filter(s -> s != null && FaceManager.faceMap.get(s) != null).forEach(s -> {
                Face fa = FaceManager.faceMap.get(s);
                br.println(s + "," + fa.getRegex() + "," + fa.getFilePath());
            });
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    /**
     * Loads the face data stored in the faces.txt file. This only gets called
     * if that file exists.
     */
    public void loadFaces() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(faceFile.toURI().toURL().openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(",");
                //                        name           name/regex   path
                FaceManager.faceMap.put(split[0], new Face(split[1], split[2]));
            }
            FaceManager.doneWithFaces = true;
            GUIMain.log("Loaded custom faces!");
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    /**
     * Saves the default twitch faces.
     */
    public void saveTwitchFaces() {
        try (PrintWriter br = new PrintWriter(twitchFaceFile)) {
            FaceManager.twitchFaceMap.keySet().stream().filter(s -> s != null && FaceManager.twitchFaceMap.get(s) != null)
                    .forEach(s -> {
                        TwitchFace fa = FaceManager.twitchFaceMap.get(s);
                        br.println(s + "," + fa.getRegex() + "," + Boolean.toString(fa.isEnabled()) + "," + fa.getEmoticonSet());
                    });
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    /**
     * Loads the default twitch faces already saved on the computer.
     */
    public void loadDefaultTwitchFaces() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(twitchFaceFile.toURI().toURL().openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] split = line.split(",");
                    int emoteID = Integer.parseInt(split[0]);
                    TwitchFace tf = new TwitchFace(split[1],
                            new File(twitchFaceDir + File.separator + String.valueOf(emoteID) + ".png").getAbsolutePath(),
                            Boolean.parseBoolean(split[2]),
                            Integer.parseInt(split[3]));
                    FaceManager.twitchFaceMap.put(emoteID, tf);
                } catch (Exception e) {
                    GUIMain.log(e);
                }
            }
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }


    /**
     * FrankerFaceZ
     * <p/>
     * We can be a little more broad about this saving, since it's a per-channel basis
     */
    public void loadFFZFaces() {
        File[] channels = frankerFaceZDir.listFiles();
        if (channels == null) return;
        for (File channel : channels) {
            if (channel.isDirectory() && channel.length() > 0) {
                File[] faces = channel.listFiles();
                if (faces != null) {
                    ArrayList<FrankerFaceZ> loadedFaces = new ArrayList<>();
                    for (File face : faces) {
                        if (face != null) {
                            loadedFaces.add(new FrankerFaceZ(face.getName(), face.getAbsolutePath(), true));
                        }
                    }
                    FaceManager.ffzFaceMap.put(channel.getName(), loadedFaces);
                } else {
                    channel.delete();
                }
            }
        }
    }



    public void loadConsoleCommands() {
        HashSet<ConsoleCommand> hardcoded = new HashSet<>();
        hardcoded.add(new ConsoleCommand("addcmd", ConsoleCommand.Action.ADD_TEXT_COMMAND, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("adddonation", ConsoleCommand.Action.ADD_DONATION, Constants.PERMISSION_DEV, null));
        hardcoded.add(new ConsoleCommand("addface", ConsoleCommand.Action.ADD_FACE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("addkeyword", ConsoleCommand.Action.ADD_KEYWORD, Constants.PERMISSION_DEV, null));
        hardcoded.add(new ConsoleCommand("addquote", ConsoleCommand.Action.ADD_QUOTE, Constants.PERMISSION_MOD,null));
        hardcoded.add(new ConsoleCommand("addrafflewinner", ConsoleCommand.Action.ADD_RAFFLE_WINNER, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("addsound", ConsoleCommand.Action.ADD_SOUND, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("botreply", ConsoleCommand.Action.SEE_OR_SET_REPLY_TYPE, Constants.PERMISSION_DEV, null));
        hardcoded.add(new ConsoleCommand("cancelpoll", ConsoleCommand.Action.CANCEL_POLL, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("cat", ConsoleCommand.Action.CAT, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("changeface", ConsoleCommand.Action.CHANGE_FACE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("changesound", ConsoleCommand.Action.CHANGE_SOUND, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("damperace", ConsoleCommand.Action.DAMPE_RACE, Constants.PERMISSION_MOD,null));
        hardcoded.add(new ConsoleCommand("delcmd", ConsoleCommand.Action.REMOVE_TEXT_COMMAND, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("editcmd", ConsoleCommand.Action.ADD_TEXT_COMMAND, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("game", ConsoleCommand.Action.SEE_STREAM_GAME, Constants.PERMISSION_ALL, null));
        hardcoded.add(new ConsoleCommand("help", ConsoleCommand.Action.HELP, Constants.PERMISSION_ALL, null));
        hardcoded.add(new ConsoleCommand("host", ConsoleCommand.Action.HOST_USER, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("judgerace", ConsoleCommand.Action.JUDGE_RACE, Constants.PERMISSION_MOD,null));
        hardcoded.add(new ConsoleCommand("lastdonationsound", ConsoleCommand.Action.SEE_PREV_SOUND_DON, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("lastsubsound", ConsoleCommand.Action.SEE_PREV_SOUND_SUB, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("play", ConsoleCommand.Action.SET_GAME, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("playad", ConsoleCommand.Action.PLAY_ADVERT, Constants.PERMISSION_DEV, null));
        hardcoded.add(new ConsoleCommand("playnothing", ConsoleCommand.Action.CLEAR_GAME, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("pollresult", ConsoleCommand.Action.POLL_RESULT, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("quote", ConsoleCommand.Action.GET_QUOTE, Constants.PERMISSION_MOD,null));
        hardcoded.add(new ConsoleCommand("removeallquotes", ConsoleCommand.Action.REMOVE_ALL_QUOTES, Constants.PERMISSION_MOD,null));
        hardcoded.add(new ConsoleCommand("removeface", ConsoleCommand.Action.REMOVE_FACE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("removekeyword", ConsoleCommand.Action.REMOVE_KEYWORD, Constants.PERMISSION_DEV, null));
        hardcoded.add(new ConsoleCommand("removenameface", ConsoleCommand.Action.REMOVE_NAME_FACE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("removequote", ConsoleCommand.Action.REMOVE_QUOTE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("removerafflewinner", ConsoleCommand.Action.REMOVE_RAFFLE_WINNER, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("removesound", ConsoleCommand.Action.REMOVE_SOUND, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("setcol", ConsoleCommand.Action.SET_USER_COL, Constants.PERMISSION_ALL, null));
        hardcoded.add(new ConsoleCommand("setgame", ConsoleCommand.Action.SET_STREAM_GAME, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("setnameface", ConsoleCommand.Action.SET_NAME_FACE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("setpermission", ConsoleCommand.Action.SET_COMMAND_PERMISSION, Constants.PERMISSION_DEV, null));
        hardcoded.add(new ConsoleCommand("setsound", ConsoleCommand.Action.SET_SOUND_DELAY, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("setsoundperm", ConsoleCommand.Action.SET_SOUND_PERMISSION, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("setsubsound", ConsoleCommand.Action.SET_SUB_SOUND, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("settitle", ConsoleCommand.Action.SET_STREAM_TITLE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("song", ConsoleCommand.Action.NOW_PLAYING, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("soundstate", ConsoleCommand.Action.SEE_SOUND_STATE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("startpoll", ConsoleCommand.Action.START_POLL, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("startraffle", ConsoleCommand.Action.START_RAFFLE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("stopallsounds", ConsoleCommand.Action.STOP_ALL_SOUNDS, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("stopraffle", ConsoleCommand.Action.STOP_RAFFLE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("stopsound", ConsoleCommand.Action.STOP_SOUND, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("talk", ConsoleCommand.Action.TALK, Constants.PERMISSION_DEV, null));
        hardcoded.add(new ConsoleCommand("throttle", ConsoleCommand.Action.THROTTLE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("throttlebot", ConsoleCommand.Action.THROTTLEBOT, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("title", ConsoleCommand.Action.SEE_STREAM_TITLE, Constants.PERMISSION_ALL, null));
        hardcoded.add(new ConsoleCommand("toggleface", ConsoleCommand.Action.TOGGLE_FACE, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("togglesound", ConsoleCommand.Action.TOGGLE_SOUND, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("uptime", ConsoleCommand.Action.SHOW_UPTIME, Constants.PERMISSION_ALL, null));
        hardcoded.add(new ConsoleCommand("volume", ConsoleCommand.Action.SEE_OR_SET_VOLUME, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("vote", ConsoleCommand.Action.VOTE_POLL, Constants.PERMISSION_ALL, null));
        hardcoded.add(new ConsoleCommand("whisper", ConsoleCommand.Action.WHISPER, Constants.PERMISSION_DEV, null));
        hardcoded.add(new ConsoleCommand("winners", ConsoleCommand.Action.SEE_WINNERS, Constants.PERMISSION_MOD, null));
        hardcoded.add(new ConsoleCommand("wr", ConsoleCommand.Action.WR, Constants.PERMISSION_ALL, null, "!wr returns the Any% WR of the current game. !wr <game> / <category> searches for the game/category"));
        hardcoded.add(new ConsoleCommand("followage", ConsoleCommand.Action.FOLLOWAGE, Constants.PERMISSION_ALL, null));

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
                    String helpText = "";
                    if (split.length == 5) helpText = split[4];
                    GUIMain.conCommands.add(new ConsoleCommand(split[0], a, classPerm, customUsers, helpText));
                }
                if (GUIMain.conCommands.size() != hardcoded.size()) { //something's not right...
                    for (ConsoleCommand hard : hardcoded) {
                        boolean isAdded = false;
                        for (ConsoleCommand loaded : GUIMain.conCommands) {
                            if (hard.getTrigger().equalsIgnoreCase(loaded.getTrigger())) {
                                isAdded = true;
                                //this ensures every hard coded ConCommand is loaded for Botnak
                                break;
                            }
                        }
                        if (!isAdded) {//if it isn't in the file, add it
                            GUIMain.conCommands.add(hard);
                        }
                    }
                }
            } catch (Exception e) {
                GUIMain.log(e);
            }
        } else { //first time boot/reset/deleted file etc.
            GUIMain.conCommands.addAll(hardcoded.stream().collect(Collectors.toList()));
        }
        GUIMain.log("Loaded console commands!");
    }


    /**
     * Keywords
     */
    public void loadKeywords() {
        if (Utils.areFilesGood(keywordsFile.getAbsolutePath())) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(keywordsFile.toURI().toURL().openStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] split = line.split(",");
                    int r, g, b;
                    try {
                        r = Integer.parseInt(split[1]);
                    } catch (Exception e) {
                        r = 255;
                    }
                    try {
                        g = Integer.parseInt(split[2]);
                    } catch (Exception e) {
                        g = 255;
                    }
                    try {
                        b = Integer.parseInt(split[3]);
                    } catch (Exception e) {
                        b = 255;
                    }
                    GUIMain.keywordMap.put(split[0], new Color(r, g, b));
                }
            } catch (Exception e) {
                GUIMain.log(e);
            }
        } else {
            if (accountManager.getUserAccount() != null) {
                GUIMain.keywordMap.put(accountManager.getUserAccount().getName(), Color.orange);
            }
        }
        GUIMain.log("Loaded keywords!");
    }

    public void saveKeywords() {
        try (PrintWriter br = new PrintWriter(keywordsFile)) {
            Set<String> keys = GUIMain.keywordMap.keySet();
            keys.stream().filter(word -> word != null).forEach(word -> {
                Color c = GUIMain.keywordMap.get(word);
                br.println(word + "," + c.getRed() + "," + c.getGreen() + "," + c.getBlue());
            });
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    /**
     * Sub icons
     */
    public void saveSubIcons() {
        try (PrintWriter br = new PrintWriter(subIconsFile)) {
            FaceManager.subIconSet.stream().forEach(i -> br.println(i.getChannel() + "," + i.getLength() + "," + i.getFileLoc()));
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    public void loadSubIcons() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(subIconsFile.toURI().toURL().openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(",");
                FaceManager.subIconSet.add(new SubscriberIcon(split[0], split[2], Integer.parseInt(split[1])));
            }
            GUIMain.log("Loaded subscriber icons!");
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    /**
     * Donators
     */
    public void loadDonors() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(donatorsFile.toURI().toURL().openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split(",");
                double amount;
                try {
                    amount = Double.parseDouble(split[1]);
                } catch (Exception e) {
                    amount = 0.0;
                }
                donationManager.getDonors().add(new Donor(split[0], amount));
            }
            GUIMain.log("Loaded donors!");
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }


    public void saveDonors() {
        try (PrintWriter br = new PrintWriter(donatorsFile)) {
            donationManager.getDonors().stream().forEach(d -> br.println(d.getName() + "," + d.getDonated()));
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }


    /**
     * Donations. This ranges from people just giving you money to
     * people subscribing to your channel.
     */
    public void saveDonations() {
        try (PrintWriter br = new PrintWriter(donationsFile)) {
            donationManager.getDonations().stream().sorted().forEach(d ->
                    br.println(d.getDonationID() + "[" + d.getFromWhom() + "[" + d.getNote() + "["
                            + d.getAmount() + "[" + Instant.ofEpochMilli(d.getDateReceived().getTime()).toString()));
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    public void loadDonations() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(donationsFile.toURI().toURL().openStream()))) {
            String line;
            HashSet<Donation> donations = new HashSet<>();
            Donation mostRecent = null;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\\[");
                double amount;
                try {
                    amount = Double.parseDouble(split[3]);
                } catch (Exception e) {
                    amount = 0.0;
                }
                Donation d = new Donation(split[0], split[1], split[2], amount, Date.from(Instant.parse(split[4])));
                if ((mostRecent == null || mostRecent.compareTo(d) > 0) &&
                        !d.getDonationID().equalsIgnoreCase("SUBSCRIBER") &&
                        !d.getDonationID().equals("LOCAL"))
                    mostRecent = d;
                donations.add(d);
            }
            if (mostRecent != null) {
                donationManager.setLastDonation(mostRecent);
                GUIMain.log(String.format("Most recent donation: %s for %s", mostRecent.getFromWhom(),
                        DonationManager.getCurrencyFormat().format(mostRecent.getAmount())));
            }
            if (!donations.isEmpty()) {
                donationManager.fillDonations(donations);
                GUIMain.log("Loaded donations!");
            }
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }


    /**
     * Subscribers of your own channel
     * Saves each subscriber with the first date Botnak meets them
     * and each month check to see if they're still subbed, if not, make them donor
     * with (months subbed * $2.50) as their amount.
     */

    public void saveSubscribers() {
        try (PrintWriter br = new PrintWriter(subsFile)) {
            subscriberManager.getSubscribers().stream().sorted().forEach(
                    s -> br.println(s.getName() + "[" + s.getStarted().toString() + "["
                            + String.valueOf(s.isActive()) + "[" + s.getStreak()));
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    public void loadSubscribers() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(subsFile.toURI().toURL().openStream()))) {
            String line;
            HashSet<Subscriber> subscribers = new HashSet<>();
            Subscriber mostRecent = null;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\\[");
                Subscriber s = new Subscriber(split[0], LocalDateTime.parse(split[1]), Boolean.parseBoolean(split[2]), Integer.parseInt(split[3]));
                subscribers.add(s);
                if (mostRecent == null || mostRecent.compareTo(s) > 0) mostRecent = s;
            }
            if (mostRecent != null) {
                subscriberManager.setLastSubscriber(mostRecent);
                GUIMain.log("Most recent subscriber: " + mostRecent.getName());
            }
            if (!subscribers.isEmpty()) subscriberManager.fillSubscribers(subscribers);
            GUIMain.log("Loaded subscribers!");
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }


    /**
     * Tab State
     * <p/>
     * This is for opening back up to the correct tab, with the correct pane showing.
     * This also handles saving the combined tabs.
     * Format:
     * <p/>
     * Single:
     * bool      bool     string
     * single[  selected[ name
     * <p/>
     * Combined:
     * bool    boolean    String         String     String[] (split(","))
     * single[ selected[ activeChannel[  title[ every,channel,in,it,separated,by,commas
     * <p/>
     * No spaces though.
     * <p/>
     * Single tabs can be invisible if they are in a combined tab.
     */
    public void saveTabState() {
    	if (GUIMain._debug) return;
        try (PrintWriter br = new PrintWriter(tabsFile)) {
            int currentSelectedIndex = GUIMain.channelPane.getSelectedIndex();
            for (int i = 1; i < GUIMain.channelPane.getTabCount() - 1; i++) {
                ChatPane current = Utils.getChatPane(i);
                if (current != null) {
                    if (current.isTabVisible()) {
                        boolean selected = current.getIndex() == currentSelectedIndex;
                        br.println("true[" + String.valueOf(selected) + "[" + current.getChannel());
                    }
                } else {
                    CombinedChatPane cc = Utils.getCombinedChatPane(i);
                    if (cc != null) {
                        //all the panes in them should be set to false
                        //their indexes are technically going to be -1
                        boolean selected = cc.getIndex() == currentSelectedIndex;
                        br.print("false[" + String.valueOf(selected) + "[" + cc.getActiveChannel() + "[" + cc.getTabTitle() + "[");
                        String[] chans = cc.getChannels();
                        for (int s = 0; s < chans.length; s++) {
                            String toPrint = chans[s];
                            br.print(toPrint);
                            if (s != chans.length - 1) br.print(",");
                        }
                        br.println();
                    }
                }
            }
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    public void loadTabState() {
    	if (GUIMain._debug){
    		ChatPane cp = ChatPane.createPane("palehors68");
    		GUIMain.channelSet.add("#palehors68");
    		GUIMain.chatPanes.put(cp.getChannel(), cp);
            GUIMain.channelPane.insertTab(cp.getChannel(), null, cp.getScrollPane(), null, cp.getIndex());
            GUIMain.channelPane.setSelectedIndex(1);
            GUIMain.channelSet.forEach(GUIMain.viewer::doConnect);
            GUIMain.channelSet.forEach(GUIMain.bot::doConnect);
    	} else {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(tabsFile.toURI().toURL().openStream()))) {
            String line;
            int index = 0;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\\[");
                boolean isSingle = Boolean.parseBoolean(split[0]);
                boolean isSelected = Boolean.parseBoolean(split[1]);
                if (isSingle) {
                    String channel = split[2];
                    if (GUIMain.currentSettings.accountManager.getUserAccount() != null) {
                        String channelName = "#" + channel;
                        GUIMain.channelSet.add(channelName);
                    }
                    ChatPane cp = ChatPane.createPane(channel);
                    GUIMain.chatPanes.put(cp.getChannel(), cp);
                    GUIMain.channelPane.insertTab(cp.getChannel(), null, cp.getScrollPane(), null, cp.getIndex());
                    if (isSelected) index = cp.getIndex();
                } else {
                    String activeChannel = split[2];
                    String title = split[3];
                    String[] channels = split[4].split(",");
                    ArrayList<ChatPane> cps = new ArrayList<>();
                    for (String c : channels) {
                        if (GUIMain.currentSettings.accountManager.getUserAccount() != null) {
                            String channelName = "#" + c;
                            GUIMain.channelSet.add(channelName);
                        }
                        ChatPane cp = ChatPane.createPane(c);
                        GUIMain.chatPanes.put(cp.getChannel(), cp);
                        cps.add(cp);
                    }
                    CombinedChatPane ccp = CombinedChatPane.createCombinedChatPane(cps.toArray(new ChatPane[cps.size()]));
                    GUIMain.channelPane.insertTab(ccp.getTabTitle(), null, ccp.getScrollPane(), null, ccp.getIndex());
                    ccp.setCustomTitle(title);
                    if (isSelected) index = ccp.getIndex();
                    if (!activeChannel.equalsIgnoreCase("all")) {
                        ccp.setActiveChannel(activeChannel);
                        ccp.setActiveScrollPane(activeChannel);
                    }
                    GUIMain.combinedChatPanes.add(ccp);
                }
            }
        
            GUIMain.channelPane.setSelectedIndex(index);
            GUIMain.log("Loaded tabs!");
            GUIMain.channelSet.forEach(GUIMain.viewer::doConnect);
            GUIMain.channelSet.forEach(GUIMain.bot::doConnect);
        } catch (Exception e) {
            GUIMain.log(e);
        }
    	}
    }


    /**
     * Name faces
     */
    public void loadNameFaces() {
        try {
            File[] nameFaces = nameFaceDir.listFiles();
            if (nameFaces == null) return;
            for (File nameFace : nameFaces) {
                String name = Utils.removeExt(nameFace.getName());
                FaceManager.nameFaceMap.put(name, new Face(name, nameFace.getAbsolutePath()));
            }
            GUIMain.log("Loaded name faces!");
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }


    /**
     * Look and Feel
     */
    public static void loadLAF() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(lafFile.toURI().toURL().openStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("jtattoo")) {
                    lookAndFeel = line;
                    break;
                }
            }
            br.close();
        } catch (Exception e) {
            lookAndFeel = "lib.jtattoo.com.jtattoo.plaf.hifi.HiFiLookAndFeel";//default to HiFi
        }
    }

    public void saveLAF() {
        try (PrintWriter pr = new PrintWriter(lafFile)) {
            pr.println(lookAndFeel);
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }

    /**
     * Window Properties (location and size)
     */
    public void loadWindow() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(windowFile.toURI().toURL().openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.substring(1).split(",");
                String first = parts[0];
                String second = parts[1];
                if (line.startsWith("p")) {
                    try {
                        int x = Integer.parseInt(first);
                        int y = Integer.parseInt(second);
                        GUIMain.instance.setLocation(x, y);
                    } catch (Exception ignored) {
                    }
                } else if (line.startsWith("s")) {
                    try {
                        double w = Double.parseDouble(first);
                        double h = Double.parseDouble(second);
                        GUIMain.instance.setSize((int) w, (int) h);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void saveWindow() {
        try (PrintWriter pr = new PrintWriter(windowFile)) {
            pr.println("p" + GUIMain.instance.getLocationOnScreen().x + "," + GUIMain.instance.getLocationOnScreen().y);
            pr.println("s" + GUIMain.instance.getSize().getWidth() + "," + GUIMain.instance.getSize().getHeight());
        } catch (Exception e) {
            GUIMain.log(e);
        }
    }
}
