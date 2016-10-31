package animation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import model.PedestrianModel;
import model.UpdateModel;
import model.VehicleModel;
import simulator.MapSetter;
import simulator.Simulator;

public class AutoDrivingAnimation {
	private ArrayList<ArrayList<Vector<Integer>>> pedestrianPosRecord;
	private ArrayList<Vector<Integer>> vehiclePath;
	private Vector<Integer> vehicleGoal;
	private ArrayList<Vector<Integer>> staticObstacleList;
	private JFrame mapFrame;
	private JPanel mapPanel;
	private JPanel[] staticObstaclePanels;
	private JPanel vehiclePanel;
	private JPanel vehicleGoalPanel;
	private JPanel[] pedestrianPanels;
	//added by liushoa
	private ArrayList<PedestrianModel> pedestrianModels;
	private VehicleModel vehicleModel;
	private MapSetter mapSetter;
	private UpdateModel updateModel;

	public AutoDrivingAnimation(ArrayList<ArrayList<Vector<Integer>>> pedestrianPosRecord,
			ArrayList<Vector<Integer>> vehiclePath, ArrayList<Vector<Integer>> staticObstacleList,
			Vector<Integer> vehicleGoal) {
//		this.pedestrianPosRecord = pedestrianPosRecord;
//		this.vehiclePath = vehiclePath;
//		this.vehicleGoal = vehicleGoal;
//		this.staticObstacleList = staticObstacleList;
//		mapFrame = new JFrame("无人汽车驾驶模拟器");
//		mapPanel = new JPanel();
//		createStaticObstaclePanel();
//		createVehiclePanel();
//		createPedestrianPanel();
//		layComponent();
//		showFrame();
//		Thread animation = new Thread(new Animation());
//		animation.start();
	}
	
	public AutoDrivingAnimation(MapSetter mapSetter) {
		this.mapSetter = mapSetter;
		this.pedestrianModels = mapSetter.getPedestrianList();
		this.vehicleModel = mapSetter.getVehicle();
		this.staticObstacleList = mapSetter.getStaticObstacleList();
		updateModel = new UpdateModel(this.vehicleModel, this.pedestrianModels);
		mapFrame = new JFrame("无人汽车驾驶模拟器");
		mapPanel = new JPanel();
		createStaticObstaclePanel();
		createVehiclePanel();
		createPedestrianPanel();
		layComponent();
		showFrame();
		Thread animation = new Thread(new Animation2());
		animation.start();
		
	}
	

	//创建固定障碍物的JPanel
	private void createStaticObstaclePanel() {
		staticObstaclePanels = new JPanel[staticObstacleList.size()];
		for (int i = 0; i < staticObstaclePanels.length; i++) {
			staticObstaclePanels[i] = new JPanel();
			staticObstaclePanels[i].setBackground(Color.black);
			// staticObstacleLabels[i].setForeground(Color.yellow);
		}
	}

	//创建汽车的JPanel
	private void createVehiclePanel() {
		vehiclePanel = new JPanel();
		vehiclePanel.setBackground(Color.blue);
		vehicleGoalPanel = new JPanel();
		vehicleGoalPanel.setBackground(new Color(255, 0, 255));
		
	}

	//创建行人的JPanel
	private void createPedestrianPanel() {
		pedestrianPanels = new JPanel[pedestrianModels.size()];
		for (int i = 0; i < pedestrianPanels.length; i++) {
			pedestrianPanels[i] = new JPanel();
			pedestrianPanels[i].setBackground(Color.red);
		}
	}

