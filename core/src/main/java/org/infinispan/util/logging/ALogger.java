package org.infinispan.util.logging;

import android.util.Log;

/**
 * Android logger adapter.
 * 
 * @author jjankovi
 *
 */
public class ALogger {

	private String tag;
	
	public ALogger(String tag) {
		this.tag = tag;
	}
	
	public boolean isTraceEnabled() {
		String level = System.getProperty("infinispan.logger.level");
		if (level == null) {
			return false;
		}
		if (level.equalsIgnoreCase("trace")) {
			return true;
		}
		return false;
	}
	
	public boolean isDebugEnabled() {
		String level = System.getProperty("infinispan.logger.level");
		if (level == null) {
			return false;
		}
		if (level.equalsIgnoreCase("trace") || level.equalsIgnoreCase("debug")) {
			return true;
		}
		return false;
	}
	
	public boolean isInfoEnabled() {
		return true;
	}
	
	public void debug(String message) {
		if (isDebugEnabled()) {
			Log.d(tag, message);
		}
	}
	
	public void debug(String message, Throwable t) {
		if (isDebugEnabled()) {
			Log.d(tag, message, t);
		}
	}
	
	public void info(String message) {
		Log.i(tag, message);
	}
	
	public void info(String message, Throwable t) {
		Log.i(tag, message, t);
	}
	
	public void warn(String message) {
		Log.w(tag, message);
	}
	
	public void warn(String message, Throwable t) {
		Log.w(tag, message, t);
	}
	
	public void trace(String message) {
		if (isTraceEnabled()) {
			Log.v(tag, message);
		}
	}
	
	public void trace(String message, Throwable t) {
		if (isTraceEnabled()) {
			Log.v(tag, message, t);
		}
	}
	
	public void error(String message) {
		Log.e(tag, message);
	}
	
	public void error(String message, Throwable t) {
		Log.e(tag, message, t);
	}
	
}
