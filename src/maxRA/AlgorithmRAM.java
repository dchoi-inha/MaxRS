package maxRA;

import java.util.ArrayList;
import java.util.Iterator;

import maxRA.record.*;
import maxRA.util.*;

public class AlgorithmRAM {
	private ArrayList<HInterval> inputSet;
	private ArrayList<HLine> outputSet;
	private Interval space;

	public AlgorithmRAM(ArrayList<HInterval> inputSet, Interval space)  {
		this.inputSet = inputSet; 
		this.outputSet = new ArrayList<HLine>();
		this.space = space;
	}

	/**
	 * @return a rectangular region such that maximizes the number of points covered by the region
	 * @throws IOException
	 */
	public void exactMaxRS()   {
		findMaxIntervals(inputSet, outputSet, space);
	}

	public ArrayList<HLine> getOutputSet(){
		return outputSet;
	}



	private void findMaxIntervals(ArrayList<HInterval> src, ArrayList<HLine> dst, Interval slab)   {

		if (src.size() > 1000) // Divide & Merge
		{
			ArrayList<HInterval> [] rectFiles = new ArrayList[2];
			ArrayList<HLine> [] slabFiles = new ArrayList[2];
			int seperator = (slab.s + slab.e) / 2;
			Interval [] slabs = new Interval [2];
			for ( int i = 0; i < 2; i++ ) {
				rectFiles[i] = new ArrayList<HInterval>();
				slabFiles[i] = new ArrayList<HLine>();
			}
			ArrayList<HInterval> spanRectFile = new ArrayList<HInterval>();


			slabs[0] = new Interval(slab.s, seperator, 0);
			slabs[1] = new Interval(seperator, slab.e, 0);

			for (HInterval hInv: src) {
				for(int i = 0; i < slabs.length; i++) {
					if (hInv.getI().isIntersect(slabs[i])) {
						if(!slabs[i].isCovered(hInv.getI())) { 
							HInterval intersect = new HInterval(hInv.getY(), hInv.getI().getIntersect(slabs[i]));
							rectFiles[i].add(intersect);
						}
						else {
							HInterval spanHInv = new HInterval(hInv.getY(), new Interval(slabs[i].s, slabs[i].e, hInv.getI().weight)); 
							i++;
							while (i < slabs.length && slabs[i].isCovered(hInv.getI())) {
								spanHInv.mergeWith(new HInterval(hInv.getY(), new Interval(slabs[i].s, slabs[i].e, hInv.getI().weight)));
								i++;
							}
							spanRectFile.add(spanHInv);
							i--;
						}

					}
				}
			}

			// do recursive process for each slab
			for ( int i = 0; i < 2; i++ ) {
				findMaxIntervals(rectFiles[i], slabFiles[i], slabs[i]);
			}

			mergeSweep(dst, slabFiles, slabs, spanRectFile); // This is a key part of the algorithm.
		}

		else if (src.size() > 0) // leaf-level
		{

			// Find max intervals for each hLine
			ArrayList<Interval> intervals = new ArrayList<Interval>();
			Interval curMaxI = slab; 
			int prevY = src.get(0).getY();
			intervals.add(slab);

			for (HInterval hInv: src) {
				if(prevY != hInv.getY()) {
					curMaxI = getMaxInterval(intervals);
					dst.add(new HLine(prevY, curMaxI));
					prevY = hInv.getY();
				}
				int intervalsNum = intervals.size();
				for(int i=0; i<intervalsNum; i++) {
					Interval inv = intervals.get(i);
					if(inv.isIntersect(hInv.getI())) {
						Interval intersect = inv.getWeightIntersect(hInv.getI());
						intervals.set(i, intersect);
						if ( !inv.isCovered(hInv.getI()) ) {
							for (Interval restInv: inv.getDiff(hInv.getI())) {
								intervals.add(restInv);
							}
						}
					}
				}

				if (hInv.getI().weight < 0) 
					mergeIntervals(intervals);
			}

			curMaxI = getMaxInterval(intervals);
			dst.add(new HLine(prevY, curMaxI));
		}
	}

