/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.rolling;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.helper.Compressor;
import ch.qos.logback.core.util.FileSize;

/**
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class NamingAndSizeBasedRollingPolicy extends AssembleRollingPolicyBase {
	private final static Logger logger = LoggerFactory.getLogger(NamingAndSizeBasedRollingPolicy.class);

	private FileSize maxFileSize;

	private String maxFileSizeAsString;

	private int maxHistory = Integer.MAX_VALUE;

	// TimeBasedFileNamingAndTriggeringPolicyBase
	// getCurrentPeriodsFileNameWithoutCompressionSuffix
	// TimeBasedRollingPolicy

	// static final String FNP_NOT_SET = "The FileNamePattern option must be set
	// before using TimeBasedRollingPolicy. ";

	public NamingAndSizeBasedRollingPolicy() {
		super();
		this.maxHistory = Integer.MAX_VALUE;
	}

	public FileSize getMaxFileSize() {
		return this.maxFileSize;
	}

	public void setMaxFileSize(String maxFileSize) {
		this.maxFileSizeAsString = maxFileSize;
		this.maxFileSize = FileSize.valueOf(maxFileSize);
	}

	public int getMaxHistory() {
		return maxHistory;
	}

	public void setMaxHistory(int maxHistory) {
		this.maxHistory = maxHistory;
	}

	@Override
	public String toString() {
		return "NamingAndSizeBasedRollingPolicy [maxFileSize=" + maxFileSize + ", maxFileSizeAsString="
				+ maxFileSizeAsString + ", maxHistory=" + maxHistory + ", fileNamePatternStr=" + fileNamePatternStr
				+ "]";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.qos.logback.core.rolling.RollingPolicy#getActiveFileName()
	 */
	@Override
	public String getActiveFileName() {
		String rawFileName = getActiveFileName(null);
		int i = rawFileName.indexOf("#{");
		int j = rawFileName.indexOf("}");
		while (i >= 0 && j > i) {
			String propKey = rawFileName.substring(i + 2, j);
			String propVal = null;
			// search for Context > MDC > System Property
			rawFileName = rawFileName.substring(0, i) + propVal + rawFileName.substring(j + 1);

			i = rawFileName.indexOf("#{");
			j = rawFileName.indexOf("}");
		}
		return rawFileName;
	}

	private String getEffectFileName(ILoggingEvent event, String fileNamePatternCS) {
		if (!isDynamicPattern(fileNamePatternCS))
			return fileNamePatternCS;
		
		FileNamePattern fileNamePattern = this.fileNamePatternWCSes.get(fileNamePatternCS);
		if (fileNamePattern == null) {
			fileNamePattern = new FileNamePattern(fileNamePatternCS, this.context);
			this.fileNamePatternWCSes.put(fileNamePatternCS, fileNamePattern);
		}
		return fileNamePattern.convert(event);
	}

	@Override
	public String getActiveFileName(ILoggingEvent event) {
		if (this.getAppenderFileName() != null)
			return getEffectFileName(event, this.getAppenderFileName());
		else
			return getEffectFileName(event, this.getFileNamePattern());
	}

	@Override
	public String getRollingFileName(ILoggingEvent event) {
		if (this.getFileNamePattern() != null)
			return getEffectFileName(event, this.getFileNamePattern());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.qos.logback.core.rolling.RollingPolicy#rollover()
	 */
	@Override
	public void rollover() throws RolloverFailure {
	}

	public void start() {
		this.renameUtil.setContext(this.context);

		this.fileNamePatternWCSes = new HashMap<String, FileNamePattern>();
		if (this.fileNamePatternStr != null) {
			determineCompressionMode();
		} else {
			addWarn("The FileNamePattern option must be set before using TimeBasedRollingPolicy. ");
			addWarn("See also http://logback.qos.ch/codes.html#tbr_fnp_not_set");
			throw new IllegalStateException(
					"The FileNamePattern option must be set before using TimeBasedRollingPolicy. See also http://logback.qos.ch/codes.html#tbr_fnp_not_set");
		}

		this.compressor = new Compressor(this.compressionMode);
		this.compressor.setContext(this.context);

		addInfo("Will use the pattern " + this.fileNamePatternWCSes + " for the active file");
		        
		super.start();
	}

	public void stop() {
		if (!(isStarted()))
			return;
		super.stop();
	}

}
