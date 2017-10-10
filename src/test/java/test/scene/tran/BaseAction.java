package test.scene.tran;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class BaseAction implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(BaseAction.class);

	private int loopA = 2;
	private int loopB = 1000;
	private boolean mergmdc = false;
	private String trcode;

	public String getTrcode() {
		return trcode;
	}

	public void setTrcode(String trcode) {
		this.trcode = trcode;
	}

	public boolean isMergmdc() {
		return mergmdc;
	}

	public void setMergmdc(boolean mergmdc) {
		this.mergmdc = mergmdc;
	}

	abstract public void pre();

	private void initMDC() {
		String threadName = Thread.currentThread().getName().replaceAll("-", " ").replaceAll("_", " ");
		String[] threadAs = threadName.split("[ ]");
		String threadNo = (threadAs != null && threadAs.length > 0) ? threadAs[threadAs.length - 1] : "0";
		if (isMergmdc()) {
			MDC.put("TRCODE", trcode + "_" + threadNo);
		} else {
			MDC.put("TRCODE", trcode);
			MDC.put("THREADNO", threadNo);
		}
	}

	@Override
	public void run() {
		pre();
		initMDC();
		logger.info(this.getClass() + " run() start >>>>");
		logger.info(this.getClass() + " MDC:" + MDC.getCopyOfContextMap());
		Random r = new Random(System.currentTimeMillis());
		if (loopA < 0)
			loopA = r.nextInt(100);
		logger.info(Thread.currentThread().getName() + ", ThreadID=" + Thread.currentThread().getId()
				+ ", first level, running for " + loopA + " time(s).");
		for (int i = 0; i < loopA; i++) {

			if (loopB < 0)
				loopB = r.nextInt(20000);
			logger.info(Thread.currentThread().getName() + ", ThreadID=" + Thread.currentThread().getId()
					+ ", second level, running for " + loopB + " time(s).");
			for (int j0 = 0; j0 < loopB; j0++) {
				logger.info(Thread.currentThread().getId() + " loop for ================" + i + "/" + loopA
						+ "================= [[[[[[[[[[[[[[[[[[[[[[[[[" + j0 + "/" + loopB + "]]]]]]]]]]]]]]]]]]");
			}
			try {
				Thread.sleep(r.nextInt(10000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info(this.getClass() + " run() over <<<<");
	}
}
