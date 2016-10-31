package pomdp.algorithms.beliefcollection;

import java.util.PriorityQueue;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.integral.Beta;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.RandomGenerator;
import pomdp.utilities.distribution.DistributionCalculator;
import pomdp.valuefunction.JigSawValueFunction;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class GapMinCollection extends BeliefCollection {

	protected JigSawValueFunction m_vfUpperBound;
	protected double m_dMaxWidthForIteration;

	Vector<BeliefState> exploredBeliefs;
	Vector<BeliefState> previousBeliefs;
	//parameters
	int defaultNumBeliefPoints = 200;
	//int count;
	
	private String actionMethod = null;
	private RandomGenerator m_rndGenerator;
	
	private DistributionCalculator calculator;
	
	public GapMinCollection(ValueIteration vi, boolean bAllowDuplicates, boolean bUseFIB)
	{	
		super(vi, bAllowDuplicates);	
		POMDP.resetMDPValueFunction();
		POMDP.getMDPValueFunction().persistQValues( true );
		POMDP.getMDPValueFunction().valueIteration( 1000, ExecutionProperties.getEpsilon());

		m_vfUpperBound = new JigSawValueFunction(POMDP, POMDP.getMDPValueFunction(), bUseFIB);
		m_rndGenerator = new RandomGenerator( "GapMinCollection" );
	}

	/* no initial belief */
	public Vector<BeliefState> initialBelief()
	{

		return new Vector<BeliefState>();
	}

	private class PriorityQueueElement implements Comparable<PriorityQueueElement>{

		public BeliefState m_bsBelief;
		public double m_dScore, m_dProb;
		public int m_iDepth;
		public String m_sHistory;

		public PriorityQueueElement(BeliefState bs, double dScore, double dProb, int iDepth, String sHistory){
			m_bsBelief = bs;
			m_dScore = dScore;
			m_dProb = dProb;
			m_iDepth = iDepth;
			m_sHistory = sHistory;
		}

		@Override
		public int compareTo(PriorityQueueElement eOther) {
			if(m_dScore > eOther.m_dScore)
				return 1;
			if(m_dScore < eOther.m_dScore)
				return -1;
			return 0;
		}

	}

	public Vector<BeliefState> expand(int numNewBeliefs, Vector<BeliefState> beliefPoints)
	{
		previousBeliefs = beliefPoints;
		exploredBeliefs = new Vector<BeliefState>();

		PriorityQueue<PriorityQueueElement> pq = new PriorityQueue<PriorityQueueElement>();

		int cUpperBoundPoints = 0;

		BeliefState bsInitial = POMDP.getBeliefStateFactory().getInitialBeliefState();
		double dInitialWidth = width(bsInitial);		
		Logger.getInstance().logln("initial width = " + dInitialWidth);


		int cPreviousBeliefs = 0;
		//do{
			m_dMaxWidthForIteration = 0.0;
			pq.add(new PriorityQueueElement(bsInitial, dInitialWidth, 1, 0, ""));
			explore(pq, epsilon, numNewBeliefs);
			//if(exploredBeliefs.size() == cPreviousBeliefs)
			//	break;
			//cPreviousBeliefs = exploredBeliefs.size();
		//}while( exploredBeliefs.size() < numNewBeliefs);
		

		if( ( m_vfUpperBound.getUpperBoundPointCount() > 1000 ) && ( m_vfUpperBound.getUpperBoundPointCount() > cUpperBoundPoints * 1.1 ) ){
			m_vfUpperBound.pruneUpperBound();
			cUpperBoundPoints = m_vfUpperBound.getUpperBoundPointCount();
		}			

		dInitialWidth = width(bsInitial);

		return exploredBeliefs;		
	}

	public Vector<BeliefState> expand(Vector<BeliefState> beliefPoints)
	{
		return expand(defaultNumBeliefPoints, beliefPoints);
	}


	protected void explore(PriorityQueue<PriorityQueueElement> pq, double dEpsilon, int cMaxBeliefs){
		int cInitialPoints = exploredBeliefs.size();
		double dDiscountFactor = valueIteration.getPOMDP().getDiscountFactor();
		Logger.getInstance().logln("Started exploring for " + cMaxBeliefs + " points.");
		int cChecked = 0, cAdded = 0, cExpanded = 0;
		long lStartTime = System.currentTimeMillis();
		while(!pq.isEmpty() && exploredBeliefs.size() < cMaxBeliefs){
			PriorityQueueElement e = pq.poll();
			BeliefState bsCurrent = e.m_bsBelief;
			Pair<Integer,Double> pBestAction = new Pair<Integer,Double>();
			
			int iBestAction =  getExplorationAction(bsCurrent, pBestAction);//m_vfUpperBound.getAction2(bsCurrent, pBestAction);
			
			double dCurrentUB = m_vfUpperBound.valueAt(bsCurrent);
			double dBestUB = pBestAction.second();
			if(dCurrentUB - dBestUB > dEpsilon){//consider removing this
				m_vfUpperBound.setValueAt(bsCurrent, dBestUB);
			}

			double dBestLB = getLowerBoundUpdateValue(bsCurrent, iBestAction);
			double dCurrentLB = valueIteration.valueAt(bsCurrent);
			if(dBestLB - dCurrentLB > 0){
				if( m_bAllowDuplicates || !exploredBeliefs.contains(bsCurrent)){
					exploredBeliefs.add(bsCurrent);
					cAdded++;
				}
			}
			if(cChecked % 10 == 0)
				Logger.getInstance().log(".");
			
			//do not expand children if this is a terminal belief
			if (POMDP.terminalStatesDefined() && bsCurrent.isDeterministic()){
				int iState = bsCurrent.getDeterministicIndex();
				if(POMDP.isTerminalState(iState))
					continue;
			}

			int iObservation = 0;
			BeliefState bsNext = null;
			double dProb = 0.0;
			double dWidth = 0.0, dScore = 0.0;
			double dCurrentDiscount = Math.pow(dDiscountFactor, e.m_iDepth + 1);
			for( iObservation = 0 ; iObservation < valueIteration.getPOMDP().getObservationCount() ; iObservation++ ){
				dProb = bsCurrent.probabilityOGivenA(iBestAction, iObservation);
				if(dProb > 0)
				{
					cExpanded++;
					bsNext = bsCurrent.nextBeliefState(iBestAction, iObservation);
					dWidth = width(bsNext);
					if(dWidth * dCurrentDiscount > 0.1){
						dScore = dProb * e.m_dProb * dCurrentDiscount * dWidth;
						pq.add(new PriorityQueueElement(bsNext, dScore, dProb * e.m_dProb, e.m_iDepth + 1, e.m_sHistory + ", <" + iBestAction + "," + iObservation + ">"));
					}
				}
			}
			cChecked++;
			long lCurrentTime = System.currentTimeMillis();
			if((lCurrentTime - lStartTime) / 1000 > 50)
				break;
		}
		
		Logger.getInstance().logln("\nDone expansion: " + cAdded + ", " + cChecked + ", " + cExpanded);	
	}

	private double getLowerBoundUpdateValue(BeliefState bs, int iAction) {
		double dSum = 0.0, dProb = 0.0;
		int iObservation = 0;
		BeliefState bsNext = null;
		double dValue = 0.0;
		for( iObservation = 0 ; iObservation < valueIteration.getPOMDP().getObservationCount() ; iObservation++ ){
			dProb = bs.probabilityOGivenA(iAction, iObservation);
			if(dProb > 0)
			{
				bsNext = bs.nextBeliefState(iAction, iObservation);
				dValue = valueIteration.valueAt(bsNext);
				dSum += dProb * dValue;
			}
		}
		dSum = valueIteration.getPOMDP().R(bs,iAction) + dSum * valueIteration.getPOMDP().getDiscountFactor();
		return dSum;
	}


	protected double width( BeliefState bsCurrent ){
		double dUpperValue = 0.0, dLowerValue = 0.0, dWidth = 0.0;

		dUpperValue = m_vfUpperBound.valueAt( bsCurrent );
		dLowerValue = valueIteration.valueAt( bsCurrent );
		dWidth = dUpperValue - dLowerValue;	

		return dWidth;
	}

	public String getActionMethod() {
		return actionMethod;
	}

	public void setActionMethod(String actionMethod) {
		this.actionMethod = actionMethod;
	}

	private int getExplorationAction(BeliefState bsCurrent, Pair<Integer,Double> pBestAction) {
		int iBestAction = 0;
		
		if (actionMethod == null) {
			iBestAction = m_vfUpperBound.getAction2(bsCurrent, pBestAction);
		}
		else {
			iBestAction = getExplorationActionByDistribution(bsCurrent, pBestAction);
		}
		
		return iBestAction;
	}
	
	private int getExplorationActionByDistribution(BeliefState bsCurrent, Pair<Integer,Double> pBestAction) {
		/*均匀分布算法*/
		/*做cMaxIteration次试验，每次试验遍历一次所有的action
		 *对action的上界和下届，根据分布取一个随机值
		 *记录取值最大的action,其count+1
		 *在cMaxIteration次试验之后，取count值最大的action 
		 */
		int cMaxIteraiton = 1000;
		double[] upperBounds = new double[POMDP.getActionCount()];
		double[] lowerBounds = new double[POMDP.getActionCount()];
		
		for (int iAction = 0; iAction<POMDP.getActionCount(); iAction++) {
			upperBounds[iAction] = m_vfUpperBound.getValueByAction(bsCurrent, iAction);
			lowerBounds[iAction] = getLowerBound(bsCurrent, iAction, valueIteration.getValueFunction());
		}

		int count[] = new int[POMDP.getActionCount()];
		for (int i = 0; i<cMaxIteraiton; i++) {
			int maxAction = 0;
			double maxValue = Double.NEGATIVE_INFINITY;
			for (int iAction = 0; iAction<POMDP.getActionCount(); iAction++) {
				// 均匀分布，上下界取随机值
				double upperBound = upperBounds[iAction];
				double lowerBound = lowerBounds[iAction];
				
				//double iValue = 0.0;
				/*if (actionMethod.equalsIgnoreCase("Avg")) {
					iValue = getValueByAverageDistribution(upperBound,lowerBound);
				}
				else if (actionMethod.equalsIgnoreCase("Tri")) {
					iValue = getValueByTriangleDistribution(upperBound, lowerBound);
				}
				else {
					iValue = getValueByBetaDistribution(upperBound, lowerBound);
				}*/
				double iValue = calculator.getValue(upperBound, lowerBound);
				//double iValue = getValueByTriangleDistribution(upperBound, lowerBound);
				//double iValue = getValueByBetaDistribution(upperBound, lowerBound);
				
				if (iValue>maxValue){
					maxValue = iValue;
					maxAction = iAction;	
				}
			}
			
			count[maxAction]++;
		}
		
		int bestAction = 0;
		int maxCount = 0;
		
		for (int iAction = 0; iAction<POMDP.getActionCount(); iAction++) {
			if (count[iAction] > maxCount) {
				bestAction = iAction;
				maxCount = count[iAction];
			}
		}
		
		double valueSum = getActionValueSum(bsCurrent, bestAction);
		pBestAction.setFirst(bestAction);
		pBestAction.setSecond(valueSum);
		
		return bestAction;
	}

	private double getLowerBound(BeliefState bs, int iAction, LinearValueFunctionApproximation vValueFunction) {
		AlphaVector[] aNext = new AlphaVector[POMDP.getObservationCount()];
		return valueIteration.findMaxAlphas(iAction, bs, vValueFunction, aNext);
	}
	
	/**
	 * 根据均匀分布取值
	 * @param upperBound	上界
	 * @param lowerBound	下界
	 * @return
	 */
	/*private double getValueByAverageDistribution(double upperBound, double lowerBound) {
		double width = upperBound - lowerBound;
		return m_rndGenerator.nextDouble(width) + lowerBound;
	}
	
	*//**
	 * 根据三角分布取值
	 * @param upperBound	上界
	 * @param lowerBound	下界
	 * @return
	 *//*
	private double getValueByTriangleDistribution(double upperBound, double lowerBound) {
		double width = upperBound - lowerBound;
		double rand_x = m_rndGenerator.nextDouble();
		
		//三角分布函数为f(x)=2x,F(x)=pow(x,2), E(X)=2/3
//		return Math.pow(rand_x, 2) * width + lowerBound;
		
		//三角分布函数为f(x)=2-2x, F(x) = 2x-pow(x,2) ,E(X)=1/3
		return (2*rand_x - Math.pow(rand_x, 2)) * width + lowerBound;
	}
	
	*//**
	 * 根据beta分布取值，参数a=1,b=3
	 * @param upperBound	上界
	 * @param lowerBound	下界
	 * @return
	 *//*
	private double getValueByBetaDistribution(double upperBound, double lowerBound) {
		Beta b = new Beta();
		double width = upperBound - lowerBound;
		double rand_x = m_rndGenerator.nextDouble();
		return b.calculateBeta(1, 3, 0, rand_x)*width + lowerBound;
	}*/
	
	private double getActionValueSum(BeliefState bs,int iAction) {
		double dValueSum = POMDP.immediateReward(bs, iAction);
		for (int iObservation = 0; iObservation < POMDP.getObservationCount(); iObservation++) {
			BeliefState bsSuccessor = bs.nextBeliefState( iAction, iObservation );
			double dPr = bs.probabilityOGivenA( iAction, iObservation );
			if( dPr > 0.0 ){
				double value = m_vfUpperBound.valueAt( bsSuccessor );
				dValueSum += POMDP.getDiscountFactor() * dPr * value;
			}
		}
		return dValueSum;
	}
	
	public String toString() {
		String name = super.toString();
		if (actionMethod != null) {
			name += ("_"+actionMethod);
		}
		return name;
	}

	public DistributionCalculator getCalculator() {
		return calculator;
	}

	public void setCalculator(DistributionCalculator calculator) {
		this.calculator = calculator;
	}
}
