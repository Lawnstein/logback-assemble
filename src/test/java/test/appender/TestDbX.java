package test.appender;

import java.io.File;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * 
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class TestDbX {
	private final static Logger logger = LoggerFactory.getLogger(TestDbX.class);
	private final static String config = TestDbX.class
			.getResource("/test/appender/logback-" + TestDbX.class.getSimpleName() + ".xml").getFile();


	public static void main(String[] args) throws JoranException {
		File confFile = new File(config);
		System.out.println("logback config file :" + config + ", exists ? " + confFile.exists());

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		lc.reset();
		configurator.setContext(lc);
		configurator.doConfigure(confFile);
		StatusPrinter.print(lc);
//        StatusPrinter.printInCaseOfErrorsOrWarnings(lc); 

		MDC.put("userid", "Alice");
		logger.debug("Application started");
		logger.trace("log trace, " + new Date());
		logger.debug("log debug, " + new Date());
		logger.info("log info, " + new Date());
		logger.warn("log warn, " + new Date());
		logger.error("log error, " + new Date());


		// StatusPrinter.print(lc);
		System.out.println("===================System will exit===============");
		// System.exit(0);
	}

}
