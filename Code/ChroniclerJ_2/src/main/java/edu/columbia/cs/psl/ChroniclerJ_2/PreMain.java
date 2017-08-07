package edu.columbia.cs.psl.ChroniclerJ_2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.SerialVersionUIDAdder;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import edu.columbia.cs.psl.chroniclerj.replay.NonDeterministicReplayClassVisitor;
import edu.columbia.cs.psl.chroniclerj.visitor.CallbackDuplicatingClassVisitor;
import edu.columbia.cs.psl.chroniclerj.visitor.NonDeterministicLoggingClassVisitor;

//Contains definition of the class transformer
public class PreMain {
	
	public static boolean replay;
	
	private static final class HackyClassWriter extends ClassWriter {
		//ClassWriter is from the ASM package and allows for writing the byte code of java classes
		private HackyClassWriter(ClassReader classReader, int flags) {
			super(classReader, flags);
		}
		
		protected String getCommonSuperClass(String type1, String type2) {
			//all objects have java/lang/Object as a common superclass
			return "java/lang/Object";
		}
	}
	
	static class ChroniclerTransformer implements ClassFileTransformer {
		
		@Override
		//transforms one class into another
		public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
			// Instrumenter.loader = loader;
			if (replay) {
//				try {
//					ClassReader cr = new ClassReader(classfileBuffer);
//					if (isIgnoredClass(cr.getClassName()))
//						return null;
//					if (DEBUG)
//						System.out.println("Inst: " + cr.getClassName());
//					ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
//					NonDeterministicReplayClassVisitor cv = new NonDeterministicReplayClassVisitor(Opcodes.ASM5, cw);
//					cr.accept(cv, ClassReader.EXPAND_FRAMES);
//					if (DEBUG) {
//						File f = new File("debug-replay/" + className + ".class");
//						f.getParentFile().mkdirs();
//						FileOutputStream fos = new FileOutputStream(f);
//						fos.write(cw.toByteArray());
//						fos.close();
//					}
//					return cw.toByteArray();
//				} catch (Throwable t) {
//					t.printStackTrace();
//					return null;
//				}
			} else {
				ClassReader cr = new ClassReader(classfileBuffer);
				className = cr.getClassName();
				//don't transform the ignored classes
				if (isIgnoredClass(cr.getClassName()))
					return null;
				if (DEBUG)
					System.out.println("Inst: " + cr.getClassName());

				boolean skipFrames = false;
				ClassNode cn = new ClassNode();
				cr.accept(cn, ClassReader.SKIP_CODE);//skip code means skip method code in the class i.e. look at annotations etc.
				//check how old the class is
				if (cn.version >= 100 || cn.version <= 50 || className.endsWith("$Access4JacksonSerializer") || className.endsWith("$Access4JacksonDeSerializer"))
					skipFrames = true;

				if (skipFrames) {
					// This class is old enough to not guarantee frames.
					// Generate new frames for analysis reasons, then make sure
					// to not emit ANY frames.
					ClassWriter cw = new HackyClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
					cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
						@Override
						public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
							//TODO ask Jon what this is
							return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
						}
					}, 0);
					cr = new ClassReader(cw.toByteArray());
				}

				try {
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					NonDeterministicLoggingClassVisitor cv = new NonDeterministicLoggingClassVisitor(new SerialVersionUIDAdder(cw));
					CallbackDuplicatingClassVisitor callbackDuplicator = new CallbackDuplicatingClassVisitor(cv);

					cr.accept(callbackDuplicator, ClassReader.EXPAND_FRAMES);
					if (DEBUG) {
						File f = new File("debug-record/" + className + ".class");
						f.getParentFile().mkdirs();
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(cw.toByteArray());
						fos.close();
					}
					cr = new ClassReader(cw.toByteArray());
					CheckClassAdapter ca = new CheckClassAdapter(new ClassWriter(0));
					cr.accept(ca, 0);
					return cw.toByteArray();
				} catch (Throwable t) {
					t.printStackTrace();

					TraceClassVisitor tcv = null;
					PrintWriter fw = null;
					try {
						File f = new File("z.class");
						if(f.getParentFile() != null)
							f.getParentFile().mkdirs();
						f.delete();
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(classfileBuffer);
						fos.close();
						fw = new PrintWriter("lastClass.txt");

						tcv = new TraceClassVisitor(fw);
						NonDeterministicLoggingClassVisitor cv = new NonDeterministicLoggingClassVisitor(new SerialVersionUIDAdder(tcv));
						CallbackDuplicatingClassVisitor callbackDuplicator = new CallbackDuplicatingClassVisitor(cv);

						cr.accept(callbackDuplicator, ClassReader.EXPAND_FRAMES);

					} catch (Throwable t2) {
						t2.printStackTrace();
					} finally {
						if(tcv != null)
						tcv.visitEnd();
						fw.close();
					}

					return null;
				}
			}
		}
	}
	
	public static boolean isIgnoredClass(String className) {
		if (whiteList != null) {
			for(String s : whiteList)
			{
				if(className.startsWith(s))
					return false;
				return true;
					
			}
		}
		//ignore classes that are needed by chronicler to run
		return className.startsWith("java") || className.startsWith("com/sun") || className.startsWith("edu/columbia/cs/psl/Chroniclerj_2") || className.startsWith("com/rits/cloning") || className.startsWith("jdk")
				|| className.startsWith("com/thoughtworks") || className.startsWith("org/xmlpull") || className.startsWith("org/kxml2");
	}
	
	static boolean DEBUG = false;
	static String[] whiteList;

}
