package maxRA.io;

import java.io.*;
import java.util.*;

import maxRA.Env;
import maxRA.record.Interval;
import maxRA.record.Point;
import maxRA.util.Bitmap;
import maxRA.util.Debug;

/**
 * @author Dongwan Choi
 * @date 2011. 12. 1.
 */
public class BlockFile {
	public static final int HEADER_SIZE = 4+4;
	
	private String name;
	private RandomAccessFile fp;
//	private RandomAccessFile fpPAT;
	private Bitmap pageAllocMap; // which is modified as an in-memory structure 2012.11.01
	private int pagesNum;
	private int recordsNum;

	/**
	 * Create the database with the given name
	 * @param name
	 */
//	public BlockFile(String fname) throws IOException {
//		name = fname;
//
//		fp = new RandomAccessFile(name, "rw");
//		fpPAT = new RandomAccessFile(name+".pat", "rw");
//
//		if( fp.length() <= 0 ) { // newly created file
//			pageAllocMap = new Bitmap(Page.PAGESIZE*Byte.SIZE); 
//			pageAllocMap.set(0); // page 0 is used a header information
//			pagesNum = 1;
//			recordsNum = 0;
//			updateHead();
//		}
//		else {
//			pagesNum = fp.readInt();
//			recordsNum = fp.readInt();
//			byte [] bits = new byte[pagesNum/Byte.SIZE + (pagesNum%Byte.SIZE != 0 ?1:0)]; 
//			if (fpPAT.read(bits) == pagesNum/Byte.SIZE + (pagesNum%Byte.SIZE != 0 ?1:0)) { 
//				pageAllocMap = new Bitmap(bits);
//				actPageNo = 1;
//			}
//			else {
//				Debug._Error(this, "failure to read page bitmap");
//			}
//		}
//	}
	
	public BlockFile(String fname, int fileSize) throws IOException {
		name = Env.HomeDir + fname;

		fp = new RandomAccessFile(name, "rw");
//		fpPAT = new RandomAccessFile(name+".pat", "rw");

		if( fp.length() <= 0 ) { // newly created file
			int pageSize = fileSize/Env.BLOCK_SIZE + (fileSize%Env.BLOCK_SIZE != 0 ?1:0) ;
			pageAllocMap = new Bitmap(pageSize+Env.BLOCK_SIZE*Byte.SIZE); // 1 block more allocated just in case
			pageAllocMap.set(0); // page 0 is used a header information
			pagesNum = 1;
			recordsNum = 0;
			updateHead();
			PATable.map.put(name+".pat", pageAllocMap);
		}
		else {
			pagesNum = fp.readInt();  
			recordsNum = fp.readInt(); 
			
			Bitmap tmpBitmap = new Bitmap(pagesNum);
			
			byte [] bits = tmpBitmap.toByteArray(); 
			pageAllocMap = PATable.map.get(name+".pat");
			if (pageAllocMap == null) {
				pageAllocMap = new Bitmap(pagesNum+Env.BLOCK_SIZE*Byte.SIZE); // 1 block more allocated just in case
				PATable.map.put(name+".pat", pageAllocMap);
			}
		}
	}
	
	/**
	 * This function just allocate a page with a given number
	 */
	public void allocatePage(int pageNo) throws IOException {
		pageAllocMap.set(pageNo);
		pagesNum++;
		updateHead();
	}
	
	/**
	 * @param page - data to be written
	 * @return newly appended(allocated) page No.
	 * @throws IOException
	 */
	public int appendPage(Page page) throws IOException {
		allocatePage(getNextFreePageNo());

		writePage(pagesNum-1, page);
		seekPage(pagesNum-1);
		return (pagesNum-1);
	}
	
	public boolean deleteLastPages(int num) throws IOException {
		if (pagesNum < num) {
			Debug._Error(this, "Invalid # of pages"); 
			return false;
		}
		for ( int i = 0; i < num; i++ ) pageAllocMap.unset(pagesNum-1-i);
		pagesNum -= num;
		updateHead();
		return true;
	}
	
	
	/**
	 * 	write header information <br>
	 * 	[# of pages:4byte] [page bitmap: page size-4bytes]
	 * @throws IOException
	 */
	private void updateHead() throws IOException  {
		seekPage(0);
		fp.writeInt(pagesNum);
		fp.writeInt(recordsNum);
//		fpPAT.seek(0);
//		fpPAT.write(pageAllocMap.toByteArray());	
	}

	
	/**
	 * Reads the contents of the specified page from disk into the page object provided.
	 * @param pageNo - the page number to be read.
	 * @param page - a reference to an already allocated Page object.
	 * @throws IOException
	 */
	public boolean readPage(int pageNo, Page page) throws IOException {
		if (!pageAllocMap.get(pageNo)) return false; // page should be allocated before read
			
		Env.ReadCount++;
//		if(++Env.ReadCount % 10000 == 0) Debug._PrintL("read-->"+Env.ReadCount);
		seekPage(pageNo);
		if ( fp.read(page.data, 0, Env.BLOCK_SIZE) > 0 ) {
			return true;
		}
		else {
			Debug._Error(this, "Disk read failure");
			return false;
		}
	}
	
	/**
	 * Writes the contents of the specified page to disk.
	 * @param pageNo - the page number to be written.
	 * @param page - a Page object with data to be written.
	 * @throws IOException
	 */
	public void writePage(int pageNo, Page page) throws IOException {
		Env.WriteCount++;
//		if(++Env.WriteCount % 10000 == 0) Debug._PrintL("write-->"+Env.WriteCount);
		seekPage(pageNo);
		fp.write(page.data, 0, Env.BLOCK_SIZE); 
	}

	
	private void seekPage(int pageNo) throws IOException {
		if (pagesNum <= pageNo) {
			Debug._Error(this, "Invalid Page Number"); return;
		}
		fp.seek(pageNo*Env.BLOCK_SIZE);
	}
	
	public void close() throws IOException {
		updateHead();
		fp.close();
	}
	
	public int getNumOfPages() {
		return pagesNum;
	}
	public int getNumOfRecords() {
		return recordsNum;
	}
	public void setNumOfRecords(int recordsNum) {
		this.recordsNum = recordsNum;
	}
	
	public boolean isAllocated(int pageNo) {
		return (pageAllocMap.get(pageNo));
	}
	
	public int getNextFreePageNo() throws IOException {
		int i;
		for ( i = 0; i <pageAllocMap.size(); i++) 
			if (!pageAllocMap.get(i))  break;

		return i;
	}
	
	public void renameTo(String newName) {
		String fullNewName = Env.HomeDir + newName;
		File f = new File(this.name);
		new File(fullNewName).delete();
		f.renameTo(new File(fullNewName));
		this.name = fullNewName;
		
//		new File(name+".pat").renameTo(new File(newName+".pat"));
	}
	
	public void destroy() throws IOException {
		fp.close();
		File f = new File(name);
		f.delete();
		
//		fpPAT.close();
//		new File(name+".pat").delete();
	}
	
}
