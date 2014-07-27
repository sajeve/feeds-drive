package dh.newspaper.test;

import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import de.greenrobot.event.EventBus;
import dh.newspaper.MainActivity;
import junit.framework.TestCase;

import java.io.Console;

/**
 * Created by hiep on 10/05/2014.
 */
public class EventBusBasicTest extends ActivityInstrumentationTestCase2<MainActivity> {

	public EventBusBasicTest() {
		super(MainActivity.class);
	}

	public void testRegisterAfterQueueEvents() {
		System.out.println("Start post()");
		EventBus.getDefault().post("1");
		EventBus.getDefault().post("2");
		System.out.println("Start register()");
		EventBus.getDefault().register(this);
		EventBus.getDefault().post("3");
	}

	public void onEvent(String i) {
		System.out.println("Received "+i);
	}

	/*public void testBundle() {
		Bundle bundle = new Bundle();

	}*/
}
