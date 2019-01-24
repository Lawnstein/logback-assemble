/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.output;

import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.classic.Level;
import ch.qos.logback.core.encoder.Encoder;

/**
 * 
 * @author Lawnstein.Chan
 * @version $aa:$
 */
public class MsgItem {
	public String fileName;
	public String rollingName;
	public String message;
	public Level level;
	public Encoder encoder;
	public AssembleRollingPolicyBase rollingPolicy;

	public MsgItem(String fileName, String message, Encoder encoder, AssembleRollingPolicyBase rollingPolicy) {
		super();
		this.fileName = fileName;
		this.message = message;
		this.encoder = encoder;
		this.rollingPolicy = rollingPolicy;
	}

	public MsgItem(String fileName, String rollingName, String message, Encoder encoder,
			AssembleRollingPolicyBase rollingPolicy) {
		super();
		this.fileName = fileName;
		this.rollingName = rollingName;
		this.message = message;
		this.encoder = encoder;
		this.rollingPolicy = rollingPolicy;
	}

	public MsgItem(String fileName, String rollingName, String message, Level level, Encoder encoder,
			AssembleRollingPolicyBase rollingPolicy) {
		super();
		this.fileName = fileName;
		this.rollingName = rollingName;
		this.message = message;
		this.level = level;
		this.encoder = encoder;
		this.rollingPolicy = rollingPolicy;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getRollingName() {
		return rollingName;
	}

	public void setRollingName(String rollingName) {
		this.rollingName = rollingName;
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

	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	@Override
	public String toString() {
		return "MsgItem [fileName=" + fileName + ", message=" + message + "]";
	}

}
