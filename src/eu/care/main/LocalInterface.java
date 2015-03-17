package eu.care.main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.*;

/**
 * 
 * @author Alexander Diefenbach
 * interface for displaying and rating single recommendations
 * 
 */

public class LocalInterface extends JFrame implements ActionListener{

	private DemonstratorMain parent;
	private JPanel mypanel;
	private JLabel mood = new JLabel("UserMood");
	private JLabel time = new JLabel("Time");
	private JLabel weather = new JLabel("Weather");
	private JLabel recImage = new JLabel();
    private JButton nextRec = new JButton("New Recommendation");
    private JButton positive = new JButton("+");
    private JButton neutral = new JButton("=");
    private JButton negative = new JButton("-");

    public LocalInterface(DemonstratorMain parent){
        super("CARE");
        
        this.parent = parent;
        
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        mypanel = new JPanel(new FlowLayout());
        
        nextRec.setActionCommand("newRec");
        positive.setActionCommand("positive");
        neutral.setActionCommand("neutral");
        negative.setActionCommand("negative");
        
        nextRec.addActionListener(this);
        positive.addActionListener(this);
        neutral.addActionListener(this);
        negative.addActionListener(this);

        mypanel.add(mood);
        mypanel.add(time);
        mypanel.add(weather);
        mypanel.add(nextRec);
        mypanel.add(positive);
        mypanel.add(neutral);
        mypanel.add(negative);
        mypanel.add(recImage);

        this.getContentPane().add(mypanel);

        pack();
        setVisible(true);
    }

	public void setMood (String userMood){
		mood.setText(userMood);
	}
	
	public void setTime (String currentTime){
		time.setText(currentTime);
	}
	
	public void setWeather(String currentWeather){
		weather.setText(currentWeather);
	}
	
	public void setImage (String imageURL){
		URL img;
		try {
			img = new URL(imageURL);
			ImageIcon image = new ImageIcon(img);
			recImage.setIcon(image);
			pack();
			enableButtons();
		} catch (MalformedURLException e) {
			Utils.printWithDate("Failed to load recommendation image.", Utils.DEBUGLEVEL.WARNING);
			recImage.setIcon(new ImageIcon());
			enableButtons();
		}
	}
	
	public void disableButtons(){
		nextRec.setEnabled(false);
		positive.setEnabled(false);
		negative.setEnabled(false);
		neutral.setEnabled(false);
	}
	
	public void enableButtons(){
		nextRec.setEnabled(true);
		positive.setEnabled(true);
		negative.setEnabled(true);
		neutral.setEnabled(true);
	}
	
	public void actionPerformed (ActionEvent event){
		String cmd = event.getActionCommand();
		switch(cmd){
		case "newRec":
			parent.newRecPressed();
			break;
		case "positive":
			parent.positivePressed();
			break;
		case "neutral":
			parent.neutralPressed();
			break;
		case "negative":
			parent.negativePressed();
			break;
		default:
			break;
		}
	}

}