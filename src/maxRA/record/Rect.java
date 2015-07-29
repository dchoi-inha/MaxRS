package maxRA.record;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * @author Dongwan Choi
 * @date 2011. 12. 1.
 * 
 * This class implements a rectangle.
 */
public class Rect extends Record {
	public static final int SIZE = 16; // (2*8bytes = 16bytes)
//--------on disk--------
	private Point p1; // a left-bottom point
	private Point p2; // a right-top point
//-----------------------
	
	private int weight;
	
	public Rect( Point center, int width, int height ) {
		this.p1 = new Point(center.getX()-width/2, center.getY()-height/2);
		this.p2 = new Point(center.getX()+width/2, center.getY()+height/2);
	}
	
	public Rect( Point p1, Point p2 ) {
		this.p1 = p1;
		this.p2 = p2;
	}
	
	public Rect( Point p1, Point p2, int weight ) {
		this.p1 = p1;
		this.p2 = p2;
		this.weight = weight;
	}
	
	public Rect( byte [] byteArray ) {
		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
		this.p1 = new Point(byteBuf.getInt(), byteBuf.getInt());
		this.p2 = new Point(byteBuf.getInt(), byteBuf.getInt());
	}
	
	public int getW() { return ((int)p2.getX() - (int)p1.getX()); }
	public int getH() { return ((int)p2.getY() - (int)p1.getY()); }
	
	public Rect getIntersect(Rect r) {
		int x1 = Math.max((int)p1.getX(), (int)r.p1.getX());
		int x2 = Math.min((int)p2.getX(), (int)r.p2.getX());
		int y1 = Math.max((int)p1.getY(), (int)r.p1.getY());
		int y2 = Math.min((int)p2.getY(), (int)r.p2.getY());
		
		if ( x1 <= x2 && y1 <= y2 ) 
			return new Rect(new Point(x1, y1), new Point(x2, y2));
		else
			return null;
	}
	
	@Override
	public byte[] toByteArray() {
		ByteBuffer byteBuf = ByteBuffer.allocate(this.size());
		byteBuf.putInt((int)p1.getX());
		byteBuf.putInt((int)p1.getY());
		byteBuf.putInt((int)p2.getX());
		byteBuf.putInt((int)p2.getY());
		
		return byteBuf.array();
	}

	@Override
	public int size() {
		return Rect.SIZE;
	}
	
	public Interval getInvX() {
		return new Interval((int)p1.getX(), (int)p2.getX(), weight);
	}
	public Interval getInvY() {
		return new Interval((int)p1.getY(), (int)p2.getY(), weight);
	}
	
	public String toString() {
		return getInvX().toString2() + "/" + getInvY().toString2()+" with count="+weight;
//		return p1.toString()+"~"+p2.toString()+" with sum="+weight;
	}
	
	public int getWeight() {
		return weight;
	}
	
	public void setWeight( int weight ) {
		this.weight = weight;
	}
	
	public void incrementWeight() {
		this.weight++;
	}
	
	public Point getCenter() {
		return p1.midPoint(p2);
	}
	
	public boolean contain(Point p) {
		return (p1.getX()<p.getX() && p1.getY()<p.getY() && p2.getX()>p.getX() && p2.getY()>p.getY());
	}

	@Override
	public int compareTo(Record o) {
		// TODO Auto-generated method stub
		return 0;
	}
}
