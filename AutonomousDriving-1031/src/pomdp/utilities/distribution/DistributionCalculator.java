package pomdp.utilities.distribution;

public interface DistributionCalculator {
	/**
	 * 根据分布取值
	 * @param upperBound	上界
	 * @param lowerBound	下界
	 * @return
	 */
	public double getValue(double upperBound, double lowerBound);
}
