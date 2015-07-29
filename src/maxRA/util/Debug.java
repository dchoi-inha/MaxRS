package maxRA.util;

import java.io.*;

import maxRA.Env;

/**
 * Debug.java, 2011. 12. 5.
 */

/**
 * @author Dongwan Choi
 * @date 2011. 12. 5.
 * @date 2013. 12. 24 updated to write logs in the DEBUG mode
 */
public class Debug {
	
	public static boolean flag = false;
	private static final String logFileName = Env.HomeDir + "/logs/ExactMaxRS.log";
	
	public static void _PrintL(String str) {
		if (flag) System.out.println(str);
		else {
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, true)));
				out.println(str);
				out.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static void _Print(String str) {
		if (flag) System.out.print(str);
		else {
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, true)));
				out.print(str);
				out.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static void _Error(Object object, String str) {
		if (object != null)
			if (flag) System.err.println("Error:" + str + " in "+object.getClass().getSimpleName());
			else {
				try {
					PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, true)));
					out.print(str);
					out.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		else
			if (flag) System.err.println("Error:" + str);
			else {
				try {
					PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, true)));
					out.print(str);
					out.close();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
	}
}
