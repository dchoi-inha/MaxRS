package maxRA;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import maxRA.record.*;
import maxRA.util.*;

public class AlgorithmEX {
	
	private static int a=0;
	private String rectFname;
	private String pointFname;
	private int level;
	
	public AlgorithmEX(String rectFname, String pointFname) throws IOException {
		this.rectFname = rectFname;
		this.pointFname = pointFname;
	}
	
	/**
	 * @return a rectangular region such that maximizes the number of points covered by the region
	 * @throws IOException
	 */
	public Rect exactMaxRS() throws IOException {
		Debug._PrintL("-------------------------------------------------------------------------------\r\nExactMaxRS Algorithm");
		int s = Env.M/Env.B - 3; //estimate s according to memory size and block size
		
		if ( s < 2 ) {
			Debug._Error(this, "Too small buffer: s should be larger than or equal to 2.");
			return null;
		}
		int n = 3*Env.MAX_RECORDS_NUM/Env.M;
		Debug._Print("M/B:"+s+"\t");
		for (int i = 1; i < 100; i++){
			if (s > Math.pow((double)n, 1.0/(double)i) && s <= (i-1>0?Math.pow((double)n, 1.0/(double)(i-1)):s)) {
				s = (int)(Math.pow(n, 1.0/(double)i));
				break;
			}
		}
		
		s = Math.max(s, 4);
		
//		if ( s > Env.MAX_RECORDS_NUM*3 / Env.M ) {
//			s = Env.MAX_RECORDS_NUM*3 / Env.M;
//		}
		Env.SLAB_BUF_SIZE = (Env.M/Env.B)/(s+3); // "divided by 's+3'" this is correct!!
		
		Debug._PrintL("# of slabs:" + s);
		String finDstFname = "slab_final";
		File finDstFile = new File(Env.HomeDir + finDstFname);
		finDstFile.deleteOnExit();
		
		return findMaxIntervals(rectFname, finDstFname, new Interval(Env.MIN, Env.MAX, 0), s);
	}
	
