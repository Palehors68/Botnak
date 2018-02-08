package gui;

import java.awt.Color;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import util.Utils;

public class GUIVote extends JFrame {

	public GUIVote(){
		initComponents();
	}
	
	private void closeButtonActionPerformed(){
		dispose();
	}
	
	private void copyButtonActionPerformed(){
//		dispose();
	}
	
	private void resetGui() {
		timeError.setText("");
		
		label2.setForeground(Color.WHITE);
		label1.setForeground(Color.WHITE);
		label5.setForeground(Color.WHITE);
	}
	
	private void sendButtonActionPerformed() {

		boolean errors = false;
		resetGui();
		
		if (option2.getText().equals("")){
			label2.setForeground(Color.RED);
			errors = true;
		}
		if (option1.getText().equals("")){
			label1.setForeground(Color.RED);
			errors = true;
		}

		if (timeField.getText().equals("")){
			label5.setForeground(Color.RED);
			errors = true;
		}
		
		if (errors) return;
		
		try {
			int test = Integer.parseInt(timeField.getText());
			if (test < 1) {
				timeError.setText("Number must be > 0");
				errors = true;
			}
			if (comboBox.getItemAt(comboBox.getSelectedIndex()).equals("sec") && test > 999) {
				timeError.setText("Seconds must be 1-999");
				errors = true;
			}
		} catch (Exception e){
			timeError.setText("Problem with your number");;
			errors = true;
		}
		
		if (errors) return;
		
		StringBuilder sb = new StringBuilder("!startpoll");
		sb.append(" " + timeField.getText() + (comboBox.getItemAt(comboBox.getSelectedIndex()).equals("min") ? "m" : ""));
		sb.append(" " + option1.getText() + "]" + option2.getText()); //These 2 are mandatory, so always append them
		if (!option3.getText().equals("")){
			sb.append("]" + option3.getText());
		}
		if (!option4.getText().equals("")){
			sb.append("]" + option4.getText());
		}
		
		
		
//		String channel = GUIMain.channelPane.getTitleAt(GUIMain.channelPane.getSelectedIndex());
		String channel = GUIMain.getCurrentPane().getChannel();
		if (channel != null && !channel.equalsIgnoreCase("system logs")){
			if (!questionField.getText().equals("")){
				GUIMain.bot.getBot().sendMessage("#" + channel, questionField.getText());
			}
			GUIMain.bot.onMessage("#" + channel, 
					GUIMain.currentSettings.accountManager.getUserAccount().getName(), 
					sb.toString());



		} else {
			CombinedChatPane ccp = Utils.getCombinedChatPane(GUIMain.channelPane.getSelectedIndex());
			boolean comboExists = ccp != null;
			if (comboExists){
				String[] channels;
				if (!ccp.getActiveChannel().equalsIgnoreCase("all")) {
					channels = new String[]{ccp.getActiveChannel()};
				} else {
					channels = ccp.getChannels();
				}
				for (String c : channels){
					if (!questionField.getText().equals("")){
						GUIMain.bot.getBot().sendMessage("#" + c, questionField.getText());
					}
					GUIMain.bot.onMessage("#" + c, 
							GUIMain.currentSettings.accountManager.getUserAccount().getName(), 
							sb.toString());
				}
			}
		}



		this.dispose();
	}
	
	@Override
	public void setVisible(boolean b){
		super.setVisible(b);
	}
	
