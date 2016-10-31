package model;

import java.util.Vector;

public class VehicleModel {
	private Vector<Integer> vehiclePos;
	private Vector<Integer> vehicleGoal;
	private double vehicleOrientation;
	private double vehicleSpeed;
	private double speedMax;

	public VehicleModel(Vector<Integer> vehiclePos,
			Vector<Integer> vehicleGoal,
			double vehicleOrientation, double vehicleSpeed,
			double speedMax) {
		this.vehicleOrientation = vehicleOrientation;
		this.vehiclePos = vehiclePos;
		this.vehicleGoal = vehicleGoal;
		this.vehicleSpeed = vehicleSpeed;
		this.speedMax = speedMax;
	}

	public Vector<Integer> getVehiclePos() {
		return this.vehiclePos;
	}

	public void setVehiclePos(
			Vector<Integer> newVehiclePos) {
		this.vehiclePos = newVehiclePos;
	}

	public Vector<Integer> getVehicleGoal() {
		return this.vehicleGoal;
	}

	public double getVehicleOrientation() {
		return this.vehicleOrientation;
	}

	public void setVehicleOrientation(
			double newVehicleOrientation) {
		this.vehicleOrientation = newVehicleOrientation;
	}

	public double getVehicleSpeed() {
		return this.vehicleSpeed;
	}

	public void setVehicleSpeed(double newVehicleSpeed) {
		this.vehicleSpeed = newVehicleSpeed;
	}

	public double getMaxSpeed() {
		return speedMax;
	}
}
