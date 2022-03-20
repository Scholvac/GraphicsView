package de.sos.gvc.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * There are two ways of logging within GVLog.
 * 1) Using the GVLog's static methods for easy logging, without need to create a logger for each class. Thereby the GVLog automatically detects the calling class
 * 2) Use the GVLog method to create a static logger for a specific classifier.
 *
 * The second method should be used if heavily logging is required and performance becomes an issue, since the static methods of GVLog do need to detect the Loggers name
 * at every method invocation.
 *
 * @author scholvac
 *
 */
public class GVLog {

	private static GVLog theInstance = new GVLog();

	public static GVLog getInstance() {
		return theInstance;
	}

	private GVLog(){
	}


	public static String getCallerClassName() {
		//using reflections should be quite fast, but unfortunately oracle removed (not only deprecated!) this method
		//return sun.reflect.Reflection.getCallerClass(callStackDepth).getName();
		final StackTraceElement[] st = Thread.currentThread().getStackTrace();
		return st[3].getClassName();
	}

	public static void error(final String message)
	{
		try{
			final Logger l = getLogger(getCallerClassName());
			if (l != null && l.isWarnEnabled())
				l.error(message);
		}catch(final Exception e){ System.out.println("Failed to Log message: " + message); }
	}


	public static void warn(final String message)
	{
		try{
			final Logger l = getLogger(getCallerClassName());
			if (l != null & l.isWarnEnabled())
				l.warn(message);
		}catch(final Exception e){ System.out.println("Failed to Log message: " + message); }
	}

	public static void info(final String message)
	{
		try{
			final Logger l = getLogger(getCallerClassName());
			if (l != null && l.isInfoEnabled())
				l.info(message);
		}catch(final Exception e){System.out.println("Failed to Log message: " + message); }
	}

	public static void debug(final String message)
	{
		try{
			final Logger l = getLogger(getCallerClassName());
			if (l != null && l.isDebugEnabled())
				l.debug(message);
		}catch(final Exception e){ System.out.println("Failed to Log message: " + message); }
	}

	public static void trace(final String message)
	{
		try{
			final Logger l = getLogger(getCallerClassName());
			if (l != null && l.isTraceEnabled())
				l.trace(message);
		}catch(final Exception e){ System.out.println("Failed to Log message: " + message); }
	}

	/**
	 * get or create a logger with a qualified name
	 * @note if the GVLog has not been initialized this method will initialize the currently used GVLog instance with default values
	 * @param fqcn
	 * @return logger
	 */
	public static Logger getLogger(final String fqcn) {
		try{
			return LoggerFactory.getLogger(fqcn);
		}catch(Exception | Error err){
			err.printStackTrace();
			return null;
		}
	}

	public static Logger getLogger(final Class<?> clazz) {
		return getLogger(clazz.getName());
	}


}
