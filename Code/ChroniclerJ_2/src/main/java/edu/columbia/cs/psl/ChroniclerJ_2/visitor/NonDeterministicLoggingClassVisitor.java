package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import java.util.HashSet;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class NonDeterministicLoggingClassVisitor extends ClassVisitor implements Opcodes {

		private String className;
		private String superName;
		private String[] interfaces;
		
		private boolean isAClass = true;
		
		//Used for eventListeners,
		public static HashSet<String> callbackClasses = new HashSet<String>();

	    public static HashSet<String> callbackMethods = new HashSet<String>();
	    
	    static {
	    	Scanner s;
	    	try {
	    		s = new Scanner(NonDeterministicLoggingClassVisitor.class.getClassLoader()
	    				.getResourceAsStream("listenerMethods.txt"));
	    		while (s.hasNextLine()) {
	    			String l = s.nextLine();
	    			callbackMethods.add(l);
	    			callbackClasses.add(l.substring(0, l.indexOf(".")));
	    		}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
	    
	    public NonDeterministicLoggingClassVisitor(ClassVisitor cv) {
	    	super(Opcodes.ASM5, cv);
	    }
	    
	    private static Logger logger = Logger.getLogger(NonDeterministicLoggingClassVisitor.class);
	    
	    @Override
	    public void visit(int version, int access, String name, String signature, String superName,
	    			String[] interfaces) {
	    	super.visit(version, access, name, signature, superName, interfaces);
	    	//initializes class information
	    	this.className = name;
	    	this.superName = superName;
	    	this.interfaces = interfaces;
	    	
	    	logger.debug("Visiting " + name + " for instrumentation");
	    	//checks whether the file is a class or an interface
	    	if ((access & Opcodes.ACC_INTERFACE) != 0)
	    		isAClass = false;
	    }
	    
	    private boolean isFirstConstructor = true;
	    
	    @Override
	    public MethodVisitor visitMethod(int acc, String name, String desc, String signature,
	    		String[] exceptions) {
	    	//TODO ask Jon about this comment 'need an annotation to disable doing this to some apps'
	    	MethodVisitor primaryMV = cv.visitMethod(acc, name, desc, signature, exceptions);
	    	//secondary method visitor
	    	MethodVisitor smv = new RefelectionInterceptionMethodVisitor(primaryMV);
	    }
}
