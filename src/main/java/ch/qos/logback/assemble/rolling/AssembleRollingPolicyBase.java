/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.rolling;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.helper.Compressor;
import ch.qos.logback.core.rolling.helper.RenameUtil;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public abstract class AssembleRollingPolicyBase extends RollingPolicyBase {
	static final int UNDEFINED = -1;
	
	/**
	 * the appender's info.
	 */
	protected boolean fileAppend;
	protected String appenderFileName;
	protected int appenderDiscardingThreshold = UNDEFINED;
	protected int appenderQueueSize = UNDEFINED;

	/**
	 * the rolling info.
	 */
	protected Map<String, FileNamePattern> fileNamePatternWCSes;
	protected Compressor compressor;
	protected RenameUtil renameUtil;

	public AssembleRollingPolicyBase() {
		this.renameUtil = new RenameUtil();
	}

	public Compressor getCompressor() {
		return compressor;
	}

	public void setCompressor(Compressor compressor) {
		this.compressor = compressor;
	}

	public RenameUtil getRenameUtil() {
		return renameUtil;
	}

	public void setRenameUtil(RenameUtil renameUtil) {
		this.renameUtil = renameUtil;
	}

	public boolean isFileAppend() {
		return fileAppend;
	}

	public void setFileAppend(boolean fileAppend) {
		this.fileAppend = fileAppend;
	}

	public String getAppenderFileName() {
		return appenderFileName;
	}

	public void setAppenderFileName(String appenderFileName) {
		this.appenderFileName = appenderFileName;
	}

	public int getAppenderDiscardingThreshold() {
		return appenderDiscardingThreshold;
	}

	public void setAppenderDiscardingThreshold(int appenderDiscardingThreshold) {
		this.appenderDiscardingThreshold = appenderDiscardingThreshold;
	}

	public int getAppenderQueueSize() {
		return appenderQueueSize;
	}

	public void setAppenderQueueSize(int appenderQueueSize) {
		this.appenderQueueSize = appenderQueueSize;
	}

	public boolean isDynamicPattern(String fileNamePattern) {
		return FileNamePattern.isDynamicPattern(fileNamePattern);
	}

	public boolean isActiveSamePattern() {
		if (this.appenderFileName == null)
			return true;
		if (this.getFileNamePattern() == null)
			return true;
		return this.appenderFileName.equals(this.getFileNamePattern());
	}

	public boolean isQueueBelowDiscardingThreshold(int remainingCapacity) {
		if (appenderDiscardingThreshold == UNDEFINED)
			return false;
		return remainingCapacity < appenderDiscardingThreshold;
	}

	abstract public String getActiveFileName(ILoggingEvent event);

	abstract public String getRollingFileName(ILoggingEvent event);

}
