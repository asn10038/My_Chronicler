package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import java.util.HashSet;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ChroniclerJ_2.CloningUtils;
import edu.columbia.cs.psl.ChroniclerJ_2.Log;


//Determines what should and shouldn't be cloned when visiting instructions
public class CloningAdviceAdapter extends InstructionAdapter implements Opcodes {

	private static final HashSet<String> immutableClasses = new HashSet<String>();

    private static final HashSet<String> nullInsteads = new HashSet<>();
    static {
        immutableClasses.add("Ljava/lang/Integer;");
        immutableClasses.add("Ljava/lang/Long;");
        immutableClasses.add("Ljava/lang/Short;");
        immutableClasses.add("Ljava/lang/Float;");
        immutableClasses.add("Ljava/lang/String;");
        immutableClasses.add("Ljava/lang/Char;");
        immutableClasses.add("Ljava/lang/Byte;");
        immutableClasses.add("Ljava/lang/Integer;");
        immutableClasses.add("Ljava/lang/Long;");
        immutableClasses.add("Ljava/lang/Short;");
        immutableClasses.add("Ljava/lang/Float;");
        immutableClasses.add("Ljava/lang/String;");
        immutableClasses.add("Ljava/lang/Char;");
        immutableClasses.add("Ljava/lang/Byte;");
        immutableClasses.add("Ljava/sql/ResultSet;");
        immutableClasses.add("Ljava/lang/Class;");
        immutableClasses.add("Ljava/net/InetAddress;");
        immutableClasses.add("Ljava/util/TimeZone;");
        immutableClasses.add("Ljava/util/zip/ZipEntry;");
        immutableClasses.add("Z");
        immutableClasses.add("B");
        immutableClasses.add("C");
        immutableClasses.add("S");
        immutableClasses.add("I");
        immutableClasses.add("J");
        immutableClasses.add("F");
        immutableClasses.add("L");
        for (Class<?> cz : CloningUtils.moreIgnoredImmutables) {
            immutableClasses.add(Type.getDescriptor(cz));
        }

        for (Class<?> cz : CloningUtils.nullInsteads) {
            nullInsteads.add(Type.getDescriptor(cz));
        }
    }
    
    CloningAdviceAdapter(MethodVisitor mv, int access, String name, String desc,
    		String classname, AnalyzerAdapter analyzer) {
    	super(Opcodes.ASM5, mv);
    	this.analyzer = analyzer;
    }

    protected void logValueAtTopOfStackToArrayNoDup(String logFieldOwner, String logFieldName,
            String logFieldTypeDesc, Type elementType, boolean isStaticLoggingField, String debug,
            boolean secondElHasArrayLen, boolean doLocking) {
    	String t;
    	switch(elementType.getSort())
    	{
	    	case Type.ARRAY:
	    	case Type.OBJECT:
	    		t = "Ljava/lang/Object;";
	    		break;
	    		default:
	    			t = elementType.getDescriptor();
    	}
    	super.visitLdcInsn(debug);
    	//clever...if object type will call log(obj,string) else will call log(prim,string)
    	//THIS TRIGGERS A CALL TO LOG THE OBJECT...I FINALLY FOUND ONE
    	super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Log.class), "log", "("+t+"Ljava/lang/String;)V", false);
    }
    
    private LocalVariablesSorter lvsorter;
	private AnalyzerAdapter analyzer;
	public void setLocalVariableSorter(LocalVariablesSorter smv) {
		this.lvsorter = smv;
	}
}
