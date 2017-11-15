/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.output;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class RingBufferedOutput extends AssembleOutputBase {
	private static MsgItem[] msgQueue = null;

	private static CountDownLatch msgNotifier = null;

	private static AtomicLong msgWriteCursor = new AtomicLong(0L);

	private static AtomicLong msgReadCursor = new AtomicLong(0L);

	// private long pollBatchSize = 200;

	private long queueCapacity = 8192000;

	private long blockBatchSize = 10;

	private long blockIntervalMillis = 500;

	private long abandonedMsges = 0;

	private static Thread pickWorker = null;

	public RingBufferedOutput() {
	}

	public void stopAllWorker() {

		if (pickWorker != null) {
			Thread w = pickWorker;
			pickWorker = null;

			w.interrupt();
			try {
				w.join(CentralizedFileWriter.getInstance().maxIdleInterval * 1000);
				// check to see if the thread ended and if not add a warning
				// message
				if (w.isAlive()) {
					addWarn("Max queue flush timeout (" + CentralizedFileWriter.getInstance().maxIdleInterval
							+ " seconds) exceeded. Approximately (" + msgWriteCursor + " - " + msgReadCursor + " = "
							+ (msgWriteCursor.get() - msgReadCursor.get())
							+ ") queued events were possibly discarded.");
				}
			} catch (InterruptedException e) {
			}
		}

		msgWriteCursor.set(0L);
		msgReadCursor.set(0L);
	}

	private synchronized void activate() {
		if (alived)
			return;

		if (msgQueue == null) {
			msgQueue = new MsgItem[(int) queueCapacity];
			msgWriteCursor.set(0L);
			msgReadCursor.set(0L);
			for (long i = 0; i < queueCapacity; i++)
				msgQueue[(int) i] = null;
		}

		alived = true;

		if (pickWorker == null) {
			pickWorker = new Thread(new Runnable() {
				@Override
				public void run() {
					Thread.currentThread().setName("Logback-AssemblePicker");
					List<MsgItem> l = new ArrayList<MsgItem>();
					while (alived) {
						try {
							if (msgReadCursor.get() == msgWriteCursor.get()) {
								msgNotifier = new CountDownLatch((int) blockBatchSize);
								// addInfo("lawn-pickWorker waiting for messages
								// from cursor " + msgWriteCursor);
								// msgNotifier.await(CentralizedFileWriter.getInstance().healthCheckInterval
								// - 1, TimeUnit.SECONDS);
								msgNotifier.await(blockIntervalMillis, TimeUnit.MILLISECONDS);
								msgNotifier = null;
							}
							// addInfo("pickWorker loop msgReadCursor("
							// +msgReadCursor.get() + "), msgWriteCursor(" +
							// msgWriteCursor.get() + ")");

							while (msgReadCursor.get() != msgWriteCursor.get()) {
								long c = msgReadCursor.incrementAndGet();
								if (c == Long.MAX_VALUE) {
									msgReadCursor.set(0L);
									addInfo("pickWorker reset read cursor. ");
								}

								int i = (int) ((c - 1) % queueCapacity);
								MsgItem m = msgQueue[i];
								int j = 20;
								while (m == null && j > 0) {
									Thread.sleep(1);
									m = msgQueue[i];
									j--;
								}
								msgQueue[i] = null;
								if (m != null) {
									l.add(m);
									if (l.size() >= 0) {
										CentralizedFileWriter.getInstance().doWriteQue(l);
										l.clear();
									}
								}
								// addInfo("pickWorker current msgReadCursor("
								// +msgReadCursor.get() + "), msgWriteCursor(" +
								// msgWriteCursor.get() + ")");
							}
						} catch (InterruptedException e) {
						}
					}

					while (msgReadCursor.get() != msgWriteCursor.get()) {
						long c = msgReadCursor.incrementAndGet();
						if (c == Long.MAX_VALUE) {
							msgReadCursor.set(0L);
							addInfo("pickWorker reset read cursor. ");
						}

						int i = (int) ((c - 1) % queueCapacity);
						MsgItem m = msgQueue[i];
						msgQueue[i] = null;
						if (m != null) {
							l.add(m);
						}
					}
					if (l.size() > 0) {
						CentralizedFileWriter.getInstance().doWriteQue(l);
						l.clear();
					}

					addInfo("pickWorker over.");
				}
			});
			pickWorker.start();
		}
	}

	public void stop() {
		alived = false;

		if (msgNotifier != null) {
			/**
			 * trig to save file.
			 */
			for (int i = 0; i < blockBatchSize; i++) {
				try {
					msgNotifier.countDown();
				} catch (Throwable th) {
				}
			}
		}

		stopAllWorker();
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

		MsgItem mn = new MsgItem(fileName, rollingFileName, message, event.getLevel(), encoder, rollingPolicy);
		long c = msgWriteCursor.incrementAndGet();
		if (c == Long.MAX_VALUE) {
			msgWriteCursor.set(0L);
			addInfo("pickWorker reset write cursor. ");
		}

		int i = (int) ((c - 1) % queueCapacity);
		MsgItem mo = msgQueue[i];
		msgQueue[i] = mn;

		if (msgNotifier != null) {
			try {
				msgNotifier.countDown();
			} catch (Throwable th) {
			}
		}

		if (mo != null) {
			abandonedMsges++;
			// if (abandonedMsges % 100 == 0)
			addWarn("msgQueue[" + i + "/" + c + "/" + msgWriteCursor.get() + "/" + msgReadCursor.get()
					+ "] contain valid message that hasn't written to log file, total " + abandonedMsges);
			if (abandonedMsges == Long.MAX_VALUE)
				abandonedMsges = 0;
		}
	}

}
