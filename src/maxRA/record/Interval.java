package maxRA.record;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import maxRA.Env;


/**
 * This class is for main memory as well as external memory.
 * @author Dongwan Choi
 * @date 2011. 11. 30.
 * 
 */
public class Interval extends Record {
	public static final int SIZE = 12; // (4bytes*3=12bytes)

//--------on disk--------
	public int s;
	public int e;
	public int weight; // currently +1 or -1
//-----------------------

	
	public Interval(int s, int e, int weight) {
		this.s = s;
		this.e = e;
		this.weight = weight;
	}
	
	public Interval(byte[] byteArray) {
		ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
		this.s = byteBuf.getInt();
		this.e = byteBuf.getInt();
		this.weight = byteBuf.getInt();
	}
	
	public Interval getWeightIntersect(Interval inv) {
		int s = Math.max(this.s, inv.s);
		int e = Math.min(this.e, inv.e);
		int weight = this.weight + inv.weight;
		
		if ( s < e ) return new Interval(s, e, weight);
		else return null;
	}
	
	public Interval getIntersect(Interval inv) {
		int s = Math.max(this.s, inv.s);
		int e = Math.min(this.e, inv.e);
				
		if ( s < e ) return new Interval(s, e, weight);
		else return null;
	}
	
	public Interval[] getDiff(Interval inv) {
		Interval intersect = this.getWeightIntersect(inv);
		
		if ( intersect == null  ) return new Interval[]{this};
		else {
			ArrayList<Interval> list = new ArrayList<Interval>();
			if (this.s < intersect.s) list.add(new Interval(this.s, intersect.s, this.weight));
			if (intersect.e < this.e) list.add(new Interval(intersect.e, this.e, this.weight));
			Interval [] intervals= new Interval [list.size()];
			list.toArray(intervals);
			return intervals;
		}
	}
	
	public void mergeWith(Interval inv) {
		if (this.s == inv.e) {
			this.s = inv.s;
		}
		else if ( this.e == inv.s ) {
			this.e = inv.e;
		}
	}

	public boolean equals(Interval inv) {
		return (this.s == inv.s && this.e == inv.e);
	}
	public boolean equalsWithWeight(Interval inv) {
		return (this.equals(inv) && this.weight == inv.weight);
	}
	public boolean isAdjacent(Interval inv) {
		return (this.s == inv.e || this.e == inv.s);
	}
	public boolean isMergeable(Interval inv) {
		return (isAdjacent(inv) && this.weight == inv.weight);
	}
	public boolean isCovered(Interval inv) {
		return (this.s >= inv.s && this.e <= inv.e);
	}
	public boolean isDisjoint(Interval inv) {
		return !isIntersect(inv);
	}
	public boolean isIntersect(Interval inv) {
		int s = Math.max(this.s, inv.s);
		int e = Math.min(this.e, inv.e);
		return ( s < e );
	}
	public boolean isCovers(Interval inv) {
		return inv.isCovered(this);
	}
	
	public boolean isCovers(int k) {
		return (this.s <= k && k <= this.e);
	}
	

	@Override
	public int size() {
		return Interval.SIZE;
	}

	@Override
	public byte[] toByteArray() {
		ByteBuffer byteBuf = ByteBuffer.allocate(this.size());
		byteBuf.putInt(s);
		byteBuf.putInt(e);
		byteBuf.putInt(weight);
		
		return byteBuf.array();
	}
	
	public String toString() {
		return "[" + (s==Env.MIN?"-inf":s) + "," + (e==Env.MAX?"+inf":e) + "]," + weight;
	}
	
	public String toString2() {
		return "[" + (s==Env.MIN?"-inf":s) + "," + (e==Env.MAX?"+inf":e) + "]";
	}

	@Override
	public int compareTo(Record o) {
		return (this.s - ((Interval)o).s); // sorting by the left-most point of interval
	}

}
