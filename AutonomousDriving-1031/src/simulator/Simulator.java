package simulator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import animation.AutoDrivingAnimation;
import model.GoalModel;
import model.PedestrianModel;
import model.UpdateModel;
import model.VehicleModel;
import pathPlanning.BeliefTracker;
import pathPlanning.PathPlanner;
import pathPlanning.Point;
import pomdp.algorithms.AlgorithmsFactory;
import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.InvalidModelFileFormatException;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.valuefunction.MDPValueFunction;
import speedPlanning.FileOperation;
import speedPlanning.PomdpModelBuilder;

public class Simulator {
	private MapSetter map;
	private ArrayList<PedestrianModel> pedestrianList;
	private VehicleModel vehicle;
	private ArrayList<Vector<Integer>> staticObstacleList;
	private ArrayList<Vector<Integer>> oldPedestrianPos;
	private ArrayList<Vector<Integer>> actualPath;
	private ArrayList<ArrayList<Vector<Integer>>> pedestrianPosRecord;
	private boolean ifCollide;
	private double reward;

	public Simulator() {
		map = new MapSetter();
		pedestrianList = map.getPedestrianList();
		vehicle = map.getVehicle();
		staticObstacleList = map.getStaticObstacleList();
		oldPedestrianPos = new ArrayList<>();
		actualPath = new ArrayList<>();
		pedestrianPosRecord = new ArrayList<>();
		for (int i = 0; i < pedestrianList.size(); i++) {
			pedestrianPosRecord.add(new ArrayList<Vector<Integer>>());
			
		}
		
		
		
		for (int i = 0; i < pedestrianPosRecord.size(); i++) {
			pedestrianPosRecord.get(i).add(pedestrianList.get(i).getPedestrianPos());
		}
		actualPath.add(vehicle.getVehiclePos());
		ifCollide = false;
		reward = 0.0;
	}

	//模拟行人行走
	public void simulatePedestrianWalk() {
		oldPedestrianPos.clear();
		for (int i = 0; i < pedestrianList.size(); i++) {
			ArrayList<GoalModel> pedestrianGoals = pedestrianList.get(i).getPedestrianGoalList();
			//随机出行人的行走目标点
			int goalNumber = (int) Math.floor(Math.random() * pedestrianGoals.size());
			GoalModel goal = pedestrianGoals.get(goalNumber);
			double distance = Math
					.sqrt(Math.pow(goal.getGoalPos().get(0) - pedestrianList.get(i).getPedestrianPos().get(0), 2)
							+ Math.pow(goal.getGoalPos().get(1) - pedestrianList.get(i).getPedestrianPos().get(1), 2));
			double rate = pedestrianList.get(i).getPedestrianSpeed() / distance;
			int x = (int) Math.round((double) (pedestrianList.get(i).getPedestrianPos().get(0))
					+ rate * (goal.getGoalPos().get(0) - pedestrianList.get(i).getPedestrianPos().get(0)));
			int y = (int) Math.round((double) (pedestrianList.get(i).getPedestrianPos().get(1))
					+ rate * (goal.getGoalPos().get(1) - pedestrianList.get(i).getPedestrianPos().get(1)));
			// System.out.println(x);
			// System.out.println(y);
			//判断是否撞上汽车或者障碍物
			boolean ifRunIntoVehicle = false;
			if (x <= vehicle.getVehiclePos().get(0) + 4 && x >= vehicle.getVehiclePos().get(0) - 4
					&& y <= vehicle.getVehiclePos().get(1) + 4 && y >= vehicle.getVehiclePos().get(1) - 4) {
				ifRunIntoVehicle = true;
			}
			boolean ifRunIntoObstacle = false;
			for (int j = 0; j < staticObstacleList.size(); j++) {
				Vector<Integer> obstacle = staticObstacleList.get(j);
				if (x <= obstacle.get(0) + 7 && x >= obstacle.get(0) - 7 && y <= obstacle.get(1) + 7
						&& y >= obstacle.get(1) - 7) {
					ifRunIntoObstacle = true;
					break;
				}
			}

			while (ifRunIntoObstacle || ifRunIntoVehicle) {
				goalNumber = (int) Math.floor(Math.random() * pedestrianGoals.size());
				goal = pedestrianGoals.get(goalNumber);
				distance = Math.sqrt(
						Math.pow(goal.getGoalPos().get(0) - pedestrianList.get(i).getPedestrianPos().get(0), 2) + Math
								.pow(goal.getGoalPos().get(1) - pedestrianList.get(i).getPedestrianPos().get(1), 2));
				rate = pedestrianList.get(i).getPedestrianSpeed() / distance;
				x = (int) Math.round((double) (pedestrianList.get(i).getPedestrianPos().get(0))
						+ rate * (goal.getGoalPos().get(0) - pedestrianList.get(i).getPedestrianPos().get(0)));
				y = (int) Math.round((double) (pedestrianList.get(i).getPedestrianPos().get(1))
						+ rate * (goal.getGoalPos().get(1) - pedestrianList.get(i).getPedestrianPos().get(1)));
			}

			oldPedestrianPos.add(pedestrianList.get(i).getPedestrianPos());
			Vector<Integer> newPedestrianPos = new Vector<>();
			newPedestrianPos.add(x);
			newPedestrianPos.add(y);
			pedestrianList.get(i).setPedestrianPos(newPedestrianPos);
			pedestrianPosRecord.get(i).add(newPedestrianPos);
		}
	}

