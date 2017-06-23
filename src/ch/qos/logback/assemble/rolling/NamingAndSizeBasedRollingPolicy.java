/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.rolling;

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

	private int maxHistory = -1;

	// TimeBasedFileNamingAndTriggeringPolicyBase
	// getCurrentPeriodsFileNameWithoutCompressionSuffix
	// TimeBasedRollingPolicy

	// static final String FNP_NOT_SET = "The FileNamePattern option must be set
	// before using TimeBasedRollingPolicy. ";

	public NamingAndSizeBasedRollingPolicy() {
		super();
		logger.trace(" contruct >>>>");
		this.maxHistory = -1;
		logger.trace(" contruct <<<<");
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
		logger.trace(".fileNamePatternWCS:" + fileNamePatternWCS.getClass());
		String rawFileName = fileNamePatternWCS.convert(null);
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

	@Override
	public String getActiveFileName(ILoggingEvent event) {
		// logger.trace(".fileNamePatternWCS:" + fileNamePatternWCS.getClass());
		String rawFileName = fileNamePatternWCS.convert(event);
		// logger.trace(".fileNamePatternWCS: rawFileName=" + rawFileName);
		return rawFileName;
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

		if (this.fileNamePatternStr != null) {
			fileNamePatternWCS = new FileNamePattern(this.fileNamePatternStr, this.context);
			determineCompressionMode();
		} else {
			addWarn("The FileNamePattern option must be set before using TimeBasedRollingPolicy. ");
			addWarn("See also http://logback.qos.ch/codes.html#tbr_fnp_not_set");
			throw new IllegalStateException(
					"The FileNamePattern option must be set before using TimeBasedRollingPolicy. See also http://logback.qos.ch/codes.html#tbr_fnp_not_set");
		}

		this.compressor = new Compressor(this.compressionMode);
		this.compressor.setContext(this.context);

		addInfo("Will use the pattern " + this.fileNamePatternWCS + " for the active file");

		/*
		 * if (this.compressionMode == CompressionMode.ZIP) { String
		 * zipEntryFileNamePatternStr =
		 * transformFileNamePattern2ZipEntry(this.fileNamePatternStr);
		 * this.zipEntryFileNamePattern = new
		 * FileNamePattern(zipEntryFileNamePatternStr, this.context); }
		 * 
		 * if (this.timeBasedFileNamingAndTriggeringPolicy == null) {
		 * this.timeBasedFileNamingAndTriggeringPolicy = new
		 * DefaultTimeBasedFileNamingAndTriggeringPolicy(); }
		 * this.timeBasedFileNamingAndTriggeringPolicy.setContext(this.context);
		 * this.timeBasedFileNamingAndTriggeringPolicy.setTimeBasedRollingPolicy
		 * (this); this.timeBasedFileNamingAndTriggeringPolicy.start();
		 * 
		 * if (!(this.timeBasedFileNamingAndTriggeringPolicy.isStarted())) {
		 * addWarn(
		 * "Subcomponent did not start. TimeBasedRollingPolicy will not start."
		 * ); return; }
		 * 
		 * if (this.maxHistory != 0) { this.archiveRemover =
		 * this.timeBasedFileNamingAndTriggeringPolicy.getArchiveRemover();
		 * this.archiveRemover.setMaxHistory(this.maxHistory);
		 * this.archiveRemover.setTotalSizeCap(this.totalSizeCap.getSize()); if
		 * (this.cleanHistoryOnStart) { addInfo("Cleaning on start up"); Date
		 * now = new
		 * Date(this.timeBasedFileNamingAndTriggeringPolicy.getCurrentTime());
		 * this.cleanUpFuture = this.archiveRemover.cleanAsynchronously(now); }
		 * } else if (this.totalSizeCap.getSize() != 0L) { addWarn(
		 * "'maxHistory' is not set, ignoring 'totalSizeCap' option with value ["
		 * + this.totalSizeCap + "]"); }
		 */
		super.start();
	}

	public void stop() {
		if (!(isStarted()))
			return;
		super.stop();
	}

}