	public Circle approxMaxCRS() throws IOException {
		Debug._PrintL("-------------------------------------------------------------------------------\r\nApproxMaxCRS Algorithm");
		Point p_0 = exactMaxRS().getCenter();
		Circle c_0 = p_0.getCircle();
		Rect r_0 = p_0.getRect();
		
		Point [] p_i = p_0.getShiftPoints(Env.SHIFT_NUM);
		Circle [] c_i = new Circle[p_i.length];
		for(int i=0; i < c_i.length; i++ ) c_i[i] = p_i[i].getCircle();
		
		RecordFile pointFile = new PointFile(pointFname, Env.FREE_MEM_SIZE, Point.SIZE, Env.MAX_RECORDS_NUM);
		for (Record r: pointFile) {
			Point p = (Point)r;
			if(c_0.contain(p)) c_0.incrementWeight();
			for(int i=0; i < c_i.length; i++ ) 
				if(c_i[i].contain(p)) c_i[i].incrementWeight();
			if(r_0.contain(p)) r_0.incrementWeight();
		}
		
		Debug._PrintL("r0:"+r_0.toString());
		Debug._PrintL("c0:"+c_0.toString());
		for(int i=0; i < c_i.length; i++ ) Debug._PrintL("c"+(i+1)+":"+c_i[i].toString());
		
		Circle maxCircle = c_0;
		for(int i=0; i < c_i.length; i++ ) 
			if(maxCircle.getWeight() < c_i[i].getWeight() )
				maxCircle = c_i[i];
		
		pointFile.close();
		return maxCircle;
	}

	
	/**
	 * @param srcFile - source file which contains a set of rectangles(actually horizontal segments)
	 * @param dstFile - destination file which should contain a set of max intervals for each horizontal line(HLine object)
	 * @param slab - a space (interval) to which every rectangle of the source file belongs 
	 * @param s - the # of partitions
	 * @return a max region in the destination file
	 * @throws IOException
	 */
	private Rect findMaxIntervals(String srcFname, String dstFname, Interval slab, int s) throws IOException {
		Rect maxRect = null;
		
		level++;
		RecordFile srcFile = new HInvFile(srcFname, Env.SLAB_BUF_SIZE, HInterval.SIZE, Env.MAX_RECORDS_NUM*2); // sorted by y-coordinate		
		RecordFile xSortedFile = new InvFile(rectFname.replace(".bin", ".bin2"), Env.SLAB_BUF_SIZE, Interval.SIZE, Env.MAX_RECORDS_NUM*2); // sorted by x-coordinate

		if (srcFile.getNumOfPages() > Env.FREE_MEM_SIZE) // external-memory algorithm
		{
		
			String [] slabFnames = new String[s];
			String [] rectFnames = new String[s];
			RecordFile[] rectFiles = new RecordFile[s];
			ArrayList<Integer> seperators = doKSelection(xSortedFile, srcFile.getNumOfRecords(), slab, s);
			ArrayList<Interval> slabs = new ArrayList<Interval>();
			for ( int i = 0; i < s; i++ ) {
				rectFnames[i] = "rect_"+level+"_"+i;
				rectFiles[i] = new HInvFile(rectFnames[i], Env.SLAB_BUF_SIZE, HInterval.SIZE, srcFile.getNumOfRecords());
			}
			String spanFName = "span_"+level;
			RecordFile spanRectFile = new HInvFile(spanFName, Env.SLAB_BUF_SIZE, HInterval.SIZE, srcFile.getNumOfRecords());
			
//			Debug._PrintL("+++++++++++++++++++++"+slabs.toString()+"++++++++++++++++++++++++");

			// divide a source file into s sub files
			if (seperators != null) { // when intervals can be distributed normally
				slabs = getIntervalsFromKeys(seperators, slab);
				for (Record r: srcFile) {
					HInterval hInv = (HInterval)r;
//					Debug._Print("\ntuple->"+hInv.toString());
					for(int i = 0; i < slabs.size(); i++) {
						if (hInv.getI().isIntersect(slabs.get(i))) {
							if(!slabs.get(i).isCovered(hInv.getI())) { 
								HInterval intersect = new HInterval(hInv.getY(), hInv.getI().getIntersect(slabs.get(i)));
								rectFiles[i].insert(intersect);

//								Debug._PrintL(" intersect->"+intersect+" slab->"+slabs.get(i).toString());
							}
							else {
								HInterval spanHInv = new HInterval(hInv.getY(), new Interval(slabs.get(i).s, slabs.get(i).e, hInv.getI().weight)); 
								i++;
								while (i < slabs.size() && slabs.get(i).isCovered(hInv.getI())) {
									spanHInv.mergeWith(new HInterval(hInv.getY(), new Interval(slabs.get(i).s, slabs.get(i).e, hInv.getI().weight)));
									i++;
								}
								spanRectFile.insert(spanHInv);
//								Debug._PrintL(" spanning interval->"+spanHInv);
								i--;
							}

						}
					}
				}
			}
			else { // when intervals are duplicated, which leads to abnormal distribution
				for (int i = 0; i < s; i++ ) slabs.add(slab);
				int rrn = 0;
				for (Record r: srcFile) {
					HInterval hInv = (HInterval)r;
					rectFiles[rrn%s].insert(hInv);
					rrn++;
				}
			}
			
			// free buffer memory by closing record files to re-use memory in the next recursive call
			for (int i = 0; i < s; i++ ) {
				rectFiles[i].close(); 
//				Debug._Print(rectFiles[i].recordsNum+"\t");
			}
			spanRectFile.close();
			srcFile.close();
			xSortedFile.close();
			
			
			// do recursive process for each slab
			for ( int i = 0; i < slabs.size(); i++ ) {
				slabFnames[i] = "slab_"+level+"_"+i;
				findMaxIntervals(rectFnames[i], slabFnames[i], slabs.get(i), s);
			}
			
			maxRect = mergeSweep(dstFname, slabFnames, slabs, spanFName, srcFile.getNumOfRecords()); // This is a key part of the algorithm.
			
			for ( int i = 0; i < s; i++ ) {
				new File(Env.HomeDir+rectFnames[i]).delete();
			}
		}
		
		else // in-memory algorithm 
		{
			long start, end;
			
			if ( srcFile.recordsNum > 0) {
				srcFile.close();
				srcFile = new HInvFile(srcFname, Env.FREE_MEM_SIZE, HInterval.SIZE, srcFile.getNumOfRecords()); // Load srcFile into memory
				ArrayList<HInterval> srcList = new ArrayList<HInterval>();
				for (Record r: srcFile)	srcList.add((HInterval)r);

//				start = System.currentTimeMillis();
				AlgorithmRAM algRam = new AlgorithmRAM(srcList, slab);
				algRam.exactMaxRS();
//				end = System.currentTimeMillis();
//				Debug._PrintL("Elapsed time: "+(end-start)/1000.0+" secs");
				
				srcFile.destroy();
				// Write results into dstFile
				RecordFile dstFile = new HLineFile(dstFname, Env.FREE_MEM_SIZE, HLine.SIZE, srcFile.getNumOfRecords());
				for(HLine hLine:algRam.getOutputSet()) dstFile.insert(hLine);
				dstFile.close();
			}
			else	srcFile.close();
			
			xSortedFile.close();
		}
		
		return maxRect;
		
	}
	