	//将行人、障碍物、汽车的JPanel放置到map上
	private void layComponent() {
		for (int i = 0; i < staticObstaclePanels.length; i++) {
			mapPanel.add(staticObstaclePanels[i]);
		}
		mapPanel.add(vehiclePanel);
		mapPanel.add(vehicleGoalPanel);
		for (int i = 0; i < pedestrianPanels.length; i++) {
			mapPanel.add(pedestrianPanels[i]);
		}
		mapPanel.setLayout(null);
		for (int i = 0; i < staticObstaclePanels.length; i++) {
			staticObstaclePanels[i].setBounds(staticObstacleList.get(i).get(0) - 5,
					staticObstacleList.get(i).get(1) - 5, 11, 11);
		}
		vehiclePanel.setBounds(vehicleModel.getVehiclePos().get(0) - 2, vehicleModel.getVehiclePos().get(1) - 2, 5, 5);
		vehicleGoalPanel.setBounds(vehicleModel.getVehicleGoal().get(0) - 2, vehicleModel.getVehicleGoal().get(1) - 2, 5, 5);
		for (int i = 0; i < pedestrianPanels.length; i++) {
			pedestrianPanels[i].setBounds(pedestrianModels.get(i).getPedestrianPos().get(0) - 5,
					pedestrianModels.get(i).getPedestrianPos().get(1) - 5, 5, 5);
		}

		mapFrame.getContentPane().add(mapPanel);
		mapFrame.getContentPane().setLayout(null);
		mapPanel.setBounds(0, 0, 1000, 1000);
	}

	private void showFrame() {
		mapFrame.setSize(1000, 1000);
		mapFrame.setResizable(false);
		mapFrame.setLocationRelativeTo(null);
		mapFrame.setVisible(true);
	}

	//动画的内部类
//	class Animation implements Runnable {
//
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			//每个单位时间分为5帧动画
//			for (int i = 1; i <= (vehiclePath.size() - 1) * 5; i++) {
//				int numOfInterval = (int) Math.ceil(i / 5.0);
//				int rank = numOfInterval * 5 - i;
//				for (int j = 0; j < pedestrianPanels.length; j++) {
//					int positionX = Math.round(pedestrianPosRecord.get(j).get(numOfInterval - 1).get(0)
//							+ (pedestrianPosRecord.get(j).get(numOfInterval).get(0)
//									- pedestrianPosRecord.get(j).get(numOfInterval - 1).get(0)) / 5 * rank);
//					int positionY = Math.round(pedestrianPosRecord.get(j).get(numOfInterval - 1).get(1)
//							+ (pedestrianPosRecord.get(j).get(numOfInterval).get(1)
//									- pedestrianPosRecord.get(j).get(numOfInterval - 1).get(1)) / 5 * rank);
//					pedestrianPanels[j].setLocation(positionX, positionY);
//				}
//				int vehiclePosX = Math.round(vehiclePath.get(numOfInterval - 1).get(0)
//						+ (vehiclePath.get(numOfInterval).get(0) - vehiclePath.get(numOfInterval - 1).get(0)) / 5
//								* rank);
//				int vehiclePosY = Math.round(vehiclePath.get(numOfInterval - 1).get(1)
//						+ (vehiclePath.get(numOfInterval).get(1) - vehiclePath.get(numOfInterval - 1).get(1)) / 5
//								* rank);
//				vehiclePanel.setLocation(vehiclePosX, vehiclePosY);
//				try {
//					Thread.sleep(50);
//				} catch (InterruptedException e) {
//					
//					e.printStackTrace();
//				}
//			}
//
//		}
//
//	}
	
	class Animation2 implements Runnable{
		@Override
		public void run() {
			while(Math.sqrt(Math.pow(vehicleModel.getVehiclePos().get(0) - vehicleModel.getVehicleGoal().get(0), 2)
					+ Math.pow(vehicleModel.getVehiclePos().get(1) - vehicleModel.getVehicleGoal().get(1), 2)) > vehicleModel
					.getMaxSpeed()){
				updateModel = Simulator.update(updateModel, staticObstacleList, mapSetter);
				vehicleModel = updateModel.getVehicle();
				pedestrianModels = updateModel.getPedestrians();
				for (int j = 0; j < pedestrianPanels.length; j++) {
					int positionX = Math.round(pedestrianModels.get(j).getPedestrianPos().get(0));
					int positionY = Math.round(pedestrianModels.get(j).getPedestrianPos().get(1));
					pedestrianPanels[j].setLocation(positionX, positionY);
				}
				int vehiclePosX = vehicleModel.getVehiclePos().get(0);
				int vehiclePosY = vehicleModel.getVehiclePos().get(1);
				vehiclePanel.setLocation(vehiclePosX, vehiclePosY);
			}
			
		}
	}
	
	public static void main(String[] args) {
		new AutoDrivingAnimation(new MapSetter());
	}

}
