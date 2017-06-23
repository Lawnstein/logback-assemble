/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.output;

import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.core.encoder.Encoder;

/**
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class MsgItem {
	public String fileName;
	public String message;
	public Encoder encoder;
	public AssembleRollingPolicyBase rollingPolicy;

	public MsgItem(String fileName, String message, Encoder encoder, AssembleRollingPolicyBase rollingPolicy) {
		super();
		this.fileName = fileName;
		this.message = message;
		this.encoder = encoder;
		this.rollingPolicy = rollingPolicy;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Encoder getEncoder() {
		return encoder;
	}

	public void setEncoder(Encoder encoder) {
		this.encoder = encoder;
	}

	public AssembleRollingPolicyBase getRollingPolicy() {
		return rollingPolicy;
	}

	public void setRollingPolicy(AssembleRollingPolicyBase rollingPolicy) {
		this.rollingPolicy = rollingPolicy;
	}

	@Override
	public String toString() {
		return "MsgItem [fileName=" + fileName + ", message=" + message + "]";
	}

}