	private Rect mergeSweep(String dstFname, String[] slabFnames,
			ArrayList<Interval> slabs, String upperFname, int sourceFileLen) throws IOException {
//		Debug._PrintL("\n***************"+slabs.toString()+"******************");
		Interval maxInvX = new Interval(Env.MIN, Env.MAX, 0);
		Interval maxInvY = new Interval(Env.MIN, Env.MAX, 0);
				
		
		RecordFile dstFile = new HLineFile(dstFname, Env.SLAB_BUF_SIZE, HLine.SIZE, sourceFileLen);
		RecordFile [] slabFiles = new RecordFile[slabs.size()];
		RecordFile upperFile = new HInvFile(upperFname, Env.SLAB_BUF_SIZE, HInterval.SIZE, sourceFileLen);
		for (int i = 0; i < slabs.size(); i++ ) {
			slabFiles[i] = new HLineFile(slabFnames[i], Env.SLAB_BUF_SIZE, HInterval.SIZE, sourceFileLen);
		}
		
		// merge process with sweeping a horizontal line
		int [] upCounts = new int [slabs.size()];
		Interval [] curMaxInvs = new Interval [slabs.size()];
		SweepingLine sweepLine = new SweepingLine(slabFiles, upperFile);
		
		// initialize current max interval for each slab to be the slab range
		for (int i=0; i < curMaxInvs.length; i++)	curMaxInvs[i] = slabs.get(i);
		
		int prevY = Env.MIN;
		while (sweepLine.hasNext()) {
			Pair<Integer, Record> pair = sweepLine.next();
			Record r = pair.p2;
			if (r instanceof HLine) { // when the line meets the tuple of slab file
				HLine hLine = (HLine) r;
				int slabIndex = pair.p1;

				if (prevY != Env.MIN && prevY != hLine.getY()) {
					Interval maxInv = getMaxInterval(curMaxInvs, upCounts);					
					HLine prevHLine = (HLine)dstFile.get(dstFile.getNumOfRecords()-1);
					if (prevHLine == null || !prevHLine.getMaxI().equalsWithWeight(maxInv)) {
						HLine insertedHLine = new HLine(prevY, maxInv);
//						Debug._Print(insertedHLine.toString());
						dstFile.insert(insertedHLine);
						
						if (maxInvX.weight < maxInv.weight) {
							maxInvX = maxInv;
							maxInvY = new Interval(prevY, Env.MAX, maxInv.weight);
						}
						else if (maxInvX.weight > maxInv.weight && prevY < maxInvY.e) 
							maxInvY.e = prevY;
					}
				}
				// update max interval for the corresponding slab
				curMaxInvs[slabIndex] = hLine.getMaxI();				
				prevY = hLine.getY();
			}
			else if (r instanceof HInterval) { // when the line meets the spanning rectangle
				HInterval hInv = (HInterval) r;
				 
				if (prevY != Env.MIN && prevY != hInv.getY()) {
					Interval maxInv = getMaxInterval(curMaxInvs, upCounts);					
					HLine prevHLine = (HLine)dstFile.get(dstFile.getNumOfRecords()-1);
					if (prevHLine == null || !prevHLine.getMaxI().equalsWithWeight(maxInv)) {
						HLine insertedHLine = new HLine(prevY, maxInv);
//						Debug._Print(insertedHLine.toString());
						dstFile.insert(insertedHLine);	
						
						if (maxInvX.weight < maxInv.weight) {
							maxInvX = maxInv;
							maxInvY = new Interval(prevY, Env.MAX, maxInv.weight);
						}
						else if (maxInvX.weight > maxInv.weight && prevY < maxInvY.e) 
							maxInvY.e = prevY;
					}
				}
				// update upper level counts according to the current spanning interval
				for (int i = 0; i < slabs.size(); i++) { 
					if(slabs.get(i).isCovered(hInv.getI()))
						upCounts[i] += hInv.getI().weight;
				}
				prevY = hInv.getY();
			}
		}
		
		// free buffer memory by closing record files to re-use
		for (int i = 0; i < slabFiles.length; i++ ) {slabFiles[i].destroy();}
		upperFile.destroy();
		dstFile.close();
		
		
		return new Rect(new Point(maxInvX.s, maxInvY.s), new Point(maxInvX.e, maxInvY.e), maxInvX.weight);
	}
	

	
	private ArrayList<Integer> doKSelection(RecordFile invFile, int endPtsNum, Interval slab, int k) throws IOException {
		ArrayList<Integer> sepList = new ArrayList<Integer>();
		int gap = endPtsNum/k;
		int cnt = 0;
		for(Record r: invFile) {
			Interval inv = (Interval)r;
			if(inv.s < slab.s) continue;
			else if (inv.s > slab.e) break;
			else if (sepList.size() == k-1) break;
			else {
				if( cnt > 0 && cnt%gap == 0 ) {
					sepList.add(inv.s);
				}
				cnt++;
			}
		}
		if ( !sepList.isEmpty() && sepList.get(0) < sepList.get(sepList.size()-1)) return sepList;
		else return null;
	}
	
