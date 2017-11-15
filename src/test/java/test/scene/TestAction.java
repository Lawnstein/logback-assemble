package test.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.scene.tran.BaseAction;
import test.scene.tran.T15001Action;
import test.scene.tran.T15002Action;
import test.scene.tran.T15003Action;

public class TestAction {
	private final static Logger logger = LoggerFactory.getLogger(TestAction.class);
	private CountDownLatch countDownLatch;
	
	public void startThreads(boolean sift) {
		int LOOPA = 10;
		int LOOPB = 10000;
//		int T15001N = 20;
//		int T15002N = 15;
//		int T15003N = 10;
		int T15001N = 8;
		int T15002N = 6;
		int T15003N = 4;
		List<BaseAction> al = new ArrayList<BaseAction>();
		List<Thread> tl = new ArrayList<Thread>();
		Random r = new Random(System.currentTimeMillis());
		if (T15001N <= 0)
			T15001N = r.nextInt(20);
		for (int i = 0; i < T15001N; i++) {
			al.add(new T15001Action(LOOPA, LOOPB));
		}
		if (T15002N <= 0)
			T15002N = r.nextInt(20);
		for (int i = 0; i < T15002N; i++) {
			al.add(new T15002Action(LOOPA, LOOPB));
		}
		if (T15003N <= 0)
			T15003N = r.nextInt(20);
		for (int i = 0; i < T15003N; i++) {
			al.add(new T15003Action(LOOPA, LOOPB));
		}
		countDownLatch = new CountDownLatch(al.size());
		for (BaseAction a : al) {
			if (sift)
				a.setMergmdc(true);
			a.setCountDownLatch(countDownLatch);
			a.setRandomSleep(false);
			tl.add(new Thread(a));
		}
		logger.error(tl.size() + " thread(s) will start ...");
		for (Thread t : tl) {
			t.start();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		logger.error("=============wait for all action process over===========");
		try {
			// 调用await方法阻塞当前线程，等待子线程完成后在继续执行
			countDownLatch.await();
		} catch (InterruptedException e) {
		}
		logger.error("===============All action has completed================");
	}
}
