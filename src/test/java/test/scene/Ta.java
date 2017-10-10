package test.scene;

import java.util.Date;

public class Ta {

	public Ta() {
	}

	public static void main(String[] args) throws InterruptedException {
		System.out.println("------------" + new Date());
		Thread.sleep(2 * 1000);
		System.out.println("============" + new Date());
	}
}
