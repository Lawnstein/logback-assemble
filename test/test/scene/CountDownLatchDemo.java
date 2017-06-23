package test.ccbs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * CountDownLatch，一个同步辅助类，在完成一组正在其他线程中执行的操作之前，它允许一个或多个线程一直等待。 <br>
 * 主要方法 public CountDownLatch(int count); <br>
 * public void countDown(); <br>
 * public void await() throws InterruptedException
 * 
 * 构造方法参数指定了计数的次数 <br>
 * countDown方法，当前线程调用此方法，则计数减一 <br>
 * awaint方法，调用此方法会一直阻塞当前线程，直到计时器的值为0 <br>
 * 
 * @author Lawnstein.Chan
 * @version $Revision:$
 */
public class CountDownLatchDemo {
	final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

	public static void main(String[] args) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(3);// 两个工人的协作
		List<Worker> wkl = new ArrayList<Worker>();
		wkl.add(new Worker("zhang san", 5000, latch));
		wkl.add(new Worker("li si", 8000, latch));
		wkl.add(new Worker("wang wu", 4000, latch));
		wkl.add(new Worker("Tom", latch));
		for (Worker w : wkl)
			w.start();

		for (Worker w : wkl) {
			// 等待所有线程执行完毕
			w.join();
		}
		System.out.println("===============All action has completed================");
		System.out.println("all work done at " + sdf.format(new Date()));
	}

	static class Worker extends Thread {
		String workerName;
		int workTime = 0;
		CountDownLatch latch = null;

		public Worker(String workerName, int workTime, CountDownLatch latch) {
			this.workerName = workerName;
			this.workTime = workTime;
			this.latch = latch;
		}

		public Worker(String workerName, CountDownLatch latch) {
			this.workerName = workerName;
			this.latch = latch;
		}

		public void run() {
			if (workTime > 0) {
				System.out.println("Worker " + workerName + " do work begin at " + sdf.format(new Date()));
				doWork();// 工作了
				System.out.println("Worker " + workerName + " do work complete at " + sdf.format(new Date()));
				latch.countDown();// 工人完成工作，计数器减一
			} else {
				try {
					// 等待所有工人完成工作
					latch.await();
					System.out.println("All worker is over, manager " + workerName + " close the door at "
							+ sdf.format(new Date()));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		private void doWork() {
			try {
				Thread.sleep(workTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
