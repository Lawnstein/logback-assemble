package test.scene.tran;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class BaseAction implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(BaseAction.class);

	private int loopA = 2;
	private int loopB = 10000;
	private boolean randomSleep = true;
	private boolean mergmdc = false;
	private String trcode;
	private CountDownLatch countDownLatch;

	public BaseAction(int loopA, int loopB) {
		this.loopA = loopA;
		this.loopB = loopB;
	}

	public int getLoopA() {
		return loopA;
	}

	public void setLoopA(int loopA) {
		this.loopA = loopA;
	}

	public int getLoopB() {
		return loopB;
	}

	public void setLoopB(int loopB) {
		this.loopB = loopB;
	}

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

	public CountDownLatch getCountDownLatch() {
		return countDownLatch;
	}

	public void setCountDownLatch(CountDownLatch countDownLatch) {
		this.countDownLatch = countDownLatch;
	}

	public boolean isRandomSleep() {
		return randomSleep;
	}

	public void setRandomSleep(boolean randomSleep) {
		this.randomSleep = randomSleep;
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
		logger.error("{} {} run() start >>>>", this.getClass(), Thread.currentThread().getName());
		logger.info("{} {} MDC:{}", this.getClass(), Thread.currentThread().getName(), MDC.getCopyOfContextMap());
		Random r = new Random(System.currentTimeMillis());
		if (loopA < 0)
			loopA = r.nextInt(100);
		logger.info("{}, ThreadID={}, first level, running for {} time(s).", Thread.currentThread().getName(),  Thread.currentThread().getId(), loopA);
		for (int i = 0; i < loopA; i++) {

			if (loopB < 0)
				loopB = r.nextInt(20000);
			logger.info("{}, ThreadID={}, second level, running for {} time(s).", Thread.currentThread().getName(), Thread.currentThread().getId(), loopB);
			
			for (int j0 = 0; j0 < loopB; j0++) {
				logger.info("{} loop for ================{}/{}================= [[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ {}/{} ]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]",
						Thread.currentThread().getId(), i, loopA, j0, loopB);
			}

			logger.error("{}, ThreadID={}, LoopA runed for {}/{} time(s).", Thread.currentThread().getName(), Thread.currentThread().getId(), i, loopA);

			if (!randomSleep) {
				try {
					Thread.sleep(r.nextInt(10000));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
		logger.error("{} {} run() over <<<<",this.getClass(), Thread.currentThread().getName());
		if (countDownLatch != null)
			countDownLatch.countDown();
	}
}
