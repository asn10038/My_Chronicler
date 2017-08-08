package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import java.util.HashSet;

import org.apache.log4j.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
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

		private boolean isFirstConstructor;
	    
	    public static boolean isND(String owner, String name, String desc) {
	        return nonDeterministicMethods.contains(owner + "." + name + ":" + desc);
	    }
	    
	    public static void registerNDMethod(String owner, String name, String desc) {
	    	nonDeterministicMethods.add(owner + "." + name + ":" + desc);
	    }
	    
	    //TODO ask Jon about AnalyzerAdapter (possible duplicate question)
	    protected NonDeterministicLoggingMethodVisitor(MethodVisitor mv, int access, 
	    		String name, String desc, String className, String superName, boolean isFirstConstructor, AnalyzerAdapter analyzer)
	    {
	    	super(mv, access, name, desc, className, analyzer);
	        this.name = name;
	        this.desc = desc;
	        this.className = className;
	        this.analyzer = analyzer;
	        this.superName = superName;
	        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
	        this.constructor = "<init>".equals(name);
	        this.isFirstConstructor = isFirstConstructor;
	    }
	    
	    private NonDeterministicLoggingClassVisitor parent;
	    
	    //Marks the parent class visitor and this is the method visitor
	    public void setClassVisitor(NonDeterministicLoggingClassVisitor coaClassVisitor) {
	    	this.parent = coaClassVisitor;
	    }
	    
	    
}
