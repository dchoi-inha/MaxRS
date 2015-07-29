package maxRA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.PriorityQueue;

import maxRA.record.*;
import maxRA.util.Debug;

public class ExMergeSort {
	
	public static void exMergeSort (RecordFile srcFile) throws IOException {
		String fname = srcFile.fname;
		int level = 0;
		int k = Env.MEM_SIZE - 2; // one for the input buffer(i.e., srcFile), another for the header block
		int numRuns = srcFile.getNumOfPages()/k + 1; // '+1' is for ceiling
		int numRecordsRun = k*(Env.BLOCK_SIZE/srcFile.recordSize);
		ArrayList<RecordFile> runs = new ArrayList<RecordFile>();
		
		// make initial runs
		for (int i = 0; i < numRuns; i++) {
			if (srcFile instanceof HInvFile) {
				runs.add(new HInvFile("run_"+level+"_"+i, k+1, HInterval.SIZE, numRecordsRun));
				for (int j = i*numRecordsRun; j < (i+1)*numRecordsRun && j < srcFile.getNumOfRecords(); j++) {
					runs.get(i).insert(srcFile.get(j));
				}
				((HInvFile)runs.get(i)).ramSort();
				runs.get(i).close(); // flush 
			}
			else if (srcFile instanceof InvFile) {
				runs.add(new InvFile("run_"+level+"_"+i,k+1, Interval.SIZE, numRecordsRun));
				for (int j = i*numRecordsRun; j < (i+1)*numRecordsRun && j < srcFile.getNumOfRecords(); j++) {
					runs.get(i).insert(srcFile.get(j));
				}
				((InvFile)runs.get(i)).ramSort();
				runs.get(i).close(); // flush
			}
		}
		srcFile.close();
		
		// merging phase
		while (runs.size() > 1) {
			level++;
			ArrayList<RecordFile> nextRuns = new ArrayList<RecordFile>();
			for ( int i = 0, j = 0; i < runs.size(); i++) {
				if ( i == runs.size()-1 || i-j == k-1 ) {
					ArrayList<RecordFile> mergingRuns = new ArrayList<RecordFile>(runs.subList(j, i+1));
					nextRuns.add(merge(mergingRuns, k, level, j));
					j = i;
				}
			}
			runs = nextRuns;
		}
		
		if ( runs.size() != 1 ) {
			Debug._Error(ExMergeSort.class, "failed to merge runs");
		}
		else {
			runs.get(0).renameTo(fname);
		}
	}
	
	private static RecordFile merge(ArrayList<RecordFile> runs, int k, int level, int postFix) throws IOException {
		if ( runs.size() > k ) {
			Debug._Error(ExMergeSort.class, "too many runs");
			return null;
		}
		int runBufSize = Env.FREE_MEM_SIZE / (runs.size()+1);		
		
		PriorityQueue<QElement> pQueue = new PriorityQueue<QElement>();
		int numRecordsRun = 0;
		for ( RecordFile run : runs ) {
			run.open(runBufSize);
			numRecordsRun += run.recordsNum;
		}
		
		RecordFile mergedRun = null;
		if (runs.get(0) instanceof HInvFile) {
			mergedRun = new HInvFile("run_"+level+"_"+postFix, runBufSize, HInterval.SIZE, numRecordsRun);
		}
		else if (runs.get(0) instanceof InvFile) {
			mergedRun = new InvFile("run_"+level+"_"+postFix, runBufSize, Interval.SIZE, numRecordsRun);
		}
		
		for (int i = 0; i < runs.size(); i++) {
			if (runs.get(i).iterator.hasNext())
				pQueue.add(new QElement(runs.get(i).iterator.next(), i));
		}
		while (!pQueue.isEmpty()) {
			QElement winner = pQueue.poll();
			mergedRun.insert(winner.record);
			if (runs.get(winner.index).iterator.hasNext()){
				pQueue.add(new QElement(runs.get(winner.index).iterator.next(), winner.index));
			}
		}
		
		for (RecordFile run : runs) run.destroy();
		mergedRun.close();
		return mergedRun;
	}
}

class QElement implements Comparable<QElement> {
	public Record record;
	public int index;
	
	public QElement(Record record, int index) {
		this.record = record;
		this.index = index;
	}
	
	@Override
	public int compareTo(QElement o) {
		return record.compareTo(o.record);
	}
	
}
