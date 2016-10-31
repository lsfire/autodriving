package pathPlanning;

import java.util.ArrayList;
import java.util.Vector;

import model.GoalModel;
import model.PedestrianModel;

public class BeliefTracker {
	private PedestrianModel pedestrian;

	public BeliefTracker(PedestrianModel pedestrian) {
		this.pedestrian = pedestrian;
	}

	public PedestrianModel trackBelief(Vector<Integer> oldPedestrianPos) {
		Vector<Integer> oldPos = oldPedestrianPos;
		Vector<Integer> newPos = pedestrian.getPedestrianPos();
		ArrayList<GoalModel> pedestrianGoalModelList = pedestrian.getPedestrianGoalList();
		double[] tempBelief = new double[pedestrianGoalModelList.size()];
		for (int i = 0; i < pedestrianGoalModelList.size(); i++) {
			//计算移动之前行人和目标点的距离
			double oldDistance = Math
					.sqrt(Math.pow(oldPos.get(0) - pedestrianGoalModelList.get(i).getGoalPos().get(0), 2)
							+ Math.pow(oldPos.get(1) - pedestrianGoalModelList.get(i).getGoalPos().get(1), 2));
			//计算移动之后行人和目标点的距离
			double newDistance = Math
					.sqrt(Math.pow(newPos.get(0) - pedestrianGoalModelList.get(i).getGoalPos().get(0), 2)
							+ Math.pow(newPos.get(1) - pedestrianGoalModelList.get(i).getGoalPos().get(1), 2));
			tempBelief[i] = pedestrianGoalModelList.get(i).getBelief() / (newDistance / oldDistance);
		}
		//belief归一化
		double sumOfTempBelief = 0.0;
		for (int i = 0; i < tempBelief.length; i++) {
			sumOfTempBelief += tempBelief[i];
		}
		for (int i = 0; i < tempBelief.length; i++) {
			tempBelief[i] /= sumOfTempBelief;
		}
		for (int i = 0; i < pedestrianGoalModelList.size(); i++) {
			pedestrianGoalModelList.get(i).setBelief(tempBelief[i]);
		}
		pedestrian.setPedestrianGoal(pedestrianGoalModelList);
		return pedestrian;
	}
}
