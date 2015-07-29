package maxRA.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import maxRA.Env;
import maxRA.util.Debug;
import maxRA.util.Util;
/**
 * USED - Read but not modified<br>
 * DIRTY - Read and modified<br>
 * NEW - Newly inserted
 */
enum State {FREE, USED, NEW, DIRTY}
/**
 * @author Dongwan Choi
 * @date 2011. 12. 4.
 */
public class Buffer {
	// Let's say 'a page' is a block in the external-memory
	// and 'a frame' is a block in the main-memory
	public final static int INVALID_NO = -1;
	private BlockFile disk;						// a block file connected to this buffer
	private Page [] frames;						// memory buffer
	private int [] inPageNo;					// corresponding page No. for each frame
	private State [] state;						// the state of each frame
	private HashMap<Integer, Integer> pageMap;  // <page No, frame No>
	private int bufSize;			// the size of this buffer in pages
	private int freeFrames; 		// the size of free space of the buffer in pages
	
	private Queue<Integer> usedFrameQ;
	
	public Buffer(int size, BlockFile blkFile) {
		if ( Env.FREE_MEM_SIZE < size ) {
			Debug._Error(this, "not enough free pages, free:"+Env.FREE_MEM_SIZE+", requested:"+size);
			return;
		}
		
		disk = blkFile;
		bufSize = size;
		freeFrames = bufSize;
		Env.FREE_MEM_SIZE -= bufSize; // reduce the free memory size as newly allocated size
		frames = new Page [bufSize];
		inPageNo = new int [bufSize];
		state = new State [bufSize];
		for (int i=0; i < bufSize; i++) { 
			frames[i] = new Page();
			inPageNo[i] = INVALID_NO;
			state[i] = State.FREE;
		}
		pageMap = new HashMap<Integer, Integer>();
		usedFrameQ = new ArrayBlockingQueue<Integer>(bufSize);
	}
	public boolean read(byte[] b, int pageNo, int offset, int len) throws IOException {
		if (pageNo <= 0) {
			Debug._Error(this, "page No " + pageNo +" is not correct.");
			return false;
		}
		
		if ( inBuffer(pageNo) ) {
			int frameNo = pageMap.get(pageNo);
			return frames[frameNo].read(b, offset, len);
		}
		else {
			int no = getNextFreeFrameNo();
			if ( no == INVALID_NO ) {
				Debug._Error(this, "no available free frame");
				return false;
			}
			else {
				if (disk.readPage(pageNo, frames[no])) {
					state[no] = State.USED;
					usedFrameQ.add(no);
					inPageNo[no] = pageNo;
					pageMap.put(pageNo, no);
					freeFrames--;
					
					return frames[no].read(b, offset, len);
				}
				else return false;
			}
		}
	}
	
	public boolean read(byte[] b, int pageNo) throws IOException {
		return read(b, pageNo, 0, Env.BLOCK_SIZE);
	}
	
	public boolean read(byte[] b, int index, int len) throws IOException {
		int pageNo = Util.getPageNo(index, len);
		int offset = Util.getOffset(index, len);
		
		return read(b, pageNo, offset, len);
	
	}
	
	public boolean write(byte[] b, int pageNo, int offset, int len) throws IOException {
		if (pageNo <= 0) {
			Debug._Error(this, "page No " + pageNo +" is not correct.");
			return false;
		}
		
		if( inBuffer(pageNo) ) {
			int no = pageMap.get(pageNo);
			if ( frames[no].write(b, offset, len) ) {
				if ( state[no] == State.USED ) {
					usedFrameQ.remove(no);
					state[no] = State.DIRTY;
				}
				return true;
			}
		}
		else {
			int no = getNextFreeFrameNo();
			if ( no == INVALID_NO ) {
				Debug._Error(this, "no available free frame");
				return false;
			}
			else {
				if (disk.isAllocated(pageNo)) {
				// Already allocated page
					if (disk.readPage(pageNo, frames[no])) {
						state[no] = State.USED;
						usedFrameQ.add(no);
						inPageNo[no] = pageNo;
						pageMap.put(pageNo, no);
						freeFrames--;
						return write(b, pageNo, offset, len);
					}
				}
				else {
				// Newly inserted page
					if (frames[no].write(b, offset, len)) {
						state[no] = State.NEW;
						pageMap.put(pageNo, no);
						freeFrames--;
						return true;
					}
				// After above things, a page with pageNo still has not allocated in disk.
				// Data has written into the buffer only.
				// After flush, this newly inserted page will be wrote into a newly allocated page in disk.
				// And, state will be changed from NEW to USED.
				}
			}
		}
		return false;
	}

