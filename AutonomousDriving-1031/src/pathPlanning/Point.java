package pathPlanning;

public class Point {
	private int x;
	private int y;
	private Point parentPoint;
	private int num;
	private double f;

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
		num = 0;
		f = 0.0;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Point getParentPoint() {
		return parentPoint;
	}

	public void setParentPoint(Point parentPoint) {
		this.parentPoint = parentPoint;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public double getF() {
		return f;
	}

	public void setF(double f) {
		this.f = f;
	}
}
