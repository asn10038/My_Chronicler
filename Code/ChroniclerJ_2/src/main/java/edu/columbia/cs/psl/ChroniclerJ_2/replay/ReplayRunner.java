package edu.columbia.cs.psl.ChroniclerJ_2.replay;

import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

import org.objectweb.asm.Type;

import com.thoughtworks.xstream.XStream;

import edu.columbia.cs.psl.ChroniclerJ_2.CallbackInvocation;
import edu.columbia.cs.psl.ChroniclerJ_2.ExportedLog;
import edu.columbia.cs.psl.ChroniclerJ_2.ExportedSerializableLog;
import edu.columbia.cs.psl.ChroniclerJ_2.Log;
import edu.columbia.cs.psl.chroniclerj.xstream.StaticReflectionProvider;

public class ReplayRunner {

	public static String[] logFiles;

	public static String[] serializableLogFiles;

	private static int nextLog = 0;

	private static int nextSerializableLog = 0;
	
    private static URLClassLoader loader = null;

    static String mainClass;
    
    static String[] params;
	
    
    public static void loadNextLog(String logClass) {
        try {
            Log.logLock.lock();
            _loadNextLog(logClass);
            Log.logLock.unlock();
            ReplayUtils.checkForDispatch();
        } catch (Exception exi) {
            exi.printStackTrace();
        }
    }
    
    private static void _loadNextLog(String logClass) {
        try {
            if (logClass.contains("Serializable")) {
                ObjectInputStream is = new ObjectInputStream(
                        loader.getResourceAsStream(serializableLogFiles[nextSerializableLog]));
                @SuppressWarnings("unused")
                ExportedSerializableLog el = (ExportedSerializableLog) is.readObject();
                is.close();
                nextSerializableLog++;
            } else {
                XStream xstream = new XStream(new StaticReflectionProvider());
                @SuppressWarnings("unused")
                //seems like this reads the object into ExportedLog and fills the static vars
                Object o = xstream.fromXML(loader.getResourceAsStream(logFiles[nextLog]));
                nextLog++;

                ReplayUtils.dispatchesToRun = new HashMap<Integer, CallbackInvocation>();
                for (Object e : ExportedLog.aLog) {
                    if (e != null && e.getClass().equals(CallbackInvocation.class)) {
                        // System.out.println(e);
                        CallbackInvocation ci = (CallbackInvocation) e;
                        ReplayUtils.dispatchesToRun.put(ci.getClock(), ci);
                    }
                }
            }
            // System.out.println(ExportedSerializableLog.aLog_fill);
            // System.out.println(ExportedLog.aLog_fill);
        } catch (Exception exi) {
            exi.printStackTrace();
        }
    }
    
    /** extracts the logs and main method info from the jar */
	public static void setupLogs(String[] classpath)
    {
		//testcase is first in array of classpath
    	if (!new File(classpath[0]).exists()) {
            System.err.println("Unable to load test case " + classpath[0]);
            System.exit(-1);
        }
        try {
            URL[] urls = new URL[classpath.length];
            for (int i = 0; i < classpath.length; i++) {
                urls[i] = new File(classpath[i]).toURI().toURL();
            }
            loader = new URLClassLoader(urls);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            System.exit(-1);
        }

        //Written to the jar file in ChroniclerJExportRunner.genTestCase
        //main info seems to have the main class, main method, main args, and log file names
        InputStream inputStream = loader.getResourceAsStream("main-info");

        Scanner s = new Scanner(inputStream);
        mainClass = s.nextLine();
        ArrayList<String> _serializableLogs = new ArrayList<String>();
        ArrayList<String> _logs = new ArrayList<String>();
        int nArgs = Integer.parseInt(s.nextLine());
        params = new String[nArgs];
        int nSerializableLogs = 0;

        int nLogs = 0;
        for (int i = 0; i < nArgs; i++) {
            params[i] = s.nextLine();
        }
        while (s.hasNextLine()) {
            String l = s.nextLine();
            if (l.contains("_serializable_")) {
                _serializableLogs.add(l);
                nSerializableLogs++;
            } else {
                _logs.add(l);
                nLogs++;

            }
        }
        s.close();
        logFiles = new String[nLogs];
        serializableLogFiles = new String[nSerializableLogs];

        logFiles = _logs.toArray(logFiles);
        serializableLogFiles = _serializableLogs.toArray(serializableLogFiles);

        System.out.println("Available logs: " + Arrays.deepToString(logFiles)
                + Arrays.deepToString(serializableLogFiles));
        _loadNextLog(Type.getDescriptor(ExportedSerializableLog.class));

        _loadNextLog(Type.getDescriptor(ExportedLog.class));
    }
	
	public static void _main(String[] classpath) {
        setupLogs(classpath);
        ReplayUtils.checkForDispatch();
        Class<?> toRun;
        try {
            toRun = loader.loadClass(mainClass.replace("/", "."));
            Method meth = toRun.getMethod("main", String[].class);
            meth.invoke(null, new Object[] {
                params
            });

        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
