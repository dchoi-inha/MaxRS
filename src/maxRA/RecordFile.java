package maxRA;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import maxRA.io.*;
import maxRA.record.*;
import maxRA.util.Debug;
import maxRA.util.Util;


/**
 * Class to represent a sequential file
 * 
 * @author Dongwan Choi
 * @date 2011. 12. 4.
 */

public abstract class RecordFile implements Iterable<Record>{
	protected Buffer buf;			// a main-memory buffer for this record file
	protected BlockFile disk;		// a block file connected to this record file
	protected int recordsNum = 0; 	// the total # of records in this record file
	protected int recordSize;		// the size of a record in bytes
	protected String fname;
	
	protected RecordIterator iterator;
		
//	public RecordFile(String fname, int bufSize, int recordSize) throws IOException {
//		this.disk  = new BlockFile(fname);
//		this.buf = new Buffer(bufSize, disk);
//		this.recordSize = recordSize;
//		this.recordsNum = disk.getNumOfRecords();
//	}
	
	public RecordFile(String fname, int bufSize, int recordSize, int recordNumUpBound) throws IOException {
		this.disk  = new BlockFile(fname, recordNumUpBound*recordSize);
		this.buf = new Buffer(bufSize, disk);
		this.recordSize = recordSize;
		this.recordsNum = disk.getNumOfRecords();
		this.fname = fname;
	}
	
	public boolean empty() {
		return this.recordsNum == 0;
	}
	
	public void clear() {
		this.recordsNum = 0;
		disk.setNumOfRecords(recordsNum);
	}

	public int getCurrentRecordNo() {return iterator.curRecordNo;}
	
	/**
	 * insert a record into the file as a last entry
	 * @param e - record to be inserted
	 * @throws IOException
	 */
	public void insert(Record e) throws IOException {
		if ( buf.write(e.toByteArray(), recordsNum, e.size()) ) { 
			recordsNum++;
		}
		else
			Debug._Error(this, "Failed to insert record " + e.toString());
	}
	
	/**
	 * insert a record into the file at given position
	 * @param e - record to be inserted
	 * @param no - (RRN)record number to be inserted
	 * @throws IOException
	 */
	public void insert(Record e, int no) throws IOException {
		if ( no < recordsNum && buf.write(e.toByteArray(), no, e.size()) ); 
		else
			Debug._Error(this, "Failed to insert record " + e.toString());
	}
	
	public abstract Record get(int no) throws IOException;

	public int getNumOfRecords() {
		return recordsNum;
	}
	
	public int getNumOfPages() {
		return disk.getNumOfPages();
	}
	
	public void close() throws IOException {
		buf.flushAll();
		buf.free();
		disk.setNumOfRecords(this.recordsNum);
		disk.close();
	}
	
	public void destroy() throws IOException {
		buf.free();
		disk.destroy();
	}
	
	public void open(int bufSize) throws IOException {
		this.disk  = new BlockFile(fname, recordsNum*recordSize);
		this.buf = new Buffer(bufSize, disk);
	}

	@Override
	public Iterator<Record> iterator() {
		return iterator;
	}
	
	public void renameTo(String newName) {
		this.disk.renameTo(newName);
	}
}

class PointFile extends RecordFile {
//	public PointFile(String fname, int bufSize, int recSize) throws IOException {
//		super(fname, bufSize, recSize);
//		iterator = new PointIterator(buf, disk, recSize);
//	}
	public PointFile(String fname, int bufSize, int recSize, int recNumUpBound) throws IOException {
		super(fname, bufSize, recSize, recNumUpBound);
		iterator = new PointIterator(buf, disk, recSize);
	}
	
	@Override
	public void open(int bufSize) throws IOException {
		super.open(bufSize);
		iterator = new PointIterator(buf, disk, recordSize);
	}
	
	@Override
	public Record get(int no) throws IOException {
		byte [] b = new byte[recordSize];

		if ( buf.read(b, no, recordSize) ) return new Point(b);
		return null;
	}
}

class InvFile extends RecordFile {
//	public InvFile(String fname, int bufSize, int recSize) throws IOException {
//		super(fname, bufSize, recSize);
//		iterator = new InvIterator(buf, disk, recSize);
//	}
	public InvFile(String fname, int bufSize, int recSize, int recNumUpBound) throws IOException {
		super(fname, bufSize, recSize, recNumUpBound);
		iterator = new InvIterator(buf, disk, recSize);
	}
	
	@Override
	public void open(int bufSize) throws IOException {
		super.open(bufSize);
		iterator = new InvIterator(buf, disk, recordSize);
	}
	
	@Override
	public Record get(int no) throws IOException {
		byte [] b = new byte[recordSize];

		if ( buf.read(b, no, recordSize) ) return new Interval(b);
		return null;
	}
	
