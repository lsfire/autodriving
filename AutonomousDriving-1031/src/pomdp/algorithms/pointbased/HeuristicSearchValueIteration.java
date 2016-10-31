package pomdp.algorithms.pointbased;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.integral.Beta;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.datastructures.LinkedList;
import pomdp.utilities.distribution.DistributionCalculator;
import pomdp.valuefunction.JigSawValueFunction;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import pomdp.valuefunction.MDPValueFunction;

public class HeuristicSearchValueIteration extends ValueIteration {
	protected JigSawValueFunction m_vfUpperBound;
	protected int m_cApplyHComputations;
	protected int m_cNewPointComputations;
	protected int m_cVisitedBeliefStates;
	protected double m_dMaxWidthForIteration;
	private static double m_dExplorationFactor;
	
	private String algorithmName;
	private DistributionCalculator calculator;
	
	public HeuristicSearchValueIteration( POMDP pomdp, double dExplorationFactor, boolean bUseFIB  ){
		super( pomdp );
		
		
		if( !m_vfMDP.persistQValues() ){
			m_vfMDP = new MDPValueFunction( pomdp, 0.0 );
			m_vfMDP.persistQValues( true );
			m_vfMDP.valueIteration( 100, m_dEpsilon );
		}
		m_vfUpperBound = new JigSawValueFunction( pomdp, m_vfMDP, bUseFIB );
		m_cNewPointComputations = 0;
		m_cApplyHComputations = 0;
		m_cVisitedBeliefStates = 0;
		m_dMaxWidthForIteration = 0.0;
		m_dExplorationFactor = dExplorationFactor;
	}
	public HeuristicSearchValueIteration( POMDP pomdp, boolean bUseFIB ){
		this( pomdp, 0.0, bUseFIB );
	}
	
	public void setAlgorithmName(String algorithmName) {
		this.algorithmName = algorithmName;
	}
	
	public String getAlgorithmName() {
		return this.algorithmName;
	}
	
	protected void applyH( BeliefState bs ){
		long lTimeBefore = 0, lTimeAfter = 0;
		
		if( ExecutionProperties.getReportOperationTime() )
			lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
	
		m_vfUpperBound.updateValue( bs );
		
		if( ExecutionProperties.getReportOperationTime() ){
			lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			
			m_cTimeInHV += ( lTimeAfter - lTimeBefore ) / 1000000;
		}
	}

	public int getAction( BeliefState bsCurrent ){
		AlphaVector avMaxAlpha = m_vValueFunction.getMaxAlpha( bsCurrent );
		return avMaxAlpha.getAction();
	}
	
	protected String toString( double[][] adArray ){
		int i = 0, j = 0;
		String sRes = "";
		for( i = 0 ; i < adArray.length ; i++ ){
			for( j = 0 ; j < adArray[i].length ; j++ ){
				sRes += adArray[i][j] + " ";
			}
			sRes += "\n";
		}
		return sRes;
	}

	protected double excess( BeliefState bsCurrent, double dEpsilon, double dDiscount ){
		return width( bsCurrent ) - ( dEpsilon / dDiscount );
	}
	
	protected double width( BeliefState bsCurrent ){
		double dUpperValue = 0.0, dLowerValue = 0.0, dWidth = 0.0;
		dUpperValue = m_vfUpperBound.valueAt( bsCurrent );
		dLowerValue = valueAt( bsCurrent );
		dWidth = dUpperValue - dLowerValue;	
		
		return dWidth;
	}
		
	public String getName(){
		if (algorithmName == null) {
			return "HSVI";
		}
		else {
			return "HSVI"+"_"+algorithmName;
		}
	}
	
