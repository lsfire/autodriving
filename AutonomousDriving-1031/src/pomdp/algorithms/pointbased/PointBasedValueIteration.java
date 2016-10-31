package pomdp.algorithms.pointbased;

import java.util.Iterator;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateVector;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.concurrent.Backup;
import pomdp.utilities.concurrent.ComputeFarthestSuccessors;
import pomdp.utilities.concurrent.ThreadPool;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class PointBasedValueIteration extends ValueIteration {

	protected Iterator m_itCurrentIterationPoints;
	protected boolean m_bSingleValueFunction = true;
	protected boolean m_bRandomizedActions;
	
	public PointBasedValueIteration( POMDP pomdp ){
		super(pomdp);
		
		m_itCurrentIterationPoints = null;
		m_bRandomizedActions = true;
	}

	public PointBasedValueIteration( POMDP pomdp, boolean bRandomizedActionExpansion ){
		super(pomdp);
		
		m_itCurrentIterationPoints = null;
		m_bRandomizedActions = bRandomizedActionExpansion;
	}

	protected BeliefStateVector<BeliefState> expandPBVI( BeliefStateVector<BeliefState> vBeliefPoints ){
		//扩充后的B，原先的B中内容已经在这里
		BeliefStateVector<BeliefState> vExpanded = new BeliefStateVector<BeliefState>( vBeliefPoints );
		Iterator it = vBeliefPoints.iterator();
		//临时变量，存放当前用来扩充的b
		BeliefState bsCurrent = null;
		//临时变量，存放得到的最远b
		BeliefState bsNext = null;

		//设置不需要缓存b
		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		//每次扩充100个b
		while( vExpanded.size() < vBeliefPoints.size() + 100 ){
			//是从扩充后B中随机取个b，计算它的最远后继！！和标准PBVI中expand不同
			//一个原因：保证能够扩充100个b
			bsCurrent = vExpanded.elementAt( m_rndGenerator.nextInt( vExpanded.size() ) );	
			//计算最远的后继
			bsNext = m_pPOMDP.getBeliefStateFactory().computeRandomFarthestSuccessor( vBeliefPoints, bsCurrent );
			if( ( bsNext != null ) && ( !vExpanded.contains( bsNext ) ) )
				vExpanded.add(bsCurrent, bsNext);
		}
		//设置回原来的值，是否要缓存b
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return vExpanded;
	}
	
	protected Vector<BeliefState> expandMultiThread( Vector<BeliefState> vBeliefPoints ){
		Vector<BeliefState> vExpanded = new Vector<BeliefState>( vBeliefPoints );
		Vector<BeliefState> vSuccessors = null;
		ComputeFarthestSuccessors[] abThreads = new ComputeFarthestSuccessors[ExecutionProperties.getThreadCount()]; 
		int iThread = 0, cThreads = ExecutionProperties.getThreadCount();
		
		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			abThreads[iThread] = new ComputeFarthestSuccessors( vBeliefPoints );
			abThreads[iThread].setPOMDP( m_pPOMDP );
		}
		
		iThread = 0;
		for( BeliefState bs : vBeliefPoints ){
			abThreads[iThread].addBelief( bs );
			iThread = ( iThread + 1 ) % cThreads;
		}
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			ThreadPool.getInstance().addTask( abThreads[iThread] );
		}
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			ThreadPool.getInstance().waitForTask( abThreads[iThread] );
		}

		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			vSuccessors = abThreads[iThread].getSuccessors();
			for( BeliefState bs : vSuccessors ){
				if( !vExpanded.contains( bs ) ){
					vExpanded.add( bs );
					//Logger.getInstance().logln( bs );
				}
			}
		}
			
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return vExpanded;
	}
	
	
	/**
	 * 通过值迭代，求出一个策略及相应的值函数 
	 * 
	 *|Vn| 	m_vValueFunction，父类ValueIteration中
	 *|B|	vBeliefPoints
	 * 
	 * @param cIterations	最大的迭代次数，TODO 400
	 * @param dEpsilon		结束条件，即值迭代前后的value之差, 0.001	ExecutionProperties.getEpsilon()
	 * @param dTargetValue	target Averaged Discounted Reward, if we want to stop if we get to some level of ADR，100.0
	 * @param maxRunningTime	最大运行时间，45s
	 * @param numEvaluations	TODO 貌似每一次该函数的计算，都算一次evaluation 评估次数，3
	 */
	public void valueIteration(int cIterations, double dEpsilon, double dTargetValue, int maxRunningTime, int numEvaluations){
		
		int currentEvaluation = 1;
		// with numEvaluations evaluations, we find out how much time allocated per evaluation
		int timePerEval = maxRunningTime / numEvaluations;
		
		//Averaged Discounted Reward
		Pair<Double, Double> pComputedADRs = new Pair<Double, Double>(new Double(0.0), new Double(0.0));
		boolean bDone = false;
		boolean bDoneInternal = false;
		long lStartTime;
		long lCurrentTime = 0;
		Runtime rtRuntime = Runtime.getRuntime();
		
		int iIteration = 0;
		int cInternalIterations = 1;
		int iInternalIteration = 0;
		double dDelta = 1.0;
		double dMinDelta = 0.01;
		int cBeliefPoints = 0;
		
		m_cElapsedExecutionTime = 0;
		m_cCPUExecutionTime = 0;
		
		long lCPUTimeBefore;
		
		long lCPUTimeAfter = 0, lCPUTimeTotal = 0;
		int cValueFunctionChanges = 0;		
			
		//初始化信念点集合B
		BeliefStateVector<BeliefState>	vBeliefPoints = new BeliefStateVector<BeliefState>();
		
		//把b0加入B
		/* initialize the list of belief points with the initial belief state */
		vBeliefPoints.add(null, m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() );
		
		Logger.getInstance().logln( "Begin " + getName() );

		for( iIteration = 0 ; iIteration < cIterations && !bDone ; iIteration++ ){
			
			//记录已花去的时间
			/* Compute quality of solution at this time */
			long elapsedTimeSeconds = m_cElapsedExecutionTime / 1000000000;
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			/* first, expand the belief set */
			if( iIteration > 0 ){
											
				Logger.getInstance().logln( "Expanding belief space" );
				m_dFilteredADR = 0.0;
				cBeliefPoints = vBeliefPoints.size();
				if( ExecutionProperties.useHighLevelMultiThread() )
//					vBeliefPoints = expandMultiThread( vBeliefPoints );
					Logger.getInstance().logln("Error: PointBasedValueIteration.valueIteration: This functionality has been emporarilys disabled.");
				else
					//扩充B
					vBeliefPoints = expandPBVI( vBeliefPoints );
				Logger.getInstance().logln( "Expanded belief space - |B| = " + vBeliefPoints.size() );
				//如果B不变化了，就停止迭代
				if( vBeliefPoints.size() == cBeliefPoints )
					bDone = true;
			}
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			m_cElapsedExecutionTime += (lCPUTimeAfter - lCPUTimeBefore);
			lCPUTimeTotal += (lCPUTimeAfter - lCPUTimeBefore);
					
			dDelta = 1.0;
			bDoneInternal = false;
			//通过一个迭代来improve，都只迭代一次 cInternalIterations == 1
			for( iInternalIteration = 0 ; 
				( iInternalIteration < cInternalIterations ) && ( dDelta > dMinDelta ) && !bDoneInternal ; iInternalIteration++ ){
				
				
				/* Compute quality of solution at this time */
				elapsedTimeSeconds = m_cElapsedExecutionTime / 1000000000;
				
				lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
				cValueFunctionChanges = m_vValueFunction.getChangesCount();
				if( ExecutionProperties.useHighLevelMultiThread() )
					dDelta = improveValueFunctionMultiThreaded( vBeliefPoints );
				else
					dDelta = improveValueFunction( vBeliefPoints );

				lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();			
				
				m_cElapsedExecutionTime += (lCPUTimeAfter - lCPUTimeBefore);
				lCPUTimeTotal += (lCPUTimeAfter - lCPUTimeBefore);
				
				//检查内部迭代的终止条件
				//dDelta < dEpsilon 或者 值函数不变了
				if( dDelta < dEpsilon && cValueFunctionChanges == m_vValueFunction.getChangesCount() ){
					Logger.getInstance().logln( "Value function did not change - iteration " + iIteration + " complete" );
					bDoneInternal = true;
				}
				else{
					//输出本次迭代的信息
					//虽然这个是在内部迭代，但是，内部迭代只有一次，所以，这其实也是一次外部迭代的信息
					if( iIteration > 0 ){
						//检查平均折扣回报是否收敛，如果收敛，就认为内部迭代可以停止了
						bDone = bDone || checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs );
						if( bDone )
							bDoneInternal = true;
					}
					
					rtRuntime.gc();
					Logger.getInstance().logln( "PBVI: Iteration " + iIteration + "," + iInternalIteration +
							" |Vn| = " + m_vValueFunction.size() +
							" |B| = " + vBeliefPoints.size() +
							" Delta = " + round( dDelta, 4 ) +
							" simulated ADR " + ((Number) pComputedADRs.first()).doubleValue() +
							" filtered ADR " + round( ((Number) pComputedADRs.second()).doubleValue(), 3 ) +
							//" Time " + ( lCurrentTime - lStartTime ) / 1000 +
							" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
							" CPU total " + lCPUTimeTotal  / 1000000000 +
							" #backups " + m_cBackups + 
							" #dot product " + AlphaVector.dotProductCount() + 
							" |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
							" memory: " + 
							" total " + rtRuntime.totalMemory() / 1000000 +
							" free " + rtRuntime.freeMemory() / 1000000 +
							" max " + rtRuntime.maxMemory() / 1000000 +
							"" );
				}
			}
		}

		Logger.getInstance().logln( "Finished " + getName() + " - time : " + m_cElapsedExecutionTime + " |BS| = " + vBeliefPoints.size() +
				" |V| = " + m_vValueFunction.size() + " backups = " + m_cBackups + " GComputations = " + AlphaVector.getGComputationsCount() );
	}

	 
	protected double improveValueFunction( BeliefStateVector vBeliefPoints ){
		//vNextValueFunction没有用到
		LinearValueFunctionApproximation vNextValueFunction = new LinearValueFunctionApproximation( m_dEpsilon, true );
		BeliefState bsCurrent = null, bsMax = null;
		AlphaVector avBackup = null, avNext = null, avCurrentMax = null;
		double dMaxDelta = 1.0, dDelta = 0.0, dBackupValue = 0.0, dValue = 0.0;
		double dMaxOldValue = 0.0, dMaxNewValue = 0.0;
		int iBeliefState = 0;

		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		
		if( m_itCurrentIterationPoints == null )
			m_itCurrentIterationPoints = vBeliefPoints.getTreeDownUpIterator();
		dMaxDelta = 0.0;
		
		//迭代所有的b
		while( m_itCurrentIterationPoints.hasNext() ){
			//当前的b
			bsCurrent= (BeliefState) m_itCurrentIterationPoints.next();
			//当前b对应的最大α
			avCurrentMax = m_vValueFunction.getMaxAlpha( bsCurrent );
			//backup操作后的α
			avBackup = backup( bsCurrent );
			
			//计算backup前后，该b点value之差
			dBackupValue = avBackup.dotProduct( bsCurrent );
			dValue = avCurrentMax.dotProduct( bsCurrent );
			dDelta = dBackupValue - dValue;
			
			
			if( dDelta > dMaxDelta ){
				dMaxDelta = dDelta;
				bsMax = bsCurrent;
				dMaxOldValue = dValue;
				dMaxNewValue = dBackupValue;
			}
			
			avNext = avBackup;
			
			//如果有提升，才会增加新的α
			if(dDelta >= 0)
				m_vValueFunction.addPrunePointwiseDominated( avBackup );
			iBeliefState++;
		}
		
		if( m_bSingleValueFunction ){
			//vNextValueFunction没有用到
			Iterator it = vNextValueFunction.iterator();
			while( it.hasNext() ){
				avNext = (AlphaVector) it.next();
				m_vValueFunction.addPrunePointwiseDominated( avNext );
			}
		}
		//没有用到
		else{
			m_vValueFunction.copy( vNextValueFunction );
		}
		
		
		if( !m_itCurrentIterationPoints.hasNext() )
			m_itCurrentIterationPoints = null;
		
		Logger.getInstance().logln( "Max delta over " + bsMax + 
				" from " + round( dMaxOldValue, 3 ) + 
				" to " + round( dMaxNewValue, 3 ) );
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return dMaxDelta;
	}
	
	protected double improveValueFunctionMultiThreaded( Vector<BeliefState> vBeliefPoints ){
		LinearValueFunctionApproximation vNextValueFunction = new LinearValueFunctionApproximation( m_dEpsilon, true );
		BeliefState bsCurrent = null, bsMax = null;
		AlphaVector avBackup = null, avNext = null;
		double dMaxDelta = 1.0, dDelta = 0.0, dBackupValue = 0.0, dValue = 0.0;
		double dMaxOldValue = 0.0, dMaxNewValue = 0.0;

		Backup[] abThreads = new Backup[ExecutionProperties.getThreadCount()]; 
		int iThread = 0, cThreads = ExecutionProperties.getThreadCount();
		int iVector = 0, cVectors = 0;
		
		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		
		Iterator<BeliefState> itCurrentIterationPoints = vBeliefPoints.iterator();
		dMaxDelta = 0.0;
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			abThreads[iThread] = new Backup( m_pPOMDP, m_vValueFunction );
		}
		
		iThread = 0;
		while( itCurrentIterationPoints.hasNext() ){
			bsCurrent= itCurrentIterationPoints.next();
			abThreads[iThread].addBelief( bsCurrent );
			iThread = ( iThread + 1 ) % cThreads;
		}
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			ThreadPool.getInstance().addTask( abThreads[iThread] );
		}
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			ThreadPool.getInstance().waitForTask( abThreads[iThread] );
		}

		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			cVectors = abThreads[iThread].getResultsCount();
			for( iVector = 0 ; iVector < cVectors ; iVector++ ){
				bsCurrent = abThreads[iThread].getBeliefState( iVector );
				avBackup = abThreads[iThread].getResult( iVector );
				dBackupValue = avBackup.dotProduct( bsCurrent );
				dValue = m_vValueFunction.valueAt( bsCurrent );
				dDelta = dBackupValue - dValue;
				if( dDelta > dMaxDelta ){
					dMaxDelta = dDelta;
					bsMax = bsCurrent;
					dMaxOldValue = dValue;
					dMaxNewValue = dBackupValue;
				}
				vNextValueFunction.addPrunePointwiseDominated( avBackup );
			}
		}
		
		if( m_bSingleValueFunction ){
			Iterator it = vNextValueFunction.iterator();
			while( it.hasNext() ){
				avNext = (AlphaVector) it.next();
				m_vValueFunction.addPrunePointwiseDominated( avNext );
			}
		}
		else{
			m_vValueFunction.copy( vNextValueFunction );
		}
		
		Logger.getInstance().logln( "Max delta over " + bsMax + 
				" from " + round( dMaxOldValue, 3 ) + 
				" to " + round( dMaxNewValue, 3 ) );
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return dMaxDelta;
	}
	
	public String getName(){
		return "PBVI";
	}
}
