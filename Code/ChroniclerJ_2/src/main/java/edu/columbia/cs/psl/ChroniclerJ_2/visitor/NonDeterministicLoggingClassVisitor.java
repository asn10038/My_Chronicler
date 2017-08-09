package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import java.util.HashSet;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ChroniclerJ_2.Constants;
import edu.columbia.cs.psl.ChroniclerJ_2.Instrumenter;

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
	    	MethodVisitor smv = new ReflectionInterceptingMethodVisitor(primaryMV);
	    	//ask Jon about redeclaring smv -- smv gets redefined to react according to what the class needs
	    	smv = new FinalizerLoggingMethodVisitor(smv, name, desc, className);
	    	if (name.equals("main") && desc.equals("[Ljava/lang/String;)V")) {
	    		smv = new MainLoggingMethodVisitor(smv, acc, name, desc, className);
	    	}
	    	
	    	if (Instrumenter.classIsCallBack(className, superName, interfaces)) {
	    		AnalyzerAdapter analyzer = new AnalyzerAdapter(className, acc, name, desc, smv);
	    		CloningAdviceAdapter caa = new CloningAdviceAdapter(analyzer, acc, name, desc, className, analyzer);
	    		//This does some logging
	    		smv = new CallbackLoggingMethodVisitor(caa, acc, name, desc, className,
	                    null, caa, superName, interfaces);
	    		//remove jumps and inline instructions
	    		smv = new JSRInlinerAdapter(smv, acc, name, desc, signature, exceptions);
	    		//need to sort local variables before adding new local variables
	    		smv = new LocalVariablesSorter(acc, desc, smv);
	    		caa.setLocalVariableSorter((LocalVariablesSorter)smv);
	    		
	    	}
	    	
	    	//If it is a class and not an interface and not part of Logging or replay
	    	if (isAClass && !name.equals(Constants.INNER_COPY_METHOD_NAME)
	    			&& !name.equals(Constants.OUTER_COPY_METHOD_NAME)
	    			&& !name.equals(Constants.SET_FIELDS_METHOD_NAME)
	    			&& !className.startsWith("com/thoughtworks")) {
	    		//AnalyzerAdapter -- simulates the stack
	    		AnalyzerAdapter analyzer = new AnalyzerAdapter(className, acc, name, desc, smv);
	    		NonDeterministicLoggingMethodVisitor cloningMV = new NonDeterministicLoggingMethodVisitor(
	    				analyzer, acc, name, desc, className, superName, isFirstConstructor, analyzer);
	    		//this seems like a hack after realizing something about first constructors
	    		if(name.equals("<init>"))
	    			isFirstConstructor = false;
	    		cloningMV.setClassVisitor(this);
	    		//removes jump instructions and inlines them
	    		JSRInlinerAdapter mv2 = new JSRInlinerAdapter(cloningMV, acc, name, desc, signature,
	                    exceptions);
	    		LocalVariablesSorter sorter = new LocalVariablesSorter(acc, desc, mv2);
	    		cloningMV.setLocalVariableSorter(sorter);
	    		return sorter;
	    	} else // if name is one of the designated fields (in the process of copying methods or fields)
	    		return smv;
	    }
}
