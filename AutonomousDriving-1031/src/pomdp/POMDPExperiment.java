package pomdp;

import pomdp.algorithms.ValueIteration;
import pomdp.algorithms.pointbased.GenericValueIteration;
import pomdp.environments.FieldVisionRockSample;
import pomdp.environments.MasterMind;
import pomdp.environments.ModifiedRockSample;
import pomdp.environments.POMDP;
import pomdp.environments.FactoredPOMDP.BeliefType;
import pomdp.utilities.ExecutionProperties;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.sun.corba.se.spi.orb.StringPair;
import com.sun.org.apache.bcel.internal.generic.NEW;

import pomdp.utilities.Logger;


public class POMDPExperiment {

	public static void main( String[] args ) throws IOException{


		//String[] aCollectionMethods = new String[]{"GapMinCollection","FSVICollection","HSVICollection","PEMACollection", "PBVICollection","RandomCollection"};
		String[] aCollectionMethods = new String[]{"GapMinCollection"};//PBVIDownToTopCollection
		//String[] aOrderingMethods = new String[]{"PerseusBackup","FullBackup","NewestPointsBackup",};
		String[] aOrderingMethods = new String[]{"FullBackup"};//,"FullBackup","NewestPointsBackup",};DownToTopBackup
		//String[] aOrderingMethods = new String[]{"FullBackup"};//,"FullBackup","NewestPointsBackup",};
		//String[] aOrderingMethods = new String[]{"NewestPointsBackup"};//,"FullBackup","NewestPointsBackup",};

		/* set defaults */
		double dTargetADR = 10000.0;//NO USE
		String sExperimentType = "Basic";
		//String sModelName = "hallway.1";
		//String sModelName = "tiger-grid.1";
		//String sModelName = "dialogue";
		//String sModelName = "RockSample_7_8";
		String sModelName = "RockSample_5_5";//"Wumpus7_0";
		//String sModelName = "tagAvoid";
		//String sModelName = "underwaterNav";
		//String collectionName = "PBVICollection";
		//String collectionName = "FSVICollection";
		//String orderingName = "FullBackup";
		boolean useBlindPolicy = false;
		

		/* target Running time (in seconds), if we want to stop at a specified time */
		int maxRunningTime = 200;				//最大运行时间
		int numEvaluations = 20;				//评估次数，每经过maxRunningTime/numEvaluations时间，计算一次线性插值进行评估
		int numIndependentTrials = 1;			
		int numBeliefsPerStep = -1, numBeliefsPerStepOrg = 100;
		int numBackupIterations = 1;
		boolean bAllowDuplicates = false;
		boolean bReversedBackupOrder = false;
		boolean bUseFIB = false;
		
		if (args.length <= 4) {
			sModelName = args[0];
			aCollectionMethods = new String[]{args[1]};
			maxRunningTime = Integer.parseInt(args[2]);
			numEvaluations = Integer.parseInt(args[3]);
		}
		
		if (args.length < 9) {
			System.out.println("Format is: POMDPExperiment TYPE MODEL_NAME RUNNING_TIME NUMBER_EVALUATIONS NUMBER_TRIALS COLLECTION_ALGORITHM ORDERING_ALGORITHM NUMBER_BELIEFS_STEP NUMBER_BACKUP_ITERATIONS ALLOW_DUPLICATES REVERSED_BACKUP_ORDER USE_FIB USE_BLIND_POLICY");
			return;
		}
		
		if (args.length >= 9)
		{
			sExperimentType = args[0];
			sModelName = args[1];
			maxRunningTime = Integer.parseInt(args[2]);
			numEvaluations = Integer.parseInt(args[3]);			
			numIndependentTrials = Integer.parseInt(args[4]);		
			//collectionName = args[5];
			//orderingName = args[6];
			aCollectionMethods = new String[]{args[5]};
			aOrderingMethods = new String[]{args[6]};
			numBeliefsPerStepOrg = Integer.parseInt(args[7]);
			numBackupIterations = Integer.parseInt(args[8]);
			
			if (args.length > 9)
				if (Boolean.parseBoolean(args[9]))
					bAllowDuplicates = true;
			if (args.length > 10)
				if (Boolean.parseBoolean(args[10]))
					bReversedBackupOrder = true;
			if (args.length > 11)
				if (Boolean.parseBoolean(args[11]))
					bUseFIB = true;
			if (args.length > 12)
				if (Boolean.parseBoolean(args[12]))
					useBlindPolicy = true;
		}

		
		
		Logger.getInstance().setOutput(true);
		Logger.getInstance().setSilent(false);
		
		//FileOutputStream fos = new FileOutputStream("logs/Basic/" + sModelName + "/" + sModelName + ".txt"); 
		//fos.close();
		for( String orderingName : aOrderingMethods ){

			for( String collectionName : aCollectionMethods ){

				if( collectionName.equals( "PEMACollection"  ) )
					numBeliefsPerStep = numBeliefsPerStepOrg / 10;	
				else
					numBeliefsPerStep = numBeliefsPerStepOrg;
				
				System.out.println("Started " + sModelName + ", " + collectionName + ", " + orderingName);

				String sOutputDir = "logs/" + sExperimentType + "/" + sModelName ;
				String sFileName = sModelName + "_" + collectionName + "_" + orderingName + "_" + numBeliefsPerStep + "_" + numBackupIterations;

				if (useBlindPolicy)
					sFileName += "_blind";

				Calendar c = Calendar.getInstance();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
				
				sFileName += "_"+sdf.format(c.getTime())+".txt";

				

				/*if ((args.length != 0) && (args.length < 9))
				{
					Logger.getInstance().logln("Format is: POMDPExperiment TYPE MODEL_NAME RUNNING_TIME NUMBER_EVALUATIONS NUMBER_TRIALS COLLECTION_ALGORITHM ORDERING_ALGORITHM NUMBER_BELIEFS_STEP NUMBER_BACKUP_ITERATIONS");
					return;
				}*/

				try
				{
					Logger.getInstance().setOutputStream(sOutputDir, sFileName);
				}
				catch( Exception e ){
					System.err.println( e );
				}

				Logger.getInstance().logln("Model:" + sModelName);
				Logger.getInstance().logln("Collection Algorithm:" + collectionName);
				Logger.getInstance().logln("Backup Ordering Algorithm:" + orderingName);
				Logger.getInstance().logln("# of Independent Trials: " + numIndependentTrials);
				Logger.getInstance().logln("# of Beliefs Collected Per Step: " + numBeliefsPerStep);	
				Logger.getInstance().logln("# of Backup Iterations Per Step: " + numBackupIterations);
				Logger.getInstance().logln("Output File: "+ sFileName);

				/* iterate through independent trials */
				for (int trial = 0; trial < numIndependentTrials; trial++)
				{

					System.gc();
					Logger.getInstance().logFull("POMDPExperiment",0,"main","Checking memory after GC");

					POMDP pomdp = null;


					/* load the POMDP model */
					try
					{
						if( sModelName.equals( "RockSample5" ) ){
							int cX = 5, cY = 5, cRocks = 5, halfSensorDistance = 4;
							pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat );
						}
						else if( sModelName.equals( "RockSample5-99" ) ){
							int cX = 5, cY = 5, cRocks = 5, halfSensorDistance = 4;
							pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat, .99);
						}
						else if( sModelName.equals( "RockSample7" ) ){
							int cX = 7, cY = 7, cRocks = 8, halfSensorDistance = 20;
							pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat );
						}
						else if( sModelName.equals( "RockSample7-99" ) ){
							int cX = 7, cY = 7, cRocks = 8, halfSensorDistance = 20;
							pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat, .99);
						}
						else if( sModelName.equals( "FieldVisionRockSample5" ) ){
							int cX = 5, cY = 5, cRocks = 5, halfSensorDistance = 4;
							pomdp = new FieldVisionRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat );
						}
						else if( sModelName.equals( "FieldVisionRockSample7" ) ){
							int cX = 7, cY = 7, cRocks = 8, halfSensorDistance = 20;
							pomdp = new FieldVisionRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat );
						}
						else if( sModelName.equals( "MasterMind" ) ){
							pomdp = new MasterMind( 4, 0.3 );
						}
						/* flat POMDP, will load from file */
						else
						{
							pomdp = new POMDP();
							pomdp.load( ExecutionProperties.getPath() + sModelName + ".POMDP" );
							Logger.getInstance().logln("max is " + pomdp.getMaxR() + " min is "+ pomdp.getMinR());
						}
					}
					catch (Exception e)
					{
						Logger.getInstance().logln( e );
						e.printStackTrace();
						System.exit( 0 );
					}

					Logger.getInstance().logln("POMDP is " + pomdp.getName());



					ValueIteration viAlgorithm = new GenericValueIteration(pomdp, collectionName, orderingName, numBeliefsPerStep, numBackupIterations, useBlindPolicy, bAllowDuplicates, bReversedBackupOrder, bUseFIB);
					int cMaxIterations = 500;
					try
					{
						/* run POMDP solver */
						viAlgorithm.valueIteration(cMaxIterations, ExecutionProperties.getEpsilon(), dTargetADR, maxRunningTime, numEvaluations);
						Logger.getInstance().log( "----------------");
						System.out.println("Done " + sModelName + ", " + collectionName + ", " + orderingName + " - " + trial);
						Logger.getInstance().logln();
					}

					catch( Exception e ){
						Logger.getInstance().logln( e );
						e.printStackTrace();
					} 
					catch( Error err ){
						Runtime rtRuntime = Runtime.getRuntime();
						Logger.getInstance().logln( "POMDPSolver: " + err +
								" allocated " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
								" free " + rtRuntime.freeMemory() / 1000000 +
								" max " + rtRuntime.maxMemory() / 1000000 );
						Logger.getInstance().log( "Stack trace: " );
						err.printStackTrace();
					}
				}
			}
		}
	}
}
