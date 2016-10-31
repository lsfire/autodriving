package pomdp.algorithms.beliefcollection;

import pomdp.algorithms.ValueIteration;
import pomdp.utilities.distribution.AverageDistributionCalculator;
import pomdp.utilities.distribution.BetaDistributionCaculator;
import pomdp.utilities.distribution.TriangleDistributionCalculator;

public class BeliefCollectionFactory {

	public static BeliefCollection getBeliefCollectionAlgorithm(String sName, ValueIteration vi, boolean bAllowDuplicates, boolean bUseFIB){

		if(sName.equals("FSVICollection"))
			return new FSVICollection(vi, bAllowDuplicates);
		if(sName.equals("HSVICollection"))
			return new HSVICollection(vi, bAllowDuplicates, bUseFIB);
		
		if(sName.equals("PBVIDownToTopCollection"))
			return new PBVIDownToTopCollection(vi, false, bAllowDuplicates);
		if(sName.equals("PBVICollection"))
			return new PBVICollection(vi, false, bAllowDuplicates);
		
		if(sName.equals("PBVILeafCollection"))
			return new PBVILeafCollection(vi, false, bAllowDuplicates);
		
		if(sName.equals("PBVIFullCollection"))
			return new PBVIDownToTopCollection(vi, true, bAllowDuplicates);
		if(sName.equals("PBVIFullLeafCollection"))
			return new PBVILeafCollection(vi, true, bAllowDuplicates);
		
		
		if(sName.equals("PEMACollection"))
			return new PEMACollection(vi, bAllowDuplicates);
		if(sName.equals("RandomCollection"))
			return new RandomCollection(vi, bAllowDuplicates);

		if(sName.equals("SARSOPCollection"))
			return new SARSOPCollection(vi, bAllowDuplicates, bUseFIB);
		if(sName.equals("GapMinCollection"))
			return new GapMinCollection(vi, bAllowDuplicates, bUseFIB);
		
		if (sName.equals("GapMinCollection_Avg")) {
			GapMinCollection gc = new GapMinCollection(vi, bAllowDuplicates, bUseFIB);
			gc.setActionMethod("Avg");
			gc.setCalculator(new AverageDistributionCalculator());
			return gc;
		}
		
		if (sName.equals("GapMinCollection_Tri")) {
			GapMinCollection gc = new GapMinCollection(vi, bAllowDuplicates, bUseFIB);
			gc.setActionMethod("Tri");
			gc.setCalculator(new TriangleDistributionCalculator());
			return gc;
		}
		
		if (sName.equals("GapMinCollection_Beta")) {
			GapMinCollection gc = new GapMinCollection(vi, bAllowDuplicates, bUseFIB);
			gc.setActionMethod("Beta");
			gc.setCalculator(new BetaDistributionCaculator());
			return gc;
		}
		
		return null;
	}
}
