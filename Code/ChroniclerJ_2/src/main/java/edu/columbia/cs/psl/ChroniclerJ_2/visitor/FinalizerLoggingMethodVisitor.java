package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.columbia.cs.psl.ChroniclerJ_2.Instrumenter;

public class FinalizerLoggingMethodVisitor extends MethodVisitor {
	
	private boolean isFinalize;
	
	private String className;
	
	//Determines if the garbage collector is called on the object
	
	public FinalizerLoggingMethodVisitor(MethodVisitor mv, String name, String desc, 
			String className) {
		super(Opcodes.ASM5, mv);
		//if method is void finalize()
		this.isFinalize = (name.equals("finalize")) && desc.equals("()V");
		
	}
	
	@Override
	public void visitCode() {
		//visits code block of the method if it is the finalize method
		super.visitCode();
		if(this.isFinalize) {
			//Log to the log and get our finalizer #
			//loads the reference to the object calling the finalize method
			visitVarInsn(Opcodes.ALOAD, 0);
			//Load Long this.className.FieldLogicalClock
			//J is a Long value
			//I think this is used for ordering in the replay
			visitFieldInsn(Opcodes.GETFIELD, this.className, Instrumenter.FIELD_LOGICAL_CLOCK, "J");
			//Puts the static variable ReplayUtils.curFinalizer
			visitFieldInsn(Opcodes.PUTSTATIC, "edu/columbia/cs/psl/ChroniclerJ_2/replay/ReplayUtils",
					"curFinalizer", "J");
			//TODO ask Jon if the Java code looks like:
			//Long Instrumenter.FIELD_LOGICAL_CLOCK = replay.ReplayUtils.curFinalizer
			//AND does the garbage collector still get called.
		}
	}

}
