/*
  * Created on 12 may. 2018
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package fr.utc.gui;

import java.util.ArrayDeque;
import java.util.Deque;
import org.antlr.v4.runtime.misc.Pair;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class Traceur {
	private Color couleur = Color.BLACK;
	private double initx = 350, inity = 350; // position initiale
	private double posx = initx, posy = inity; // position courante
	private double angle = 90;
	private double teta;
	ObjectProperty<GraphLineParameter> line;
	boolean crayon;
	private Deque<Pair<Double, Double>> position; // memoire de la position LIFO

	public Traceur() {
		this.crayon = true;
		setTeta();
		line = new SimpleObjectProperty<GraphLineParameter>();
		this.position = new ArrayDeque<Pair<Double, Double>>();
	}

	public ObjectProperty<GraphLineParameter> lineProperty() {
		return line;
	}

	private void setTeta() {
		teta = Math.toRadians(angle);
	}

	private void addLine(double x1, double y1, double x2, double y2) {
		if (this.crayon) {
			line.setValue(new GraphLineParameter(x1, y1, x2, y2, couleur));	
		}
	}

	public void avance(double r) {
		double a = posx + r * Math.cos(teta);
		double b = posy - r * Math.sin(teta);
		addLine(posx, posy, a, b);

		posx = a;
		posy = b;
	}
	
	public void teleport(double x, double y) {
		
		addLine(posx, posy, x, y);
		
		posx = x;
		posy= y;
	}

	public void td(double r) {
		angle = (angle - r) % 360;
		setTeta();
	}
	
	public void setCrayon(boolean b) {
		this.crayon=b;
	}
	
	public void fixeCap(double r) {
		angle = r+90;
		setTeta();
	}
	
	public void setColor(int n) {
		switch (n%8) {
			case 0:
				couleur = Color.BLACK;
				break;
			case 1:
				couleur = Color.RED;
				break;
			case 2:
				couleur = Color.GREEN;
				break;
			case 3:
				couleur = Color.YELLOW;
				break;
			case 4:
				couleur = Color.BLUE;
				break;
			case 5:
				couleur = Color.PURPLE;
				break;
		}
	}
	
	public void store() {
		this.position.push(new Pair<Double, Double>(posx,posy));
	}
	
	public boolean move() {
		if(!this.position.isEmpty()) {
			Pair<Double, Double> lastSavPos = this.position.pop();
			this.teleport(lastSavPos.a, lastSavPos.b);	
			return true;
		}
		return false;
	}

}
