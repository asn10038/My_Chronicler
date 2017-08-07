package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.objectweb.asm.commons.AnalyzerAdapter;


public class NonDeterministicLoggingMethodVisitor
	extends CloningAdviceAdapter {
	    private static Logger logger = Logger.getLogger(NonDeterministicLoggingMethodVisitor.class);

	    private String name;

	    private String desc;

	    private String className;

	    private String superName;

	    private int pc;

	    public static HashSet<String> nonDeterministicMethods = new HashSet<String>();

	    private boolean isStatic;

	    private boolean constructor;

	    private boolean superInitialized;

	    private static HashSet<String> ignoredNDMethods = new HashSet<String>();

	    private AnalyzerAdapter analyzer;
	    
	    public static boolean isND(String owner, String name, String desc) {
	        return nonDeterministicMethods.contains(owner + "." + name + ":" + desc);
	    }
}
