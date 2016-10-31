package pathPlanning;

import java.util.ArrayList;
import java.util.Vector;

import model.PedestrianModel;

public class CostMapPlacer {
	private ArrayList<Vector<Integer>> staticObstacleList;
	private ArrayList<PedestrianModel> pedestrianList;
	private int[][] costMap;

	public CostMapPlacer(ArrayList<Vector<Integer>> staticObstacleList, ArrayList<PedestrianModel> pedestrianList,
			int mapWidth, int mapHeight) {
		this.staticObstacleList = staticObstacleList;
		this.pedestrianList = pedestrianList;
		costMap = new int[mapWidth][mapHeight];
		for (int i = 0; i < costMap.length; i++) {
			for (int j = 0; j < costMap[i].length; j++) {
				costMap[i][j] = 0;
			}
		}
		placeObstacleAndPedestrian();
		placeStaticObstacleCostMap();
		placePedestrianCostMap();
	}

	public void placeObstacleAndPedestrian() {
		//将固定障碍物周围放置数值-1表示不能到达
		for (int i = 0; i < staticObstacleList.size(); i++) {
			Vector<Integer> staticObstacle = staticObstacleList.get(i);
			int x = staticObstacle.get(0);
			int y = staticObstacle.get(1);
			for (int j = x - 7; j < x + 8; j++) {
				for (int k = y - 7; k < y + 8; k++) {
					costMap[j][k] = -1;
				}
			}
		}

		//将行人周围放置-1表示不能到达
		for (int i = 0; i < pedestrianList.size(); i++) {
			PedestrianModel pedestrian = pedestrianList.get(i);
			int x = pedestrian.getPedestrianPos().get(0);
			int y = pedestrian.getPedestrianPos().get(1);
			for (int j = x - 4; j < x + 5; j++) {
				for (int k = y - 4; k < y + 5; k++) {
					costMap[j][k] = -2;
				}
			}
		}
	}

	//设置因固定障碍物产生的CostMap，规则可以采取被认为合理的任意规则（可以改进）
	public void placeStaticObstacleCostMap() {
		for (int i = 0; i < staticObstacleList.size(); i++) {
			Vector<Integer> staticObstacle = staticObstacleList.get(i);
			int x = staticObstacle.get(0);
			int y = staticObstacle.get(1);
			for (int j = x - 17; j < x - 7; j++) {
				for (int k = y + 8; k < y + 18; k++) {
					if (j >= 0 && j < 1000 && k >= 0 && k < 1000 && costMap[j][k] >= 0) {
						costMap[j][k] += Math
								.round((140 - 14 * (Math.abs(y - k) - 8)) / Math.pow(1.4, Math.abs(x - j) - 7));
					}
				}
			}
			for (int j = x - 7; j < x + 8; j++) {
				for (int k = y + 8; k < y + 18; k++) {
					if (j >= 0 && j < 1000 && k >= 0 && k < 1000 && costMap[j][k] >= 0) {
						costMap[j][k] += (140 - 14 * (Math.abs(y - k) - 8));
					}
				}
			}
			for (int j = x + 8; j < x + 18; j++) {
				for (int k = y + 8; k < y + 18; k++) {
					if (j >= 0 && j < 1000 && k >= 0 && k < 1000 && costMap[j][k] >= 0) {
						costMap[j][k] += Math
								.round((140 - 14 * (Math.abs(y - k) - 8)) / Math.pow(1.4, Math.abs(x - j) - 7));
					}
				}
			}
			for (int j = x - 17; j < x - 7; j++) {
				for (int k = y - 7; k < y + 8; k++) {
					if (j >= 0 && j < 1000 && k >= 0 && k < 1000 && costMap[j][k] >= 0) {
						costMap[j][k] += (140 - 14 * (Math.abs(x - j) - 8));
					}
				}
			}
			for (int j = x + 8; j < x + 18; j++) {
				for (int k = y - 7; k < y + 8; k++) {
					if (j >= 0 && j < 1000 && k >= 0 && k < 1000 && costMap[j][k] >= 0) {
						costMap[j][k] += (140 - 14 * (Math.abs(x - j) - 8));
					}
				}
			}
			for (int j = x - 17; j < x - 7; j++) {
				for (int k = y - 17; k < y - 7; k++) {
					if (j >= 0 && j < 1000 && k >= 0 && k < 1000 && costMap[j][k] >= 0) {
						costMap[j][k] += Math
								.round((140 - 14 * (Math.abs(y - k) - 8)) / Math.pow(1.4, Math.abs(x - j) - 7));
					}
				}
			}
			for (int j = x - 7; j < x + 8; j++) {
				for (int k = y - 17; k < y - 7; k++) {
					if (j >= 0 && j < 1000 && k >= 0 && k < 1000 && costMap[j][k] >= 0) {
						costMap[j][k] += (140 - 14 * (Math.abs(y - k) - 8));
					}
				}
			}
			for (int j = x + 8; j < x + 18; j++) {
				for (int k = y - 17; k < y - 7; k++) {
					if (j >= 0 && j < 1000 && k >= 0 && k < 1000 && costMap[j][k] >= 0) {
						costMap[j][k] += Math
								.round((140 - 14 * (Math.abs(y - k) - 8)) / Math.pow(1.4, Math.abs(x - j) - 7));
					}
				}
			}
		}
	}

