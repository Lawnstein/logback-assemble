package test.scene;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import test.appender.TestDbX;
import test.scene.tran.BaseAction;
import test.scene.tran.T15001Action;
import test.scene.tran.T15002Action;
import test.scene.tran.T15003Action;

/**
 * 
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class TestScene {
	private final static Logger logger = LoggerFactory.getLogger(TestScene.class);

	private final static String config = TestScene.class
			.getResource("/test/scene/logback-" + TestScene.class.getSimpleName() + ".xml").getFile();

	public static void startThreads(boolean sift) {
		int T15001N = 20;
		int T15002N = 15;
		int T15003N = 10;
		List<BaseAction> al = new ArrayList<BaseAction>();
		List<Thread> tl = new ArrayList<Thread>();
		Random r = new Random(System.currentTimeMillis());
		if (T15001N <= 0)
			T15001N = r.nextInt(20);
		for (int i = 0; i < T15001N; i++) {
			al.add(new T15001Action());
		}
		if (T15002N <= 0)
			T15002N = r.nextInt(20);
		for (int i = 0; i < T15002N; i++) {
			al.add(new T15002Action());
		}
		if (T15003N <= 0)
			T15003N = r.nextInt(20);
		for (int i = 0; i < T15003N; i++) {
			al.add(new T15003Action());
		}
		for (BaseAction a : al) {
			if (sift)
				a.setMergmdc(true);
			tl.add(new Thread(a));
		}
		logger.debug(tl.size() + " thread(s) will start ...");
		for (Thread t : tl) {
			t.start();
			try {
				Thread.sleep(r.nextInt(10) * 1000);
			} catch (InterruptedException e) {
			}
		}
		logger.debug("===============All action has started==============");
		for (Thread t : tl) {
			try {
				// 等待所有线程执行完毕
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.debug("===============All action has completed================");
		try {
			int i = r.nextInt(100);
			logger.debug("Sleep for " + i + " second(s).");
			Thread.sleep(i * 1000);
		} catch (InterruptedException e) {
		}

	}

	public static void main(String[] args) throws JoranException {
		File confFile = new File(config);
		System.out.println("logback config file :" + config + ", exists ? " + confFile.exists());

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		lc.reset();
		configurator.setContext(lc);
		configurator.doConfigure(confFile);
		StatusPrinter.print(lc);

		MDC.put("userid", "Alice");
		logger.debug("Application started");
		logger.trace("log trace, " + new Date());
		logger.debug("log debug, " + new Date());
		logger.info("log info, " + new Date());
		logger.warn("log warn, " + new Date());
		logger.error("log error, " + new Date());

		logger.debug("Alice says hello");
		// startThreads(false);

		// StatusPrinter.print(lc);
		StatusPrinter.print(lc);
		System.out.println("===================System will exit===============");
		try {
			Random r = new Random(System.currentTimeMillis());
			int i = r.nextInt(10);
			logger.debug("Sleep for " + i + " second(s).");
			Thread.sleep(i * 1000);
		} catch (InterruptedException e) {
		}
		StatusPrinter.print(lc);
		System.exit(0);
	}

}
