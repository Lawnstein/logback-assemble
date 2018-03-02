package test.sample1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class TestService1 {
	public int a;

	private final static Logger logger = LoggerFactory.getLogger(TestService1.class);

	public TestService1(int a) {
		this.a = a;
	}

	public void printInfo() {
		logger.error("TestService1.printError : a={}", a);
		logger.info("TestService1.printInfo : a={}", a);
		logger.debug("TestService1.printDebug : a={}", a);
		logger.trace("TestService1.printTrace : a={}", a);
	}
}
