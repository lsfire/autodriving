package pomdp.integral;

public class Beta extends Eval{
	
	/**
	 * 
	 * @param a 
	 * @param b
	 * @param beginValue 积分下限
	 * @param endValue 积分上限
	 * @return
	 */
	public double calculateBeta(int a, int b, double beginValue,double endValue ){
		double coefficient = calculateGama(a+b)/(calculateGama(a)*calculateGama(b));
		String expression = coefficient+"*pow(x,"+(a-1)+")*pow((1-x),"+(b-1)+")";
		double result = 0;
		try {
			 result = integral(expression, beginValue+"", endValue+"", 8);//3000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 计算伽马函数
	 * @param n
	 * @return
	 */
	private int calculateGama(int n){
		int result = 1;
		for(int i = 1;i < n;i++){
			result = result * i;
		}
		return result;
	}
}
