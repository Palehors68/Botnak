import gui.forms.GUIMain;
import gui.forms.GUIUpdate;
import thread.ExceptionHandler;
import thread.ShutdownHook;
import util.settings.Settings;

import javax.swing.*;
import java.awt.*;

import java.lang.reflect.Field;
import java.nio.charset.Charset;


/**
 * TODO:
 * Settings GUI
 * Lotsofs: change the output AudioLine
 * Gif faces
 * TextCommands GUI
 * (autocommand)
 * Log Viewer GUI
 *
 * Blacklists for commands
 * Bttv emotes?
 *
 * <p>
 * Bug:
 * Cheer donors should be able to play sounds/have donor benefits after a certain amount
 * fix heartbeat threads from spouting issues (connection-wise) -- use WebSockets?
 * Use Paths.get() instead of File.separators everywhere
 * Refer to List as parameters/member variables
 * Utils.checkText() removal?
 */


public class Boot {
    public static void main(final String[] args) {
        /* Thread-safe initialization */
    	
    	/* This section handles the file encoding for Emojis */
    	    	try{
    	    	System.setProperty("file.encoding", "UTF-8");
    	    	Field charset = Charset.class.getDeclaredField("defaultCharset");
    	    	charset.setAccessible(true);
    	    	charset.set(null, null);
    	    	} catch (Exception e) {}
    	
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                setLookAndFeel();
                GUIMain g;
                try {
                	g = new GUIMain(Boolean.parseBoolean(args[0]));
                } catch (Exception e) {
                	g = new GUIMain(false);
                }
                g.setVisible(true);
                if (GUIUpdate.checkForUpdate()) {
                    GUIUpdate gu = new GUIUpdate();
                    gu.setVisible(true);
                }
            }

            /**
             * Tries to set the swing look and feel.
             * All relevant exceptions are caught.
             */
            private void setLookAndFeel() {
                try {
                    Settings.LAF.load();
                    UIManager.setLookAndFeel(Settings.lookAndFeel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}