package animation;

import java.awt.Graphics;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class PedestrianPanel extends JPanel{
	public PedestrianPanel() {
	}
	
	
	@Override
	protected void paintComponent(Graphics g) {
		int x= 0,y=0;
		ImageIcon icon = new ImageIcon("pedestrian.jpg");
		g.drawImage(icon.getImage(), x, y, getSize().width,getSize().height,this);
	}

}
