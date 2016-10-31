package pomdp.utilities.distribution;

import pomdp.utilities.RandomGenerator;

public abstract class AbstractDistributionCalculator implements DistributionCalculator{

	protected RandomGenerator m_rndGenerator;
	
	public AbstractDistributionCalculator() {
		m_rndGenerator = new RandomGenerator("calculator");
	}
	
}
