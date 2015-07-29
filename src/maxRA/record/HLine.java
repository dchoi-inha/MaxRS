package maxRA.record;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * @author Dongwan Choi
 * @date 2011. 12. 1.
 */

/*
 * Currently, this is the same as HInterval.
 * However, this can be extended to support Max_k_RA problem.
 * In that case, maxI can be an array of Intervals.
 * commented by DW Choi (2012.01.09) 
 */
public class HLine extends Record{
	public static final int SIZE = 4+Interval.SIZE; // (4bytes+12bytes=16bytes)
//--------on disk--------
	private int y;
	private Interval maxI;
//-----------------------

	public HLine(byte[] byteArray) {
		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
		this.y = byteBuf.getInt();
		this.maxI = new Interval(byteBuf.getInt(), byteBuf.getInt(), byteBuf.getInt());	}
	
	public HLine(int y, Interval I) {
		this.y = y;
		this.maxI = I;
	}
	
	@Override
	public int size() {
		return HLine.SIZE;
	}
	@Override
	public byte[] toByteArray() {
		ByteBuffer byteBuf = ByteBuffer.allocate(this.size());
		byteBuf.putInt(y);
		byteBuf.putInt(maxI.s);
		byteBuf.putInt(maxI.e);
		byteBuf.putInt(maxI.weight);
		
		return byteBuf.array();	
	}
	
	public String toString() {
		return "(" + y + ", " + maxI + ")\n";
	}

	public int getY() {
		return y;
	}

	public Interval getMaxI() {
		return maxI;
	}

	@Override
	public int compareTo(Record o) {
		return 0;
	}
}
