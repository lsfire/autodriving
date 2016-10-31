package model;

import java.util.ArrayList;
import java.util.List;

public class UpdateModel {

	private VehicleModel vehicle;
	private ArrayList<PedestrianModel> pedestrians;
	
	public UpdateModel(VehicleModel vehicle,ArrayList<PedestrianModel> pedestrianModels) {
		this.vehicle = vehicle;
		this.pedestrians = pedestrianModels;
	}

	public VehicleModel getVehicle() {
		return vehicle;
	}

	public void setVehicle(VehicleModel vehicle) {
		this.vehicle = vehicle;
	}

	public ArrayList<PedestrianModel> getPedestrians() {
		return pedestrians;
	}

	public void setPedestrians(ArrayList<PedestrianModel> pedestrians) {
		this.pedestrians = pedestrians;
	}
	
	@Override
	public String toString() {
		return "vehicle position : " + "(" + vehicle.getVehiclePos().get(0) + 
				"," + vehicle.getVehiclePos().get(1) + ")\n"
				+ "pedestrian position : " + "(" + pedestrians.get(0).getPedestrianPos().get(0) + "," + pedestrians.get(0).getPedestrianPos().get(1) + ")";
	}
	

}
