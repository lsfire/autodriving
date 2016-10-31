package speedPlanning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import model.PedestrianModel;
import model.VehicleModel;

public class PomdpModelBuilder {
	private ArrayList<PedestrianModel> pedestrianList;
	private String pomdpModelFileName;
	private VehicleModel vehicle;
	int amountOfStates;

	public PomdpModelBuilder(ArrayList<PedestrianModel> pedestrianList, VehicleModel vehicle) {
		this.pedestrianList = pedestrianList;
		this.vehicle = vehicle;
		pomdpModelFileName = "autonomous_driving.POMDP";
		amountOfStates = 1;
	}

	//建立POMDP模型文件
	public void buildPomdpModelFile() {
		// System.out.println("Yeah!success!");
		try {
			FileOperation.createFile(pomdpModelFileName);
			// System.out.println("success!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//写入折扣因子
	public void writeDiscount(float discount) {
		FileOperation.appendFileContent(pomdpModelFileName, "discount: " + discount + "\n");
	}

	//写入值函数计算方式（reward or cost）
	public void writeValues(String values) {
		FileOperation.appendFileContent(pomdpModelFileName, "values: " + values + "\n");
	}

	//写入状态个数
	public void writeStates() {
		for (int i = 0; i < pedestrianList.size(); i++) {
			amountOfStates *= pedestrianList.get(i).getPedestrianGoalList().size();
		}
		FileOperation.appendFileContent(pomdpModelFileName, "states: " + amountOfStates + "\n");
	}

	//写入动作个数
	public void writeActions() {
		FileOperation.appendFileContent(pomdpModelFileName, "actions: 3\n");
	}

	//写入观察个数
	public void writeObservations() {
		FileOperation.appendFileContent(pomdpModelFileName, "observations: " + amountOfStates + "\n");
	}

	//写入初始信念状态
	public void writeStartingBeliefState() {
		FileOperation.appendFileContent(pomdpModelFileName, "start: ");
		double[] startingBeliefState = calStartBeliefState(pedestrianList);
		for (int i = 0; i < startingBeliefState.length; i++) {
			FileOperation.appendFileContent(pomdpModelFileName, String.format("%.6f", startingBeliefState[i]));
			if (i != startingBeliefState.length - 1) {
				FileOperation.appendFileContent(pomdpModelFileName, " ");
			} else {
				FileOperation.appendFileContent(pomdpModelFileName, "\n");
			}
		}
	}

	//计算初始信念状态
	public double[] calStartBeliefState(ArrayList<PedestrianModel> pedestrianModelList) {
		int counter = 1;
		for (int i = 0; i < pedestrianModelList.size(); i++) {
			counter *= pedestrianModelList.get(i).getPedestrianGoalList().size();
		}
		double[] beliefState = new double[counter];
		double[] lastLayerBeliefState;
		//如果List里面不止一个行人
		if (pedestrianModelList.size() > 1) {
			//去掉第一个行人，其余组成新List
			ArrayList<PedestrianModel> newPedestrianModelList = new ArrayList<>();
			for (int i = 1; i < pedestrianModelList.size(); i++) {
				newPedestrianModelList.add(pedestrianModelList.get(i));
			}
			//递归
			lastLayerBeliefState = calStartBeliefState(newPedestrianModelList);
			//第一个行人的belief数组和递归得到的belief数组两两相乘
			for (int i = 0; i < pedestrianModelList.get(0).getPedestrianGoalList().size(); i++) {
				for (int j = 0; j < lastLayerBeliefState.length; j++) {
					beliefState[i * lastLayerBeliefState.length + j] = pedestrianModelList.get(0)
							.getPedestrianGoalList().get(i).getBelief() * lastLayerBeliefState[j];
				}
			}
			//归一
			double tempSum = 0.0;
			for (int i = 0; i < beliefState.length; i++) {
				tempSum += beliefState[i];
			}
			for (int i = 0; i < beliefState.length; i++) {
				beliefState[i] /= tempSum;
			}
			return beliefState;
		} else {
			//如果行人只有一个，返回行人各个目标点belief组成的数组
			lastLayerBeliefState = new double[pedestrianModelList.get(0).getPedestrianGoalList().size()];
			for (int i = 0; i < lastLayerBeliefState.length; i++) {
				lastLayerBeliefState[i] = pedestrianModelList.get(0).getPedestrianGoalList().get(i).getBelief();
			}
			return lastLayerBeliefState;
		}

	}

	//计算编号为situationNumber的状态是由每个行人的第几个目标点组成的
	public int[] calSituationOfState(int situationNumber) {
		int[] situationOfState = new int[pedestrianList.size()];
		int pedestrianNumber = 0;
		int tempProduct = 1;
		for (int i = pedestrianNumber + 1; i < pedestrianList.size(); i++) {
			tempProduct *= pedestrianList.get(i).getPedestrianGoalList().size();
		}
		situationOfState[0] = (int) Math.floor(situationNumber / tempProduct);
		pedestrianNumber++;
		while (pedestrianNumber < pedestrianList.size()) {
			int alreadyStateNumber = 0;
			for (int i = 0; i < pedestrianNumber; i++) {
				int temp = 1;
				for (int j = i + 1; j < pedestrianList.size(); j++) {
					temp *= pedestrianList.get(j).getPedestrianGoalList().size();
				}
				alreadyStateNumber += situationOfState[i] * temp;
			}
			int restStateNumber = situationNumber - alreadyStateNumber;
			tempProduct = 1;
			for (int i = pedestrianNumber + 1; i < pedestrianList.size(); i++) {
				tempProduct *= pedestrianList.get(i).getPedestrianGoalList().size();
			}
			situationOfState[pedestrianNumber] = (int) Math.floor(restStateNumber / tempProduct);

			pedestrianNumber++;
		}
		return situationOfState;
	}

	//写入状态转移函数
	public void writeStateTransitionProbabilities() {
		for (int i = 0; i < 3; i++) {
			double[][] probability = new double[amountOfStates][amountOfStates];
			for (int j = 0; j < amountOfStates; j++) {
				for (int k = 0; k < amountOfStates; k++) {
					probability[j][k] = 1.0;
				}
			}
			for (int j = 0; j < amountOfStates; j++) {
				for (int k = 0; k < amountOfStates; k++) {
					int[] situationOfStateJ = calSituationOfState(j);
					int[] situationOfStateK = calSituationOfState(k);
					for (int l = 0; l < pedestrianList.size(); l++) {
						float[][] stateTransitionProbabilities = calculateStateTransitionProbabilities(l, i);
						probability[j][k] *= stateTransitionProbabilities[situationOfStateJ[l]][situationOfStateK[l]];
					}
				}
			}
			for (int j = 0; j < amountOfStates; j++) {
				double[] tempProbability = probability[j];
				double tempSum = 0.0;
				for (int k = 0; k < tempProbability.length; k++) {
					tempSum += tempProbability[k];
				}
				for (int k = 0; k < tempProbability.length; k++) {
					probability[j][k] /= tempSum;
				}
			}
			for (int j = 0; j < amountOfStates; j++) {
				for (int k = 0; k < amountOfStates; k++) {
					FileOperation.appendFileContent(pomdpModelFileName,
							"T: " + i + " : " + j + " : " + k + " " + String.format("%.6f", probability[j][k]) + "\n");
				}
			}
		}
	}

	//计算执行动作actionNumber的时候，编号pedestrianNumber的行人内部各个目标点相互转化的概率
	public float[][] calculateStateTransitionProbabilities(int pedestrianNumber, int actionNumber) {
		float[][] probabilities = new float[pedestrianList.get(pedestrianNumber).getPedestrianGoalList()
				.size()][pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size()];
		double[][] tempProbabilities = new double[pedestrianList.get(pedestrianNumber).getPedestrianGoalList()
				.size()][pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size()];
		double[] vehicleVector = new double[2];
		vehicleVector[0] = 1.0;
		vehicleVector[1] = Math.tan(vehicle.getVehicleOrientation());
		switch (actionNumber) {
		case 0:
			for (int i = 0; i < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); i++) {
				for (int j = 0; j < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); j++) {
					tempProbabilities[i][j] = pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i)
							.getBelief()
							* pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getBelief();
				}
			}
			break;

		case 1:
			for (int i = 0; i < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); i++) {
				for (int j = 0; j < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); j++) {
					double[] goalIVehicleVector = new double[2];
					goalIVehicleVector[0] = vehicle.getVehiclePos().get(0)
							- pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i).getGoalPos().get(0);
					goalIVehicleVector[1] = vehicle.getVehiclePos().get(1)
							- pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i).getGoalPos().get(1);
					double[] goalJVehicleVector = new double[2];
					goalJVehicleVector[0] = vehicle.getVehiclePos().get(0)
							- pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getGoalPos().get(0);
					goalJVehicleVector[1] = vehicle.getVehiclePos().get(1)
							- pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getGoalPos().get(1);
					double productI = vehicleVector[0] * goalIVehicleVector[0]
							+ vehicleVector[1] * goalIVehicleVector[1];
					double productJ = vehicleVector[0] * goalJVehicleVector[0]
							+ vehicleVector[1] * goalJVehicleVector[1];
					double distanceI = Math.sqrt(Math
							.pow(vehicle.getVehiclePos().get(0) - pedestrianList.get(pedestrianNumber)
									.getPedestrianGoalList().get(i).getGoalPos().get(0), 2)
							+ Math.pow(vehicle.getVehiclePos().get(1) - pedestrianList.get(pedestrianNumber)
									.getPedestrianGoalList().get(i).getGoalPos().get(1), 2));
					double distanceJ = Math.sqrt(Math
							.pow(vehicle.getVehiclePos().get(0) - pedestrianList.get(pedestrianNumber)
									.getPedestrianGoalList().get(j).getGoalPos().get(0), 2)
							+ Math.pow(vehicle.getVehiclePos().get(1) - pedestrianList.get(pedestrianNumber)
									.getPedestrianGoalList().get(j).getGoalPos().get(1), 2));
					if (productI < 0 && productJ < 0) {
						tempProbabilities[i][j] = pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i)
								.getBelief()
								* pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getBelief()
								/ distanceI * distanceJ;
					} else if (productI >= 0 && productJ >= 0) {
						tempProbabilities[i][j] = pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i)
								.getBelief()
								* pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getBelief();
					} else if (productI < 0 && productJ >= 0) {
						tempProbabilities[i][j] = pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i)
								.getBelief()
								* pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getBelief()
								* (distanceJ / distanceI + 1);
					} else {
						tempProbabilities[i][j] = pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i)
								.getBelief()
								* pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getBelief()
								/ (distanceJ / distanceI + 1);
					}
				}
			}
			break;

		case 2:
			for (int i = 0; i < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); i++) {
				for (int j = 0; j < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); j++) {
					double[] goalIVehicleVector = new double[2];
					goalIVehicleVector[0] = vehicle.getVehiclePos().get(0)
							- pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i).getGoalPos().get(0);
					goalIVehicleVector[1] = vehicle.getVehiclePos().get(1)
							- pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i).getGoalPos().get(1);
					double[] goalJVehicleVector = new double[2];
					goalJVehicleVector[0] = vehicle.getVehiclePos().get(0)
							- pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getGoalPos().get(0);
					goalJVehicleVector[1] = vehicle.getVehiclePos().get(1)
							- pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getGoalPos().get(1);
					double productI = vehicleVector[0] * goalIVehicleVector[0]
							+ vehicleVector[1] * goalIVehicleVector[1];
					double productJ = vehicleVector[0] * goalJVehicleVector[0]
							+ vehicleVector[1] * goalJVehicleVector[1];
					double distanceI = Math.sqrt(Math
							.pow(vehicle.getVehiclePos().get(0) - pedestrianList.get(pedestrianNumber)
									.getPedestrianGoalList().get(i).getGoalPos().get(0), 2)
							+ Math.pow(vehicle.getVehiclePos().get(1) - pedestrianList.get(pedestrianNumber)
									.getPedestrianGoalList().get(i).getGoalPos().get(1), 2));
					double distanceJ = Math.sqrt(Math
							.pow(vehicle.getVehiclePos().get(0) - pedestrianList.get(pedestrianNumber)
									.getPedestrianGoalList().get(j).getGoalPos().get(0), 2)
							+ Math.pow(vehicle.getVehiclePos().get(1) - pedestrianList.get(pedestrianNumber)
									.getPedestrianGoalList().get(j).getGoalPos().get(1), 2));
					if (productI < 0 && productJ < 0) {
						tempProbabilities[i][j] = pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i)
								.getBelief()
								* pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getBelief()
								* Math.sqrt(distanceJ / distanceI);
					} else if (productI >= 0 && productJ >= 0) {
						tempProbabilities[i][j] = pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i)
								.getBelief()
								* pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getBelief();
					} else if (productI < 0 && productJ >= 0) {
						tempProbabilities[i][j] = pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i)
								.getBelief()
								* pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getBelief()
								* Math.sqrt(distanceJ / distanceI + 1);
					} else {
						tempProbabilities[i][j] = pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(i)
								.getBelief()
								* pedestrianList.get(pedestrianNumber).getPedestrianGoalList().get(j).getBelief()
								/ Math.sqrt(distanceJ / distanceI + 1);
					}
				}
			}
			break;

		default:
			break;
		}
		double sum = 0.0;
		for (int i = 0; i < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); i++) {
			for (int j = 0; j < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); j++) {
				sum += tempProbabilities[i][j];
			}
		}
		for (int i = 0; i < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); i++) {
			for (int j = 0; j < pedestrianList.get(pedestrianNumber).getPedestrianGoalList().size(); j++) {
				probabilities[i][j] = (float) (tempProbabilities[i][j] / sum);
			}
		}
		return probabilities;
	}

	//写入观察函数，这里需要按照论文及其补充说明修改
	public void writeObservationProbabilities() {
		for (int i = 0; i < amountOfStates; i++) {
			FileOperation.appendFileContent(pomdpModelFileName, "O: * : " + i + "\n");
			for (int j = 0; j < amountOfStates; j++) {
				if (j == i) {
					FileOperation.appendFileContent(pomdpModelFileName, String.format("%.6f", 0.500005));
				} else {
					FileOperation.appendFileContent(pomdpModelFileName,
							String.format("%.6f", 0.5 / (amountOfStates - 1)));
				}
				if (j == amountOfStates - 1) {
					FileOperation.appendFileContent(pomdpModelFileName, "\n");
				} else {
					FileOperation.appendFileContent(pomdpModelFileName, " ");
				}
			}
		}
	}

	//写入回报函数
	public void writeReward() {
		for (int i = 0; i < 3; i++) {
			double reward = 0.0;
			FileOperation.appendFileContent(pomdpModelFileName, "R: " + i + " : * : * : * ");
			for (int k = 0; k < pedestrianList.size(); k++) {
				reward -= 1 / (Math
						.sqrt(Math.pow(vehicle.getVehiclePos().get(0) - pedestrianList.get(k).getPedestrianPos().get(0),
								2)
						+ Math.pow(vehicle.getVehiclePos().get(1) - pedestrianList.get(k).getPedestrianPos().get(1), 2))
						/ Math.sqrt(2 * 4 * 4));
			}
			reward += 1 / (Math.sqrt(Math.pow(vehicle.getVehiclePos().get(0) - vehicle.getVehicleGoal().get(0), 2)
					+ Math.pow(vehicle.getVehiclePos().get(1) - vehicle.getVehicleGoal().get(1), 2)));
			reward += (vehicle.getVehicleSpeed() - vehicle.getMaxSpeed()) / vehicle.getMaxSpeed();
			switch (i) {
			case 1:
				reward -= 0.1;
				break;

			case 2:
				reward -= 0.1;
				break;

			default:
				break;
			}

			FileOperation.appendFileContent(pomdpModelFileName, String.format("%.6f", reward) + "\n");
		}
	}
}