	private void mergeSweep(ArrayList<HLine> dstFile, ArrayList<HLine>[] slabFiles,
			Interval[] slabs, ArrayList<HInterval> upperFile)  {
		//		Debug._PrintL("\n***************"+slabs.toString()+"******************");
		Interval maxInvX = new Interval(space.s, space.e, 0);
		Interval maxInvY = new Interval(Env.MIN, Env.MAX, 0);


		// merge process with sweeping a horizontal line
		int [] upCounts = new int [2];
		Interval [] curMaxInvs = new Interval [2];
		SweepingLineRam sweepLine = new SweepingLineRam(slabFiles, upperFile);

		// initialize current max interval for each slab to be the slab range
		for (int i=0; i < curMaxInvs.length; i++)	curMaxInvs[i] = slabs[i];

		int prevY = Env.MIN;
		while (sweepLine.hasNext()) {
			Pair<Integer, Record> pair = sweepLine.next();
			Record r = pair.p2;
			if (r instanceof HLine) { // when the line meets the tuple of slab file
				HLine hLine = (HLine) r;
				int slabIndex = pair.p1;

				if (prevY != Env.MIN && prevY != hLine.getY()) {
					Interval maxInv = getMaxInterval(curMaxInvs, upCounts);
					HLine prevHLine = null;
					if(!dstFile.isEmpty()) prevHLine = dstFile.get(dstFile.size()-1);
					if (prevHLine == null || !prevHLine.getMaxI().equalsWithWeight(maxInv)) {
						HLine insertedHLine = new HLine(prevY, maxInv);
						dstFile.add(insertedHLine);

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
					HLine prevHLine = null;
					if(!dstFile.isEmpty()) prevHLine = dstFile.get(dstFile.size()-1);
					if (prevHLine == null || !prevHLine.getMaxI().equalsWithWeight(maxInv)) {
						HLine insertedHLine = new HLine(prevY, maxInv);
						dstFile.add(insertedHLine);	

						if (maxInvX.weight < maxInv.weight) {
							maxInvX = maxInv;
							maxInvY = new Interval(prevY, space.e, maxInv.weight);
						}
						else if (maxInvX.weight > maxInv.weight && prevY < maxInvY.e) 
							maxInvY.e = prevY;
					}
				}
				// update upper level counts according to the current spanning interval
				for (int i = 0; i < 2; i++) { 
					if(slabs[i].isCovered(hInv.getI()))
						upCounts[i] += hInv.getI().weight;
				}
				prevY = hInv.getY();
			}
		}

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
class SweepingLineRam implements Iterator<Pair<Integer,Record>> {
	private ArrayList<HLine> [] slabFiles;
	private ArrayList<HInterval> upperFile;
	private HLine [] curHLines;
	private HInterval upperHInv;
	private Iterator<HInterval> upperFileIterator;
	Iterator<HLine>[] slabFileIterators;

	public SweepingLineRam(ArrayList<HLine> [] slabFiles, ArrayList<HInterval> upperFile) {
		this.slabFiles = slabFiles;
		this.upperFile = upperFile;
		this.curHLines = new HLine[slabFiles.length];
		upperFileIterator = upperFile.iterator();
		slabFileIterators = new Iterator[2];
		if ( upperFileIterator.hasNext())
			this.upperHInv = upperFileIterator.next();
		for (int i=0; i < curHLines.length; i++) {
			slabFileIterators[i] = slabFiles[i].iterator();
			if (slabFileIterators[i].hasNext())
				curHLines[i] = (HLine) slabFileIterators[i].next();
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
			if (upperFileIterator.hasNext()) 
				upperHInv = upperFileIterator.next();
			else upperHInv = null;
		}
		else {
			if (slabFileIterators[maxIndex].hasNext()) 
				curHLines[maxIndex] = (HLine)slabFileIterators[maxIndex].next();
			else curHLines[maxIndex] = null;
		}

		return new Pair<Integer, Record>(maxIndex, bottom);
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
	}

}