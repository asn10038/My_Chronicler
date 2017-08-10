package edu.columbia.cs.psl.ChroniclerJ_2.replay;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.MethodInsnNode;

import edu.columbia.cs.psl.ChroniclerJ_2.CallbackRegistry;
import edu.columbia.cs.psl.ChroniclerJ_2.ChroniclerJExportRunner;
import edu.columbia.cs.psl.ChroniclerJ_2.Instrumenter;
import edu.columbia.cs.psl.ChroniclerJ_2.MethodCall;
import edu.columbia.cs.psl.ChroniclerJ_2.visitor.NonDeterministicLoggingMethodVisitor;

public class NonDeterministicReplayMethodVisitor extends InstructionAdapter implements Opcodes {

    private static Logger logger = Logger.getLogger(NonDeterministicReplayMethodVisitor.class);

    private String name;

    private String desc;

    private String classDesc;

    private boolean isStatic;

    private boolean constructor;

    private boolean superInitialized;

    private boolean isCallbackInit;
    
    private ArrayList<MethodCall> methodCallsToClear = new ArrayList<MethodCall>();
    
    @Override
    public void visitCode() {
    	super.visitCode();
    	if (constructor) {
    		//load the reference
    		super.visitVarInsn(ALOAD, 0);
    		//Get the Field clock from the log
    		super.visitFieldInsn(Opcodes.GETSTATIC, "edu/columbia/cs/psl/ChroniclerJ_2/Log",
    				Instrumenter.FIELD_LOGICAL_CLOCK, "J");
    		//Duplicate 1 or 2 values and insert them at a lower position on the operand stack
    		super.visitInsn(DUP2_X1);
    		//Add one to the FieldLogicalClock
    		super.visitFieldInsn(Opcodes.PUTFIELD, this.classDesc, Instrumenter.FIELD_LOGICAL_CLOCK, "J");
    		super.visitInsn(LCONST_1);
    		super.visitInsn(LADD);
    		//Makes the Field Logical Clock a static field
    		super.visitFieldInsn(Opcodes.PUTSTATIC, "edu/columbia/cs/psl/ChroniclerJ_2/Log",
    				Instrumenter.FIELD_LOGICAL_CLOCK, "J");
    	}
    	//if not a constructor method than the object it belongs to has already been init'd
    	if(!constructor)
    		superInitialized = true;
    	
    }
    
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
    	super.visitMaxs(maxStack+5, maxLocals);
    }
    
    private boolean isFirstConstructor;
    
    AnalyzerAdapter analyzer;
    
    protected NonDeterministicReplayMethodVisitor(int api, MethodVisitor mv, int access,
    		String name, String desc, String classDesc, boolean isFirstConstructor,
    		AnalyzerAdapter analyzer, boolean isCallbackInit) {
    	super(api, mv);
    	this.name = name;
    	this.desc = desc;
    	this.classDesc = classDesc;
    	this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
    	this.constructor = "<init>".equals("name");
    	this.isFirstConstructor = isFirstConstructor;
    	this.analyzer = analyzer;
    	this.isCallbackInit = isCallbackInit;
    }
    
    private NonDeterministicReplayClassVisitor parent;
    
    private HashMap<String, MethodInsnNode> captureMethodsToGenerate = new HashMap<String, MethodInsnNode>();
    
    public void setClassVisitor(NonDeterministicReplayClassVisitor cv) {
    	this.parent = cv;
    }
	
    @Override
    public void visitEnd() {
    	super.visitEnd();
    	parent.addFieldMarkup(methodCallsToClear);
    	parent.addCaptureMethodsToGenerate(captureMethodsToGenerate);
    }
    
    private int lineNumber = 0;

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        lineNumber = line;
    }

    
    //HEART OF THIS CLASS
    private boolean inited;

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itfc) {
        if (owner.equals(Type.getInternalName(ChroniclerJExportRunner.class))
                && name.equals("genTestCase"))
            return;
        if (owner.equals("java/lang/reflect/Method") && name.equals("invoke")) {
            opcode = Opcodes.INVOKESTATIC;
            owner = "edu/columbia/cs/psl/chroniclerj/MethodInterceptor";
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "edu/columbia/cs/psl/chroniclerj/MethodInterceptor", "invokeReplay",
                    "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
            return;
        }
        try {
            MethodCall m = new MethodCall(this.name, this.desc, this.classDesc, 0, lineNumber,
                    owner, name, desc, isStatic);
            Type returnType = Type.getMethodType(desc).getReturnType();
            //case of a contstructor?
            if (opcode == INVOKESPECIAL
                    && name.equals("<init>")
                    && NonDeterministicLoggingMethodVisitor.nonDeterministicMethods.contains(owner
                            + "." + name + ":" + desc)) {
            	//I don't think that there is actually anything legal to be done here? -- JON
                super.visitMethodInsn(opcode, owner, name, desc, itfc);
                
                //if not a constructor
            } else if ((!constructor || isFirstConstructor || superInitialized)
                    && returnType.equals(Type.VOID_TYPE)
                    && !name.equals("<init>")
                    && NonDeterministicLoggingMethodVisitor.nonDeterministicMethods.contains(owner
                            + "." + name + ":" + desc)) {
            	//Pop the arguments off the stack
                Type[] args = Type.getArgumentTypes(desc);
                for (int i = args.length - 1; i >= 0; i--) {
                    Type t = args[i];
                    if (t.getSize() == 2)
                        mv.visitInsn(POP2);
                    else
                        mv.visitInsn(POP);
                }
                //if not a static method (i.e. requires object ref) pop the reference to the object off the stack 
                if (opcode != INVOKESTATIC)
                    mv.visitInsn(POP);

                // else
                // super.visitMethodInsn(opcode, owner, name, desc);
                
                // if nondeterministic method and is not a constructor i.e. read from the log
                // NOTE the return type is NOT VOID
            } else if ((!constructor || isFirstConstructor || superInitialized)
                    && !returnType.equals(Type.VOID_TYPE)
                    && NonDeterministicLoggingMethodVisitor.nonDeterministicMethods.contains(owner
                            + "." + name + ":" + desc)) {
                logger.debug("Adding field in MV to list " + m.getLogFieldName());
                methodCallsToClear.add(m);
				Type[] targs = Type.getArgumentTypes(desc);
				for (int i = targs.length - 1; i >= 0; i--) {
					Type t = targs[i];
					if (t.getSort() == Type.ARRAY) {
						//read the log
						getNextReplay(t);
						super.visitInsn(DUP);
						super.visitInsn(ARRAYLENGTH);
						//Copy the contents of the replay'ed array into the one on stack.
						super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReplayUtils.class), "copyInto", "(Ljava/lang/Object;Ljava/lang/Object;I)V", false);
					} else {
						//Pop the values off the stack to make space for replay'ed values
						switch (t.getSize()) {
						case 2:
							mv.visitInsn(POP2);
							break;
						case 1:
						default:
							mv.visitInsn(POP);
							break;
						}
					}
				}
				//Pop off the reference if needed
				if (opcode != INVOKESTATIC)
                    mv.visitInsn(POP);
				
				//if you need tot get something go get it
                if (returnType.getSort() != Type.VOID)
                    getNextReplay(m.getReturnType());
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itfc);
                //TODO ask Jon how this is different from the top case
                if(constructor && !superInitialized && opcode == INVOKESPECIAL && name.equals("<init>"))
             	{
             		onMethodEnter();
             		superInitialized = true;
             	}
            }
        } catch (Exception ex) {
            logger.error("Unable to instrument method call", ex);
        }
    }
    
	private void getNextReplay(Type t) {
		switch (t.getSort()) {
		case Type.OBJECT:
		case Type.ARRAY:
			//if an array call Object ReplayUtils.getNextObject()
			super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReplayUtils.class), "getNextObject", "()Ljava/lang/Object;", false);
			super.visitTypeInsn(CHECKCAST, t.getInternalName());
			break;	
		default:
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ReplayUtils.class), "getNext"+t.getDescriptor(), "()"+t.getDescriptor(), false);
			break;
		}
	}
	
	protected void onMethodEnter() {
		//If this is a callback constructor 
		//	 CallbackRegistry.register(method)
        if (this.name.equals("<init>") && isCallbackInit) {
        	//load object reference
            super.visitVarInsn(ALOAD, 0);
            //register the object with the callback registry
            super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(CallbackRegistry.class),
                    "register", "(Ljava/lang/Object;)V", false);
            inited = true;
        }
    }

}