	private void initComponents() {
		copyButton = new JButton();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		//======== this ========
		setTitle("Create a user poll");
		setResizable(false);
        
        //======== copyButton ========
        copyButton.setText("Copy");
        copyButton.addActionListener(e -> copyButtonActionPerformed());
        
        
        //======== textFields ========
        timeField = new JTextField();
        timeField.setToolTipText("0-999sec or Xmin");
        timeField.setText("1");
        
        
        
        //======== comboBox ========
        comboBox = new JComboBox<String>();
        comboBox.addItem("min");
        comboBox.addItem("sec");
        comboBox.setSelectedIndex(0);
        
        //======== labels ========
        timeError = new JLabel();
        timeError.setForeground(Color.RED);
        label1 = new JLabel();
        label1.setText("Option 1");
        label2 = new JLabel();
        label2.setText("Option 2");
        label3 = new JLabel();
        label3.setText("Option 3");
        label4 = new JLabel();
        label4.setText("Option 4");
        label5 = new JLabel();
        label5.setText("Time");
        
        //======== options ========
        option1 = new JTextField();
        option1.setToolTipText("<Required>");
        option2 = new JTextField();
        option2.setToolTipText("<Required>");
        option3 = new JTextField();
        option3.setToolTipText("<Optional>");
        option4 = new JTextField();
        option4.setToolTipText("<Optional>");
        
        //======== sendButton ========
        sendButton = new JButton();
        sendButton.setText("Start Poll");
        sendButton.addActionListener(e -> sendButtonActionPerformed());
        
        //======== closeButton ========
        closeButton = new JButton();
        closeButton.setText("Close");
        closeButton.addActionListener(e -> closeButtonActionPerformed());
        
        questionField = new JTextField();
        questionField.setToolTipText("<optional question>");
        questionField.setColumns(10);
        
        JLabel lblQuestion = new JLabel("Question");
        
        
        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
        	groupLayout.createParallelGroup(Alignment.LEADING)
        		.addGroup(groupLayout.createSequentialGroup()
        			.addContainerGap()
        			.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        				.addGroup(groupLayout.createSequentialGroup()
        					.addComponent(label3)
        					.addPreferredGap(ComponentPlacement.RELATED)
        					.addComponent(option3, GroupLayout.PREFERRED_SIZE, 209, GroupLayout.PREFERRED_SIZE))
        				.addGroup(groupLayout.createSequentialGroup()
        					.addComponent(label4)
        					.addPreferredGap(ComponentPlacement.RELATED)
        					.addComponent(option4, GroupLayout.PREFERRED_SIZE, 209, GroupLayout.PREFERRED_SIZE))
        				.addGroup(groupLayout.createSequentialGroup()
        					.addComponent(sendButton)
        					.addPreferredGap(ComponentPlacement.RELATED)
        					.addComponent(closeButton))
        				.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
        					.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
        						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        							.addComponent(label1)
        							.addComponent(label5)
        							.addComponent(lblQuestion))
        						.addPreferredGap(ComponentPlacement.RELATED)
        						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        							.addGroup(groupLayout.createSequentialGroup()
        								.addComponent(timeField, GroupLayout.PREFERRED_SIZE, 41, GroupLayout.PREFERRED_SIZE)
        								.addPreferredGap(ComponentPlacement.RELATED)
        								.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        									.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        									.addComponent(timeError, Alignment.TRAILING)))
        							.addComponent(questionField)
        							.addComponent(option1, GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)))
        					.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
        						.addComponent(label2)
        						.addPreferredGap(ComponentPlacement.RELATED)
        						.addComponent(option2, GroupLayout.PREFERRED_SIZE, 209, GroupLayout.PREFERRED_SIZE))))
        			.addGap(51))
        );
        groupLayout.setVerticalGroup(
        	groupLayout.createParallelGroup(Alignment.LEADING)
        		.addGroup(groupLayout.createSequentialGroup()
        			.addContainerGap()
        			.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(lblQuestion)
        				.addComponent(questionField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        			.addPreferredGap(ComponentPlacement.RELATED)
        			.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        				.addComponent(timeError)
        				.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
        					.addComponent(timeField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        					.addComponent(label5)
        					.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
        			.addPreferredGap(ComponentPlacement.RELATED)
        			.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(option1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        				.addComponent(label1))
        			.addGap(5)
        			.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(option2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        				.addComponent(label2))
        			.addGap(5)
        			.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(option3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        				.addComponent(label3))
        			.addGap(5)
        			.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(option4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        				.addComponent(label4))
        			.addGap(5)
        			.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(closeButton)
        				.addComponent(sendButton))
        			.addContainerGap())
        );
        getContentPane().setLayout(groupLayout);
        
        	pack();
            setLocationRelativeTo(getOwner());
        
        
		
	}
	
	private JLabel label1, label2, label3, label4, label5, timeError;
	private JComboBox<String> comboBox;
	private JTextField timeField, option1, option2, option3, option4;
	private JButton sendButton, closeButton, copyButton;
	private JTextField questionField;
}
