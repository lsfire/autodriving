package model;

import java.util.ArrayList;
import java.util.Vector;

public class PedestrianModel {
	private Vector<Integer> pedestrianPos;
	private ArrayList<GoalModel> pedestrianGoalList;
	private double pedestrianSpeed;

	public PedestrianModel(Vector<Integer> pedestrianPos, ArrayList<GoalModel> pedestrianGoal, double pedestrianSpeed) {
		this.pedestrianPos = pedestrianPos;
		this.pedestrianGoalList = pedestrianGoal;
		this.pedestrianSpeed = pedestrianSpeed;
	}

	public Vector<Integer> getPedestrianPos() {
		return this.pedestrianPos;
	}

	public void setPedestrianPos(Vector<Integer> newPedestrianPos) {
		this.pedestrianPos = newPedestrianPos;
	}

	public ArrayList<GoalModel> getPedestrianGoalList() {
		return this.pedestrianGoalList;
	}

	public void setPedestrianGoal(ArrayList<GoalModel> newPedestrianGoalList) {
		this.pedestrianGoalList = newPedestrianGoalList;
	}

	public double getPedestrianSpeed() {
		return this.pedestrianSpeed;
	}

	public void setPedestrianSpeed(double newPedestrianSpeed) {
		this.pedestrianSpeed = newPedestrianSpeed;
	}
}
