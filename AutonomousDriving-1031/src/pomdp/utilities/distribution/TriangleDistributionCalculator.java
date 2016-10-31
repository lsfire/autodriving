package pomdp.utilities.distribution;

public class TriangleDistributionCalculator extends AbstractDistributionCalculator{

	@Override
	public double getValue(double upperBound, double lowerBound) {
		/*double width = upperBound - lowerBound;
		double rand_x = m_rndGenerator.nextDouble();
		
		return (2*rand_x - Math.pow(rand_x, 2)) * width + lowerBound;*/
		
		double x = m_rndGenerator.nextDouble();
		double y = m_rndGenerator.nextDouble();
		
		double value = 0.0;
		//double linearFunctionValue = 2*y + x - 2;
		double linearFunctionValue = 2*y - x;
		
		if ( linearFunctionValue > 0) {
			value = 0.5 * (2 - x) * (upperBound - lowerBound) + lowerBound;
		}
		else {
			value = 0.5 * x * (upperBound - lowerBound) + lowerBound;
		}
		
		return value;
	}

}
