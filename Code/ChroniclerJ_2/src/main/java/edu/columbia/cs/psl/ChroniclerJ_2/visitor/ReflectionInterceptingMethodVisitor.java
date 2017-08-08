package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReflectionInterceptingMethodVisitor extends MethodVisitor {

    public ReflectionInterceptingMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
    }
	
    //Visits the method instruction
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
		//owner: Class that owns the instruction
		// name: name of the method
		//TODO ask Jon if this intercepts all method calls
		//if instruction calls java.lang.reflect.Method.invoke
		if (owner.equals("java/lang/reflect/Method") && name.equals("invoke")) {
			//set the opcode to call the static method
			opcode = Opcodes.INVOKESTATIC;
			owner = "edu/columbia/cs/psl/ChroniclerJ_2/MethodInterceptor";
			//call the method, but instead of running java.lang.reflect.Method.invoke run edu...Chronicler*.MethodInterceptor.invoke
			//java code is like: Object MethodInterceptor.invoke(Method, Object, Object...)
			super.visitMethodInsn(opcode, owner, "invoke",
					"(Ljava/lang/reflect/Method;Ljava.lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
					false);
		} else
			super.visitMethodInsn(opcode, owner, name, desc, itfc);
			
	}

}
