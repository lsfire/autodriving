package pomdp.integral;

public interface IEval {
	
	/**
	 * 将科学计数转换为普通计数
	 * 
	 * @author 北大青鸟--杜永耀
	 * @param number 科学计数
	 * @return 普通计数
	 * @throws Exception
	 */
	public String scientificExpressionToNormal(String number) throws Exception;

	/**
	 * 积分
	 * <br/>
	 * 理论依据--复化抛物形公式
	 * <br/>
	 * 公式的详细证明见：
	 * <br/>
	 * 《数值分析》 重庆大学出版社--杨大地、涂光裕 1998年1月第1版 第132页
	 * 
	 * @author 北大青鸟--杜永耀
	 * @param expression 表达式。变量由小写 x 表示，可以包含数学函数（以 java.util.Math 里的函数为准）
	 * @param beginValue 初始值，即定积分的开始值
	 * @param endValue 结束值，定积分的结束值
	 * 
	 * @param partNumber 分块数量。分块越多，精度越高，但是消耗时间更多
	 * @return 当前表达式在某个范围的定积分。积分范围由 beginValue 和 endValue 确定
	 * @throws Exception
	 */
	public double integral(String expression, String beginValue,
			String endValue, int partNumber) throws Exception;

	/**
	 * 求导
	 * <br/>
	 * 理论依据--三点公式
	 * <br/>
	 * 公式的详细证明见：
	 * <br/>
	 * 《数值分析》 重庆大学出版社--杨大地、涂光裕 1998年1月第1版 第97页
	 * <br/>
	 * <br/>
	 * 注意：该方法不适用于不可导点，或不可导函数
	 * @author 北大青鸟--杜永耀
	 * @param expression 表达式。变量由小写 x 表示，可以包含数学函数（以 java.util.Math 里的函数为准）
	 * @param value 在何处求导。
	 * @return 当前表达式在某点的导数
	 * @throws Exception
	 */
	public double differentiate(String expression, String value)
			throws Exception;

	/**
	 * 计算表达式
	 * @author 北大青鸟--杜永耀
	 * @param expression 表达式。可以包含数学函数（以 java.util.Math 里的函数为准）
	 * @return 表达式计算后的结果
	 * @throws Exception
	 */
	public double eval2(String expression) throws Exception;

	/**
	 * 四则运算
	 * @author 北大青鸟--杜永耀
	 * @param expression 表达式。不能包含数学函数，只能由数字、括号或基本运算符组成
	 * @return 表达式计算后的结果
	 * @throws Exception
	 */
	public double eval(String expression) throws Exception;
}
