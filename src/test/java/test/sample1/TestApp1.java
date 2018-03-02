package test.sample1;

import java.io.File;
import java.util.Date;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import test.scene.LoggerAssembleTest;
import test.scene.TestAction;

/**
 * 
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class TestApp1 {

	private final static Logger logger = LoggerFactory.getLogger(TestApp1.class);

	private final static String config = LoggerAssembleTest.class
			.getResource("/test/sample1/logback-" + TestApp1.class.getSimpleName() + ".xml").getFile();

	public static void main(String[] args) throws JoranException {
		File confFile = new File(config);
		logger.error("logback config file :" + config + ", exists ? " + confFile.exists());

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		lc.reset();
		configurator.setContext(lc);
		configurator.doConfigure(confFile);
		// StatusPrinter.print(lc);

		/**
		 * 启用ch.qos.logback.assemble.AssembleAppender， 设置 日志上线文： <br>
		 * userid: 自定义的关键字，供输出文件名使用，参照logback-TestApp1.xml中TEST部分的配置;可根据需要增加其他关键字。
		 * <br>
		 * LEVEL: 自定义日志级别，可填值：ERROR>INFO>DEBUG>TRACE；<br>
		 * 注意logback配置文件中为第一层级别（属于全局配置），该配置为第二层配置，属于自定义配置；一般全局设置为DEBUG，
		 * 而自定义可实现按交易、模块等动态变化。<br>
		 * 该AssembleAppender采用异步化实现，也可限制队列。<br>
		 * 该上下文设置建议放置在我们应用平台统一处理的入口处。<br>
		 */
		MDC.put("userid", "Alice");
		MDC.put("LEVEL", "ERROR");

		Random r = new Random();
		TestService1 service1 = new TestService1(r.nextInt());
		TestService2 service2 = new TestService2(r.nextInt());
		service1.printInfo();
		service2.printInfo();

		logger.error("===================System will exit===============");
		// StatusPrinter.print(lc);
		System.exit(0);
	}

}
