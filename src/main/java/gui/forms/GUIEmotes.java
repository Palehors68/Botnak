package gui.forms;

import javax.swing.*;

import util.APIRequests;
import face.FaceManager;
import face.Icons;
import face.TwitchFace;
import face.FrankerFaceZ;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.MalformedURLException;

import lib.JSON.JSONObject;
import lib.pircbot.User;

public class GUIEmotes extends JFrame {

	public GUIEmotes(JTextArea userChat, GUIMain guiMain) {
		this.userChat = userChat;
		this.guiMain = guiMain;
		botnakUser = GUIMain.currentSettings.channelManager.getUser(GUIMain.currentSettings.accountManager.getUserAccount().getName(), false);
		initComponents();
	}
	
	static {
		emoteXref = buildEmoteXref();
    }

	private static HashMap<String, Integer> buildEmoteXref(){
		HashMap<String, Integer> toReturn = new HashMap<String, Integer>();
		try {
			JSONObject emoteXrefList = APIRequests.Twitch.getEmoteXref();
			if (emoteXrefList == null) return null;
			JSONObject sets = emoteXrefList.getJSONObject("sets");
			Iterator<String> setKeys = sets.keys();
			while (setKeys.hasNext()){
				String emoteId = setKeys.next();
				toReturn.put(sets.getString(emoteId), new Integer(Integer.parseInt(emoteId) * -1));
			}
		} catch (Exception e) {
			GUIMain.log(e.getMessage());
			return null;
		}
		
		
		return toReturn;
	}
	public void closeButtonActionPerformed() {
		GUIMain.channelPane.setSelectedIndex(GUIMain.channelPane
				.getSelectedIndex());
		dispose();
	}

	public static void setFfzEmoteToggle(boolean newBool) {
		ffzEmotesToggle = newBool;
	}

	public static void setSubEmoteToggle(boolean newBool) {
		subEmotesToggle = newBool;
	}

	public static boolean getFfzEmoteToggle() {
		return ffzEmotesToggle;
	}

	public static boolean getSubEmoteToggle() {
		return subEmotesToggle;
	}

	/**
	 * Emote regex looks something like this: :-(p|P) where the emote can end in
	 * either a 'p' or 'P'. So this rebuilds the emote using the | char as the
	 * driver. Since the emote can use the char on either side of the |, this
	 * just picks the one before it every time.
	 * 
	 * @param regex
	 *            the String that contains the regular expression for emotes
	 * @return String where regex is selectively removed
	 */
	private String handleRegex(String regex) {
		if (regex.indexOf('|') < 0)
			return regex;
		String toReturn = "";
		int sub = 0;
		int index = regex.indexOf('|', sub);
		do {
			if (sub == index) {
				// This catches the case of the :-| emote that uses the |
				// character
				index = regex.indexOf('|', ++sub);
				continue;
			}
			toReturn += regex.substring(sub, index - 2);
			toReturn += regex.charAt(index - 1);
			sub = index + 3;
			index = regex.indexOf('|', sub);
		} while (index >= 0);
		return toReturn;

	}

