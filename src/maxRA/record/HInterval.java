package maxRA.record;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import maxRA.util.Debug;


/**
 * @author Dongwan Choi
 * @date 2012. 1. 9.
 */
public class HInterval extends Record {
	public static final int SIZE = 4+Interval.SIZE; // (4bytes+12bytes=16bytes)
//--------on disk--------
	private int y;
	private Interval I;
//-----------------------

	public HInterval(byte[] byteArray) {
		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
		this.y = byteBuf.getInt();
		this.I = new Interval(byteBuf.getInt(), byteBuf.getInt(), byteBuf.getInt());
	}
	
	public HInterval(int y, Interval I) {
		this.y = y;
		this.I = I;
	}
	
	public int getY() {
		return y;
	}
	
	public Interval getI() {
		return I;
	}
	
	public boolean isAdjacent(HInterval hInv) {
		return (this.y == hInv.y && this.I.isAdjacent(hInv.I));
	}
	
	public void mergeWith(HInterval hInv) {
		if (!this.isAdjacent(hInv))
			Debug._Error(this, "Not mergeable");
		else this.I.mergeWith(hInv.getI());
	}
	
	@Override
	public int size() {
		return HInterval.SIZE;
	}
	@Override
	public byte[] toByteArray() {
		ByteBuffer byteBuf = ByteBuffer.allocate(this.size());
		byteBuf.putInt(y);
		byteBuf.putInt(I.s);
		byteBuf.putInt(I.e);
		byteBuf.putInt(I.weight);
		
		return byteBuf.array();
	}
	@Override
	public int compareTo(Record o) {
		
		if (o == null) System.out.println("what???"+this);
		
		if ( this.y == ((HInterval)o).y ) 
			return (this.I.weight-((HInterval)o).I.weight);
		else return (this.y - ((HInterval)o).y);
	}
	
	public String toString() {
		return "(" + y + ", " + I + ")";
	}
	
}
