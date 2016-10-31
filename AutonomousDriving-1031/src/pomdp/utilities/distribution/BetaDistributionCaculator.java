package pomdp.utilities.distribution;

import pomdp.integral.Beta;

public class BetaDistributionCaculator extends AbstractDistributionCalculator{

	@Override
	public double getValue(double upperBound, double lowerBound) {
		Beta b = new Beta();
		double width = upperBound - lowerBound;
		double rand_x = m_rndGenerator.nextDouble();
		
		return b.calculateBeta(1, 3, 0, rand_x)*width + lowerBound;
	}
	
}
