/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import ch.qos.logback.assemble.rolling.NamingAndSizeBasedRollingPolicy;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.FileUtil;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class CentralizedFileWriter extends ContextAwareBase {

	protected static int availProcessors = Runtime.getRuntime().availableProcessors();

	protected static Map<String, FileItem> outputFileMap = new ConcurrentHashMap<String, FileItem>();

	protected static CentralizedFileWriter instance = null;

	public int healthCheckInterval = 10; // seconds

	public int maxIdleInterval = healthCheckInterval * 3;

	private long registeredAppenders = 0L;

	private int minWorkers = 4;

	private int curWorkers = 0;

	private int maxWorkers = availProcessors > minWorkers ? availProcessors : minWorkers;

	private Thread[] writeWorkers = null;

	private boolean[] writeState = null;

	private AtomicLong writeStatistic[][];

	private Thread healthWorker = null;

	private int pollBatchSize = 1000;

	private boolean alived = false;

	public CentralizedFileWriter() {
	}

	public static CentralizedFileWriter getInstance() {
		synchronized (outputFileMap) {
			if (instance == null) {
				instance = new CentralizedFileWriter();
			}
		}
		return instance;
	}

	private void openFile(FileItem f) throws Exception {
		openFile(f, false);
	}

	private void openFile(FileItem f, boolean forceClearContent) throws Exception {
		f.activeName = f.getFileName();
		if (f.activeName.indexOf("%i") > 0) {
			f.activeName = f.activeName.replaceFirst("%i", "0");
		}
		f.file = new File(f.activeName);
		FileUtil.createMissingParentDirectories(f.file);
		FileOutputStream fos = null;
		if (!f.append || forceClearContent) {
			fos = new FileOutputStream(f.file, false);
		} else {
			fos = new FileOutputStream(f.file, true);
		}
		f.channel = fos.getChannel();
		if (f.charset == null)
			f.writer = new BufferedWriter(new OutputStreamWriter(fos));
		else
			f.writer = new BufferedWriter(new OutputStreamWriter(fos, f.charset.name()));
		this.addInfo("Open log file " + f.fileName + (f.charset == null ? "" : " " + f.charset.name()));
	}

	private void closeFile(FileItem f) throws Exception {
		f.close();
	}

	public void checkAndOpenFile(FileItem f) throws Exception {
		if (f.file == null) {
			f.curIdx = -1;
			openFile(f);
		}

		// if (f.file.getFreeSpace() == 0) {
		// this.addError("No space left in " + f.file.getParent() + " to write
		// log.");
		// }

		if (f.rollingPolicy != null && f.rollingPolicy instanceof NamingAndSizeBasedRollingPolicy) {
			NamingAndSizeBasedRollingPolicy nasRollingPolicy = (NamingAndSizeBasedRollingPolicy) f.rollingPolicy;

			if (nasRollingPolicy.getMaxFileSize() != null
					&& f.channel.size() >= nasRollingPolicy.getMaxFileSize().getSize()) {

				closeFile(f);

				if (nasRollingPolicy.getMaxHistory() <= 0) {
					openFile(f, true);
				} else if (f.rollingName == null || f.rollingName.length() == 0
						|| (!f.rollingName.contains("%i") && f.rollingName.equals(f.activeName))) {
					openFile(f, true);
				} else if (!f.rollingName.contains("%i") && !f.rollingName.equals(f.activeName)) {
					f.rollingPolicy.getRenameUtil().rename(f.activeName, f.rollingName);
					openFile(f);
				} else {
					closeFile(f);

					/**
					 * rename the history files.
					 */
					if (f.curIdx < 0) {
						f.curIdx = 0;
						for (int i = 1; i <= nasRollingPolicy.getMaxHistory(); i++) {
							File check = new File(f.rollingName.replaceFirst("%i", (i + "")));
							if (!check.exists()) {
								check = null;
								break;
							}
							check = null;
							f.curIdx = i;
						}
					}
					String pre = null;
					String nxt = null;
					if (f.curIdx > 0) {
						for (int j = (f.curIdx >= nasRollingPolicy.getMaxHistory() ? nasRollingPolicy.getMaxHistory()
								: f.curIdx + 1); j > 1; j--) {
							pre = f.rollingName.replaceFirst("%i", (j - 1) + "");
							nxt = f.rollingName.replaceFirst("%i", (j + ""));
							File pref = new File(pre);
							if (pref.exists()) {
								File nxtf = new File(nxt);
								if (nxtf.exists())
									nxtf.getAbsoluteFile().delete();
								nxtf = null;
								f.rollingPolicy.getRenameUtil().rename(pre, nxt);
							}
							pref = null;
						}
					}

					/**
					 * rename current log file.
					 */
					pre = f.activeName;
					nxt = f.rollingName.replaceFirst("%i", "1");
					File pref = new File(pre);
					if (pref.exists()) {
						File nxtf = new File(nxt);
						if (nxtf.exists())
							nxtf.getAbsoluteFile().delete();
						nxtf = null;
						f.rollingPolicy.getRenameUtil().rename(pre, nxt);
					}
					if (f.curIdx < nasRollingPolicy.getMaxHistory())
						f.curIdx++;

					openFile(f);
				}

				this.addInfo("Reopen log file " + f.fileName + (f.charset == null ? "" : " " + f.charset.name()));
			}
		}
	}

	public void doWriteQue(List<MsgItem> messages) {
		for (MsgItem msg : messages) {
			String fileName = msg.getFileName() == null ? msg.getRollingName() : msg.getFileName();
			String rollingName = msg.getRollingName() == null ? msg.getFileName() : msg.getRollingName();
			FileItem f = outputFileMap.get(fileName);
			try {
				if (f == null) {
					synchronized (outputFileMap) {
						f = outputFileMap.get(fileName);
						if (f == null) {
							f = new FileItem(fileName, rollingName, msg.encoder, msg.rollingPolicy);
							outputFileMap.put(fileName, f);
						}
					}
				}
				if (!f.isQueueFullToDiscard(msg.getLevel())) {
					f.mq.put(msg.getMessage());
				}
			} catch (Exception e) {
				this.addError("Create  FileItem for log file " + f.fileName + " expception : " + msg.getFileName(), e);
			}
		}
	}

	private void doWriteFile(int wkid) {
		List<String> messages = new ArrayList<String>();
		long writs = 0L;
		while (true) {
			writs = 0L;
			Iterator<String> itrf = outputFileMap.keySet().iterator();
			while (itrf.hasNext()) {
				if (wkid >= 0 && !writeState[wkid]) {
					return;
				}

				FileItem fi = outputFileMap.get(itrf.next());
				if (fi == null)
					continue;

				if (wkid >= 0 && fi.wkid >= 0)
					continue;

				synchronized (fi) {
					if (wkid >= 0 && fi.wkid >= 0)
						continue;
					fi.wkid = wkid;
				}

				/**
				 * poll out the message from queue.
				 */
				if (alived) {
					fi.mq.drainTo(messages, pollBatchSize);
				} else {
					fi.mq.drainTo(messages);
				}

				if (wkid >= 0 && writeState[wkid]) {
					writeStatistic[wkid][0].getAndIncrement();
					writeStatistic[wkid][1].getAndAdd(messages.size());
				}

				// this.addInfo("doWriteFile-" + wkid + " poll " +
				// messages.size() + " message(s)...");
				if (messages.size() > 0) {
					/**
					 * write the messages to log file.
					 */
					try {
						for (String m : messages) {
							checkAndOpenFile(fi);
							fi.write(m);
							if (fi.immediateFlush)
								fi.flush();
						}
					} catch (Exception e) {
						// this.addError("doWriteFile-" + wkid + " message
						// failed. " + e);
					}
					messages.clear();
					fi.lastModifyTime = System.currentTimeMillis();
					writs += messages.size();

				} else if (alived) {
					/**
					 * check the idle time.
					 */
					long elapsedTimeMillis = System.currentTimeMillis() - fi.lastModifyTime;
					if (elapsedTimeMillis > healthCheckInterval * 1000) {
						/**
						 * flush and close the log file.
						 */
						if (fi.isOpened()) {
							try {
								fi.close();
								addInfo("Close log file " + fi.fileName);
							} catch (IOException e) {
								addError("flushAndClose log file " + fi.fileName + " exception", e);
							}
						}
					}
					if (elapsedTimeMillis > this.maxIdleInterval * 1000) {
						/**
						 * unregister the log file.
						 */
						outputFileMap.remove(fi.fileName);
					}
				}

				if (!alived) {
					try {
						fi.close();
					} catch (IOException e) {
						addError("flushAndClose log file " + fi.fileName + " exception", e);
					}
				}
				fi.wkid = -1;
			}

			if (wkid >= 0 && !writeState[wkid]) {
				return;
			}

			try {
				if (writs == 0) {
					Thread.sleep(1000 + wkid);
				} else if (writs < pollBatchSize / 2) {
					Thread.sleep(100 + wkid);
				} else if (writs < pollBatchSize) {
					Thread.sleep(10 + wkid);
				}
			} catch (InterruptedException e) {
			}

		}
	}

	private void openWorkers(int endPosition) {
		int max = endPosition > maxWorkers ? maxWorkers : endPosition;
		for (int i = 0; i < max; i++) {

			if (writeWorkers[i] == null || !writeWorkers[i].isAlive()) {
				addInfo("CentralizedFileWriter start writeWorker-" + i);
				final int wid = i;
				Thread worker = new Thread(new Runnable() {
					@Override
					public void run() {
						Thread.currentThread().setName("Logback-AssembleFileWriter_" + wid);
						writeState[wid] = true;
						addInfo("CentralizedFileWriter writeWorker-" + wid + " start.");
						doWriteFile(wid);
						addInfo("CentralizedFileWriter writeWorker-" + wid + " quit.");
					}
				});
				writeStatistic[i][0].set(0);
				writeStatistic[i][1].set(0);
				writeWorkers[i] = worker;
				worker.start();
			}
		}

		curWorkers = max;
	}

	private void closeWorkers(int beginPosition) {
		int max = beginPosition > maxWorkers ? maxWorkers : beginPosition;
		if (max >= maxWorkers)
			return;

		for (int i = maxWorkers - 1; i >= max; i--) {
			if (writeWorkers[i] != null) {
				writeState[i] = false;
				writeWorkers[i] = null;
				addInfo("CentralizedFileWriter close writeWorker-" + i);
			}
		}

		curWorkers = max;
	}

	private void healthExamine() {

		while (alived) {

			long nb = 0;
			long ct = 0;
			for (int i = 0; i < curWorkers; i++) {
				nb += writeStatistic[i][0].getAndSet(0);
				ct += writeStatistic[i][1].getAndSet(0);
			}

			long pct = (nb <= 0 ? 0 : (long) (ct / nb)) * 100 / pollBatchSize;
			addInfo("CentralizedFileWriter Examine: times=" + nb + ", count=" + ct + ", pct=" + pct);
			int c = curWorkers;
			if (pct <= 40) {
				c = curWorkers > minWorkers ? curWorkers - 1 : minWorkers;
			} else if (pct >= 90) {
				c = curWorkers < maxWorkers ? (curWorkers + 1) : maxWorkers;
			}

			if (c != curWorkers) {
				addInfo("CentralizedFileWriter Examine: change worker from " + curWorkers + " to " + c);
				if (c > curWorkers)
					openWorkers(c);
				else
					closeWorkers(c);
			}

			try {
				addInfo("CentralizedFileWriter Health Examine sleep for " + (healthCheckInterval * 1000) + "ms.");
				Thread.sleep(healthCheckInterval * 1000);
			} catch (InterruptedException e) {
			}

		}

		addInfo("CentralizedFileWriter Examiner quit.");

	}

	public void start() {
		registeredAppenders++;
		if (writeWorkers != null) {
			// addInfo("CentralizedFileWriter started， registered" +
			// registeredAppenders + "Appenders ");
			return;
		}

		synchronized (outputFileMap) {
			if (writeWorkers != null)
				return;

			alived = true;

			writeWorkers = new Thread[maxWorkers];
			writeState = new boolean[maxWorkers];
			writeStatistic = new AtomicLong[maxWorkers][2];
			for (int i = 0; i < maxWorkers; i++) {
				writeState[i] = false;
				writeStatistic[i][0] = new AtomicLong(0);
				writeStatistic[i][1] = new AtomicLong(0);
			}

			openWorkers(minWorkers);
		}

		/**
		 * ShutdownHook
		 */
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				addInfo("shutdown Logback-Assemble");
				alived = false;

				if (healthWorker != null) {
					healthWorker.interrupt();
					healthWorker = null;
				}

				int ws = writeWorkers.length;
				while (ws > 0) {
					ws = 0;
					for (Thread w : writeWorkers) {
						if (w == null)
							continue;

						ws++;
						/**
						 * sleeping.
						 */
						if (w.getState().equals(State.TIMED_WAITING)) {
							w.interrupt();
						}
					}

					if (ws > 0) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
					}
				}

				doWriteFile(-1);
				addInfo("Destroyer over.");
			}
		}, "Logback-AssembleDestroyer"));
		// addInfo("CentralizedFileWriter started， registered" +
		// registeredAppenders + "Appenders ");

		healthWorker = new Thread(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("Logback-AssembleExaminer");
				addInfo("CentralizedFileWriter Examiner start.");
				healthExamine();
				addInfo("CentralizedFileWriter Examiner quit.");
			}
		});
		healthWorker.start();
	}

	public void stop() {
		registeredAppenders--;
		if (registeredAppenders <= 0) {
			registeredAppenders = 0;
			alived = false;
		}
		closeWorkers(0);
	}
}
