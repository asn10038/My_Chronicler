package edu.columbia.cs.psl.ChroniclerJ_2;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.objectweb.asm.Type;

/** Object representing invoking a callback method
 * 
 * @author ant
 *
 */
//TODO ask Jon about CallbackInvocation
public class CallbackInvocation {
	 @Override
	    public String toString() {
	        return "CallbackInvocation [clazz=" + clazz + ", methodName=" + methodName
	                + ", methodDesc=" + methodDesc + ", ownerID=" + ownerID + ", executed=" + executed
	                + ", clock=" + clock + "]";
	    }

	    private String clazz;

	    private String methodName;

	    private String methodDesc;

	    private Object[] args;

	    private String ownerID;

	    private boolean executed;

	    private int clock;

	    private String threadName;
	    
	    public CallbackInvocation(String clazz, String methodName, String methodDesc, Object[] args,
	            Object owner) {
	        this.clazz = clazz;
	        this.methodName = methodName;
	        this.methodDesc = methodDesc;
	        this.args = args;
	        this.ownerID = CallbackRegistry.getId(owner);
	        //clock records the order in which it was executed by using the fill index
	        this.clock = SerializableLog.aLog_fill + SerializableLog.bLog_fill
	                + SerializableLog.cLog_fill + SerializableLog.dLog_fill + SerializableLog.fLog_fill
	                + SerializableLog.jLog_fill + SerializableLog.sLog_fill + SerializableLog.zLog_fill
	                + Log.aLog_fill;
	        this.threadName = Thread.currentThread().getName();
	    }
	    
	    public String getThreadName() {
	        return threadName;
	    }

	    public int getClock() {
	        return clock;
	    }

	    public void resetExecuted() {
	        executed = false;
	    }
	    
	    public boolean invoke() {
	        if (executed)
	            return false;
	        executed = true;
	        try {
	            if (CallbackRegistry.get(ownerID) == null) {
	                // System.out.println("Queued");
	                CallbackRegistry.queueInvocation(ownerID, this);
	            } else {
	                if (!this.threadName.startsWith("AWT-EventQueue-")) {
	                    try {
	                        final Object owner = CallbackRegistry.get(ownerID);
	                        final Method method = getMethod();
	                        EventQueue.invokeAndWait(new Runnable() {

	                            @Override
	                            public void run() {
	                                try {
	                                    // System.out.println("Dispatching to AWT");
	                                    method.invoke(owner, args);
	                                    // System.out.println("Executed");
	                                } catch (IllegalAccessException | IllegalArgumentException
	                                        | InvocationTargetException e) {
	                                    // TODO Auto-generated catch block
	                                    e.getCause().printStackTrace();
	                                    System.exit(-1);
	                                }
	                            }
	                        });
	                    } catch (InterruptedException e) {
	                        // TODO Auto-generated catch block
	                        e.printStackTrace();
	                    }
	                } else {
	                    try {
	                    	//TODO why is AWT-EventQueue important?
	                        getMethod().invoke(CallbackRegistry.get(ownerID), args);
	                    } catch (IllegalAccessException | IllegalArgumentException
	                            | InvocationTargetException e) {
	                        // TODO Auto-generated catch block
	                        e.getCause().printStackTrace();
	                        System.exit(-1);
	                    }
	                }
	            }
	        } catch (IllegalArgumentException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        } catch (InvocationTargetException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        return true;
	    }
	    
	    /** 
	     * pulls the method matching methodDesc static variable and the Class of the ownerID at this point
	     * seems useful in replay 
	     * @return
	     */
	    public Method getMethod() {
	        Type[] argDesc = Type.getMethodType(methodDesc).getArgumentTypes();
	        return getMethod(methodName, argDesc, CallbackRegistry.get(ownerID).getClass());
	    }
	    
	    protected Method getMethod(String methodName, Type[] types, Class<?> clazz) {
	    	try {
	    		for (Method m : clazz.getMethods()) {
	    			boolean ok = true;
	    			if(methodName.equals(m.getName())) {
	    				Class<?>[] mArgs = m.getParameterTypes();
	    				if(mArgs.length != types.length)
	    					break;
	    				for(int i=0; i<mArgs.length; i++) {
	    					if(!mArgs[i].getName().equals(types[i].getClassName()))
	    						ok = false;
	    				}
	    				
	    				if(ok) {
	    					if(!m.isAccessible())
	    						m.setAccessible(true);
	    					return m;
	    				}
	    					
	    			}
	    				
	    		}
	    	}
	    	catch(SecurityException se) {
	    		se.printStackTrace();
	    	}
	    	if(clazz.getSuperclass() != null)
	    		return getMethod(methodName, types, clazz.getSuperclass());
	    	return null;
	    }


}
