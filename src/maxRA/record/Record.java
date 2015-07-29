package maxRA.record;
/**
 * @author Dongwan Choi
 * @date 2011. 12. 4.
 */
public abstract class Record implements Comparable<Record>{
	
	/**
	 * @return the size of a record in bytes
	 */
	public abstract int size();
	public abstract byte[] toByteArray();
	
}