	public void valueIteration(int cIterations, double dEpsilon, double dTargetValue, int maxRunningTime, int numEvaluations)
	{
		// 初始化各种变量
		// 初始信念点
		BeliefState bsInitial = m_pPOMDP.getBeliefStateFactory().getInitialBeliefState();
		// 初始信念点宽度
		double dInitialWidth = width( bsInitial );
		
		// 遍历计数器 / 最大explore深度
		int iIteration = 0, iMaxDepth = 0;
		
		// 时间变量
		long lStartTime = System.currentTimeMillis(), lCurrentTime = 0;
		
		// 运行环境
		Runtime rtRuntime = Runtime.getRuntime();
		
		// 控制循环终止的变量
		boolean bDone = false;
		
		// 记录迭代中计算的ADR
		Pair<Double, Double> pComputedADRs = new Pair<Double, Double>();
		
		//观测到的信念点
		Vector<BeliefState> vObservedBeliefStates = new Vector<BeliefState>();
		int cUpperBoundPoints = 0, cNoChange = 0;
		String sMsg = "";
		
		m_cElapsedExecutionTime = 0;
		m_cCPUExecutionTime = 0;
		
		//计算时间
		long lCPUTimeBefore = 0, lCPUTimeAfter = 0, lCPUTimeTotal = 0;
		
		int cValueFunctionChanges = 0;
		
		//最大执行时间，默认设置为10分钟
		long maxExecutionTime = m_maxExecutionTime;//1000*60*10;
		
		Logger.getInstance().logln( "Begin " + getName() + ", Initial width = " + dInitialWidth );
		
		// 循环主体
		// 结束条件：循环超出指定次数 || 达到终止条件（超出时间）
		for( iIteration = 0 ; ( iIteration < cIterations ) && !bDone && !m_bTerminate ; iIteration++ ){
			// 获取迭代开始的时间戳
			lStartTime = System.currentTimeMillis();
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			
			// 记录迭代的最大宽度
			m_dMaxWidthForIteration = 0.0;
			
			// explore的主体过程;返回explore的最大深度
			iMaxDepth = explore( bsInitial, dEpsilon, 0, 1.0, vObservedBeliefStates );
			
			// 若上界函数的点的个数超过1000 && 两次迭代之间上界函数的点的个数增长超过10%; 对上界函数进行裁剪。
			if( ( m_vfUpperBound.getUpperBoundPointCount() > 1000 ) && ( m_vfUpperBound.getUpperBoundPointCount() > cUpperBoundPoints * 1.1 ) ){
				// 裁剪过程：去掉上界函数中，函数值和初始值的差小于epsilon的点
				m_vfUpperBound.pruneUpperBound();
				cUpperBoundPoints = m_vfUpperBound.getUpperBoundPointCount();
			}
			
			// 累计explore深度
			m_cVisitedBeliefStates += iMaxDepth;
			
			// 重新计算b0点的width
			dInitialWidth = width( bsInitial );			
			
			//计算时间
			lCurrentTime = System.currentTimeMillis();
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			m_cElapsedExecutionTime += ( lCurrentTime - lStartTime );
			m_cCPUExecutionTime += ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000;
			lCPUTimeTotal += lCPUTimeAfter - lCPUTimeBefore;
			
			//如果maxExecutionTime > 0 而且总时间超过maxExecutionTime;终止迭代
			if (maxExecutionTime > 0) {
				bDone = (m_cElapsedExecutionTime > maxExecutionTime);
			}
			
			// 每5次且下界函数发生变化时，计算ADR
			if( ( iIteration >= 5 ) && ( ( lCPUTimeTotal  / 1000000000 ) >= 0 ) && ( iIteration % 5 == 0 ) && m_vValueFunction.getChangesCount() > cValueFunctionChanges ){
				//原始算法;计算ADR的值是否大于给定值，如果是则算是收敛			
				//bDone = checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs );
				//不把这个条件作为终止迭代的条件
				checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs );
				
				// 记录本次下界函数的变化值
				cValueFunctionChanges = m_vValueFunction.getChangesCount();
				
				//显式调用 垃圾回收
				rtRuntime.gc();
				
				//输出消息
				sMsg = getName() + ": Iteration " + iIteration + 
									" initial width " + round( dInitialWidth, 3 ) +
									" V(b) " + round( m_vValueFunction.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 4 ) +
									" V^(b) " + round( m_vfUpperBound.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 4 ) +
									//" max width bs = " + bsMaxWidth + 
									//" max width " + round( width( bsMaxWidth ), 3 ) +
									" max depth " + iMaxDepth +
									" max width " + round( m_dMaxWidthForIteration, 3 ) +
									//" simulated ADR " + ((Number) pComputedADRs.first()).doubleValue() +
									//" filtered ADR " + round( ((Number) pComputedADRs.second()).doubleValue(), 3 ) +
									" Time " + ( lCurrentTime - lStartTime ) / 1000 +
									" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
									" CPU total " + lCPUTimeTotal  / 1000000000 +
									" |V| " + m_vValueFunction.size() +
									" |V^| " + m_vfUpperBound.getUpperBoundPointCount() +
									" V changes " + m_vValueFunction.getChangesCount() +
									" #ObservedBS = " + vObservedBeliefStates.size() +
									" #BS " + m_pPOMDP.getBeliefStateFactory().getBeliefUpdatesCount() +
									" #backups " + m_cBackups +
									" #V^(b) = " + m_cNewPointComputations +
									" max depth " + iMaxDepth +
									" free memory " + rtRuntime.freeMemory() / 1000000 +
									" total memory " + rtRuntime.totalMemory() / 1000000 +
									" max memory " + rtRuntime.maxMemory() / 1000000;
			}
			else{
				//输出消息
				sMsg = getName() + ": Iteration " + iIteration + 
						" initial width " + round( dInitialWidth, 3 ) +
						" V(b) " + round( m_vValueFunction.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 4 ) +
						" V^(b) " + round( m_vfUpperBound.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 4 ) +
						" max depth " + iMaxDepth +
						" max width " + round( m_dMaxWidthForIteration, 3 ) +
						" Time " + ( lCurrentTime - lStartTime ) / 1000 +
						" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
						" CPU total " + lCPUTimeTotal  / 1000000000 +
						" |V| " + m_vValueFunction.size() +
						" |V^| " + m_vfUpperBound.getUpperBoundPointCount() +
						" #ObservedBS = " + vObservedBeliefStates.size() +
						" #BS " + m_pPOMDP.getBeliefStateFactory().getBeliefUpdatesCount() +
						" #backups " + m_cBackups +
						" #V^(b) = " + m_cNewPointComputations +
						" #HV(B) = " + m_cApplyHComputations +
						" free memory " + rtRuntime.freeMemory() / 1000000 +
						" total memory " + rtRuntime.totalMemory() / 1000000 +
						" max memory " + rtRuntime.maxMemory() / 1000000 + 
						"\t";
			}
			Logger.getInstance().log( getName(), 0, "VI", sMsg );
			Logger.getInstance().logln();
			
			//记录下界函数不发生变化的次数
			if( m_vValueFunction.getChangesCount() == cValueFunctionChanges ){
				cNoChange++;
			}
			//发生变化则置零
			else
				cNoChange = 0;
		}
		
