package maxRA;
import java.io.*;
import java.util.*;

import maxRA.io.PATable;
import maxRA.io.Page;
import maxRA.record.*;
import maxRA.util.Bitmap;
import maxRA.util.Debug;
import maxRA.util.Util;

/**
 * @author Dongwan Choi
 * @date 2011. 12. 1.
 */
public class Main {


	public static void main(String[] args) throws IOException {
		if (args.length < 5) {
			Debug._PrintL("java -jar ExactMaxRS.jar {file path} {block size} {memory size} {width} {height} {sample size}");
			return;
		}
		
		String dsFname = args[0];
		
		Env.BLOCK_SIZE = Integer.parseInt(args[1]); // bytes
		Env.B = Env.BLOCK_SIZE/16;
		
		PointFile pointFile = new PointFile(dsFname, Env.MEM_SIZE, Point.SIZE, Env.MAX_RECORDS_NUM);
		Env.MAX_RECORDS_NUM = pointFile.getNumOfRecords();
		pointFile.close();

//		Env.MAX_RECORDS_NUM = ((int)(new File(Env.HomeDir+dsFname).length()-Env.BLOCK_SIZE)/Point.SIZE);
		
		Env.MEM_SIZE = Integer.parseInt(args[2])/Env.BLOCK_SIZE; // pages
		Env.FREE_MEM_SIZE = Env.MEM_SIZE; // pages
		Env.M = Env.MEM_SIZE*Env.B;
		
		Env.WIDTH = Integer.parseInt(args[3]);
		Env.HEIGHT = Integer.parseInt(args[4]);
		Env.RADIUS = Env.WIDTH/2;
		
		if (Env.MAX_RECORDS_NUM > 1000000)
			Env.MAX_COORD = 1000000000;
		else
			Env.MAX_COORD = 1000000;
//		Env.MAX_COORD = 4*Env.MAX_RECORDS_NUM;
		Env.MIN_COORD = 0;
		Env.MAX = Math.max(Env.MAX_COORD+Env.WIDTH, Env.MAX_COORD+Env.HEIGHT);
		Env.MIN = Math.min(Env.MIN_COORD-Env.WIDTH, Env.MIN_COORD-Env.HEIGHT);

		if (args.length == 5) {
			Env.SAMPLE_NUM = Env.MAX_RECORDS_NUM;
		}
		else {
			Env.SAMPLE_NUM = Math.min(Integer.parseInt(args[5]), Env.MAX_RECORDS_NUM);
		}


		try {
			// header information setting.... manually............
			RandomAccessFile inBinFile = new RandomAccessFile(Env.HomeDir+dsFname, "rw");
			int pagesNum = (int)Math.ceil(((double)inBinFile.length()/(double)Env.BLOCK_SIZE));
			inBinFile.seek(0);
			inBinFile.writeInt(pagesNum);
			inBinFile.writeInt(Env.MAX_RECORDS_NUM);
			inBinFile.close();
			Bitmap inPat = new Bitmap(pagesNum+Env.BLOCK_SIZE*Byte.SIZE);
			inPat.setAll();
			PATable.map.put(Env.HomeDir + dsFname+".pat", inPat);

			Debug._PrintL("-------------------------------------------------------------------------------");
			Debug._PrintL("BLOCK SIZE:\t"+Env.BLOCK_SIZE+"B");
			Debug._PrintL("MEMORY SIZE:\t"+(Env.MEM_SIZE*Env.BLOCK_SIZE)/1024+"KB("+Env.MEM_SIZE+"Blocks)");
			Debug._PrintL("N:\t\t"+Env.MAX_RECORDS_NUM+"Points");
			if (args.length == 6) Debug._PrintL("S:\t\t"+Env.SAMPLE_NUM+"Points");
			Debug._PrintL("SPACE:\t\t"+Env.MAX_COORD+" X "+Env.MAX_COORD);
			Debug._PrintL("WIDTH:\t\t"+Env.WIDTH);
			Debug._PrintL("HEIGHT:\t\t"+Env.HEIGHT);
			Debug._PrintL("-------------------------------------------------------------------------------");
			
			AlgorithmEX alg = new AlgorithmEX(preProcessing(dsFname), dsFname);

			long readCnt = Env.ReadCount; 
			long writeCnt = Env.WriteCount;
			long cpuTimeElapsed = Util.getCpuTime();
			Rect rect = alg.exactMaxRS();
			cpuTimeElapsed = Util.getCpuTime() - cpuTimeElapsed;
			Debug._PrintL("Optimal Location is within "+rect.toString());
			Debug._PrintL("Input/Output of ExactMaxRS: " + (Env.ReadCount-readCnt)+"/"+(Env.WriteCount-writeCnt)+"="+(Env.ReadCount+Env.WriteCount-readCnt-writeCnt));
			Debug._PrintL("-------------------------------------------------------------------------------");
			Debug._PrintL("Total Input/Output: " + Env.ReadCount+"/"+Env.WriteCount+"="+(Env.ReadCount+Env.WriteCount));
			Debug._PrintL("Total Elapsed Time: " + (double)cpuTimeElapsed/1000.0 + " secs");
			
			if (!Debug.flag) {
				System.out.print(Env.ReadCount+Env.WriteCount-readCnt-writeCnt);
				System.err.print((double)cpuTimeElapsed/1000.0);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Debug._PrintL("-------------------------------------------------------------------------------");

	}

	/**
	 * Convert a file of points into a sorted file of rectangles(i.e., intervals)
	 * @param dsFname - the name of a binary file which consists of points
	 * @return the name of a converted binary file which consists of rectangles
	 * @throws IOException
	 */
	private static String preProcessing (String dsFname) throws IOException {
		PointFile pointFile = new PointFile(dsFname, Env.MEM_SIZE/3, Point.SIZE, Env.MAX_RECORDS_NUM);
		String binFname = dsFname.replace(".binary", ".bin");
		String binFname2 = dsFname.replace(".binary", ".bin2");
		new File(binFname).delete(); new File(binFname2).delete();
		RecordFile hInvFile = new HInvFile(binFname, Env.MEM_SIZE/3, HInterval.SIZE, Env.MAX_RECORDS_NUM*2);
		RecordFile invFile = new InvFile(binFname2, Env.MEM_SIZE/3, Interval.SIZE, Env.MAX_RECORDS_NUM*2);
		new File(Env.HomeDir+binFname).deleteOnExit(); new File(Env.HomeDir+binFname2).deleteOnExit();
		
		if(!hInvFile.empty()) hInvFile.clear();
		if(!invFile.empty()) invFile.clear();
		
		Random rand = new Random(System.currentTimeMillis());
		double prob = (double)Env.SAMPLE_NUM/(double)Env.MAX_RECORDS_NUM;
		int sampleCnt = 0;
		Bitmap sampleMap = new Bitmap(Env.MAX_RECORDS_NUM);
		
		for (int iter = 0; iter < 3; iter++) {
			for (int i = 0; i < Env.MAX_RECORDS_NUM && sampleCnt < Env.SAMPLE_NUM; i++) {
				if (rand.nextDouble() <= prob && !sampleMap.get(i)) {
					sampleMap.set(i); sampleCnt++;
					Point p = (Point)pointFile.get(i);
//					p.setX((int)p.getX()*2);
//					p.setY((int)p.getY()*2);

					hInvFile.insert(p.getHIntervals()[0]);
					hInvFile.insert(p.getHIntervals()[1]);

					invFile.insert(new Interval(p.getXInterval().s, p.getXInterval().s, 0));
					invFile.insert(new Interval(p.getXInterval().e, p.getXInterval().e, 0));
				}
			}
		}
		
		Env.MAX_RECORDS_NUM = Env.SAMPLE_NUM;
		
		pointFile.close(); hInvFile.close(); invFile.close(); // flush
		Debug._PrintL("Input/Output of Transformation: " + (Env.ReadCount)+"/"+(Env.WriteCount)+"="+(Env.ReadCount+Env.WriteCount));

		long readCnt = Env.ReadCount; 
		long writeCnt = Env.WriteCount;
		
		hInvFile.open(1); 
		ExMergeSort.exMergeSort(hInvFile);
		
		invFile.open(1);
		ExMergeSort.exMergeSort(invFile);
		
		Debug._PrintL("Input/Output of Sorting: " + (Env.ReadCount-readCnt)+"/"+(Env.WriteCount-writeCnt)+"="+(Env.ReadCount+Env.WriteCount-readCnt-writeCnt));

		
		return binFname;
	}
}


