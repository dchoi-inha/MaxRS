package maxRA.io;

import java.util.*;

import maxRA.Env;
import maxRA.util.Debug;


/**
 * @author Dongwan Choi
 * @date 2011. 12. 1.
 */
public class Page {
	public byte[] data;
	
	private int freeSpace = Env.BLOCK_SIZE; // current available space (bytes) in this page 
	
	public Page() {
		data = new byte[Env.BLOCK_SIZE];
	}
	
	public boolean read(byte[] b, int offset, int len) {
		if ( b.length < len || offset+len > Env.BLOCK_SIZE) {
			Debug._Error(this, "read failure from frame due to the incorrect len or offset");
			return false;
		}
		else {
			System.arraycopy(data, offset, b, 0, len);
			freeSpace -= len;
			return true;
		}
	}

	public boolean write(byte[] b, int offset, int len){
		if ( b.length < len || offset+len > Env.BLOCK_SIZE) {
			Debug._Error(this, "write failure from frame due to the incorrect len or offset");
			return false;
		}
		else {
			System.arraycopy(b, 0, data, offset, len);
			freeSpace -= len;
			return true;
		}
	}
	
	/**
	 * @param len - the size (bytes) of a record to be inserted.
	 * @return true, when the current free space is smaller than 'len', 
	 * even if the current free space is not zero.
	 */
	public boolean isFull(int len) {
		return ( freeSpace < len );
	}
	
	public boolean isEmpty() {
		return ( freeSpace == Env.BLOCK_SIZE );
	}

	public void clear(){
		Arrays.fill(data, (byte)0);
		freeSpace = Env.BLOCK_SIZE;
	}
}