	//开始场景模拟
	public void startAutonomousDriving(String[] args) {
		while (Math.sqrt(Math.pow(vehicle.getVehiclePos().get(0) - vehicle.getVehicleGoal().get(0), 2)
				+ Math.pow(vehicle.getVehiclePos().get(1) - vehicle.getVehicleGoal().get(1), 2)) > vehicle
						.getMaxSpeed()) {//判断汽车是否到达目标点
			// for (int m = 0; m < 2; m++) {

			System.out.println("-----------------------------------------------------");
			// System.out.println(pedestrianList.get(0).getPedestrianPos());
			simulatePedestrianWalk();//行人拟合
			// System.out.println(pedestrianList.get(0).getPedestrianPos());
			//行人信念跟踪
			BeliefTracker beliefTracker;
			for (int i = 0; i < pedestrianList.size(); i++) {
				beliefTracker = new BeliefTracker(pedestrianList.get(i));
				pedestrianList.set(i, beliefTracker.trackBelief(oldPedestrianPos.get(i)));
			}
			System.out.println(pedestrianList.get(1).getPedestrianGoalList().get(0).getBelief());
			System.out.println(pedestrianList.get(1).getPedestrianGoalList().get(1).getBelief());
			//规划路线
			PathPlanner pathPlanner = new PathPlanner(staticObstacleList, pedestrianList, vehicle, map.getMapWidth(),
					map.getMapHeight());
			pathPlanner.findPath();

			ArrayList<Point> minCostPath = pathPlanner.getMinCostPath();
			// for (int i = 0; i < minCostPath.size(); i++) {
			// System.out.println(minCostPath.get(i).getX() + ", " +
			// minCostPath.get(i).getY());
			// }
			//计算汽车行驶角度
			Point nextDestination = minCostPath.get(1);
			if (nextDestination.getX() == vehicle.getVehiclePos().get(0)) {
				if (nextDestination.getY() > vehicle.getVehiclePos().get(1)) {
					vehicle.setVehicleOrientation(Math.PI * 0.5);
				} else {
					vehicle.setVehicleOrientation(Math.PI * 1.5);
				}
			} else {
				vehicle.setVehicleOrientation(Math.atan((nextDestination.getY() - vehicle.getVehiclePos().get(1))
						/ (nextDestination.getX() - vehicle.getVehiclePos().get(0))));

			}
			//生成pomdp模型文件
			PomdpModelBuilder pomdpModelBuilder = new PomdpModelBuilder(pedestrianList, vehicle);
			pomdpModelBuilder.buildPomdpModelFile();
			pomdpModelBuilder.writeDiscount((float) 0.95);
			pomdpModelBuilder.writeValues("reward");
			pomdpModelBuilder.writeStates();
			pomdpModelBuilder.writeActions();
			pomdpModelBuilder.writeObservations();
			pomdpModelBuilder.writeStartingBeliefState();
			pomdpModelBuilder.writeStateTransitionProbabilities();
			pomdpModelBuilder.writeObservationProbabilities();
			pomdpModelBuilder.writeReward();
			
			//pomdp模型求解
			JProf.getCurrentThreadCpuTimeSafe();
			String sPath = ExecutionProperties.getPath();
			String sModelName = "autonomous_driving";
			String sMethodName = "FSVI";
			long maxExecutionTime = 1000 * 60 * 10;
			int maxIteration = 100;
			if (args.length > 0)
				sModelName = args[0];
			if (args.length > 1)
				sMethodName = args[1];
			if (args.length > 2) {
				String maxTimeString = args[2];
				try {
					maxExecutionTime = Long.parseLong(maxTimeString) * 1000 * 60;
				} catch (NumberFormatException e) {

				}
			}
			if (args.length > 3) {
				String maxIterationArg = args[3];
				try {
					maxIteration = Integer.parseInt(maxIterationArg);
				} catch (NumberFormatException e) {

				}
			}
			Logger.getInstance().setOutput(true);
			Logger.getInstance().setSilent(false);
			try {
				String sOutputDir = "logs/exp_logs";
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm");

				String sFileName = sModelName + "_" + sMethodName + "_" + sdf.format(new Date()) + ".txt";
				Logger.getInstance().setOutputStream(sOutputDir, sFileName);
			} catch (Exception e) {
				System.err.println(e);
			}
			POMDP pomdp = null;
			double dTargetADR = 100.0;
			int maxRunningTime = 45;
			int numEvaluations = 3;
			try {
				pomdp = new POMDP();
				pomdp.load(sPath + sModelName + ".POMDP");
				Logger.getInstance().logln("max is " + pomdp.getMaxR() + " min is " + pomdp.getMinR());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Logger.getInstance().logln(e);
				e.printStackTrace();
				System.exit(0);
			}
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm");
				String sFileName = sModelName + "_" + sMethodName + "_" + sdf.format(new Date()) + ".txt";
				// Logger.getInstance().setOutputStream( pomdp.getName() + "_" +
				// sMethodName + ".txt" );
				Logger.getInstance().setOutputStream(sFileName);
			} catch (Exception e) {
				System.err.println(e);
			}
			if (sMethodName.equals("QMDP")) {
				MDPValueFunction vfQMDP = pomdp.getMDPValueFunction();
				vfQMDP.persistQValues(true);
				vfQMDP.valueIteration(100, 0.001);
				double dDiscountedReward = pomdp.computeAverageDiscountedReward(1000, 100, vfQMDP);
				Logger.getInstance().log("POMDPSolver", 0, "main", "ADR = " + dDiscountedReward);
				System.exit(0);
			}

			// TODO ����blind policy�����PointBasedValueIteration
			ValueIteration viAlgorithm = AlgorithmsFactory.getAlgorithm(sMethodName, pomdp);
			viAlgorithm.setM_maxExecutionTime(maxExecutionTime);

			int cMaxIterations = maxIteration;
			try {
				/* run POMDP solver */
				viAlgorithm.valueIteration(cMaxIterations, ExecutionProperties.getEpsilon(), dTargetADR, maxRunningTime,
						numEvaluations);

				/* compute the averaged return */
				double dDiscountedReward = pomdp.computeAverageDiscountedReward(2000, 150, viAlgorithm);
				Logger.getInstance().log("POMDPSolver", 0, "main", "ADR = " + dDiscountedReward);
			}

			catch (Exception e) {
				Logger.getInstance().logln(e);
				e.printStackTrace();
			} catch (Error err) {
				Runtime rtRuntime = Runtime.getRuntime();
				Logger.getInstance()
						.logln("POMDPSolver: " + err + " allocated "
								+ (rtRuntime.totalMemory() - rtRuntime.freeMemory()) / 1000000 + " free "
								+ rtRuntime.freeMemory() / 1000000 + " max " + rtRuntime.maxMemory() / 1000000);
				Logger.getInstance().log("Stack trace: ");
				err.printStackTrace();
			}
			//求最佳动作
			MDPValueFunction valueFunction = pomdp.getMDPValueFunction();
			int[] state = new int[pedestrianList.size()];
			for (int i = 0; i < state.length; i++) {
				state[i] = 0;
			}
			for (int i = 0; i < state.length; i++) {
				double tempBelief = 0.0;
				for (int j = 0; j < pedestrianList.get(i).getPedestrianGoalList().size(); j++) {
					if (pedestrianList.get(i).getPedestrianGoalList().get(j).getBelief() > tempBelief) {
						tempBelief = pedestrianList.get(i).getPedestrianGoalList().get(j).getBelief();
						state[i] = j;
					}
				}
			}
			int stateNumber = 0;
			for (int i = 0; i < pedestrianList.size(); i++) {
				int tempProduct = 1;
				for (int j = i + 1; j < pedestrianList.size(); j++) {
					tempProduct *= pedestrianList.get(j).getPedestrianGoalList().size();
				}
				stateNumber += tempProduct * state[i];
			}
			double[] value = new double[3];
			for (int i = 0; i < 3; i++) {
				value[i] = valueFunction.getQValue(stateNumber, i);
			}
			//求reward
			int actionNumber = 0;
			double tempValue = value[0];
			for (int i = 0; i < value.length; i++) {
				if (value[i] > tempValue) {
					tempValue = value[i];
					actionNumber = i;
				}
			}
			for (int k = 0; k < pedestrianList.size(); k++) {
				reward -= 1 / (Math
						.sqrt(Math.pow(vehicle.getVehiclePos().get(0) - pedestrianList.get(k).getPedestrianPos().get(0),
								2)
						+ Math.pow(vehicle.getVehiclePos().get(1) - pedestrianList.get(k).getPedestrianPos().get(1), 2))
						/ Math.sqrt(2 * 4 * 4));
			}
			reward += 2 / (Math.sqrt(Math.pow(vehicle.getVehiclePos().get(0) - vehicle.getVehicleGoal().get(0), 2)
					+ Math.pow(vehicle.getVehiclePos().get(1) - vehicle.getVehicleGoal().get(1), 2)));
			
			if (actionNumber == 1 || actionNumber == 2) {
				reward -= 0.05;
			}
			
			if (actionNumber == 1) {
				vehicle.setVehicleSpeed(vehicle.getMaxSpeed() < vehicle.getVehicleSpeed() + map.getASpeed()
						? vehicle.getMaxSpeed() : vehicle.getVehicleSpeed() + map.getASpeed());
			} else if (actionNumber == 2) {
				vehicle.setVehicleSpeed(0 > vehicle.getVehicleSpeed() - map.getASpeed() ? 0
						: vehicle.getVehicleSpeed() - map.getASpeed());
			}
			//模拟汽车行驶
			Vector<Integer> newVehiclePos = new Vector<>();
			int newX = (int) Math.round(vehicle.getVehiclePos().get(0)
					+ vehicle.getVehicleSpeed() * Math.cos(vehicle.getVehicleOrientation()));
			int newY = (int) Math.round(vehicle.getVehiclePos().get(1)
					+ vehicle.getVehicleSpeed() * Math.sin(vehicle.getVehicleOrientation()));
			//判断是否撞上地图边界
			if (newX < 0) {
				newX = 0;
			}
			if (newX >= map.getMapWidth()) {
				newX = map.getMapWidth() - 1;
			}
			if (newY < 0) {
				newY = 0;
			}
			if (newY >= map.getMapHeight()) {
				newY = map.getMapHeight() - 1;
			}
			newVehiclePos.add(newX);
			newVehiclePos.add(newY);
			vehicle.setVehiclePos(newVehiclePos);
			//记录汽车路线
			actualPath.add(newVehiclePos);
			//判断是否撞车
			for (int i = 0; i < staticObstacleList.size(); i++) {
				Vector<Integer> obstacle = staticObstacleList.get(i);
				if (newX <= obstacle.get(0) + 7 && newX >= obstacle.get(0) - 7 && newY <= obstacle.get(1) + 7
						&& newY >= obstacle.get(1) - 7) {
					ifCollide = true;
					break;
				}
			}
			for (int i = 0; i < pedestrianList.size(); i++) {
				Vector<Integer> pedestrianGoalPos = pedestrianList.get(i).getPedestrianPos();
				if (newX <= pedestrianGoalPos.get(0) + 4 && newX >= pedestrianGoalPos.get(0) - 4
						&& newY <= pedestrianGoalPos.get(1) + 4 && newY >= pedestrianGoalPos.get(1) - 4) {
					ifCollide = true;
					break;
				}
			}
			// System.out.println();
			// System.out.println(vehicle.getVehiclePos().get(0) + ", " +
			// vehicle.getVehiclePos().get(1));
			// System.out.println(vehicle.getVehicleGoal().get(0) + ", " +
			// vehicle.getVehicleGoal().get(1));
		}
		//输出结果（到cmd、文件）
		System.out.println("get to the destination!\nThe path is: ");
		for (int i = 0; i < actualPath.size(); i++) {
			System.out.print("(" + actualPath.get(i).get(0) + ", " + actualPath.get(i).get(1) + ") ");
			if (i % 5 == 4) {
				System.out.print("\n");
			}
		}
		try {
			FileOperation.createFile("VehiclePositionRecord.txt");
			FileOperation.appendFileContent("VehiclePositionRecord.txt", "The path of this vehicle is:\n");
			for (int i = 0; i < actualPath.size(); i++) {
				FileOperation.appendFileContent("VehiclePositionRecord.txt",
						"time" + i + ": (" + actualPath.get(i).get(0) + ", " + actualPath.get(i).get(1) + ")\n");
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			FileOperation.createFile("PedestrianPositionRecord.txt");
			for (int i = 0; i < pedestrianPosRecord.size(); i++) {
				ArrayList<Vector<Integer>> eachPedestrianPos = pedestrianPosRecord.get(i);
				FileOperation.appendFileContent("PedestrianPositionRecord.txt",
						"The position record of the No." + i + " pedestrian:\n");
				for (int j = 0; j < eachPedestrianPos.size(); j++) {
					FileOperation.appendFileContent("PedestrianPositionRecord.txt", "time" + j + ": ("
							+ eachPedestrianPos.get(j).get(0) + ", " + eachPedestrianPos.get(j).get(1) + ")\n");
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\n" + "The reward is " + String.format("%.6f", reward));
		if (!ifCollide) {
			System.out.println("\n没有撞车哟！");
		} else {
			System.out.println("\n撞车了哟！");
		}
		//执行动画
		new AutoDrivingAnimation(pedestrianPosRecord, actualPath, staticObstacleList, vehicle.getVehicleGoal());
	}

	// }
	
	public static void  updatePedestrian(List<PedestrianModel> pedestrianModels,List<Vector<Integer>> staticObstacles,VehicleModel vehicleModel){

		List<Vector<Integer>> oldPedestrianPos = new ArrayList<>();
		for (int i = 0; i < pedestrianModels.size(); i++) {
			ArrayList<GoalModel> pedestrianGoals = pedestrianModels.get(i).getPedestrianGoalList();
			//随机出行人的行走目标点
			int goalNumber = (int) Math.floor(Math.random() * pedestrianGoals.size());
			GoalModel goal = pedestrianGoals.get(goalNumber);
			double distance = Math
					.sqrt(Math.pow(goal.getGoalPos().get(0) - pedestrianModels.get(i).getPedestrianPos().get(0), 2)
							+ Math.pow(goal.getGoalPos().get(1) - pedestrianModels.get(i).getPedestrianPos().get(1), 2));
			double rate = pedestrianModels.get(i).getPedestrianSpeed() / distance;
			int x = (int) Math.round((double) (pedestrianModels.get(i).getPedestrianPos().get(0))
					+ rate * (goal.getGoalPos().get(0) - pedestrianModels.get(i).getPedestrianPos().get(0)));
			int y = (int) Math.round((double) (pedestrianModels.get(i).getPedestrianPos().get(1))
					+ rate * (goal.getGoalPos().get(1) - pedestrianModels.get(i).getPedestrianPos().get(1)));
			// System.out.println(x);
			// System.out.println(y);
			//判断是否撞上汽车或者障碍物
			boolean ifRunIntoVehicle = false;
			if (x <= vehicleModel.getVehiclePos().get(0) + 4 && x >= vehicleModel.getVehiclePos().get(0) - 4
					&& y <= vehicleModel.getVehiclePos().get(1) + 4 && y >= vehicleModel.getVehiclePos().get(1) - 4) {
				ifRunIntoVehicle = true;
			}
			boolean ifRunIntoObstacle = false;
			for (int j = 0; j < staticObstacles.size(); j++) {
				Vector<Integer> obstacle = staticObstacles.get(j);
				if (x <= obstacle.get(0) + 7 && x >= obstacle.get(0) - 7 && y <= obstacle.get(1) + 7
						&& y >= obstacle.get(1) - 7) {
					ifRunIntoObstacle = true;
					break;
				}
			}

			while (ifRunIntoObstacle || ifRunIntoVehicle) {
				goalNumber = (int) Math.floor(Math.random() * pedestrianGoals.size());
				goal = pedestrianGoals.get(goalNumber);
				distance = Math.sqrt(
						Math.pow(goal.getGoalPos().get(0) - pedestrianModels.get(i).getPedestrianPos().get(0), 2) + Math
								.pow(goal.getGoalPos().get(1) - pedestrianModels.get(i).getPedestrianPos().get(1), 2));
				rate = pedestrianModels.get(i).getPedestrianSpeed() / distance;
				x = (int) Math.round((double) (pedestrianModels.get(i).getPedestrianPos().get(0))
						+ rate * (goal.getGoalPos().get(0) - pedestrianModels.get(i).getPedestrianPos().get(0)));
				y = (int) Math.round((double) (pedestrianModels.get(i).getPedestrianPos().get(1))
						+ rate * (goal.getGoalPos().get(1) - pedestrianModels.get(i).getPedestrianPos().get(1)));
			}

			oldPedestrianPos.add(pedestrianModels.get(i).getPedestrianPos());
			Vector<Integer> newPedestrianPos = new Vector<>();
			newPedestrianPos.add(x);
			newPedestrianPos.add(y);
			pedestrianModels.get(i).setPedestrianPos(newPedestrianPos);
			//pedestrianPosRecord.get(i).add(newPedestrianPos);
		}
	
		BeliefTracker beliefTracker;
		for (int i = 0; i < pedestrianModels.size(); i++) {
			
			beliefTracker = new BeliefTracker(pedestrianModels.get(i));
			//trackBelief方法重新计算了行人信念状态
			pedestrianModels.set(i, beliefTracker.trackBelief(oldPedestrianPos.get(i)));
		}
//		System.out.println(pedestrianModels.get(1).getPedestrianGoalList().get(0).getBelief());
//		System.out.println(pedestrianModels.get(1).getPedestrianGoalList().get(1).getBelief());
		
	}
	
	public static void updateVehicle(ArrayList<PedestrianModel> pedestrians,VehicleModel vehicleModel,ArrayList<Vector<Integer>> staticObstacles,MapSetter mapSetter){
		PathPlanner pathPlanner = new PathPlanner(staticObstacles, pedestrians, vehicleModel, mapSetter.getMapWidth(),
				mapSetter.getMapHeight());
		pathPlanner.findPath();

		ArrayList<Point> minCostPath = pathPlanner.getMinCostPath();
		// for (int i = 0; i < minCostPath.size(); i++) {
		// System.out.println(minCostPath.get(i).getX() + ", " +
		// minCostPath.get(i).getY());
		// }
		//计算汽车行驶角度
		Point nextDestination = minCostPath.get(1);
		if (nextDestination.getX() == vehicleModel.getVehiclePos().get(0)) {
			if (nextDestination.getY() > vehicleModel.getVehiclePos().get(1)) {
				vehicleModel.setVehicleOrientation(Math.PI * 0.5);
			} else {
				vehicleModel.setVehicleOrientation(Math.PI * 1.5);
			}
		} else {
			vehicleModel.setVehicleOrientation(Math.atan((nextDestination.getY() - vehicleModel.getVehiclePos().get(1))
					/ (nextDestination.getX() - vehicleModel.getVehiclePos().get(0))));

		}
		
	}
	
	public static void solvePomdp(ArrayList<PedestrianModel> pedestrians,VehicleModel vehicle,MapSetter mapSetter,List<Vector<Integer>> staticObstacles){
		double reward = 0.0;
		
		//生成pomdp模型文件
		PomdpModelBuilder pomdpModelBuilder = new PomdpModelBuilder(pedestrians, vehicle);
		pomdpModelBuilder.buildPomdpModelFile();
		pomdpModelBuilder.writeDiscount((float) 0.95);
		pomdpModelBuilder.writeValues("reward");
		pomdpModelBuilder.writeStates();
		pomdpModelBuilder.writeActions();
		pomdpModelBuilder.writeObservations();
		pomdpModelBuilder.writeStartingBeliefState();
		pomdpModelBuilder.writeStateTransitionProbabilities();
		pomdpModelBuilder.writeObservationProbabilities();
		pomdpModelBuilder.writeReward();
		
		//pomdp模型求解
		JProf.getCurrentThreadCpuTimeSafe();
		String sPath = ExecutionProperties.getPath();
		String sModelName = "autonomous_driving";
		String sMethodName = "FSVI";
		long maxExecutionTime = 1000 * 60 * 10;
		int maxIteration = 100;
		
		Logger.getInstance().setOutput(true);
		Logger.getInstance().setSilent(false);
		try {
			String sOutputDir = "logs/exp_logs";
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm");

			String sFileName = sModelName + "_" + sMethodName + "_" + sdf.format(new Date()) + ".txt";
			Logger.getInstance().setOutputStream(sOutputDir, sFileName);
		} catch (Exception e) {
			System.err.println(e);
		}
		POMDP pomdp = null;
		double dTargetADR = 100.0;
		int maxRunningTime = 45;
		int numEvaluations = 3;
		try {
			pomdp = new POMDP();
			pomdp.load(sPath + sModelName + ".POMDP");
			Logger.getInstance().logln("max is " + pomdp.getMaxR() + " min is " + pomdp.getMinR());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Logger.getInstance().logln(e);
			e.printStackTrace();
			System.exit(0);
		}
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm");
			String sFileName = sModelName + "_" + sMethodName + "_" + sdf.format(new Date()) + ".txt";
			// Logger.getInstance().setOutputStream( pomdp.getName() + "_" +
			// sMethodName + ".txt" );
			Logger.getInstance().setOutputStream(sFileName);
		} catch (Exception e) {
			System.err.println(e);
		}
		if (sMethodName.equals("QMDP")) {
			MDPValueFunction vfQMDP = pomdp.getMDPValueFunction();
			vfQMDP.persistQValues(true);
			vfQMDP.valueIteration(100, 0.001);
			double dDiscountedReward = pomdp.computeAverageDiscountedReward(1000, 100, vfQMDP);
			Logger.getInstance().log("POMDPSolver", 0, "main", "ADR = " + dDiscountedReward);
			System.exit(0);
		}

		// TODO ����blind policy�����PointBasedValueIteration
		ValueIteration viAlgorithm = AlgorithmsFactory.getAlgorithm(sMethodName, pomdp);
		viAlgorithm.setM_maxExecutionTime(maxExecutionTime);

		int cMaxIterations = maxIteration;
		try {
			/* run POMDP solver */
			viAlgorithm.valueIteration(cMaxIterations, ExecutionProperties.getEpsilon(), dTargetADR, maxRunningTime,
					numEvaluations);

			/* compute the averaged return */
			double dDiscountedReward = pomdp.computeAverageDiscountedReward(2000, 150, viAlgorithm);
			Logger.getInstance().log("POMDPSolver", 0, "main", "ADR = " + dDiscountedReward);
		}

		catch (Exception e) {
			Logger.getInstance().logln(e);
			e.printStackTrace();
		} catch (Error err) {
			Runtime rtRuntime = Runtime.getRuntime();
			Logger.getInstance()
					.logln("POMDPSolver: " + err + " allocated "
							+ (rtRuntime.totalMemory() - rtRuntime.freeMemory()) / 1000000 + " free "
							+ rtRuntime.freeMemory() / 1000000 + " max " + rtRuntime.maxMemory() / 1000000);
			Logger.getInstance().log("Stack trace: ");
			err.printStackTrace();
		}
		//求最佳动作
		MDPValueFunction valueFunction = pomdp.getMDPValueFunction();
		int[] state = new int[pedestrians.size()];
		for (int i = 0; i < state.length; i++) {
			state[i] = 0;
		}
		for (int i = 0; i < state.length; i++) {
			double tempBelief = 0.0;
			for (int j = 0; j < pedestrians.get(i).getPedestrianGoalList().size(); j++) {
				if (pedestrians.get(i).getPedestrianGoalList().get(j).getBelief() > tempBelief) {
					tempBelief = pedestrians.get(i).getPedestrianGoalList().get(j).getBelief();
					state[i] = j;
				}
			}
		}
		int stateNumber = 0;
		for (int i = 0; i < pedestrians.size(); i++) {
			int tempProduct = 1;
			for (int j = i + 1; j < pedestrians.size(); j++) {
				tempProduct *= pedestrians.get(j).getPedestrianGoalList().size();
			}
			stateNumber += tempProduct * state[i];
		}
		double[] value = new double[3];
		for (int i = 0; i < 3; i++) {
			value[i] = valueFunction.getQValue(stateNumber, i);
		}
		//求reward
		int actionNumber = 0;
		double tempValue = value[0];
		for (int i = 0; i < value.length; i++) {
			if (value[i] > tempValue) {
				tempValue = value[i];
				actionNumber = i;
			}
		}
		for (int k = 0; k < pedestrians.size(); k++) {
			reward -= 1 / (Math
					.sqrt(Math.pow(vehicle.getVehiclePos().get(0) - pedestrians.get(k).getPedestrianPos().get(0),
							2)
					+ Math.pow(vehicle.getVehiclePos().get(1) - pedestrians.get(k).getPedestrianPos().get(1), 2))
					/ Math.sqrt(2 * 4 * 4));
		}
		reward += 2 / (Math.sqrt(Math.pow(vehicle.getVehiclePos().get(0) - vehicle.getVehicleGoal().get(0), 2)
				+ Math.pow(vehicle.getVehiclePos().get(1) - vehicle.getVehicleGoal().get(1), 2)));
		
		if (actionNumber == 1 || actionNumber == 2) {
			reward -= 0.05;
		}
		
		if (actionNumber == 1) {
			vehicle.setVehicleSpeed(vehicle.getMaxSpeed() < vehicle.getVehicleSpeed() + mapSetter.getASpeed()
					? vehicle.getMaxSpeed() : vehicle.getVehicleSpeed() + mapSetter.getASpeed());
		} else if (actionNumber == 2) {
			vehicle.setVehicleSpeed(0 > vehicle.getVehicleSpeed() - mapSetter.getASpeed() ? 0
					: vehicle.getVehicleSpeed() - mapSetter.getASpeed());
		}
		//模拟汽车行驶
		Vector<Integer> newVehiclePos = new Vector<>();
		int newX = (int) Math.round(vehicle.getVehiclePos().get(0)
				+ vehicle.getVehicleSpeed() * Math.cos(vehicle.getVehicleOrientation()));
		int newY = (int) Math.round(vehicle.getVehiclePos().get(1)
				+ vehicle.getVehicleSpeed() * Math.sin(vehicle.getVehicleOrientation()));
		//判断是否撞上地图边界
		if (newX < 0) {
			newX = 0;
		}
		if (newX >= mapSetter.getMapWidth()) {
			newX = mapSetter.getMapWidth() - 1;
		}
		if (newY < 0) {
			newY = 0;
		}
		if (newY >= mapSetter.getMapHeight()) {
			newY = mapSetter.getMapHeight() - 1;
		}
		newVehiclePos.add(newX);
		newVehiclePos.add(newY);
		vehicle.setVehiclePos(newVehiclePos);
		boolean ifCollide = false;
		//判断是否撞车
		for (int i = 0; i < staticObstacles.size(); i++) {
			Vector<Integer> obstacle = staticObstacles.get(i);
			if (newX <= obstacle.get(0) + 7 && newX >= obstacle.get(0) - 7 && newY <= obstacle.get(1) + 7
					&& newY >= obstacle.get(1) - 7) {
				ifCollide = true;
				break;
			}
		}
		for (int i = 0; i < pedestrians.size(); i++) {
			Vector<Integer> pedestrianGoalPos = pedestrians.get(i).getPedestrianPos();
			if (newX <= pedestrianGoalPos.get(0) + 4 && newX >= pedestrianGoalPos.get(0) - 4
					&& newY <= pedestrianGoalPos.get(1) + 4 && newY >= pedestrianGoalPos.get(1) - 4) {
				ifCollide = true;
				break;
			}
		}
		
	}
	
	/**
	 * 计算下一步汽车和行人的位置
	 * @return
	 */
	public static UpdateModel update(UpdateModel updateModel,ArrayList<Vector<Integer>> staticObstacleList,MapSetter mapSetter){
		ArrayList<PedestrianModel> pedestrianModels = updateModel.getPedestrians();
		updatePedestrian(pedestrianModels, staticObstacleList,updateModel.getVehicle());
		updateVehicle(pedestrianModels, updateModel.getVehicle(), staticObstacleList,mapSetter);
		solvePomdp(pedestrianModels, updateModel.getVehicle(),mapSetter,staticObstacleList);
		
		return updateModel;
	}

	public static void main(String[] args) {
//		Simulator test = new Simulator();
//		test.startAutonomousDriving(args);
		
		MapSetter map = new MapSetter();
		ArrayList<PedestrianModel> pedestrians = map.getPedestrianList();
		VehicleModel vehicle = map.getVehicle();
		ArrayList<Vector<Integer>> staticObstacleList = map.getStaticObstacleList();
		UpdateModel updateModel = new UpdateModel(vehicle, pedestrians);
		ArrayList<Vector<Integer>> posRecord = new ArrayList<>();
		while (Math.sqrt(Math.pow(vehicle.getVehiclePos().get(0) - vehicle.getVehicleGoal().get(0), 2)
				+ Math.pow(vehicle.getVehiclePos().get(1) - vehicle.getVehicleGoal().get(1), 2)) > vehicle
				.getMaxSpeed()) {
			update(updateModel, staticObstacleList, map);
			posRecord.add(pedestrians.get(0).getPedestrianPos());
		}
		
		for(Vector<Integer> position : posRecord){
			System.out.print("(" + position.get(0) + "," + position.get(1) + "),");
		}
	}
	
	
	
	
	
}
