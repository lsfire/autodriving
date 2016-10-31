package model;

import java.util.Vector;

public class GoalModel {
	private Vector<Integer> goalPos;
	private double belief;

	public GoalModel(Vector<Integer> goalPos, double belief) {
		this.goalPos = goalPos;
		this.belief = belief;
	}

	public Vector<Integer> getGoalPos() {
		return goalPos;
	}

	public void setGoalPos(Vector<Integer> newGoalPos) {
		this.goalPos = newGoalPos;
	}

	public double getBelief() {
		return belief;
	}

	public void setBelief(double newBelief) {
		this.belief = newBelief;
	}
}
