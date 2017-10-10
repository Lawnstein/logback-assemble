/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.rolling;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.helper.Compressor;
import ch.qos.logback.core.rolling.helper.RenameUtil;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public abstract class AssembleRollingPolicyBase extends RollingPolicyBase {
	protected FileNamePattern fileNamePatternWCS;
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

	abstract public String getActiveFileName(ILoggingEvent event);

}
