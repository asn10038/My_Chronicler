package edu.columbia.cs.psl.ChroniclerJ_2.replay;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class FinalizerReplayingMethodVisitor extends MethodVisitor{

    private boolean isFinalize;

    private String className;
    
    public FinalizerReplayingMethodVisitor(MethodVisitor mv, String name, String desc,
            String className) {
        super(Opcodes.ASM5, mv);
        this.isFinalize = (name.equals("finalize")) && desc.equals("()V");
        this.className = className;
    }
	
}
