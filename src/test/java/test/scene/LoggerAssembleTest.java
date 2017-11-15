package test.scene;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
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
public class LoggerAssembleTest {
	private final static Logger logger = LoggerFactory.getLogger(LoggerAssembleTest.class);

	private final static String config = LoggerAssembleTest.class
			.getResource("/test/scene/logback-" + LoggerAssembleTest.class.getSimpleName() + ".xml").getFile();

	public static void main(String[] args) throws JoranException {
		File confFile = new File(config);
		logger.error("logback config file :" + config + ", exists ? " + confFile.exists());

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
		
		TestAction ta = new TestAction();
		
		long s = System.currentTimeMillis();
		ta.startThreads(false);
		long e = System.currentTimeMillis() - s;
		logger.error("Total elapsed " + e + " millis.");

		//StatusPrinter.print(lc);
		logger.error("===================System will exit===============");
		StatusPrinter.print(lc);
		System.exit(0);
	}

}
