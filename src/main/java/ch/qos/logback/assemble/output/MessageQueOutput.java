/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.output;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ch.qos.logback.assemble.AssembleAppender;
import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class MessageQueOutput extends ContextAwareBase {

	private static final int UNDEFINED = -1;

	private AssembleAppender owner = null;

	private String ownerName = null;

	private BlockingQueue<ILoggingEvent> loggingEventQue = null;

	private boolean async = true;
	private int asyncThreads = 1;

	/**
	 * the max file message queue size, default unlimited.
	 */
	private int queueSize = UNDEFINED;

	private int defaultQueueCapacity = 8192;

	/**
	 * the contition for threshold to discard message , default 20% *
	 * $queueSize.<br>
	 * if remainCapacity less then $discardingThreshold, the discard the
	 * TRACE/DEBUG/INFO messages, just keep WARN/ERROR messages.<br>
	 */
	private int discardingThreshold = UNDEFINED;
	boolean neverBlock = false;

	private long discardCount = 0L;

	private Thread[] workers = null;

	/**
	 * The default maximum queue flush time allowed during appender stop. If the
	 * worker takes longer than this time it will exit, discarding any remaining
	 * items in the queue
	 */
	public static final int DEFAULT_MAX_FLUSH_TIME = 1000;
	int maxFlushTime = DEFAULT_MAX_FLUSH_TIME;

	boolean includeCallerData = false;

	private boolean alived = false;

	// private boolean actived = false;

	public MessageQueOutput(ContextAware declaredOrigin) {
		super(declaredOrigin);
		this.setContext(declaredOrigin.getContext());
	}

	public MessageQueOutput(AssembleAppender owner, boolean async) {
		super(owner);
		this.setContext(owner.getContext());
		this.owner = owner;
		this.ownerName = owner.getName();
		this.async = async;
	}

	public synchronized void start() {
		if (alived)
			return;

		alived = true;
		if (!async) {
			return;
		}

		this.includeCallerData = owner.isIncludeCallerData();
		this.queueSize = owner.getQueueSize();
		this.discardingThreshold = owner.getDiscardingThreshold();
		this.asyncThreads = owner.getAsyncThreads();

		if (queueSize <= 0) {
			loggingEventQue = new LinkedBlockingQueue<ILoggingEvent>();
		} else {
			queueSize = queueSize > 1 ? queueSize : defaultQueueCapacity;
			loggingEventQue = new ArrayBlockingQueue(queueSize);
			if (discardingThreshold == UNDEFINED)
				discardingThreshold = queueSize / 5;
		}

		if (workers == null) {
			if (this.asyncThreads <= 0) {
				this.asyncThreads = 1;
			}
			workers = new Thread[this.asyncThreads];
			for (int i = 0; i < this.asyncThreads; i++) {

				final int ti = i;
				workers[i] = new Thread(new Runnable() {

					@Override
					public void run() {
						Thread.currentThread().setName("Logback-AssembleWorker-" + owner.getName() + "-" + ti);
						while (alived) {
							try {
								ILoggingEvent event = loggingEventQue.take();
								if (event != null)
									writeInner(event);
							} catch (InterruptedException e) {
								break;
							}
						}

						synchronized (loggingEventQue) {
							addInfo("Worker thread will flush remaining " + loggingEventQue.size()
									+ " events before exiting. ");
							for (ILoggingEvent event : loggingEventQue) {
								loggingEventQue.remove(event);
								if (event != null)
									writeInner(event);
							}
						}

						addInfo("AssembleWorker-" + owner.getName() + "-" + ti + " over.");
					}
				});
				workers[i].setDaemon(true);
				workers[i].start();
			}
		}

	}

	public void stop() {
		alived = false;
		stopWorkers();
	}

	private void stopWorker(Thread worker) {
		if (worker == null)
			return;
		if (worker.isInterrupted())
			return;
		if (!worker.isAlive())
			return;

		// interrupt the worker thread so that it can terminate. Note that the
		// interruption can be consumed
		// by sub-appenders
		worker.interrupt();
		try {
			worker.join(maxFlushTime);

			// check to see if the thread ended and if not add a warning message
			if (worker.isAlive()) {
				addWarn("Max queue flush timeout (" + maxFlushTime + " ms) exceeded. Approximately "
						+ loggingEventQue.size() + " queued events were possibly discarded.");
			} else {
				addInfo("Queue flush finished successfully within timeout.");
			}

		} catch (InterruptedException e) {
			addError("Failed to join worker thread. " + loggingEventQue.size() + " queued events may be discarded.", e);
		}
	}

	private synchronized void stopWorkers() {
		if (workers == null || workers.length == 0) {
			return;
		}

		for (int i = 0, j = workers.length; i < j; i++) {
			stopWorker(workers[i]);
			workers[i] = null;
		}
		workers = null;
	}

	/**
	 * Is the eventObject passed as parameter discardable? The base class's
	 * implementation of this method always returns 'false' but sub-classes may
	 * (and do) override this method.
	 * <p/>
	 * <p>
	 * Note that only if the buffer is nearly full are events discarded.
	 * Otherwise, when the buffer is "not full" all events are logged.
	 *
	 * Events of level TRACE, DEBUG and INFO are deemed to be discardable.
	 * 
	 * @param event
	 * @return true if the event is of level TRACE, DEBUG or INFO false
	 *         otherwise.
	 */
	private boolean isDiscardable(ILoggingEvent event) {
		Level level = event.getLevel();
		return level.toInt() <= Level.INFO_INT;
	}

	private boolean isQueueBelowDiscardingThreshold() {
		return (discardingThreshold > 0 && loggingEventQue.remainingCapacity() < discardingThreshold);
	}

	protected void preprocess(ILoggingEvent eventObject) {
		eventObject.prepareForDeferredProcessing();
		if (includeCallerData)
			eventObject.getCallerData();
	}

	private void put(ILoggingEvent eventObject) {
		if (neverBlock) {
			loggingEventQue.offer(eventObject);
		} else {
			try {
				loggingEventQue.put(eventObject);
			} catch (InterruptedException e) {
				// Interruption of current thread when in doAppend method should
				// not be consumed
				// by AsyncAppender
				Thread.currentThread().interrupt();
			}
		}
	}

	public void write(ILoggingEvent event) {
		if (!async) {
			writeInner(event);
			return;
		}

		if (!alived)
			start();

		if (isQueueBelowDiscardingThreshold() && isDiscardable(event)) {
			if (++discardCount % 1000 == 0) {
				addWarn("[" + this.ownerName + "] dicarded " + discardCount + " logs");
			}
			FileWriterBase.getInstance(getContext()).discard(ownerName, discardCount);
			return;
		}

		preprocess(event);
		put(event);
	}

	private void writeInner(ILoggingEvent eventObject) {
		String message = doEncode(eventObject);
		String fileName = doActiveFileName(eventObject);
		String rollingName = doRollingFileName(eventObject, fileName);

		FileWriterBase.getInstance(getContext()).write(fileName, rollingName, message, eventObject.getLevel(),
				owner.getEncoder(), (AssembleRollingPolicyBase) owner.getRollingPolicy());
	}

	private String doEncode(ILoggingEvent eventObject) {
		if (owner.getEncoder() instanceof LayoutWrappingEncoder) {
			return ((LayoutWrappingEncoder) owner.getEncoder()).getLayout().doLayout(eventObject);
		} else {
			addError("unsupported encoder " + owner.getEncoder().getClass());
		}
		return null;
	}

	private String doActiveFileName(ILoggingEvent eventObject) {
		if (owner.getRollingPolicy() != null)
			return owner.getRollingPolicy().getActiveFileName((ILoggingEvent) eventObject);
		else
			return owner.getFile();
	}

	private String doRollingFileName(ILoggingEvent eventObject, String activeFileName) {
		if (owner.getRollingPolicy() == null)
			return null;
		if (owner.getRollingPolicy().isActiveSamePattern())
			return activeFileName;
		return owner.getRollingPolicy().getRollingFileName((ILoggingEvent) eventObject);
	}

}
