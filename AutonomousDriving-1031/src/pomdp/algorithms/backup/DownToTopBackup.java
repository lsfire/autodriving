package pomdp.algorithms.backup;

import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateVector;

public class DownToTopBackup extends BackupOrdering {

	public DownToTopBackup(ValueIteration vi, boolean bReversedBackupOrder)
	{
		super(vi, bReversedBackupOrder);	
	}
	
	public void improveValueFunction(Vector<BeliefState> vOriginalPointsIn, Vector<BeliefState> vNewBeliefsIn){
		
		BeliefStateVector<BeliefState> vOriginalPoints = (BeliefStateVector<BeliefState>)vOriginalPointsIn;
		BeliefStateVector<BeliefState> vNewBeliefs = (BeliefStateVector<BeliefState>)vNewBeliefsIn;
		
		
		/* we will operate on all belief points */
		BeliefStateVector<BeliefState> vBeliefPoints = new BeliefStateVector<BeliefState>(vOriginalPoints);
		vBeliefPoints.addAll(vNewBeliefs);
		
		improveValueFunctionSinglePassDownToTop(vBeliefPoints);
		//buildNewValueFunctionSinglePass(vBeliefPoints);	

	}

}
