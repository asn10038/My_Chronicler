package edu.columbia.cs.psl.ChroniclerJ_2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.log4j.Logger;

import edu.columbia.cs.psl.ChroniclerJ_2.PreMain.ChroniclerTransformer;


public class Instrumenter {
	// this is the instrumenter class for ChroniclerJ
	
	//static variables
	private static ClassLoader loader;
	
	private static Logger logger = Logger.getLogger(Instrumenter.class);
	
	private static final int NUM_PASSES = 2;
	
	private static final int PASS_ANALYZE = 0;
	
	private static final int PASS_OUTPUT = 1;

    public static final boolean IS_DACAPO = false;
    
    private static int pass_number = 0;

    public static final boolean FINALIZERS_ENABLED = true;

    public static final String FIELD_LOGICAL_CLOCK = "_chronicler_clock";

    private static File rootOutputDir;

    private static String lastInstrumentedClass;
    
    static ChroniclerTransformer transformer = new ChroniclerTransformer();
    
    public static void _main(String[] args) {
    	//main method that is run after the -instrument call in Main.java
    	//takes arguments of the form {parent folder to instrument, output deploy location, output replay location}
    	String outputFolder = args[1];
        rootOutputDir = new File(outputFolder);
        if (!rootOutputDir.exists())
            rootOutputDir.mkdir();
        String inputFolder = args[0];
        // Setup the class loader for additional classpath entries
        URL[] urls = new URL[args.length - 2]; 
        for (int i = 2; i < args.length; i++) {
            File f = new File(args[i]);
            if (!f.exists()) {
                System.err.println("Unable to read path " + args[i]);
                System.exit(-1);
            }
            if (f.isDirectory() && !f.getAbsolutePath().endsWith("/"))
                f = new File(f.getAbsolutePath() + "/");
            try {
                urls[i - 2] = f.getCanonicalFile().toURI().toURL();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        loader = new URLClassLoader(urls, Instrumenter.class.getClassLoader());
        
        for (pass_number = 0; pass_number < NUM_PASSES; pass_number++) // Do
            // each
            // pass. TODO find out what do each pass means
		{
			File f = new File(inputFolder);
			if (!f.exists()) {
				System.err.println("Unable to read path " + inputFolder);
				System.exit(-1);
			}
			if (f.isDirectory())
				processDirectory(f, rootOutputDir, true);
//			else if (inputFolder.endsWith(".jar"))
//				processJar(f, rootOutputDir);
//			else if (inputFolder.endsWith(".zip"))
//				processZip(f, rootOutputDir);
//			else if (inputFolder.endsWith(".class"))
//				try {
//					processClass(f.getName(), new FileInputStream(f), rootOutputDir);
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			else {
//				System.err.println("Unknown type for path " + inputFolder);
//				System.exit(-1);
//			}
//			finishedPass();
		}
        
    }
    
    private static void processClass(String name, InputStream is, File outputDir) {
    	switch(pass_number) {
    		case PASS_ANALYZE: 
    			// analyzeClass(is) -- this is an empty method TODO find out why this is needed
    			break;
    		case PASS_OUTPUT:
    			try {
    				FileOutputStream fos = new FileOutputStream(outputDir.getPath()
    						+ File.separator + name);
    				ByteArrayOutputStream bos = new ByteArrayOutputStream();
    				//set the last instrumented class ... this could be incorrect if error is thrown and class isn't instrumented
    				lastInstrumentedClass = outputDir.getPath() + File.separator + name;
    				bos.write(instrumentClass(is));
    				bos.writeTo(fos);
    				fos.close();
    			} catch(Exception ex) {
    				ex.printStackTrace();
    			}
    	}
    }
}
