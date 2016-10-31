package simulator;

import java.util.ArrayList;
import java.util.Vector;

import model.GoalModel;
import model.PedestrianModel;
import model.VehicleModel;

public class MapSetter {
	private ArrayList<PedestrianModel> pedestrianList;
	private VehicleModel vehicle;
	private ArrayList<Vector<Integer>> staticObstacleList;
	private int mapWidth;
	private int mapHeight;
	private int aSpeedOfVehicle;

	public MapSetter() {
		initPedestrianList();
		initVehicle();
		initStaticObstacleList();
		mapWidth = 1000;
		mapHeight = 1000;
		aSpeedOfVehicle = 5;
	}

	public int getASpeed() {
		return aSpeedOfVehicle;
	}

	public int getMapWidth() {
		return mapWidth;
	}

	public int getMapHeight() {
		return mapHeight;
	}

	public void initPedestrianList() {
		pedestrianList = new ArrayList<>();

		Vector<Integer> pedestrianPosOne = new Vector<>();
		pedestrianPosOne.add(100);
		pedestrianPosOne.add(100);

		ArrayList<GoalModel> pedestrianGoalOne = new ArrayList<>();
		Vector<Integer> pedestrianGoalPosOneOne = new Vector<>();
		pedestrianGoalPosOneOne.add(200);
		pedestrianGoalPosOneOne.add(200);
		GoalModel goalOneOne = new GoalModel(pedestrianGoalPosOneOne, 0.25);
		Vector<Integer> pedestrianGoalPosOneTwo = new Vector<>();
		pedestrianGoalPosOneTwo.add(150);
		pedestrianGoalPosOneTwo.add(250);
		GoalModel goalOneTwo = new GoalModel(pedestrianGoalPosOneTwo, 0.1);
		Vector<Integer> pedestrianGoalPosOneThree = new Vector<>();
		pedestrianGoalPosOneThree.add(400);
		pedestrianGoalPosOneThree.add(220);
		GoalModel goalOneThree = new GoalModel(pedestrianGoalPosOneThree, 0.45);
		Vector<Integer> pedestrianGoalPosOneFour = new Vector<>();
		pedestrianGoalPosOneFour.add(226);
		pedestrianGoalPosOneFour.add(260);
		GoalModel goalOneFour = new GoalModel(pedestrianGoalPosOneFour, 0.2);
		pedestrianGoalOne.add(goalOneOne);
		pedestrianGoalOne.add(goalOneTwo);
		pedestrianGoalOne.add(goalOneThree);
		pedestrianGoalOne.add(goalOneFour);
		PedestrianModel pedestrianOne = new PedestrianModel(pedestrianPosOne, pedestrianGoalOne, 4);

		Vector<Integer> pedestrianPosTwo = new Vector<>();
		pedestrianPosTwo.add(500);
		pedestrianPosTwo.add(400);

		ArrayList<GoalModel> pedestrianGoalTwo = new ArrayList<>();
		Vector<Integer> pedestrianGoalPosTwoOne = new Vector<>();
		pedestrianGoalPosTwoOne.add(580);
		pedestrianGoalPosTwoOne.add(400);
		GoalModel goalTwoOne = new GoalModel(pedestrianGoalPosTwoOne, 0.35);
		Vector<Integer> pedestrianGoalPosTwoTwo = new Vector<>();
		pedestrianGoalPosTwoTwo.add(420);
		pedestrianGoalPosTwoTwo.add(680);
		GoalModel goalTwoTwo = new GoalModel(pedestrianGoalPosTwoTwo, 0.15);
		Vector<Integer> pedestrianGoalPosTwoThree = new Vector<>();
		pedestrianGoalPosTwoThree.add(400);
		pedestrianGoalPosTwoThree.add(800);
		GoalModel goalTwoThree = new GoalModel(pedestrianGoalPosTwoThree, 0.4);
		Vector<Integer> pedestrianGoalPosTwoFour = new Vector<>();
		pedestrianGoalPosTwoFour.add(550);
		pedestrianGoalPosTwoFour.add(550);
		GoalModel goalTwoFour = new GoalModel(pedestrianGoalPosTwoFour, 0.1);
		pedestrianGoalTwo.add(goalTwoOne);
		pedestrianGoalTwo.add(goalTwoTwo);
		pedestrianGoalTwo.add(goalTwoThree);
		pedestrianGoalTwo.add(goalTwoFour);
		PedestrianModel pedestrianTwo = new PedestrianModel(pedestrianPosTwo, pedestrianGoalTwo, 4);

		pedestrianList.add(pedestrianOne);
		pedestrianList.add(pedestrianTwo);
	}

	public void initVehicle() {
		Vector<Integer> vehiclePos = new Vector<>();
		vehiclePos.add(300);
		vehiclePos.add(400);

		Vector<Integer> vehicleGoal = new Vector<>();
		vehicleGoal.add(600);
		vehicleGoal.add(150);

		vehicle = new VehicleModel(vehiclePos, vehicleGoal, 0, 20, 30);
	}

	public void initStaticObstacleList() {
		staticObstacleList = new ArrayList<>();

		Vector<Integer> obstacleOne = new Vector<>();
		obstacleOne.add(520);
		obstacleOne.add(320);

		Vector<Integer> obstacleTwo = new Vector<>();
		obstacleTwo.add(800);
		obstacleTwo.add(400);

		staticObstacleList.add(obstacleOne);
		staticObstacleList.add(obstacleTwo);
	}

	public ArrayList<PedestrianModel> getPedestrianList() {
		return pedestrianList;
	}

	public VehicleModel getVehicle() {
		return vehicle;
	}

	public ArrayList<Vector<Integer>> getStaticObstacleList() {
		return staticObstacleList;
	}
}
