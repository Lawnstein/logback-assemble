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
import java.util.concurrent.atomic.AtomicLong;

import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class FileItem extends ContextAwareBase {
	public Encoder encoder = null;
	public AssembleRollingPolicyBase rollingPolicy = null;
	public String fileName = null;
	public String rollingName = null;
	public String activeName = null;
	public CompressionMode compressionMode = CompressionMode.NONE;
	private long lastConfigured = 0L;

	public Charset charset = null;
	public boolean append = true;
	public boolean immediateFlush = false;
	public File file = null;
	public FileChannel channel = null;
	public Writer writer = null;
	public long lastModifyTime = 0L;
	private AtomicLong token = new AtomicLong(0);
	public int curIdx = -1;

	// public BlockingQueue<String> mq = null;
	// public boolean discardable = false;
	// public int wkid = -1;

	public FileItem(Context context, String fileName, String rollingName, Encoder encoder,
			AssembleRollingPolicyBase rollingPolicy) {
		super();
		this.setContext(context);

		curIdx = -1;
		this.fileName = fileName;

		update(System.currentTimeMillis(), rollingName, encoder, rollingPolicy);

		// if (rollingPolicy != null && rollingPolicy.getAppenderQueueSize() >
		// 0) {
		// this.mq = new
		// ArrayBlockingQueue<String>(rollingPolicy.getAppenderQueueSize());
		// this.discardable = true;
		// } else {
		// this.mq = new LinkedBlockingQueue<String>();
		// }
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
		this.append = this.rollingPolicy.isFileAppend();
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
		this.compressionMode = rollingName == null ? CompressionMode.NONE : determineCompressionMode(this.rollingName);
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

//	public int getCurIdx() {
//		return curIdx;
//	}
//
//	public void setCurIdx(int curIdx) {
//		this.curIdx = curIdx;
//	}

	// public BlockingQueue<String> getMq() {
	// return mq;
	// }
	//
	// public void setMq(BlockingQueue<String> mq) {
	// this.mq = mq;
	// }

	public String getActiveName() {
		return activeName;
	}

	public void setActiveName(String activeName) {
		this.activeName = activeName;
	}

	// public boolean isDiscardable() {
	// return discardable;
	// }

	/**
	 * check whether to discard the current message with level.
	 * 
	 * @param level
	 * @return
	 */
	// public boolean isQueueFullToDiscard(Level level) {
	// if (!isDiscardable())
	// return false;
	//
	// int rc = this.mq.remainingCapacity();
	// if (rc == 0) {
	// return true;
	// } else if (this.rollingPolicy.isQueueBelowDiscardingThreshold(rc)) {
	// if (level == null)
	// return false;
	// if (level.isGreaterOrEqual(Level.WARN))
	// return true;
	// }
	// return false;
	// }

	// public void setDiscardable(boolean discardable) {
	// this.discardable = discardable;
	// }

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

	public boolean lock(long stamp) {
		if (stamp <= 0) {
			stamp = System.currentTimeMillis();
		}
		boolean r = token.compareAndSet(0L, stamp);
		if (r) {
			return true;
		}
		long currStamp = System.currentTimeMillis();
		while (!r) {
			stamp = System.currentTimeMillis();
			if (stamp - currStamp > 30000) {
				return false;
			}
			r = token.compareAndSet(0L, stamp);
		}
		return r;
	}

	public void unlock() {
		token.set(0L);
	}

	public void update(long stamp, String rollingName, Encoder encoder, AssembleRollingPolicyBase rollingPolicy) {
		if (lastConfigured > 0L && stamp - lastConfigured < 10000L) {
			return;
		}

		if (rollingName != null && !rollingName.equals(this.rollingName)) {
			if (this.rollingName != null)
				addInfo("Configure for [" + this.fileName + "] rollingName changed to " + this.rollingName);
			setRollingName(rollingName);
		}
		if (encoder != null && encoder != this.encoder) {
			if (this.encoder != null)
				addInfo("Configure for [" + this.fileName + "] encoder changed");
			setEncoder(encoder);
		}
		if (rollingPolicy != null && rollingPolicy != this.rollingPolicy) {
			if (this.rollingPolicy != null)
				addInfo("Configure for [" + this.fileName + "] rollingPolicy changed");
			setRollingPolicy(rollingPolicy);
		}

		lastConfigured = stamp;
	}
}
