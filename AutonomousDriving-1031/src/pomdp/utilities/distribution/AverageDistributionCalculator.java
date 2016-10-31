package pomdp.utilities.distribution;

public class AverageDistributionCalculator extends AbstractDistributionCalculator{

	@Override
	public double getValue(double upperBound, double lowerBound) {
		double width = upperBound - lowerBound;
		return m_rndGenerator.nextDouble(width) + lowerBound;
	}
	
}
