/**
 * Logback:
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.MDC;

import ch.qos.logback.assemble.output.FileWriterBase;
import ch.qos.logback.assemble.output.MessageQueOutput;
import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.ErrorStatus;

/**
 * Assembly appender.
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class AssembleAppender<E> extends FileAppender<E> {
	private static final int UNDEFINED = -1;

	/**
	 * the log filename, support name pattern.
	 */
	private String fileName;

	/**
	 * rolling policy include the filename pattern.
	 */
	private AssembleRollingPolicyBase rollingPolicy = null;

	protected ThreadLocal<Boolean> mdcDefaulted = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}
	};

	/**
	 * the default MDC pairs.
	 */
	private Map<String, String> defaultMDCProperties = null;

	private String defaultMDCPropertiesStr;

	/**
	 * the custom thread LogLevel define label name.
	 */
	private String defaultMDCLogLevelLabel = "LEVEL";

	/**
	 * the LogLevel can be setted in defaultMDCProperties.
	 */
	private String defaultMDCLogLevelValue = null;

	/**
	 * inner output writer.
	 */
	private MessageQueOutput output = null;

	/**
	 * whether asynchronized logging action.
	 */
	private boolean asyncFlag = false;
	
	private int asyncThreads = 1;

	/**
	 * When async, whether need to support caller infomation , such as file
	 * name, line number.<br>
	 */
	private boolean includeCallerData = false;

	/**
	 * When async, the max file message queue size.
	 */
	private int queueSize = 8092;

	/**
	 * When async, <br>
	 * the contition for threshold to discard message , default 20% *
	 * $queueSize.<br>
	 * if remainCapacity less then $discardingThreshold, the discard the
	 * TRACE/DEBUG/INFO messages, just keep WARN/ERROR messages.<br>
	 */
	private int discardingThreshold = UNDEFINED;

	static {
		MDC.put("Assemble", "true");
	}

	public AssembleAppender() {
		super();
		defaultMDCProperties = new HashMap();
	}

	public Encoder<E> getEncoder() {
		return encoder;
	}

	public String getFile() {
		return fileName;
	}

	public void setFile(String fileName) {
		this.fileName = fileName == null ? null : fileName.trim();
		if (this.getRollingPolicy() != null && this.getRollingPolicy() instanceof AssembleRollingPolicyBase)
			((AssembleRollingPolicyBase) this.getRollingPolicy()).setAppenderFileName(this.getFile());
	}

	public void setAppend(boolean append) {
		super.setAppend(append);
		if (this.getRollingPolicy() != null && this.getRollingPolicy() instanceof AssembleRollingPolicyBase)
			((AssembleRollingPolicyBase) this.getRollingPolicy()).setFileAppend(this.isAppend());
	}

	public AssembleRollingPolicyBase getRollingPolicy() {
		return rollingPolicy;
	}

	public void setRollingPolicy(AssembleRollingPolicyBase rollingPolicy) {
		this.rollingPolicy = rollingPolicy;
		// this.initOutput();
		// this.output.setRollingPolicy(this.rollingPolicy);
		if (this.getFile() != null)
			rollingPolicy.setAppenderFileName(this.getFile());
		this.rollingPolicy.setFileAppend(this.isAppend());
		// if (this.queueSize != UNDEFINED)
		// this.rollingPolicy.setAppenderQueueSize(this.queueSize);
		// if (this.discardingThreshold != UNDEFINED)
		// this.rollingPolicy.setAppenderDiscardingThreshold(this.discardingThreshold);
	}

	public String getDefaultMDCLogLevelLabel() {
		return defaultMDCLogLevelLabel;
	}

	public void setDefaultMDCLogLevelLabel(String defaultMDCLogLevelLabel) {
		this.defaultMDCLogLevelLabel = defaultMDCLogLevelLabel;
	}

	public String getDefaultMDCLogLevelValue() {
		return defaultMDCLogLevelValue;
	}

	public void setDefaultMDCLogLevelValue(String defaultMDCLogLevelValue) {
		this.defaultMDCLogLevelValue = defaultMDCLogLevelValue;
	}

	public void setEncoder(Encoder<E> encoder) {
		this.encoder = encoder;
	}

	private MessageQueOutput buildOutput() {
		output = new MessageQueOutput(this, asyncFlag);
		output.start();
		return output;
	}

	public Map<String, String> getDefaultMDCProperties() {
		return defaultMDCProperties;
	}

	/**
	 * 
	 * <defaultMDCProperties>TRDATE=Y,TRCODE=X</defaultMDCProperties><br>
	 * 
	 * @param defaultMDCPropertiesStr
	 */
	public void setDefaultMDCProperties(String defaultMDCPropertiesStr) {
		this.defaultMDCPropertiesStr = defaultMDCPropertiesStr;
		String ps = defaultMDCPropertiesStr.replaceAll(" ", "").replaceAll("	", "");
		String[] pa = ps.split("[;,:]");
		for (String pi : pa) {
			String[] pia = pi.split("=");
			if (pia.length == 2) {
				defaultMDCProperties.put(pia[0], pia[1]);
			} else {
				addError("Illegal format of defaultMDCProperties setting : " + defaultMDCPropertiesStr);
			}
		}

		/**
		 * set the MDC for main thread and will be Inherited by children thread.
		 */
		for (Iterator ite = defaultMDCProperties.entrySet().iterator(); ite.hasNext();) {
			Map.Entry entry = (Map.Entry) ite.next();
			String k = (String) entry.getKey();
			String v = (String) entry.getValue();
			if (MDC.get(k) == null) {
				addInfo("Initializing set MDC " + k + "=" + v + " in " + Thread.currentThread().getName());
				MDC.put(k, v);
			}
		}
	}

	public boolean isAsync() {
		return this.asyncFlag;
	}

	public void setAsync(boolean async) {
		this.asyncFlag = async;
	}

	public int getAsyncThreads() {
		return asyncThreads;
	}

	public void setAsyncThreads(int asyncThreads) {
		this.asyncThreads = asyncThreads;
	}

	public boolean isIncludeCallerData() {
		return includeCallerData;
	}

	public void setIncludeCallerData(boolean includeCallerData) {
		this.includeCallerData = includeCallerData;
	}

	public int getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public int getDiscardingThreshold() {
		return discardingThreshold;
	}

	public void setDiscardingThreshold(int discardingThreshold) {
		this.discardingThreshold = discardingThreshold;
	}

	public String toString() {
		return this.getClass().getSimpleName() + "@" + this.hashCode() + "[" + this.name + "]";
	}

	public String toFullString() {
		return "AssembleAppender [fileName=" + fileName + ", encoder=" + encoder + ", rollingPolicy=" + rollingPolicy
				+ ", mdcDefaulted=" + mdcDefaulted + ", defaultMDCProperties=" + defaultMDCProperties
				+ ", defaultMDCPropertiesStr=" + defaultMDCPropertiesStr + ", output=" + output + ", name=" + name
				+ "]";
	}

	protected void setThreadDefaultMDC(ILoggingEvent event) {
		if (mdcDefaulted.get().equals(Boolean.FALSE) && defaultMDCProperties.size() > 0) {
			Map eventMDCMap = event.getMDCPropertyMap();
			if (eventMDCMap == null) {
				if (event instanceof ch.qos.logback.classic.spi.LoggingEvent) {
					eventMDCMap = new HashMap();
					((ch.qos.logback.classic.spi.LoggingEvent) event).setMDCPropertyMap(eventMDCMap);
				}
			} else if (eventMDCMap.equals(Collections.EMPTY_MAP)) {
				// ch.qos.logback.classic.spi.LoggingEvent not support the
				// setMDCPropertyMap for replace.
			}
			for (Iterator ite = defaultMDCProperties.entrySet().iterator(); ite.hasNext();) {
				Map.Entry entry = (Map.Entry) ite.next();
				String k = (String) entry.getKey();
				String v = (String) entry.getValue();

				if (MDC.get(k) == null) {
					MDC.put(k, v);
					addInfo("LoggingEvent set MDC " + k + "=" + v + " in " + Thread.currentThread().getName());
				}

				if (eventMDCMap != null && eventMDCMap.get(k) == null) {
					if (!eventMDCMap.equals(Collections.EMPTY_MAP)) {
						try {
							eventMDCMap.put(k, v);
							addInfo("LoggingEvent set eventMDC " + k + "=" + v + " in "
									+ Thread.currentThread().getName());
						} catch (Throwable e) {
							addError("put eventMDCMap(" + k + "," + defaultMDCProperties.get(k)
									+ "), event.getMDCPropertyMap.class " + event.getMDCPropertyMap().getClass()
									+ ", event.class " + event.getClass() + " in " + Thread.currentThread().getName(),
									e);
						}
					} else {
						addWarn("Unsupported LoggingEvent(" + event.getClass() + ") MDCMap type "
								+ event.getMDCPropertyMap().getClass() + " in " + Thread.currentThread().getName());
					}
				}
			}
			mdcDefaulted.set(Boolean.TRUE);
		}
	}

	/**
	 * thread local level setting from MDC("LEVEL").
	 * 
	 * @param event
	 * @return
	 */
	protected FilterReply getFilterThreadLevelDecision(ILoggingEvent event) {
		String threadLocalLevelStr = event.getMDCPropertyMap().get(defaultMDCLogLevelLabel);
		if (threadLocalLevelStr == null || threadLocalLevelStr.length() == 0)
			return FilterReply.ACCEPT;
		Level threadLocalLevel = Level.toLevel(threadLocalLevelStr, null);
		if (threadLocalLevel == null)
			return FilterReply.ACCEPT;
		else if (threadLocalLevel.equals(Level.OFF))
			return FilterReply.DENY;
		else if (event.getLevel().isGreaterOrEqual(threadLocalLevel))
			return FilterReply.ACCEPT;
		return FilterReply.DENY;
	}

	@Override
	protected void append(E eventObject) {
		if (!(isStarted())) {
			addError(this + " append have not started, ignore it.", null);
			return;
		}

		if (getFilterThreadLevelDecision((ILoggingEvent) eventObject).equals(FilterReply.DENY)) {
			return;
		}

		try {
			setThreadDefaultMDC((ILoggingEvent) eventObject);
			output.write((ILoggingEvent) eventObject);
		} catch (Exception e) {
			addError("append expception", e);
		}
	}

	public void start() {
		addInfo(this + " starting ...");
		int errors = 0;
		if (this.encoder == null) {
			addStatus(new ErrorStatus("No encoder set for the appender named \"" + this.name + "\".", this));
			++errors;
		}
		if (this.fileName == null && this.rollingPolicy == null) {
			addStatus(new ErrorStatus("No filename set for the appender named \"" + this.name + "\".", this));
			++errors;
		}

		if (null == buildOutput()) {
			addStatus(new ErrorStatus("Build output failed.", this));
			++errors;
		}

		if (errors == 0) {
			FileWriterBase.getInstance(this.getContext());
			this.started = true;
		}
	}

	public void stop() {
		addInfo(this + " stopping ...");
		if (output != null)
			output.stop();
		started = false;

		super.stop();
	}

}
