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

import ch.qos.logback.assemble.rolling.NamingAndSizeBasedRollingPolicy;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.FileUtil;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class CentralizedFileWriter extends ContextAwareBase {

	protected static Map<String, FileItem> outputFileMap = new ConcurrentHashMap<String, FileItem>();

	protected static CentralizedFileWriter instance = null;

	public int healthCheckInterval = 10; // seconds

	public int maxIdleInterval = healthCheckInterval * 3;

	private long registeredAppenders = 0L;

	private long maxWorkers = 4L;

	private Thread[] writeWorkers = null;

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

	public void reopenFile(FileItem f) throws Exception {
		if (f.file == null) {
			/**
			 * first time open the file.
			 */
			String realFileName = null;
			int currIndex = -1;
			int lastIndex = -1;
			if (f.name.indexOf("%i") > 0) {
				// check wether the file with index has exists.
				// NamingAndSizeBasedRollingPolicy extends
				// AssembleRollingPolicyBase
				// AssembleRollingPolicyBase rollingPolicy;
				if (f.rollingPolicy instanceof NamingAndSizeBasedRollingPolicy) {
					NamingAndSizeBasedRollingPolicy nabRollingPolicy = (NamingAndSizeBasedRollingPolicy) f.rollingPolicy;
					if (nabRollingPolicy.getMaxFileSize() == null)
						currIndex = 0;
					else {
						if (nabRollingPolicy.getMaxHistory() > 0)
							lastIndex = nabRollingPolicy.getMaxHistory();
						for (int i = 0; i >= 0; i++) {
							File check = new File(f.name.replaceFirst("%i", (i + "")));
							if (!check.exists() || check.length() < nabRollingPolicy.getMaxFileSize().getSize()) {
								currIndex = i;
								check = null;
								break;
							}
							check = null;
							if (lastIndex > 0 && i >= (lastIndex - 1)) {
								break;
							}
						}
					}
				}
				if (currIndex < 0)
					currIndex = 0;
				else
					f.curIdx = currIndex;
				realFileName = f.name.replaceFirst("%i", (currIndex + ""));
			} else {
				realFileName = f.name;
				f.curIdx = -1;
			}

			f.file = new File(realFileName);
			FileUtil.createMissingParentDirectories(f.file);
			FileOutputStream fos = new FileOutputStream(f.file, true);
			f.channel = fos.getChannel();
			if (f.charset == null)
				f.writer = new BufferedWriter(new OutputStreamWriter(fos));
			else
				f.writer = new BufferedWriter(new OutputStreamWriter(fos, f.charset.name()));
			this.addInfo("Open log file " + f.name + (f.charset == null ? "" : " " + f.charset.name()));
		} else {
			/**
			 * second reopen the file.
			 */
			if (f.curIdx < 0)
				return;

			if (f.file.getFreeSpace() == 0) {
				this.addError("No space left in " + f.file.getParent() + " to write log.");
			}

			// check wether the file with index has exists.
			// NamingAndSizeBasedRollingPolicy extends AssembleRollingPolicyBase
			// AssembleRollingPolicyBase rollingPolicy;
			if (f.rollingPolicy instanceof NamingAndSizeBasedRollingPolicy) {
				NamingAndSizeBasedRollingPolicy nabRollingPolicy = (NamingAndSizeBasedRollingPolicy) f.rollingPolicy;

				if (nabRollingPolicy.getMaxFileSize() != null
						&& f.channel.size() >= nabRollingPolicy.getMaxFileSize().getSize()) {
					f.curIdx++;
					f.close();
					if (nabRollingPolicy.getMaxHistory() > 0) {
						if (f.curIdx > nabRollingPolicy.getMaxHistory()) {
							for (int j = 1; j < f.curIdx; j++) {
								String pre = f.name.replaceFirst("%i", (j - 1) + "");
								String nxt = f.name.replaceFirst("%i", (j + ""));
								f.rollingPolicy.getRenameUtil().rename(nxt, pre);
							}
							f.curIdx--;
						}
					}

					f.file = new File(f.name.replaceFirst("%i", (f.curIdx + "")));
					FileUtil.createMissingParentDirectories(f.file);
					FileOutputStream fos = new FileOutputStream(f.file, true);
					f.channel = fos.getChannel();
					if (f.charset == null)
						f.writer = new BufferedWriter(new OutputStreamWriter(fos));
					else
						f.writer = new BufferedWriter(new OutputStreamWriter(fos, f.charset.name()));

					this.addInfo("Reopen log file " + f.name + (f.charset == null ? "" : " " + f.charset.name()));
				}
			}
		}
	}

	public void doWriteQue(List<MsgItem> messages) {
		//this.addInfo("doWriteQue " + messages.size() + " message(s).");
		for (MsgItem msg : messages) {
			FileItem f = outputFileMap.get(msg.getFileName());
			try {
				if (f == null) {
					synchronized (outputFileMap) {
						f = new FileItem(msg.getFileName(), msg.encoder, msg.rollingPolicy);
						outputFileMap.put(f.name, f);
					}
				}
				f.mq.put(msg.getMessage());
			} catch (Exception e) {
				this.addError("Create  FileItem for log file " + f.name + " expception : " + msg.getFileName(), e);
			}
		}
	}

	private void doWriteFile(int wkid) {
		List<String> messages = new ArrayList<String>();
		while (true) {
			long wrts = 0L;
			Iterator<String> itrf = outputFileMap.keySet().iterator();
			while (itrf.hasNext()) {
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

				//	this.addInfo("doWriteFile-" + wkid + " poll " + messages.size() + " message(s)...");
				if (messages.size() > 0) {
					/**
					 * write the messages to log file.
					 */
					try {
						for (String m : messages) {
							reopenFile(fi);
							fi.write(m);
							if (fi.immediateFlush)
								fi.flush();
						}
					} catch (Exception e) {
						//this.addError("doWriteFile-" + wkid + " message failed. " + e);
					}
					messages.clear();
					fi.lastModifyTime = System.currentTimeMillis();
					wrts += messages.size();
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
								addInfo("Close log file " + fi.name);
							} catch (IOException e) {
								addError("flushAndClose log file " + fi.name + " exception", e);
							}
						}
					}
					if (elapsedTimeMillis > this.maxIdleInterval * 1000) {
						/**
						 * unregister the log file.
						 */
						outputFileMap.remove(fi.name);
					}
				}

				if (!alived) {
					try {
						fi.close();
					} catch (IOException e) {
						addError("flushAndClose log file " + fi.name + " exception", e);
					}
				}
				fi.wkid = -1;
			}

			if (alived) {
				try {
					if (wrts == 0) {
						Thread.sleep(1000 + wkid);
					} else if (wrts < pollBatchSize / 2) {
						Thread.sleep(100 + wkid);
					} else if (wrts < pollBatchSize) {
						Thread.sleep(10 + wkid);
					}
				} catch (InterruptedException e) {
				}
				if (!alived) {
					break;
				}
			} else {
				break;
			}
		}
		if (wkid >= 0) {
			addInfo("writeWorker-" + wkid + " quit.");
			writeWorkers[wkid] = null;
		}
	}

	public void start() {
		registeredAppenders++;
		if (writeWorkers != null) {
			//addInfo("CentralizedFileWriter started， registered" + registeredAppenders + "Appenders ");
			return;
		}
		
		synchronized (outputFileMap) {
			if (writeWorkers != null)
				return;

			alived = true;
			writeWorkers = new Thread[(int) maxWorkers];
			for (int i = 0; i < maxWorkers; i++) {
				final int wid = i;
				Thread worker = new Thread(new Runnable() {
					@Override
					public void run() {
						Thread.currentThread().setName("Logback-AssembleFileWriter_" + wid);
						doWriteFile(wid);
					}
				});
				writeWorkers[i] = worker;
			}
			for (Thread w : writeWorkers) {
				if (w != null)
					w.start();
			}
		}
		/**
		 * ShutdownHook
		 */
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				addInfo("shutdown Logback-Assemble");
				// System.err.println("shutdown Logback-Assemble");
				alived = false;

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
		// addInfo("CentralizedFileWriter started， registered" + registeredAppenders + "Appenders ");
	}

	public void stop() {
		registeredAppenders--;
		if (registeredAppenders <= 0) {
			registeredAppenders = 0;
			alived = false;
		}
	}
}
