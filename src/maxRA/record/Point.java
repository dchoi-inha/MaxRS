package maxRA.record;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import maxRA.Env;

/**
 * @author Dongwan Choi
 * @date 2011. 12. 1.
 * 
 * This is a class for a data object
 */
public class Point extends Record {
	public static final int SIZE = 8; //(2*4bytes = 16bytes)
//--------on disk--------	
	protected int x; 
	protected int y;
//-----------------------
	
	public Point ( int x, int y ) {
		this.x = x;
		this.y = y;
	}
	
//	public Point (byte[] byteArray) {
//		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
//		this.x = byteBuf.getInt();
//		this.y = byteBuf.getInt();
//	}
	
	
	// This is only for accepting the input data set by Xiaochen Hu.
	public Point (byte[] byteArray) {
		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN);
//		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
		this.x = byteBuf.getInt();
		this.y = byteBuf.getInt();
	}
	
	// Construct a point with the same location as the specified point
	public Point(Point point) 
	{
		x = point.x;
		y = point.y;
	}
	public Point (double x, double y) {
		this.x = (int)x;
		this.y = (int)y;
	}
	
	public double getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
	
	public Interval getXInterval() {
		return new Interval((int)this.x-Env.WIDTH/2, (int)this.x+Env.WIDTH/2, 0);
	}
	
	public HInterval[] getHIntervals() {
		int yTop = (int)this.y+Env.HEIGHT/2;
		int yBottom = (int)this.y-Env.HEIGHT/2;
		Interval iTop = new Interval((int)this.x-Env.WIDTH/2, (int)this.x+Env.WIDTH/2, -1);
		Interval iBottom = new Interval((int)this.x-Env.WIDTH/2, (int)this.x+Env.WIDTH/2, 1);
		return new HInterval[] {new HInterval(yBottom, iBottom), new HInterval(yTop, iTop)};
	}

	public Rect getRect() {
		return new Rect(this, Env.WIDTH, Env.HEIGHT);
	}
	
	public Circle getCircle() {
		return new Circle(this, Env.RADIUS);
	}
	
	public Point[] getShiftPoints(int k){
		Point points [] = new Point[k];
		double sigma = Env.RADIUS/2;
		for (int i = 0; i < k; i++ ) {
			double ang = 2*Math.PI/(2*k) + (2*Math.PI/k)*i;
			points[i] = new Point(x+Math.cos(ang)*sigma, y+Math.sin(ang)*sigma);
		}
		
		return points;
	}
	
	
//	public int getWeight() {
//		return weight;
//	}
//
//	public void setWeight(int weight) {
//		this.weight = weight;
//	}
	
	public String toString() {
		return "("+x+","+y+")";
//		return "("+x+" "+String.format("%08X", x)+","+y+" "+String.format("%08X", y)+")";
	}

	@Override
	public int compareTo(Record o) {
		int xDiff = (int)(this.x-((Point)o).x);
		int yDiff = (int)(this.y-((Point)o).y)*-1;
		
		if ( yDiff == 0 ) return xDiff;
		else return yDiff;
	}

	@Override
	public int size() {
		return Point.SIZE;
	}

	@Override
	public byte[] toByteArray() {
		ByteBuffer byteBuf = ByteBuffer.allocate(this.size()).order(ByteOrder.LITTLE_ENDIAN);
		byteBuf.putInt((int)x);
		byteBuf.putInt((int)y);
		return byteBuf.array();	
	}
	
	public boolean equals(Point p) {
		return (this.x == p.x && this.y == p.y);
	}

    public double distance(Point point)
    {
		double dx = x - point.x;
		double dy = y - point.y;
		return Math.sqrt(dx*dx + dy*dy);
    }

    // Calculate the middle point between two points
    public Point midPoint(Point point)
    {
		return new Point((x+point.x)/2, (y+point.y)/2);
    }
}