	public void ramSort() throws IOException {
		int numUsedPages = getNumOfRecords()/(Env.BLOCK_SIZE/recordSize) + 1;
		if (buf.getBufSize() < numUsedPages) {
			Debug._Error(this, "Cannot sort the entire file in the main memory");
			return;
		}
		
		long writeCnt = Env.WriteCount;
		
		ArrayList<Interval> invList = new ArrayList<Interval>();
		for (int i = 0; i < recordsNum; i++) {
			invList.add((Interval)get(i));
		}
		Collections.sort(invList);
		for (int i = 0; i < invList.size(); i++ ) {
			this.insert(invList.get(i), i);
		}
		
		if (writeCnt != Env.WriteCount) {
			Debug._Error(this, "I/O counts error");
		}
	}
}

class HInvFile extends RecordFile {
//	public HInvFile(String fname, int bufSize, int recSize) throws IOException {
//		super(fname, bufSize, recSize);
//		iterator = new HInvIterator(buf, disk, recSize);
//	}
	public HInvFile(String fname, int bufSize, int recSize, int recNumUpBound) throws IOException {
		super(fname, bufSize, recSize, recNumUpBound);
		iterator = new HInvIterator(buf, disk, recSize);
	}
	
	@Override
	public void open(int bufSize) throws IOException {
		super.open(bufSize);
		iterator = new HInvIterator(buf, disk, recordSize);
	}
	
	@Override
	public Record get(int no) throws IOException {
		byte [] b = new byte[recordSize];

		if ( buf.read(b, no, recordSize) ) return new HInterval(b);
		return null;
	}
	
	public void ramSort() throws IOException {
		int numUsedPages = recordsNum/(Env.BLOCK_SIZE/recordSize) + 1;
		if (buf.getBufSize() < numUsedPages) {
			Debug._Error(this, "Cannot sort the entire file in the main memory");
			return;
		}
		
		long writeCnt = Env.WriteCount;
		
		ArrayList<HInterval> hInvList = new ArrayList<HInterval>();
		for (int i = 0; i < recordsNum; i++) {
			hInvList.add((HInterval)get(i));
		}
		Collections.sort(hInvList);
		for (int i = 0; i < hInvList.size(); i++ ) {
			this.insert(hInvList.get(i), i);
		}
		
		if (writeCnt != Env.WriteCount) {
			Debug._Error(this, "I/O counts error");
		}
	}
}

class HLineFile extends RecordFile {
//	public HLineFile(String fname, int bufSize, int recSize) throws IOException {
//		super(fname, bufSize, recSize);
//		iterator = new HLineIterator(buf, disk, recSize);
//	}
	public HLineFile(String fname, int bufSize, int recSize, int recNumUpBound) throws IOException {
		super(fname, bufSize, recSize, recNumUpBound);
		iterator = new HLineIterator(buf, disk, recSize);
	}
	
	@Override
	public void open(int bufSize) throws IOException {
		super.open(bufSize);
		iterator = new HLineIterator(buf, disk, recordSize);
	}


	@Override
	public Record get(int no) throws IOException {
		byte [] b = new byte[recordSize];

		if ( buf.read(b, no, recordSize) ) return new HLine(b);
		return null;
	}
}

abstract class RecordIterator implements Iterator<Record> {
	protected int curRecordNo = 0; 	// indicate the next record to be read
	protected int recordSize;		// the size of a record in bytes
	protected Buffer buf;			// a main-memory buffer for this record file
	protected BlockFile disk;		// a block file connected to this record file
	
	public RecordIterator(Buffer buf, BlockFile disk, int recordSize) {
		this.buf = buf;
		this.disk = disk;
		this.recordSize = recordSize;
	}
	
	@Override
	public boolean hasNext() {
		// whether the current position is the end of file
		return ((Util.getPageNo(curRecordNo, recordSize) < disk.getNumOfPages()) &&
				(curRecordNo < disk.getNumOfRecords()));
	}

	@Override
	public abstract Record next();

	@Override
	public void remove() {
	}
}

class PointIterator extends RecordIterator {
	public PointIterator(Buffer buf, BlockFile disk, int recordSize) {
		super(buf, disk, recordSize);
	}

	@Override
	public Record next() {
		byte [] b = new byte[recordSize];
		
		try {
			if ( buf.read(b, curRecordNo, recordSize) ) {
				curRecordNo++;
				return new Point(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}

class InvIterator extends RecordIterator {
	public InvIterator(Buffer buf, BlockFile disk, int recordSize) {
		super(buf, disk, recordSize);
	}

	@Override
	public Record next() {
		byte [] b = new byte[recordSize];
		
		try {
			if ( buf.read(b, curRecordNo, recordSize) ) {
				curRecordNo++;
				return new Interval(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}

class HInvIterator extends RecordIterator {
	public HInvIterator(Buffer buf, BlockFile disk, int recordSize) {
		super(buf, disk, recordSize);
	}

	@Override
	public Record next() {
		byte [] b = new byte[recordSize];
		
		try {
			if ( buf.read(b, curRecordNo, recordSize) ) {
				curRecordNo++;
				return new HInterval(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}

class HLineIterator extends RecordIterator {
	public HLineIterator(Buffer buf, BlockFile disk, int recordSize) {
		super(buf, disk, recordSize);
	}

	@Override
	public Record next() {
		byte [] b = new byte[recordSize];
		
		try {
			if ( buf.read(b, curRecordNo, recordSize) ) {
				curRecordNo++;
				return new HLine(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