	private ArrayList<Interval> getIntervalsFromKeys(ArrayList<Integer> keyList, Interval slab) {
		ArrayList<Interval> invList = new ArrayList<Interval>();
		
		int start = slab.s;
		for(int i = 0; i < keyList.size(); i++) {
			invList.add(new Interval(start, keyList.get(i), 0));
			start = keyList.get(i);
		}
		if ( start < slab.e )	
			invList.add(new Interval(start, slab.e, 0));
		
		return invList;
	}
	
	private void mergeIntervals(ArrayList<Interval> intervals) {
		for(int i = 0; i < intervals.size() ; i++) {
			for (int j = 0; j < intervals.size(); j++) {
				if(i==j || i >= intervals.size()) continue;
				if(intervals.get(i).isMergeable(intervals.get(j))) {
					intervals.get(i).mergeWith(intervals.get(j));
					intervals.remove(j);
				}
			}
		}
	}
	
	private Interval getMaxInterval(ArrayList<Interval> intervals) {
		Interval maxI = null;
		
		for (Interval inv:intervals) {
			if(maxI == null || maxI.weight < inv.weight) 
				maxI = inv;
			else if ( maxI.weight == inv.weight && maxI.isAdjacent(inv)) {
				maxI.mergeWith(inv);
			}
		}
		return maxI;
	}
	
	private Interval getMaxInterval(Interval [] curMaxInvs, int [] upCounts ) {
		Interval maxInv = null; // upper level count also need to be added to this max interval's weight
		
//		Debug._Print("\n");
		for (int i = 0; i < curMaxInvs.length; i++) {
//			Debug._Print(curMaxInvs[i].toString() + "(" + upCounts[i]+") | ");
			
			int weight = (curMaxInvs[i].weight) + upCounts[i];
			if (maxInv == null || weight > maxInv.weight) {
				maxInv = new Interval(curMaxInvs[i].s, curMaxInvs[i].e, weight);
			}
			else if ( weight == maxInv.weight && curMaxInvs[i].isAdjacent(maxInv) ) {
				maxInv.mergeWith(curMaxInvs[i]);
			}
		}
		return maxInv;
	}
	
}
	
	
	
	
	
	
	
/**
 * Class for the sweeping line.
 * For each round, return a pair which consists of the slab index and the next lowest record(interval).
 * @author Dongwan Choi
 * @date 2012. 1. 26.
 */
class SweepingLine implements Iterator<Pair<Integer,Record>> {
	private RecordFile [] slabFiles;
	private RecordFile upperFile;
	private HLine [] curHLines;
	private HInterval upperHInv;

	public SweepingLine(RecordFile [] slabFiles, RecordFile upperFile) {
		this.slabFiles = slabFiles;
		this.upperFile = upperFile;
		this.curHLines = new HLine[slabFiles.length];
		
		if ( upperFile.iterator.hasNext())
			this.upperHInv = (HInterval) upperFile.iterator.next();
		for (int i=0; i < curHLines.length; i++) {
			if (slabFiles[i].iterator.hasNext())
				curHLines[i] = (HLine) slabFiles[i].iterator.next();
		}
	}
	
	@Override
	public boolean hasNext() {
		for(int i = 0; i < curHLines.length; i++) {
			if (curHLines[i] != null) return true;
		}
		return (upperHInv != null);
	}

	@Override
	public Pair<Integer, Record> next() {
		Record bottom = null;
		int i, maxIndex = -1;
		for(i=0; i < curHLines.length; i++) {
			if(curHLines[i] != null && (bottom == null || ((HLine)bottom).getY() > curHLines[i].getY())) {
				bottom = curHLines[i];
				maxIndex = i;
			}
		}
		
		if (upperHInv != null && (maxIndex < 0 || ((HLine)bottom).getY() > upperHInv.getY())) {
			bottom = upperHInv; 
			maxIndex = -1; // don't need the slab index in the case of spanning intervals which can cover multiple slabs 
			if (upperFile.iterator.hasNext()) 
				upperHInv = (HInterval)upperFile.iterator.next();
			else upperHInv = null;
		}
		else {
			if (slabFiles[maxIndex].iterator.hasNext()) 
				curHLines[maxIndex] = (HLine)slabFiles[maxIndex].iterator.next();
			else curHLines[maxIndex] = null;
		}
		
		return new Pair<Integer, Record>(maxIndex, bottom);
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
	}
	
}