	public boolean write(byte[] b, int pageNo) throws IOException {
		return write(b, pageNo, 0, Env.BLOCK_SIZE);
	}
	
	public boolean write(byte[] b, int index, int len) throws IOException {
		int pageNo = Util.getPageNo(index, len);
		int offset = Util.getOffset(index, len);

		return write(b, pageNo, offset, len);
	}
	
	
	
	private int getNextFreeFrameNo() throws IOException {
		if( freeFrames > 0 ) {
			for ( int i = 0; i < bufSize; i++) {
				if (state[i] == State.FREE)	return i;
			}
		}
		else {
			if ( usedFrameQ.isEmpty() ) {
			// this means that there is neither FREE nor USED frames.
			// so, we need to flush
				flushAll();
			}
			
			int no = usedFrameQ.poll(); // choose a victim based on LRU policy.
			state[no] = State.FREE;
			frames[no].clear();
			pageMap.remove(inPageNo[no]);
			freeFrames++;
			return no;
		}
		return INVALID_NO;
	}
	
	
	public void flushAll() throws IOException {
//		Debug._PrintL("FLUSH______ALL_____");
		for ( int i=0; i < bufSize; i++ ) {
			switch (state[i]) {
			case DIRTY:
				disk.writePage(inPageNo[i], frames[i]);
				state[i] = State.USED;
				usedFrameQ.add(i);
				break;
			case NEW:
				inPageNo[i] = disk.appendPage(frames[i]);
				state[i] = State.USED;
				usedFrameQ.add(i);
				pageMap.put(inPageNo[i], i);
				break;
			default:;
			}
		}
	}
	
	public void flush() throws IOException {
//		Debug._PrintL("FLUSH_____________");
		for ( int i=0; i < bufSize; i++ ) {
			switch (state[i]) {
			case DIRTY:
				disk.writePage(inPageNo[i], frames[i]);
				state[i] = State.USED;
				usedFrameQ.add(i);
				return;
			case NEW:
				inPageNo[i] = disk.appendPage(frames[i]);
				state[i] = State.USED;
				usedFrameQ.add(i);
				pageMap.put(inPageNo[i], i);
				return;
			default:;
			}
		}
	}
	
	
	public void free() {
		clear();
		frames = null;
		Env.FREE_MEM_SIZE += bufSize;
	}
	
	private void clear() {
		for(int i=0; i < bufSize; i++) { 
			state[i] = State.FREE;
			frames[i].clear();
			inPageNo[i] = INVALID_NO;
		}
		pageMap.clear();
		freeFrames = bufSize;
	}
	
	/**
	 * @param pageNo - page No. to be checked
	 * @return true, if this page has already loaded from the disk
	 * <br> false, otherwise.
	 */
	private boolean inBuffer(int pageNo) {
		return pageMap.containsKey(pageNo);
	}
	public void allocateFrame(int pageNo) throws IOException {
		disk.allocatePage(pageNo);
		int frameNo = getNextFreeFrameNo();
		
		inPageNo[frameNo] = pageNo;
		state[frameNo] = State.USED;
		pageMap.put(pageNo, frameNo);
		freeFrames--;
		
	}
	public int getBufSize() {
		return bufSize;
	}
	
	
	
}
