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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.assemble.rolling.AssembleRollingPolicyBase;
import ch.qos.logback.assemble.rolling.NamingAndSizeBasedRollingPolicy;
import ch.qos.logback.classic.Level;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.Compressor;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.util.FileUtil;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class FileWriterBase extends ContextAwareBase {

	protected static Map<String, FileItem> fileWriters = new ConcurrentHashMap<String, FileItem>();

	protected static Map<CompressionMode, Compressor> compressorMap = new ConcurrentHashMap<CompressionMode, Compressor>();

	protected static Map<String, Long> discarded = new ConcurrentHashMap<String, Long>();

	protected static FileWriterBase instance = null;

	public int healthCheckInterval = 10; // seconds

	public int maxIdleInterval = healthCheckInterval * 3;

	private Thread healthWorker = null;

	private boolean alived = false;

	private boolean shutdownHooked = false;

	public FileWriterBase(Context context) {
		super();
		setContext(context);
	}

	public FileWriterBase(ContextAware declaredOrigin) {
		super(declaredOrigin);
		if (declaredOrigin != null && this.getContext() == null) {
			this.setContext(declaredOrigin.getContext());
		}
	}

	public static FileWriterBase getInstance() {
		return getInstance(null);
	}

	public static FileWriterBase getInstance(Context context) {
		synchronized (fileWriters) {
			if (instance == null) {
				instance = new FileWriterBase(context);
			}
		}
		return instance;
	}

	protected Compressor getCompressor(CompressionMode compressionMode) {
		Compressor compressor = compressorMap.get(compressionMode);
		if (compressor == null) {
			compressor = new Compressor(compressionMode);
			compressor.setContext(this.getContext());
			compressorMap.put(compressionMode, compressor);
		}
		return compressor;
	}

	protected String getSimpleFilename(String fileName) {
		return new File(fileName).getName();
	}

	protected String getUncompressedFilename(String fileName) {
		if (fileName == null)
			return fileName;
		if (fileName.endsWith(".gz"))
			fileName = fileName.substring(0, fileName.length() - 3);
		else if (fileName.endsWith(".zip"))
			fileName = fileName.substring(0, fileName.length() - 4);
		return fileName;
	}

	protected void rename(FileItem f, String origFileName, String newFileName) {
		File of = new File(origFileName);
		if (!of.exists()) {
			of = null;
			return;
		}
		File nf = new File(newFileName);
		if (nf.exists())
			nf.getAbsoluteFile().delete();
		nf = null;

		f.rollingPolicy.getRenameUtil().rename(origFileName, newFileName);
	}

	protected void openFile(FileItem f) throws Exception {
		openFile(f, false);
	}

	protected void openFile(FileItem f, boolean forceClearContent) throws Exception {
		f.activeName = f.getFileName();
		if (f.activeName.indexOf("%i") > 0) {
			f.activeName = f.activeName.replaceFirst("%i", "0");
		}
		if (f.compressionMode != CompressionMode.NONE) {
			if (f.activeName.endsWith(".gz"))
				f.activeName = f.activeName.substring(0, f.activeName.length() - 3);
			else if (f.activeName.endsWith(".zip"))
				f.activeName = f.activeName.substring(0, f.activeName.length() - 4);
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

	protected void closeFile(FileItem f) throws Exception {
		f.close();
	}

	protected void compress(FileItem f, String innerEntryName) {
		if (f.compressionMode == CompressionMode.NONE) {
			return;
		}
		String validInnerEntryName = innerEntryName;
		if (validInnerEntryName == null || validInnerEntryName.length() == 0) {
			validInnerEntryName = f.activeName;
		}
		validInnerEntryName = getUncompressedFilename(validInnerEntryName);
		String nameOfCompressedFile = f.activeName + "." + System.currentTimeMillis();
		String fullNameOfCompressedFile = nameOfCompressedFile;
		switch (f.compressionMode) {
		case GZ:
			fullNameOfCompressedFile += ".gz";
			break;
		case ZIP:
			fullNameOfCompressedFile += ".zip";
			break;
		}
		File cf = new File(fullNameOfCompressedFile);
		if (cf.exists()) {
			cf.getAbsoluteFile().delete();
		}
		cf = null;
		getCompressor(f.compressionMode).compress(f.activeName, nameOfCompressedFile,
				getSimpleFilename(validInnerEntryName));
		rename(f, fullNameOfCompressedFile, f.activeName);
	}

	protected void checkAndOpenFile(FileItem f) throws Exception {
		if (f.file == null) {
			f.curIdx = -1;
			openFile(f);
		}

		if (f.rollingPolicy != null && f.rollingPolicy instanceof NamingAndSizeBasedRollingPolicy) {
			NamingAndSizeBasedRollingPolicy nasRollingPolicy = (NamingAndSizeBasedRollingPolicy) f.rollingPolicy;

			if (nasRollingPolicy.getMaxFileSize() != null
					&& f.channel.size() >= nasRollingPolicy.getMaxFileSize().getSize()) {

				closeFile(f);

				if (nasRollingPolicy.getMaxHistory() <= 0) {
					/**
					 * None history configured, just overwrite the original log
					 * file.
					 */
					openFile(f, true);
				} else if (f.rollingName == null || f.rollingName.length() == 0
						|| (!f.rollingName.contains("%i") && f.rollingName.equals(f.activeName))) {
					/**
					 * rolling fileName = active fileName, just overwrite the
					 * original log file.
					 */
					openFile(f, true);
				} else if (!f.rollingName.contains("%i") && !f.rollingName.equals(f.activeName)) {
					/**
					 * no rolling policy.
					 */
					compress(f, f.rollingName);
					rename(f, f.activeName, f.rollingName);
					openFile(f);
				} else {
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
							rename(f, pre, nxt);
						}
					}

					/**
					 * rename current log file.
					 */
					pre = f.activeName;
					nxt = f.rollingName.replaceFirst("%i", "1");
					compress(f, f.activeName + "." + System.currentTimeMillis());
					rename(f, pre, nxt);

					if (f.curIdx < nasRollingPolicy.getMaxHistory())
						f.curIdx++;

					openFile(f);
				}

				this.addInfo("Reopen log file " + f.fileName + (f.charset == null ? "" : " " + f.charset.name()));
			}
		}
	}

	private boolean chkCloseAndRemove(FileItem fi) {
		boolean remove = false;
		long currStamp = System.currentTimeMillis();

		/**
		 * check the idle time.
		 */
		long elapsedTimeMillis = currStamp - fi.lastModifyTime;
		if (elapsedTimeMillis > healthCheckInterval * 1000) {
			/**
			 * flush and close the log file.
			 */
			if (fi.isOpened() && fi.lock(currStamp)) {
				try {
					fi.close();
					addInfo("Close log file " + fi.fileName);
				} catch (IOException e) {
					addError("Close log file " + fi.fileName + " exception", e);
				} finally {
					fi.unlock();
				}
			}
		}
		if (elapsedTimeMillis > maxIdleInterval * 1000) {
			/**
			 * unregister the log file.
			 */
			synchronized (fileWriters) {
				addInfo("Remove log file " + fi.fileName);
				remove = true;
			}
		}
		return remove;
	}

	protected void healthExamine() {
		long lastStamp = System.currentTimeMillis();
		while (alived) {
			try {
				Thread.sleep(healthCheckInterval * 1000);
			} catch (InterruptedException e) {
				break;
			}
			if (!alived) {
				break;
			}

			addInfo("Health Examine " + fileWriters.size() + " log file(s), sleeped for " + (healthCheckInterval * 1000)
					+ "ms.");
			Iterator<String> itr = fileWriters.keySet().iterator();
			while (itr.hasNext()) {
				if (!alived) {
					break;
				}

				String finame = itr.next();
				FileItem fi = fileWriters.get(finame);
				if (chkCloseAndRemove(fi)) {
					itr.remove();
				}
			}

			long currStamp = System.currentTimeMillis();
			if (currStamp - lastStamp >= maxIdleInterval * 1000) {
				String s = "";
				for (Entry<String, Long> en : discarded.entrySet()) {
					if (s.length() > 0) {
						s += ", ";
					}
					s = en.getKey() + ":" + en.getValue();
				}
				addInfo("Health Examine " + discarded.size() + " appender(s) discarded logs {" + s + "}");
				lastStamp = currStamp;
			}
		}

		Iterator<String> itr = fileWriters.keySet().iterator();
		while (itr.hasNext()) {
			String finame = itr.next();
			FileItem fi = fileWriters.get(finame);
			if (chkCloseAndRemove(fi)) {
				itr.remove();
			}
		}
	}

	public synchronized void start() {
		if (alived) {
			return;
		}

		synchronized (fileWriters) {
			if (healthWorker != null)
				return;

			alived = true;
			
			healthWorker = new Thread(new Runnable() {
				@Override
				public void run() {
					Thread.currentThread().setName("Logback-AssembleExaminer");
					addInfo("FileWriterBase Examiner start.");
					healthExamine();
					addInfo("FileWriterBase Examiner quit.");
				}
			});
			healthWorker.setDaemon(true);
			healthWorker.start();

			/**
			 * ShutdownHook
			 */
			if (!shutdownHooked) {
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					public void run() {
						addInfo("shutdown Logback-Assemble");
						alived = false;

						if (healthWorker != null) {
							healthWorker.interrupt();
							healthWorker = null;
						}
						addInfo("Destroyer over.");
					}
				}, "Logback-AssembleDestroyer"));
				shutdownHooked = true;
			}
		}

	}

	public void stop() {
	}

	public void discard(String appenderName, long count) {
		discarded.put(appenderName, count);
	}

	public void write(String fileNameIn, String rollingNameIn, String message, Level level, Encoder encoder,
			AssembleRollingPolicyBase rollingPolicy) {

		if (!alived) {
			start();
		}

		String fileName = fileNameIn == null ? rollingNameIn : fileNameIn;
		String rollingName = rollingNameIn == null ? fileNameIn : rollingNameIn;

		FileItem fi = fileWriters.get(fileName);
		if (fi == null) {
			synchronized (fileWriters) {
				fi = fileWriters.get(fileName);
				if (fi == null) {
					try {
						fi = new FileItem(getContext(), fileName, rollingName, encoder, rollingPolicy);
						fileWriters.put(fileName, fi);
					} catch (Exception e) {
						addError("Create  FileItem for log file " + fileName + " failed.", e);
						return;
					}
				}
			}
		}

		/**
		 * write the messages to log file.
		 */
		long currStamp = System.currentTimeMillis();
		if (fi.lock(currStamp)) {
			fi.update(currStamp, rollingName, encoder, rollingPolicy);
			try {
				String m = message;
				checkAndOpenFile(fi);
				fi.write(m);
				if (fi.immediateFlush)
					fi.flush();

				fi.lastModifyTime = currStamp;
			} catch (Throwable e) {
				this.addError("write message to " + fileName + " failed,  " + e.getMessage(), e);
			} finally {
				fi.unlock();
			}
		} else {
			this.addError("write message to " + fileName + " failed,  cannot lock file handler.");
			return;
		}
	}
}