	//设置因行人产生的CostMap，规则可以采取被认为合理的任意规则（可以改进）
	public void placePedestrianCostMap() {
		for (int i = 0; i < pedestrianList.size(); i++) {
			PedestrianModel pedestrian = pedestrianList.get(i);
//			System.out.println(pedestrianList.indexOf(pedestrian));
			double[] pedestrianBelief = new double[pedestrian.getPedestrianGoalList().size()];
			for (int j = 0; j < pedestrianBelief.length; j++) {
				pedestrianBelief[j] = pedestrian.getPedestrianGoalList().get(j).getBelief();
			}
			double avgOfBelief = 1.0 / pedestrianBelief.length;
			double squareSum = 0.0;
			for (int k = 0; k < pedestrianBelief.length; k++) {
//				System.out.println(pedestrianBelief[k]);
				squareSum += Math.pow((pedestrianBelief[k] - avgOfBelief), 2);
//				System.out.println(squareSum);

			}
			double sigma = Math.sqrt(squareSum / pedestrianBelief.length);
//			System.out.println(squareSum);
//			System.out.println(avgOfBelief);

			if (sigma > 0.15) {
				int x = pedestrian.getPedestrianPos().get(0);
				int y = pedestrian.getPedestrianPos().get(1);
				for (int k = x - 14; k < x - 4; k++) {
					for (int l = y + 5; l < y + 15; l++) {
						if (k >= 0 && k < 1000 && l >= 0 && l < 1000 && costMap[k][l] >= 0) {
							costMap[k][l] += Math
									.round((140 - 14 * (Math.abs(y - l) - 5)) / Math.pow(1.4, Math.abs(x - k) - 4));
						}
					}
				}
				for (int k = x - 4; k < x + 5; k++) {
					for (int l = y + 5; l < y + 15; l++) {
						if (k >= 0 && k < 1000 && l >= 0 && l < 1000 && costMap[k][l] >= 0) {
							costMap[k][l] += (140 - 14 * (Math.abs(y - l) - 5));
						}
					}
				}
				for (int k = x + 5; k < x + 15; k++) {
					for (int l = y + 5; l < y + 15; l++) {
						if (k >= 0 && k < 1000 && l >= 0 && l < 1000 && costMap[k][l] >= 0) {
							costMap[k][l] += Math
									.round((140 - 14 * (Math.abs(y - l) - 5)) / Math.pow(1.4, Math.abs(x - k) - 4));
						}
					}
				}
				for (int k = x - 14; k < x - 4; k++) {
					for (int l = y - 4; k < y + 5; l++) {
						if (k >= 0 && k < 1000 && l >= 0 && l < 1000 && costMap[k][l] >= 0) {
							costMap[k][l] += (140 - 14 * (Math.abs(x - k) - 5));
						}
					}
				}
				for (int k = x + 5; k < x + 15; k++) {
					for (int l = y - 4; l < y + 5; l++) {
						if (k >= 0 && k < 1000 && l >= 0 && l < 1000 && costMap[k][l] >= 0) {
							costMap[k][l] += (140 - 14 * (Math.abs(x - k) - 5));
						}
					}
				}
				for (int k = x - 14; k < x - 4; k++) {
					for (int l = y - 14; l < y - 4; l++) {
						if (k >= 0 && k < 1000 && l >= 0 && l < 1000 && costMap[k][l] >= 0) {
							costMap[k][l] += Math
									.round((140 - 14 * (Math.abs(y - l) - 5)) / Math.pow(1.4, Math.abs(x - k) - 4));
						}
					}
				}
				for (int k = x - 4; k < x + 5; k++) {
					for (int l = y - 14; l < y - 4; l++) {
						if (k >= 0 && k < 1000 && l >= 0 && l < 1000 && costMap[k][l] >= 0) {
							costMap[k][l] += (140 - 14 * (Math.abs(y - l) - 5));
						}
					}
				}
				for (int k = x + 5; k < x + 15; k++) {
					for (int l = y - 14; l < y - 4; l++) {
						if (k >= 0 && k < 1000 && l >= 0 && l < 1000 && costMap[k][l] >= 0) {
							costMap[k][l] += Math
									.round((140 - 14 * (Math.abs(y - l) - 4)) / Math.pow(1.4, Math.abs(x - k) - 4));
						}
					}
				}
			} else {
				double maxBelief = 0.0;
				int maxBeliefCount = 0;
				for (int k = 0; k < pedestrianBelief.length; k++) {
					if (pedestrianBelief[k] > maxBelief) {
						maxBelief = pedestrianBelief[k];
						maxBeliefCount = k;
					}
				}
				int startX = 0;
				int startY = 0;
				int endX = 0;
				int endY = 0;
				if (pedestrian.getPedestrianPos().get(0) < pedestrian.getPedestrianGoalList().get(maxBeliefCount)
						.getGoalPos().get(0)) {
					startX = pedestrian.getPedestrianPos().get(0);
					startY = pedestrian.getPedestrianPos().get(1);
					endX = pedestrian.getPedestrianGoalList().get(maxBeliefCount).getGoalPos().get(0);
					endY = pedestrian.getPedestrianGoalList().get(maxBeliefCount).getGoalPos().get(1);
				} else {
					endX = pedestrian.getPedestrianPos().get(0);
					endY = pedestrian.getPedestrianPos().get(1);
					startX = pedestrian.getPedestrianGoalList().get(maxBeliefCount).getGoalPos().get(0);
					startY = pedestrian.getPedestrianGoalList().get(maxBeliefCount).getGoalPos().get(1);
				}
				if (startX != endX) {
					double slope = (startY - endY) / (startX - endX);
					for (int k = startX; k < (10 < endX ? 10 : endX); k++) {
						if (costMap[k][(int) Math.round(startY + (k - startX) * slope)] >= 0) {
							costMap[k][(int) Math.round(startY + (k - startX) * slope)] += (504 - 28 * (k - startX));
						}
					}
					for (int k = 1; k < 8; k++) {
						for (int l = startX - k; l < endX - k; l++) {
							if (costMap[l][(int) Math.round(startY + k + (l - (startX - k)) * slope)] >= 0) {
								costMap[l][(int) Math.round(startY + k + (l - (startX - k)) * slope)] += (504
										/ Math.pow(1.4, k) - 28 / Math.pow(1.4, k) * (l - (startX - k)));
							}
						}
						for (int l = startX + (10 < endX ? 10 : endX); l < endX + (10 < endX ? 10 : endX); l++) {
							if (costMap[l][(int) Math.round(startY - k + (l - (startX + k)) * slope)] >= 0) {
								costMap[l][(int) Math.round(startY - k + (l - (startX + k)) * slope)] += (504
										/ Math.pow(1.4, k) - 28 / Math.pow(1.4, k) * (l - (startX + k)));
							}
						}
					}
				}
			}
		}
	}

	public int[][] getCostMap() {
		return costMap;
	}
}
