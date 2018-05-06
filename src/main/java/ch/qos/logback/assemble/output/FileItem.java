/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.output;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.classic.Level;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.helper.CompressionMode;

/**
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class FileItem {
	public Encoder encoder;
	public AssembleRollingPolicyBase rollingPolicy;
	public String fileName;
	public String rollingName;
	public String activeName;
	public CompressionMode compressionMode;

	public Charset charset;
	public boolean append = true;
	public boolean immediateFlush = false;
	public File file;
	public FileChannel channel;
	public Writer writer;
	public long lastModifyTime;
	public int curIdx;

	public BlockingQueue<String> mq = null;
	public boolean discardable = false;
	public int wkid = -1;

	public FileItem(String fileName, String rollingName, Encoder encoder, AssembleRollingPolicyBase rollingPolicy) {
		super();
		curIdx = -1;
		this.fileName = fileName;
		this.rollingName = rollingName;
		this.encoder = encoder;
		this.rollingPolicy = rollingPolicy;
		this.compressionMode = rollingName == null ? CompressionMode.NONE : determineCompressionMode(this.rollingName);
		if (this.rollingPolicy != null) {
			this.append = this.rollingPolicy.isFileAppend();
		}
		if (this.encoder != null && this.encoder instanceof LayoutWrappingEncoder) {
			this.charset = ((LayoutWrappingEncoder) this.encoder).getCharset();
			this.immediateFlush = ((LayoutWrappingEncoder) this.encoder).isImmediateFlush();
		}
		if (rollingPolicy.getAppenderQueueSize() > 0) {
			this.mq = new ArrayBlockingQueue<String>(rollingPolicy.getAppenderQueueSize());
			this.discardable = true;
		} else {
			this.mq = new LinkedBlockingQueue<String>();
		}
		this.wkid = -1;
	}

	public Encoder getEncoder() {
		return encoder;
	}

	public void setEncoder(Encoder encoder) {
		this.encoder = encoder;
		if (this.encoder != null && this.encoder instanceof LayoutWrappingEncoder) {
			this.charset = ((LayoutWrappingEncoder) this.encoder).getCharset();
			this.immediateFlush = ((LayoutWrappingEncoder) this.encoder).isImmediateFlush();
		}
	}

	public AssembleRollingPolicyBase getRollingPolicy() {
		return rollingPolicy;
	}

	public void setRollingPolicy(AssembleRollingPolicyBase rollingPolicy) {
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

	public CompressionMode getCompressionMode() {
		return compressionMode;
	}

	public void setCompressionMode(CompressionMode compressionMode) {
		this.compressionMode = compressionMode;
	}

	protected Charset getCharset() {
		return charset;
	}

	protected void setCharset(Charset charset) {
		this.charset = charset;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public long getLastModifyTime() {
		return lastModifyTime;
	}

	public void setLastModifyTime(long lastModifyTime) {
		this.lastModifyTime = lastModifyTime;
	}

	protected Writer getWriter() {
		return writer;
	}

	protected void setWriter(Writer writer) {
		this.writer = writer;
	}

	protected int getcurIdx() {
		return curIdx;
	}

	protected void setcurIdx(int curIdx) {
		this.curIdx = curIdx;
	}

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

	public boolean isImmediateFlush() {
		return immediateFlush;
	}

	public void setImmediateFlush(boolean immediateFlush) {
		this.immediateFlush = immediateFlush;
	}

	public FileChannel getChannel() {
		return channel;
	}

	public void setChannel(FileChannel channel) {
		this.channel = channel;
	}

	public int getCurIdx() {
		return curIdx;
	}

	public void setCurIdx(int curIdx) {
		this.curIdx = curIdx;
	}

	public BlockingQueue<String> getMq() {
		return mq;
	}

	public void setMq(BlockingQueue<String> mq) {
		this.mq = mq;
	}

	public String getActiveName() {
		return activeName;
	}

	public void setActiveName(String activeName) {
		this.activeName = activeName;
	}

	public boolean isDiscardable() {
		return discardable;
	}

	/**
	 * check whether to discard the current message with level.
	 * 
	 * @param level
	 * @return
	 */
	public boolean isQueueFullToDiscard(Level level) {
		if (!isDiscardable())
			return false;

		int rc = this.mq.remainingCapacity();
		if (rc == 0) {
			return true;
		} else if (this.rollingPolicy.isQueueBelowDiscardingThreshold(rc)) {
			if (level == null)
				return false;
			if (level.isGreaterOrEqual(Level.WARN))
				return true;
		}
		return false;
	}

	public void setDiscardable(boolean discardable) {
		this.discardable = discardable;
	}

	public void write(String message) throws IOException {
		if (writer != null) {
			writer.write(message);
		}
	}

	public void write(MsgItem message) throws IOException {
		if (writer != null) {
			writer.write(message.message);
		}
	}

	public boolean isOpened() {
		return writer != null;
	}

	public void flush() throws IOException {
		if (writer != null) {
			writer.flush();
		}
	}

	public void close() throws IOException {
		if (writer != null) {
			writer.flush();
			writer.close();
			channel.close();
		}
		channel = null;
		writer = null;
		file = null;
	}

	protected CompressionMode determineCompressionMode(String rollingFileName) {
		if (rollingFileName.endsWith(".gz")) {
			return CompressionMode.GZ;
		} else if (rollingFileName.endsWith(".zip")) {
			return CompressionMode.ZIP;
		} else {
			return CompressionMode.NONE;
		}
	}

}
