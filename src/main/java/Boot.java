import gui.GUIMain;
import gui.GUIUpdate;
import util.settings.Settings;

import javax.swing.*;

import java.awt.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class Boot {
    public static void main(final String[] args) {
    	
    	/* This section handles the file encoding for Emojis */
    	try{
    	System.setProperty("file.encoding", "UTF-8");
    	Field charset = Charset.class.getDeclaredField("defaultCharset");
    	charset.setAccessible(true);
    	charset.set(null, null);
    	} catch (Exception e) {}
    	
    	
        /* Thread-safe initialization */
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                setLookAndFeel();
                GUIMain g;
                try{
                	g = new GUIMain(Boolean.parseBoolean(args[0]));	
                } catch (Exception e){
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
                    Settings.loadLAF();
                    UIManager.setLookAndFeel(Settings.lookAndFeel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}