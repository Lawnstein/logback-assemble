/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.output;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class BlockingQueuedOutput extends AssembleOutputBase {
	private static BlockingQueue<MsgItem> msgQueue = null;

	private int pollBatchSize = 100;

	private int queueCapacity = 819200;

	private static Thread pickWorker = null;

	public BlockingQueuedOutput() {
	}

	protected BlockingQueue<MsgItem> getMsgQueue() {
		return msgQueue;
	}

	protected void setMsgQueue(BlockingQueue<MsgItem> msgQueue) {
		this.msgQueue = msgQueue;
	}

	protected int getPollBatchSize() {
		return pollBatchSize;
	}

	protected void setPollBatchSize(int pollBatchSize) {
		this.pollBatchSize = pollBatchSize;
	}

	protected int getQueueCapacity() {
		return queueCapacity;
	}

	protected void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	private synchronized void activate() {
		if (alived)
			return;

		if (msgQueue == null)
			msgQueue = new ArrayBlockingQueue(queueCapacity);

		// if (msgQueue == null)
		// msgQueue = new LinkedBlockingQueue<MsgItem>(queueCapacity);

		alived = true;

		if (pickWorker == null) {
			pickWorker = new Thread(new Runnable() {

				@Override
				public void run() {
					Thread.currentThread().setName("Logback-AssemblePicker");
					List<MsgItem> l = new ArrayList<MsgItem>();
					while (alived) {
						try {
							l.add(msgQueue.take());
							msgQueue.drainTo(l, pollBatchSize - 1);

							if (l.size() > 0) {
								CentralizedFileWriter.getInstance().doWriteQue(l);
								l.clear();
							}
						} catch (InterruptedException e) {
							break;
						}
					}

					msgQueue.drainTo(l);
					if (l.size() > 0) {
						CentralizedFileWriter.getInstance().doWriteQue(l);
						l.clear();
					}
					stopWorker();
					addInfo("pickWorker over.");
				}
			});
			pickWorker.setDaemon(true);
			pickWorker.start();
		}
	}

	public void stop() {
		alived = false;
	}
	
	private void stopWorker() {
		this.pickWorker = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.qos.logback.assemble.output.AssembleOutputBase#write(ch.qos.logback.
	 * classic.spi.ILoggingEvent, java.lang.String, java.lang.String)
	 */
	@Override
	public void write(ILoggingEvent event, String fileName, String message) {
		write(event, fileName, null, message);
	}

	@Override
	public void write(ILoggingEvent event, String fileName, String rollingFileName, String message) {
		if (!alived)
			activate();

		try {
			this.msgQueue
					.put(new MsgItem(fileName, rollingFileName, message, event.getLevel(), encoder, rollingPolicy));
		} catch (InterruptedException e) {
			addError("write InterruptedException", e);
		}
	}

}
