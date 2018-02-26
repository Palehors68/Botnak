package gui.forms;

import util.Utils;
import util.comm.Command;

import javax.swing.*;

import java.awt.*;

//import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;

import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

import lib.pircbot.Channel;

/**
 * @author Nick K
 */
public class GUITextCommandEditor extends JFrame {

    public GUITextCommandEditor(Channel ch) {
    	this.ch = ch;
        initComponents();
    }

    

    private void saveButtonActionPerformed() {
    	
    	String trigger = (String) commandNameComboBox.getSelectedItem();
    	Command c;
    	if (addComm){
    		if (trigger.equals("") || commandTextArea.getText().equals("")){
    			return;
    		}
    		c = new Command(trigger, commandTextArea.getText());
    		getCommandSet().add(c);
    	} else {
    		c = Utils.getCommand(trigger, ch);
    		if (c != null){
    			c.setContents(commandTextArea.getText());
    		}
    	}
		reset();
		refreshList();
		commandNameComboBox.setSelectedItem(trigger);
    	
        
    }
    
    private void addButtonActionPerformed() {
    	commandNameComboBox.setEditable(true);
    	commandNameComboBox.removeAllItems();
    	commandNameComboBox.addItem("");
    	commandTextArea.setText("");
    	deleteButton.setText("Cancel");
    	addButton.setEnabled(false);
    	addComm = true;
    }

    private void deleteButtonActionPerformed() {
    	if (addComm){//Cancel button
    		reset();
    		refreshList();
    	} else { 
    		String trigger = (String) commandNameComboBox.getSelectedItem();
	    	Command c = Utils.getCommand(trigger, ch);
	    	getCommandSet().remove(c);
	    	refreshList();
    	}
    }
    
    private void reset(){
    	commandTextArea.setText("");
		deleteButton.setText("Delete");
		addButton.setEnabled(true);
		addComm = false;
    }

    private void closeButtonActionPerformed() {
    	reset();
    	refreshList();
        dispose();
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
    }
    
    public void refreshList(){
    	commandNameComboBox.removeAllItems();
    	commandNameComboBox.addItem("Select a text command...");
        for (Command c : getCommandSet()) {
			commandNameComboBox.addItem(c.getTrigger());
		}
    }
    
    private void comboBoxItemSelected(){
    	String trigger = (String) commandNameComboBox.getSelectedItem();
    	//pass in null to edit global text commands only
    	Command c = Utils.getCommand(trigger, ch);
    	if (c == null) { 
    		commandTextArea.setText("");
    	} else {
    		StringBuilder sb = new StringBuilder();
    		for (String s : c.getMessage().data){
    			sb.append(s);
    		}
    		commandTextArea.setText(sb.toString());
    	}
    	
    	
    	
    }
    
    private Set<Command> getCommandSet(){
    	if (ch != null) {
    		return ch.getCommandSet();
    	} else {
    		return GUIMain.commandSet;
    	}
    }

    private void initComponents() {
        saveButton = new JButton();
        closeButton = new JButton();
        addButton = new JButton();
        addButton.setText("Add");
        deleteButton = new JButton();
        deleteButton.setText("Delete");
        commandTextArea = new JTextArea("");
        commandNameComboBox = new JComboBox<String>();
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        //======== this ========
        setTitle("Edit Text Commands");
        setResizable(false);
        Container contentPane = getContentPane();

        //---- saveButton ----
        saveButton.setText("Save");
        saveButton.addActionListener(e -> saveButtonActionPerformed());

        //---- closeButton ----
        closeButton.setText("Close");
        closeButton.addActionListener(e -> closeButtonActionPerformed());
        
        //---- addButton ----
        addButton.setText("Add");
        addButton.addActionListener(e -> addButtonActionPerformed());
        
        //---- deleteButton ----
        deleteButton.setText("Delete");
        deleteButton.addActionListener(e -> deleteButtonActionPerformed());
        
        
        refreshList();
        commandNameComboBox.addActionListener(e -> comboBoxItemSelected());
        
        commandTextArea.setEditable(true);
        commandTextArea.setLineWrap(true);
        commandTextArea.setWrapStyleWord(true);
        scrollPane = new JScrollPane(commandTextArea);
        

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPaneLayout.setHorizontalGroup(
        	contentPaneLayout.createParallelGroup(Alignment.TRAILING)
        		.addGroup(contentPaneLayout.createSequentialGroup()
        			.addContainerGap(17, Short.MAX_VALUE)
        			.addGroup(contentPaneLayout.createParallelGroup(Alignment.LEADING, false)
        				.addGroup(contentPaneLayout.createSequentialGroup()
        					.addComponent(addButton, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
        					.addPreferredGap(ComponentPlacement.RELATED)
        					.addComponent(deleteButton)
        					.addPreferredGap(ComponentPlacement.RELATED)
        					.addComponent(saveButton)
        					.addPreferredGap(ComponentPlacement.RELATED)
        					.addComponent(closeButton))
        				.addComponent(commandNameComboBox, GroupLayout.PREFERRED_SIZE, 199, GroupLayout.PREFERRED_SIZE)
        				.addComponent(scrollPane))
        			.addGap(34))
        );
        contentPaneLayout.setVerticalGroup(
        	contentPaneLayout.createParallelGroup(Alignment.TRAILING)
        		.addGroup(contentPaneLayout.createSequentialGroup()
        			.addContainerGap()
        			.addComponent(commandNameComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        			.addPreferredGap(ComponentPlacement.UNRELATED)
        			.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE)
        			.addPreferredGap(ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
        			.addGroup(contentPaneLayout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(addButton)
        				.addComponent(deleteButton)
        				.addComponent(saveButton)
        				.addComponent(closeButton))
        			.addGap(40))
        );
        contentPane.setLayout(contentPaneLayout);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
    private JButton saveButton;
    private JButton closeButton;
    private JButton deleteButton;
    private JButton addButton;
    private JTextArea commandTextArea;
    private JComboBox<String> commandNameComboBox;
    private JScrollPane scrollPane;
    private boolean addComm = false;
    private Channel ch;
}