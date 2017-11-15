/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.output;

import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public abstract class AssembleOutputBase extends ContextAwareBase {

	protected Encoder encoder = null;

	protected AssembleRollingPolicyBase rollingPolicy = null;

	protected boolean alived = false;

	public AssembleOutputBase() {
	}

	public AssembleRollingPolicyBase getRollingPolicy() {
		return rollingPolicy;
	}

	public void setRollingPolicy(AssembleRollingPolicyBase rollingPolicy) {
		this.rollingPolicy = rollingPolicy;
	}

	public Encoder getEncoder() {
		return encoder;
	}

	public void setEncoder(Encoder encoder) {
		this.encoder = encoder;
	}

	public boolean isAlived() {
		return alived;
	}

	public void setAlived(boolean alived) {
		this.alived = alived;
	}

	abstract public void write(ILoggingEvent event, String fileName, String message);
	
	abstract public void write(ILoggingEvent event, String fileName, String rollingFileName, String message);

	abstract public void stop();

}
