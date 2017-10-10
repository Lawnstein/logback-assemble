package test.scene.tran;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class T15001Action extends BaseAction {
	private final static Logger logger = LoggerFactory.getLogger(T15001Action.class);

	@Override
	public void pre() {
		this.setTrcode("15001");
	}
}