	public void refreshEmotes() {
		JLabel emote;
		String emoteName;
		TwitchFace tf;
		URL image;

		while (!(FaceManager.doneWithTwitchFaces && FaceManager.doneWithFrankerFaces)) {
			try {
				Thread.sleep(1000);

			} catch (InterruptedException e) {
				GUIMain.log(e);
			}
		}
		if (this.bigPanel.getComponentCount() > 0)
			this.bigPanel.removeAll();

		SortedMap<Integer, SortedSet<Integer>> sortedFaceMap = new TreeMap<Integer, SortedSet<Integer>>();
		// This will sort each emote twice: first by the set, then by the emote
		// id
		// The sets are stored as negative numbers, and default twitch emotes as
		// set 0
		// This sorts the sub emotes to the top
		for (Map.Entry<Integer, TwitchFace> entry : FaceManager.twitchFaceMap
				.entrySet()) {
			if (sortedFaceMap.get(entry.getValue().getEmoticonSet()) != null) {
				sortedFaceMap.get(entry.getValue().getEmoticonSet()).add(
						entry.getKey());
			} else {
				SortedSet<Integer> emoteIDs = new TreeSet<Integer>();
				emoteIDs.add(entry.getKey());
				sortedFaceMap.put(entry.getValue().getEmoticonSet(), emoteIDs);
			}
		}

		for (Map.Entry<Integer, SortedSet<Integer>> sortedEntry : sortedFaceMap.entrySet()) {
			panel = new JPanel();
			panel.setLayout(new GridLayout(0, 8));
			panel.setVisible(true);
			if (!subEmotesToggle) {
				//Don't show sub emotes
				if (sortedEntry.getKey() < 0)
					continue;
			} else {
				//Skip emotes that the user is not subscribed to
				if (!botnakUser.hasEmoteSet(sortedEntry.getKey())) continue;
//				if (!userSubs.containsKey(sortedEntry.getKey())) continue;
				
			}
			for (Integer emoteID : sortedEntry.getValue()) {
				tf = FaceManager.twitchFaceMap.get(emoteID);
				emote = new JLabel();
				emote.setOpaque(true);
				emote.setHorizontalAlignment(SwingConstants.CENTER);
				emote.setVerticalAlignment(SwingConstants.CENTER);
				emote.setBackground(Color.BLACK);
				emoteName = handleRegex(tf.getRegex().replaceAll("\\?", "")
						.replaceAll("\\\\", ""));
				emoteName = emoteName.replaceAll("\\[", "").replaceAll("\\]",
						"");
				emote.setName(emoteName);
				emote.setToolTipText(emoteName);

				try {
					image = new File(tf.getFilePath()).toURI().toURL();
					emote.setIcon(Icons.sizeIcon(image));
				} catch (MalformedURLException e) {
					GUIMain.log(e);
				}

				emote.addMouseListener(new MouseListener() {
					@Override
					public void mouseReleased(java.awt.event.MouseEvent e) {
						JLabel emote = (JLabel) e.getSource();
						emote.setBackground(Color.DARK_GRAY);

					}

					@Override
					public void mousePressed(java.awt.event.MouseEvent e) {
						JLabel emote = (JLabel) e.getSource();
						emote.setBackground(Color.WHITE);

					}

					@Override
					public void mouseExited(java.awt.event.MouseEvent e) {
						JLabel emote = (JLabel) e.getSource();
						emote.setBackground(Color.BLACK);

					}

					@Override
					public void mouseEntered(java.awt.event.MouseEvent e) {
						JLabel emote = (JLabel) e.getSource();
						emote.setBackground(Color.DARK_GRAY);

					}

					@Override
					public void mouseClicked(java.awt.event.MouseEvent e) {
						JLabel emote = (JLabel) e.getSource();
						// if the chat box is empty, append the emote + space,
						// otherwise
						// if the current text doesn't end in a space character,
						// append one first. Otherwise, just put the emote
						userChat.append(userChat.getText().length() == 0 ? emote
								.getName() + " "
								: userChat.getText().charAt(
										userChat.getText().length() - 1) == ' ' ? emote
										.getName() + " "
										: " " + emote.getName() + " ");

					}
				});
				panel.add(emote);
			}
			bigPanel.add(panel);
		}

		if (ffzEmotesToggle) {
			for (Map.Entry<String, ArrayList<FrankerFaceZ>> ffzEntry : FaceManager.ffzFaceMap
					.entrySet()) {
				if (!GUIMain.getCurrentPane().getChannel().contains(ffzEntry.getKey())) continue;
				panel = new JPanel();
				panel.setLayout(new GridLayout(0, 8));
				panel.setVisible(true);
				for (FrankerFaceZ ffz : ffzEntry.getValue()) {
					emote = new JLabel();
					emote.setOpaque(true);
					emote.setHorizontalAlignment(SwingConstants.CENTER);
					emote.setVerticalAlignment(SwingConstants.CENTER);
					emote.setBackground(Color.BLACK);
					emoteName = ffz.getRegex();
					if (emoteName.contains(".")) emoteName = emoteName.substring(0, emoteName.indexOf('.'));
					emote.setName(emoteName);
					emote.setToolTipText(emoteName);
					try {
						image = new File(ffz.getFilePath()).toURI().toURL();
						emote.setIcon(Icons.sizeIcon(image));
					} catch (MalformedURLException e) {
						GUIMain.log(e);
					}
					emote.addMouseListener(new MouseListener() {
						@Override
						public void mouseReleased(java.awt.event.MouseEvent e) {
							JLabel emote = (JLabel) e.getSource();
							emote.setBackground(Color.DARK_GRAY);

						}

						@Override
						public void mousePressed(java.awt.event.MouseEvent e) {
							JLabel emote = (JLabel) e.getSource();
							emote.setBackground(Color.WHITE);

						}

						@Override
						public void mouseExited(java.awt.event.MouseEvent e) {
							JLabel emote = (JLabel) e.getSource();
							emote.setBackground(Color.BLACK);

						}

						@Override
						public void mouseEntered(java.awt.event.MouseEvent e) {
							JLabel emote = (JLabel) e.getSource();
							emote.setBackground(Color.DARK_GRAY);

						}

						@Override
						public void mouseClicked(java.awt.event.MouseEvent e) {
							JLabel emote = (JLabel) e.getSource();
							// if the chat box is empty, append the emote +
							// space, otherwise
							// if the current text doesn't end in a space
							// character, append one first. Otherwise, just put
							// the emote
							userChat.append(userChat.getText().length() == 0 ? emote
									.getName() + " "
									: userChat.getText().charAt(
											userChat.getText().length() - 1) == ' ' ? emote
											.getName() + " "
											: " " + emote.getName() + " ");

						}
					});
					panel.add(emote);
				}
				bigPanel.add(panel);
			}
		}
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY
		// //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Nick K
		closeButton = new JButton();
		// panel = new JPanel();
		bigPanel = new JPanel();

		// ======== this ========
		setTitle("Click an Emote");
		setIconImage(new ImageIcon(getClass().getResource("/image/icon.png"))
				.getImage());
		setResizable(false);
		Container contentPane = getContentPane();

		// ---- closeButton ----
		closeButton.setText("Close");
		closeButton.setFocusable(false);
		closeButton.addActionListener(e -> closeButtonActionPerformed());

		// ---- panel ----
		bigPanel.setLayout(new BoxLayout(bigPanel, BoxLayout.PAGE_AXIS));
		bigPanel.setVisible(true);

		// ---- addEmotes ----
		refreshEmotes();

		// ---- scrollPane ----
		scrollPane = new JScrollPane(bigPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setPreferredSize(new Dimension(350, 150));
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					dispose();
				}

				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					guiMain.chatButtonActionPerformed();
				}
			}
		});

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout
				.setHorizontalGroup(contentPaneLayout
						.createParallelGroup()
						.addGroup(
								contentPaneLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												contentPaneLayout
														.createParallelGroup()
														.addGroup(
																contentPaneLayout
																		.createSequentialGroup()
																		// .addComponent(panel,GroupLayout.PREFERRED_SIZE,
																		// 100,
																		// GroupLayout.PREFERRED_SIZE)
																		.addComponent(
																				scrollPane)
																		.addPreferredGap(
																				LayoutStyle.ComponentPlacement.UNRELATED)
																		.addComponent(
																				closeButton)))
										.addContainerGap()));
		contentPaneLayout
				.setVerticalGroup(contentPaneLayout
						.createParallelGroup()
						.addGroup(
								contentPaneLayout
										.createSequentialGroup()
										.addGap(8, 8, 8)
										.addPreferredGap(
												LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												contentPaneLayout
														.createParallelGroup(
																GroupLayout.Alignment.BASELINE)
														.addComponent(
																closeButton,
																GroupLayout.DEFAULT_SIZE,
																29,
																Short.MAX_VALUE)
														.addComponent(
																scrollPane))
										.addGap(28, 28, 28)));
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization
		// //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY
	// //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Nick K
	public static JButton closeButton;
	public JPanel panel, bigPanel;
	public JScrollPane scrollPane;
	private JTextArea userChat;
	private GUIMain guiMain;
	private static boolean ffzEmotesToggle;
	private static boolean subEmotesToggle;
	private static HashMap<String, Integer> emoteXref;
	private User botnakUser;

	// JFormDesigner - End of variables declaration //GEN-END:variables

}