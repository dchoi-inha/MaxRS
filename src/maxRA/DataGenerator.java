package maxRA;
import java.io.*;
import java.util.*;

import maxRA.util.Util;

/**
 * DataGenerator.java, 2011. 12. 2.
 */

/**
 * @author Dongwan Choi
 * @date 2011. 12. 2.
 */
public class DataGenerator {

	public String generateData(int recordsNum) throws IOException {
		
		String dsFname = "ds_"+recordsNum+"_"+Util.getTodayString()+".txt";
		BufferedWriter bw = new BufferedWriter(new FileWriter(dsFname));

		Random rnd = new Random();
		if(Env.DIST == Dist.UNF) {
			for ( int i = 0; i < recordsNum; i++ ) {
				int x = rnd.nextInt(Env.MAX_COORD);
				int y = rnd.nextInt(Env.MAX_COORD);
				bw.write(x +"/"+y+"\r\n");
			}
		}
		else if (Env.DIST == Dist.GAUSS) {
			for ( int i = 0; i < recordsNum; i++ ) {
				double x, y;
				while((x = getGaussian(rnd, Env.MAX_COORD/2, 200*Env.MAX_COORD/Env.WIDTH)) < 0 || x > Env.MAX_COORD);
				while((y = getGaussian(rnd, Env.MAX_COORD/2, 200*Env.MAX_COORD/Env.HEIGHT)) < 0 || y > Env.MAX_COORD);
				
				bw.write((int)x +"/"+(int)y+"\r\n");
			}
		}
		bw.close();
		return dsFname;
	}
	
	private double getGaussian(Random rnd, double mean, double var) {
		return mean + rnd.nextGaussian()*var;
	}
}