		m_cElapsedExecutionTime /= 1000;
		m_cCPUExecutionTime /= 1000;
		
		//输出完成的信息
		sMsg = "Finished " + getName() + " - time : " + m_cElapsedExecutionTime +
				" |V| = " + m_vValueFunction.size() + 
				" backups = " + m_cBackups + 
				" GComputations = " + AlphaVector.getGComputationsCount() +
				" #V^(b) = " + m_cNewPointComputations +
				" Dot products = " + AlphaVector.dotProductCount();
		Logger.getInstance().log( "HSVI", 0, "VI", sMsg );
		
		if( ExecutionProperties.getReportOperationTime() ){
			sMsg = "Avg time: backup " + ( m_cTimeInBackup / ( m_cBackups * 1.0 ) ) + 
					" G " + AlphaVector.getAvgGTime() +
					" Tau " + m_pPOMDP.getBeliefStateFactory().getAvgTauTime() + 
					" DP " + AlphaVector.getAvgDotProductTime() +
					" V^(b) " + ( m_cTimeInV / ( m_cNewPointComputations * 1.0 ) / 1000000 ) + 
					" HV(b) " + ( m_cTimeInHV / ( m_cApplyHComputations * 1.0 ) );
			Logger.getInstance().log( "HSVI", 0, "VI", sMsg );
		}
	}

	// 更新bs点的上下界
	protected void updateBounds( BeliefState bsCurrent ){		
		AlphaVector avNext = backup( bsCurrent );
		AlphaVector avCurrent = m_vValueFunction.getMaxAlpha( bsCurrent );
		double dCurrentValue = valueAt( bsCurrent );
		double dNewValue = avNext.dotProduct( bsCurrent );
		if( dNewValue > dCurrentValue ){
			m_vValueFunction.addPrunePointwiseDominated( avNext );
		}
		applyH( bsCurrent );
	}

	
	// explore获取下一个信念点
	protected BeliefState getNextBeliefState( BeliefState bsCurrent, double dEpsilon, double dDiscount ){
		//获取最优的action
		int iAction = getExplorationAction( bsCurrent );
		
		//根据action获取最优的观测
		int iObservation = getExplorationObservation( bsCurrent, iAction, dEpsilon, dDiscount );
		
		if( iObservation == -1 ){
			return null;
		}
		
		return bsCurrent.nextBeliefState( iAction, iObservation );		
	}
	
	//explore过程
	protected int explore( BeliefState bsCurrent, double dEpsilon, int iTime, double dDiscount, Vector<BeliefState> vObservedBeliefStates ){
		//初始化各种变量
		double dWidth = width( bsCurrent );
		int iAction = 0, iObservation = 0;
		BeliefState bsNext = null;
		int iMaxDepth = 0;

		if( m_bTerminate )
			return iTime;
		
		// 如果bs点之前没有explore过，添加到ObservedBeliefStates点集合。
		if( !vObservedBeliefStates.contains( bsCurrent ) )
			vObservedBeliefStates.add( bsCurrent );
		
		// 记录最大width
		if( dWidth > m_dMaxWidthForIteration )
			m_dMaxWidthForIteration = dWidth;
		
		// 深度大于200或者宽度小于阈值（epsilon/pow(gama,t))
		if( iTime > 200 || dWidth < ( dEpsilon / dDiscount ) )
			return iTime;
		
		// 获得下一个信念点
		bsNext = getNextBeliefState( bsCurrent, dEpsilon, dDiscount * m_dGamma );

		// 下一信念点不为空 且 不等于当前点；递归进行explore过程
		if( ( bsNext != null ) && ( bsNext != bsCurrent ) ){
			iMaxDepth = explore( bsNext, dEpsilon, iTime + 1, dDiscount * m_dGamma, vObservedBeliefStates );
		}
		else{
			iMaxDepth = iTime;
		}
		
		// 更新当前点的上下界
		updateBounds( bsCurrent );	
		
		
		// 根据随机概率当前点继续explore
		// 默认参数条件下不触发
		if( m_dExplorationFactor > 0.0 ){
			int iActionAfterUpdate = getExplorationAction( bsCurrent );
			if( iActionAfterUpdate != iAction ){
				if( m_rndGenerator.nextDouble() < m_dExplorationFactor ){
					iObservation = getExplorationObservation( bsCurrent, iActionAfterUpdate, dEpsilon, dDiscount );
					bsNext = bsCurrent.nextBeliefState( iAction, iObservation );
					if( bsNext != null ){
						iMaxDepth = explore( bsNext, dEpsilon, iTime + 1, dDiscount * m_dGamma, vObservedBeliefStates );
						updateBounds( bsCurrent );	
					}
				}					
			}
		}
					
		return iMaxDepth;
	}

	// 获取最优的观测
	protected int getExplorationObservation( BeliefState bsCurrent, int iAction, 
			double dEpsilon, double dDiscount ){
		int iObservation = 0, iMaxObservation = -1;
		double dProb = 0.0, dExcess = 0.0, dValue = 0.0, dMaxValue = 0.0;
		BeliefState bsNext = null;
		
		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dProb = bsCurrent.probabilityOGivenA( iAction, iObservation );
			if( dProb > 0 ){
				bsNext = bsCurrent.nextBeliefState( iAction, iObservation );
				dExcess = excess( bsNext, dEpsilon, dDiscount );
				dValue = dProb * dExcess;  
				if( dValue > dMaxValue ){
					dMaxValue = dValue;
					iMaxObservation = iObservation;
				}
			}
		}
		return iMaxObservation;
	}

	protected int getExplorationAction( BeliefState bsCurrent ){
		/*原始HSVI算法，根据上界取action*/
//		if (algorithmName == null) {
//			return m_vfUpperBound.getAction( bsCurrent );
//		}
//		
//		/*根据分布取action*/
//		else {
//			return getActionByDistribution(bsCurrent);
//		}
//		long s1 = System.currentTimeMillis();
//		m_vfUpperBound.getAction( bsCurrent );
//		long e1 = System.currentTimeMillis();
//		long t1 = e1-s1;
//		
//		long s2 = System.currentTimeMillis();
//		getActionByDistribution(bsCurrent);
//		long e2 = System.currentTimeMillis();
//		long t2 = e2-s2;
//		
//		System.out.println("dis:"+t2+"\tori:"+t1+"\tdis-ori:"+(t2-t1));
		
		if (algorithmName == null) {
			return m_vfUpperBound.getAction( bsCurrent );
		}
		
		/*根据分布取action*/
		else {
			return getActionByDistribution(bsCurrent);
		}
		
		
	}
	
	private int getActionByDistribution ( BeliefState bs ) {
		/*均匀分布算法*/
		/*做cMaxIteration次试验，每次试验遍历一次所有的action
		 *对action的上界和下届，根据分布取一个随机值
		 *记录取值最大的action,其count+1
		 *在cMaxIteration次试验之后，取count值最大的action 
		 */
		int cMaxIteraiton = 1000;
		double[] upperBounds = new double[m_pPOMDP.getActionCount()];
		double[] lowerBounds = new double[m_pPOMDP.getActionCount()];
		
		for (int iAction = 0; iAction<m_pPOMDP.getActionCount(); iAction++) {
			upperBounds[iAction] = m_vfUpperBound.getValueByAction(bs, iAction);
//			System.out.println("up:"+upperBounds[iAction]);
			//lowerBounds[iAction] = G(iAction, bs, m_vValueFunction).dotProduct(bs);
			lowerBounds[iAction] = getLowerBound(bs, iAction, m_vValueFunction);
//			System.out.println("low:"+lowerBounds[iAction]);
		}
		
		int count[] = new int[m_pPOMDP.getActionCount()];
		for (int i = 0; i<cMaxIteraiton; i++) {
			int maxAction = 0;
			double maxValue = Double.NEGATIVE_INFINITY;
			for (int iAction = 0; iAction<m_pPOMDP.getActionCount(); iAction++) {
				// 均匀分布，上下界取随机值
				double upperBound = upperBounds[iAction];
				double lowerBound = lowerBounds[iAction];
				
				//double iValue = 0.0;
				double iValue = calculator.getValue(upperBound, lowerBound);
				/*if (algorithmName.equalsIgnoreCase("Avg")) {
					iValue = getValueByAverageDistribution(upperBound,lowerBound);
				}
				else if (algorithmName.equalsIgnoreCase("Tri")) {
					iValue = getValueByTriangleDistribution(upperBound, lowerBound);
				}
				else {
					iValue = getValueByBetaDistribution(upperBound, lowerBound);
				}*/
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
		
		for (int iAction = 0; iAction<m_pPOMDP.getActionCount(); iAction++) {
			if (count[iAction] > maxCount) {
				bestAction = iAction;
				maxCount = count[iAction];
			}
		}
		
		return bestAction;
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
	 * 根据ｂｅｔａ分布取值，参数a=1,b=3
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
	
	//获取当前信念点的下界值。
	//代码来自backup过程
	private double getLowerBound(BeliefState bs, int iAction, LinearValueFunctionApproximation vValueFunction) {
		// 初始化各种值。
		/*AlphaVector avMax = null, avG = null, avSum = null;
		List<AlphaVector> vVectors = new LinkedList<AlphaVector>(vValueFunction.getVectors());
		double dMaxValue = MIN_INF, dValue = 0, dProb = 0.0;
		
		// 遍历所有的观测，
		for(int iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dProb = bs.probabilityOGivenA( iAction, iObservation );
			if( dProb > 0.0 ){
				//dMaxValue = MIN_INF;
				//argmax_i g^i_a,o \cdot b
				//计算每个向量的回报值，取回报最大的向量的点乘结果。
				// 即下界
				for( AlphaVector avAlpha : vVectors ){
					if( avAlpha != null ){
						avG = avAlpha.G( iAction, iObservation );
						
						dValue = avG.dotProduct( bs );
						if( ( avMax == null ) || ( dValue >= dMaxValue ) ){
							dMaxValue = dValue;
							avMax = avG;
						}
					}
				}
			}
		}
		return dMaxValue;*/
		AlphaVector[] aNext = new AlphaVector[m_cObservations];
		return findMaxAlphas(iAction, bs, vValueFunction, aNext);
	}
	
	
	public class ValueFunctionEntry{
		private double m_dValue;
		private int m_iAction;
		private double[] m_adQValues;
		private int m_cActions;
		
		public ValueFunctionEntry( double dValue, int iAction ){
			m_dValue = dValue;
			m_iAction = iAction;
			m_cActions = m_pPOMDP.getActionCount();
			m_adQValues = new double[m_cActions];
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				m_adQValues[iAction] = Double.POSITIVE_INFINITY;
			}
		}
		public void setValue( double dValue ){
			m_dValue = dValue;
		}
		public double getValue(){
			return m_dValue;
		}
		public void setAction( int iAction ){
			m_iAction = iAction;
		}
		public int getAction(){
			return m_iAction;
		}
		public void setQValue( int iAction, double dValue ){
			m_adQValues[iAction] = dValue;
		}
		public double getQValue( int iAction ){
			return m_adQValues[iAction];
		}
		public double getMaxQValue(){
			double dMaxValue = Double.NEGATIVE_INFINITY;
			int iAction = 0;
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				if( m_adQValues[iAction] > dMaxValue )
					dMaxValue = m_adQValues[iAction];
			}
			return dMaxValue;
		}
		public int getMaxAction(){
			double dMaxValue = Double.NEGATIVE_INFINITY;
			int iAction = 0, iMaxAction = -1;
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				if( m_adQValues[iAction] > dMaxValue ){
					dMaxValue = m_adQValues[iAction];
					iMaxAction = iAction;
				}
			}
			return iMaxAction;
		}
	}


	public DistributionCalculator getCalculator() {
		return calculator;
	}
	public void setCalculator(DistributionCalculator calculator) {
		this.calculator = calculator;
	}

}
