package test.scene.tran;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class T15002Action extends BaseAction {
	private final static Logger logger = LoggerFactory.getLogger(T15001Action.class);

	public T15002Action(int loopA, int loopB) {
		super(loopA, loopB);
	}
	
	@Override
	public void pre() {
		this.setTrcode("15002");
	}
}
