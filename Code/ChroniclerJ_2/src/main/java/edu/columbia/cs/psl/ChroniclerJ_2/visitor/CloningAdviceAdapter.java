package edu.columbia.cs.psl.ChroniclerJ_2.visitor;

import java.util.HashSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.cs.psl.ChroniclerJ_2.CloningUtils;

public class CloningAdviceAdapter {

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

    private LocalVariablesSorter lvsorter;
	private AnalyzerAdapter analyzer;
}
