package test.sample1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class TestService2 {
	public int a;

	private final static Logger logger = LoggerFactory.getLogger(TestService2.class);

	public TestService2(int a) {
		this.a = a;
	}
	public void printInfo() {
		logger.error("TestService2.printError : a={}", a);
		logger.info("TestService2.printInfo : a={}", a);
		logger.debug("TestService2.printDebug : a={}", a);
		logger.trace("TestService2.printTrace : a={}", a);
	}
}
