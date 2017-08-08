package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.cs.psl.ChroniclerJ_2.ChroniclerJExportRunner;

public class MainLoggingMethodVisitor extends MethodVisitor {
	
	private String className;
	
	protected MainLoggingMethodVisitor(MethodVisitor mv, int access, String name, 
			String desc, String className) {
		super(Opcodes.ASM5, mv);
		this.className = className;
		
	}
	
	@Override
	public void visitCode() {
		super.visitCode();
		//push the class name
		visitLdcInsn(this.className);
		//load the reference
		super.visitVarInsn(Opcodes.ALOAD, 0);
		//call the log main method code in ChroniclerJExportRunner
		//ChroniclerJExportRunner.logMain(String, StrinArgs)
		super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ChroniclerJExportRunner.class), "logMain", "(Ljava/lang/String;[Ljava/lang/String;)V", false);
		
	}

}
