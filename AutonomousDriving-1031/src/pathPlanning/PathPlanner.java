package pathPlanning;

import java.util.ArrayList;
import java.util.Vector;

import model.PedestrianModel;
import model.VehicleModel;
/*
 * A*自动寻路的实现类
 */
public class PathPlanner {
	private CostMapPlacer costMapPlacer;
	private int[][] costMap;
	private VehicleModel vehicle;
	private ArrayList<Point> openList;
	private ArrayList<Point> closeList;
	private double searchDistance;
	private double searchOrientation;
	private double lambda;
	private ArrayList<Point> minCostPath;
	private int counter;

	public PathPlanner(ArrayList<Vector<Integer>> staticObstacleList, ArrayList<PedestrianModel> pedestrianList,
			VehicleModel vehicle, int mapWidth, int mapHeight) {
		costMapPlacer = new CostMapPlacer(staticObstacleList, pedestrianList, mapWidth, mapHeight);
		costMap = costMapPlacer.getCostMap();
		this.vehicle = vehicle;
		openList = new ArrayList<>();
		closeList = new ArrayList<>();
		this.searchDistance = vehicle.getMaxSpeed() * 2;
		this.searchOrientation = Math.PI / 18;
		lambda = 0.9;
		minCostPath = new ArrayList<>();
		counter = 0;
	}

