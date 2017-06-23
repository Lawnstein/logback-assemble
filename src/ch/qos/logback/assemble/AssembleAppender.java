/**
 * Logback:
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.MDC;

import ch.qos.logback.assemble.output.AssembleOutputBase;
import ch.qos.logback.assemble.output.BlockingQueuedOutput;
import ch.qos.logback.assemble.output.CentralizedFileWriter;
import ch.qos.logback.assemble.output.RingBufferedOutput;
import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.ErrorStatus;

/**
 * .
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class AssembleAppender<E> extends FileAppender<E> {

	private String fileName;

	private AssembleRollingPolicyBase rollingPolicy = null;

	protected ThreadLocal<Boolean> mdcDefaulted = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}
	};

	private Map<String, String> defaultMDCProperties = null;

	private String defaultMDCPropertiesStr;

	private AssembleOutputBase output = null;
	
	/**
	 * RingBuffer && BlockQueue.
	 */
	private String outputType = "BlockQueue"; 

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
		this.fileName = fileName;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public RollingPolicy getRollingPolicy() {
		return rollingPolicy;
	}

	public void setRollingPolicy(AssembleRollingPolicyBase rollingPolicy) {
		this.rollingPolicy = rollingPolicy;
		this.initOutput();
		this.output.setRollingPolicy(this.rollingPolicy);
	}

	public void setEncoder(Encoder<E> encoder) {
		this.encoder = encoder;
		this.initOutput();
		this.output.setEncoder(this.encoder);
		// this.addInfo("Assemble.Encoder " + encoder.getClass());
	}

	public void initOutput() {
		if (this.output == null) {
			// this.output = new BlockingQueuedOutput();
			if (getOutputType().equalsIgnoreCase("RingBuffer"))
				this.output = new RingBufferedOutput();
			else
				this.output = new BlockingQueuedOutput();
			this.output.setContext(this.getContext());
			CentralizedFileWriter.getInstance().setContext(this.getContext());
			this.addInfo(this + " OutputType " + getOutputType());
		}
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
				this.addInfo("Initializing set MDC " + k + "=" + v + " in " + Thread.currentThread().getName());
				MDC.put(k, v);
			}
		}
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
					this.addInfo("LoggingEvent set MDC " + k + "=" + v + " in " + Thread.currentThread().getName());
				}

				if (eventMDCMap != null && eventMDCMap.get(k) == null) {
					if (!eventMDCMap.equals(Collections.EMPTY_MAP)) {
						try {
							eventMDCMap.put(k, v);
							this.addInfo("LoggingEvent set eventMDC " + k + "=" + v + " in "
									+ Thread.currentThread().getName());
						} catch (Throwable e) {
							addError("put eventMDCMap(" + k + "," + defaultMDCProperties.get(k)
									+ "), event.getMDCPropertyMap.class " + event.getMDCPropertyMap().getClass()
									+ ", event.class " + event.getClass() + " in " + Thread.currentThread().getName(),
									e);
						}
					} else {
						this.addWarn("Unsupported LoggingEvent(" + event.getClass() + ") MDCMap type "
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
		String threadLocalLevelStr = event.getMDCPropertyMap().get("LEVEL");
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
			addError("append have not started, ignore it.", null);
			return;
		}

		if (getFilterThreadLevelDecision((ILoggingEvent) eventObject).equals(FilterReply.DENY)) {
			return;
		}

		try {
			setThreadDefaultMDC((ILoggingEvent) eventObject);
			String msg = doEncode(eventObject);
			String fileName = this.doActiveFileName(eventObject);
			output.write((ILoggingEvent) eventObject, fileName, msg);
		} catch (Exception e) {
			addError("append expception", e);
		}
	}

	public void start() {
		this.addInfo(this + " starting ...");
		int errors = 0;
		if (this.encoder == null) {
			addStatus(new ErrorStatus("No encoder set for the appender named \"" + this.name + "\".", this));
			++errors;
		}
		if (this.fileName == null && this.rollingPolicy == null) {
			addStatus(new ErrorStatus("No filename set for the appender named \"" + this.name + "\".", this));
			++errors;
		}

		if (errors == 0) {
			CentralizedFileWriter.getInstance().start();
			this.started = true;
		}
	}

	public void stop() {
		this.addInfo(this + " stopping ...");
		if (this.output != null)
			this.output.stop();
		CentralizedFileWriter.getInstance().stop();
		this.started = false;
	}

	public String doEncode(E eventObject) {
		if (this.encoder instanceof LayoutWrappingEncoder) {
			return ((LayoutWrappingEncoder) this.encoder).getLayout().doLayout(eventObject);
		} else {
			this.addError("unsupported encoder " + encoder.getClass());
		}
		return null;
	}

	public String doActiveFileName(E eventObject) {
		if (rollingPolicy != null) {
			return this.rollingPolicy.getActiveFileName((ILoggingEvent) eventObject);
		} else if (this.fileName != null && this.fileName.length() > 0) {
			return this.fileName;
		}
		return null;
	}

	public static String getStackTrace(Throwable e) {
		try {
			ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
			e.printStackTrace(new java.io.PrintWriter(buf, true));
			String stack = buf.toString();
			buf.close();
			return stack;
		} catch (IOException e1) {
		}
		return null;
	}
}
