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

//		if (f.file.getFreeSpace() == 0) {
//			this.addError("No space left in " + f.file.getParent() + " to write log.");
//		}
		
		if (f.rollingPolicy != null && f.rollingPolicy instanceof NamingAndSizeBasedRollingPolicy) {
			NamingAndSizeBasedRollingPolicy nasRollingPolicy = (NamingAndSizeBasedRollingPolicy) f.rollingPolicy;

			if (nasRollingPolicy.getMaxFileSize() != null
					&& f.channel.size() >= nasRollingPolicy.getMaxFileSize().getSize()) {

				closeFile(f);
				
				if (nasRollingPolicy.getMaxHistory() <= 0) {
					openFile(f, true);
				} else if (f.rollingName == null || f.rollingName.length() == 0 || (!f.rollingName.contains("%i") && f.rollingName.equals(f.activeName))) {
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
						for (int j = (f.curIdx >= nasRollingPolicy.getMaxHistory() ? nasRollingPolicy.getMaxHistory() : f.curIdx + 1); j > 1; j--) {
							pre = f.rollingName.replaceFirst("%i", (j - 1) + "");
							nxt = f.rollingName.replaceFirst("%i", (j + ""));
							File pref = new File(pre);
							if (pref.exists()) {
								File nxtf = new File(nxt);
								if (nxtf.exists())
									nxtf.getAbsoluteFile().delete();
								nxtf = null;
								f.rollingPolicy.getRenameUtil().rename(pre,nxt);
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
						f.rollingPolicy.getRenameUtil().rename(pre,nxt);
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

			if (alived) {
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
			// addInfo("CentralizedFileWriter started， registered" +
			// registeredAppenders + "Appenders ");
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
		// addInfo("CentralizedFileWriter started， registered" +
		// registeredAppenders + "Appenders ");
	}

	public void stop() {
		registeredAppenders--;
		if (registeredAppenders <= 0) {
			registeredAppenders = 0;
			alived = false;
		}
	}
}
