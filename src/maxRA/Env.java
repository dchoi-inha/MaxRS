package maxRA;
/**
 * @author Dongwan Choi
 * @date 2011. 12. 1.
 */

enum Dist {UNF, GAUSS};
public class Env {
/* variables for theoretical analysis */
	public static int B = 256; // the # of elements in a block
	public static int M = B*B/2; // the # of elements in the main-memory

	
/* variables for implementation */
	public static int WIDTH = 1000;
	public static int HEIGHT = 1000;
	
	//-- Synthetic data set --//
	public static int MAX_RECORDS_NUM = 0;

	public static int MAX_COORD = MAX_RECORDS_NUM*4;
//	public final static int MAX_COORD = 1000000;
	public static int MIN_COORD = 0;
	
	public static int MAX = Math.max(MAX_COORD+WIDTH, MAX_COORD+HEIGHT);
	public static int MIN = Math.min(MIN_COORD-WIDTH, MIN_COORD-HEIGHT);


	public static int MEM_SIZE = M/B;  // pages
	public static int BLOCK_SIZE = 16*B; // bytes	
	public static int FREE_MEM_SIZE = MEM_SIZE; // pages
	
	public static Dist DIST = Dist.GAUSS;
	
	public static long WriteCount = 0;
	public static long ReadCount = 0;
	
	
	public static int SLAB_BUF_SIZE = 1;
	
	public static int SAMPLE_NUM=0;
	
	
	/* for MaxCRS */	
	public static int RADIUS = 500;
	public static final int SHIFT_NUM=4;
	
	
	public static final String HomeDir = "/mnt/exp/ExactMaxRS/"; 
	
}