	//寻路方法
	public void findPath() {
		//设置起点和终点
		Point startPos = new Point(vehicle.getVehiclePos().get(0), vehicle.getVehiclePos().get(1));
		startPos.setF(costMap[startPos.getX()][startPos.getY()]);
		Point endPos = new Point(vehicle.getVehicleGoal().get(0), vehicle.getVehicleGoal().get(1));
		//将起点加入“开启列表”
		openList.add(startPos);
		//寻找起点周围可到达的点，设置F值和父节点，加入开启列表
		for (int i = 0; i < 36; i++) {
			int newX = (int) Math.round(startPos.getX() + searchDistance * Math.cos(searchOrientation * i));
			int newY = (int) Math.round(startPos.getY() + searchDistance * Math.sin(searchOrientation * i));
			if (newX >= 0 && newX < costMap.length && newY >= 0 && newY < costMap.length && costMap[newX][newY] >= 0) {
				Point nextPoint = new Point(newX, newY);
				nextPoint.setParentPoint(startPos);
				nextPoint.setNum(nextPoint.getParentPoint().getNum() + 1);
				double nonSmoothCost = Math.abs(vehicle.getVehicleOrientation() - searchOrientation * i)
						/ searchOrientation * 1;
				double distanceCost = Math.sqrt(Math.pow(nextPoint.getX() - vehicle.getVehiclePos().get(0), 2)
						+ Math.pow(nextPoint.getY() - vehicle.getVehiclePos().get(1), 2));
				nextPoint.setF(nextPoint.getParentPoint().getF()
						+ Math.pow(lambda, nextPoint.getNum()) * costMap[nextPoint.getX()][nextPoint.getY()]
						+ nonSmoothCost + distanceCost);
				openList.add(nextPoint);
			}
		}
		//将起点从开启列表移除
		openList.remove(0);
		//将其点加入关闭列表
		closeList.add(startPos);
		counter++;
		while (!closeList.contains(endPos)) {//循环条件为关闭列表中不包含终点（目标点）
			//寻找F值最低的点，从开启列表中移除，加入关闭列表
			Point minCostPoint = openList.get(0);
			for (int i = 0; i < openList.size(); i++) {
				if (openList.get(i).getF() < minCostPoint.getF()) {
					minCostPoint = openList.get(i);
				}
			}
			openList.remove(minCostPoint);
			closeList.add(minCostPoint);
			//如果该点到终点的距离小于搜索距离的一半，认为已经搜索到路线，将用终点代替刚刚选取的点，跳出循环
			double distanceToEndPos = Math.sqrt(Math.pow(minCostPoint.getX() - endPos.getX(), 2)
					+ Math.pow(minCostPoint.getY() - endPos.getY(), 2));
			if (distanceToEndPos < 0.5 * searchDistance) {
				endPos.setParentPoint(minCostPoint.getParentPoint());
				endPos.setNum(minCostPoint.getNum());
				closeList.add(endPos);
				closeList.remove(minCostPoint);
				break;
			}
			//如果寻找超过1000步，跳出循环
			if (counter >= 1000) {
				endPos = minCostPoint;
				break;
			}
			//寻找周围可到达点
			for (int i = 0; i < 36; i++) {
				int newX = (int) Math.round(minCostPoint.getX() + searchDistance * Math.cos(searchOrientation * i));
				int newY = (int) Math.round(minCostPoint.getY() + searchDistance * Math.sin(searchOrientation * i));
				if (newX >= 0 && newX < costMap.length && newY >= 0 && newY < costMap.length
						&& costMap[newX][newY] >= 0) {
					Point nextPoint = new Point(newX, newY);
					if (!openList.contains(nextPoint)) {
						//如果开启列表中不包含这个点，就设置父节点，计算F值，加入开启列表
						nextPoint.setParentPoint(minCostPoint);
						nextPoint.setNum(nextPoint.getParentPoint().getNum() + 1);
						double parentOrientation;
						if (minCostPoint.getX() != minCostPoint.getParentPoint().getX()) {
							parentOrientation = Math.atan((minCostPoint.getY() - minCostPoint.getParentPoint().getY())
									/ (minCostPoint.getX() - minCostPoint.getParentPoint().getX()));

						} else if (minCostPoint.getY() > minCostPoint.getParentPoint().getY()) {
							parentOrientation = Math.PI * 0.5;
						} else {
							parentOrientation = Math.PI * 1.5;
						}
						double nonSmoothCost = Math.abs(parentOrientation - searchOrientation * i) / searchOrientation
								* 10;
						nextPoint
								.setF(nextPoint.getParentPoint().getF()
										+ Math.pow(lambda, nextPoint.getNum())
												* costMap[nextPoint.getX()][nextPoint.getY()]
										+ nonSmoothCost + Math.sqrt(Math.pow(nextPoint.getX() - endPos.getX(), 2)
												+ Math.pow(nextPoint.getY() - endPos.getY(), 2)));
						openList.add(nextPoint);
					} else {
						//如果这个点已经存在于开启列表，计算新路线下的F值
						double minCostPointOrientation = Math
								.atan((minCostPoint.getY() - minCostPoint.getParentPoint().getY())
										/ (minCostPoint.getX() - minCostPoint.getParentPoint().getX()));
						double nonSmoothCost = Math.abs(minCostPointOrientation - searchOrientation * i)
								/ searchOrientation * 10;
						double newF = minCostPoint.getF()
								+ Math.pow(lambda, minCostPoint.getNum() + 1) * minCostPoint.getF() + nonSmoothCost
								+ Math.sqrt(Math.pow(nextPoint.getX() - endPos.getX(), 2)
										+ Math.pow(nextPoint.getY() - endPos.getY(), 2));
						//如果新路线先的F值小于原来的F值，就将其F值设置为新F值，父节点设置为上一个F最低的点，否则什么都不做
						if (newF < nextPoint.getF()) {
							nextPoint.setParentPoint(minCostPoint);
							nextPoint.setNum(minCostPoint.getNum() + 1);
							nextPoint.setF(newF);
						}
					}
				}
			}
			counter++;
		}
		//获取路线
		ArrayList<Point> inverseMinCostPath = new ArrayList<>();
		inverseMinCostPath.add(endPos);
		Point lastPos = endPos;
		while (lastPos.getParentPoint() != null) {
			inverseMinCostPath.add(lastPos.getParentPoint());
			lastPos = lastPos.getParentPoint();
		}
		for (int i = inverseMinCostPath.size() - 1; i >= 0; i--) {
			minCostPath.add(inverseMinCostPath.get(i));
		}
	}

	public ArrayList<Point> getMinCostPath() {
		return minCostPath;
	}
}
