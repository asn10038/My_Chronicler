package edu.columbia.cs.psl.ChroniclerJ_2.replay;

import java.util.HashMap;

import edu.columbia.cs.psl.ChroniclerJ_2.CallbackInvocation;
import edu.columbia.cs.psl.ChroniclerJ_2.ExportedLog;

public class ReplayUtils {
	
	public static HashMap<Integer, CallbackInvocation> dispatchesToRun;

	public static void checkForDispatch() {
		//TODO ask Jon why there may be null and you have to check two down the log
		int curClock = ExportedLog.globalReplayIndex;
		// System.out.println("Looking for dispatches at " + curClock);
		if (dispatchesToRun != null && dispatchesToRun.get(curClock) != null) {
			// System.out.println("Invoke " + dispatchesToRun.get(curClock));
			if (dispatchesToRun.get(curClock).invoke()) {
				// System.out.println("Success");
				ExportedLog.globalReplayIndex++;
				checkForDispatch();
			}
		}
		//If you hit a null, check the next log entry
		curClock++;
		if (dispatchesToRun != null && dispatchesToRun.get(curClock) != null) {
			// System.out.println("Invoke " + dispatchesToRun.get(curClock));
			if (dispatchesToRun.get(curClock).invoke()) {
				// System.out.println("Success");
				ExportedLog.globalReplayIndex += 2;
				checkForDispatch();
			}
		}
		//both if statements were null exit
	}
}
