package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import java.util.HashSet;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.MethodNode;

import edu.columbia.cs.psl.ChroniclerJ_2.Instrumenter;


/**
 * If we identify a method as a callback method: Rename it. the renamed one will
 * then not be logged. Create a new method with the original name, and the only
 * instructions are to call the _chronicler_ version
 * WOW!! JON DOES USE JAVA DOCS :-0
 * @author jon
 */
public class CallbackDuplicatingClassVisitor extends ClassVisitor {

    private String className;

    private String superName;
    private String[] interfaces;
    
    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.superName = superName;
        this.interfaces = interfaces;
    }
    
    public CallbackDuplicatingClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    private HashSet<MethodNode> methodsToGenerateLogging = new HashSet<MethodNode>();

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        if (Instrumenter.methodIsCallBack(className, name, desc, superName, interfaces)) {
        	//Log the callback method
            methodsToGenerateLogging.add(new MethodNode(access, name, desc, signature, exceptions));
            //Rename original method so it won't be logged
            return super.visitMethod(access, "_chronicler_" + name, desc, signature, exceptions);
        }
        //if the method isn't a callback do nothing
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
    
    @Override
    public void visitEnd() {
    	for (MethodNode mn : methodsToGenerateLogging) {
    		//TODO ask Jon why he calls this
    		MethodVisitor mv = super.visitMethod(mn.access, mn.name, mn.desc, mn.signature,
                    (String[]) mn.exceptions.toArray(new String[0]));
    		//Looks like it simulates the stack and the frame etc.
            AnalyzerAdapter analyzer = new AnalyzerAdapter(className, mn.access, mn.name, mn.desc, mv);
            //Has the ability to make calls to the log methods
            CloningAdviceAdapter caa = new CloningAdviceAdapter(analyzer, mn.access,
                    mn.name, mn.desc, className, analyzer);
            //sorts the variables so code can be instrumented
            LocalVariablesSorter lvsorter = new LocalVariablesSorter(mn.access, mn.desc, mv);
            CallbackLoggingMethodVisitor clmv = new CallbackLoggingMethodVisitor(mv,
                    mn.access, mn.name, mn.desc, className, lvsorter, caa, superName, interfaces);
            if ((mn.access & Opcodes.ACC_STATIC) == 0) // not static
                clmv.visitVarInsn(Opcodes.ALOAD, 0);
            
         // load all of the arguments onto the stack again
            Type[] args = Type.getArgumentTypes(mn.desc);
            
            int j = 0;
            for(int i=0; i<args.length; i++) {
            	clmv.load(j, args[i]);
            	j += args[i].getSize();
            }
            //make the call to the chronicler named one
            clmv.visitMethodInsn((mn.access & Opcodes.ACC_STATIC) == 0 ? Opcodes.INVOKESPECIAL
                    : Opcodes.INVOKESTATIC, className, "_chronicler_" + mn.name, mn.desc, false);
            clmv.visitInsn(Type.getReturnType(mn.desc).getOpcode(Opcodes.IRETURN));
            clmv.visitMaxs(0, 0);

            clmv.visitEnd();
        }
    	super.visitEnd();
            	
    }
}
