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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;

/**
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class FileItem {
	public Encoder encoder;
	public AssembleRollingPolicyBase rollingPolicy;
	public String name;

	public Charset charset;
	public boolean immediateFlush;
	public File file;
	public FileChannel channel;
	public Writer writer;
	public long lastModifyTime;
	public int curIdx;

	public BlockingQueue<String> mq = null;
	public int wkid = -1;

	public FileItem(String name, Encoder encoder, AssembleRollingPolicyBase rollingPolicy) {
		super();
		curIdx = -1;
		this.name = name;
		this.encoder = encoder;
		this.rollingPolicy = rollingPolicy;
		if (this.encoder != null && this.encoder instanceof LayoutWrappingEncoder) {
			this.charset = ((LayoutWrappingEncoder) this.encoder).getCharset();
			this.immediateFlush = ((LayoutWrappingEncoder) this.encoder).isImmediateFlush();
		}
		this.mq = new LinkedBlockingQueue<String>();
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

	protected String getName() {
		return name;
	}

	protected void setName(String name) {
		this.name = name;
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

	public void write(String message) throws IOException {
		if (writer != null) {
			writer.write(message);
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

	@Override
	public String toString() {
		return "FileItem [name=" + name + ", charset=" + charset + ", file=" + file + ", writer=" + writer
				+ ", lastModifyTime=" + lastModifyTime + ", curIdx=" + curIdx + "]";
	}
}
