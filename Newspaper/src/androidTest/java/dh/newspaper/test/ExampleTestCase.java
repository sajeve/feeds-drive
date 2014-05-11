package dh.newspaper.test;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExampleTestCase extends TestCase {

	public void testTry() throws IOException {
	}

	public void testParentChild() {
		Child a = new Child();
		assertEquals("Parent: [10, 20, 30]", a.onCreate());

		Parent b = new Child();
		assertEquals("Parent: [10, 20, 30]", b.onCreate());

		Parent c = new Child();
		assertEquals("Parent: [10, 20, 30]", c.onResume());

		Parent d = new Child();
		assertEquals("Parent: [10, 20, 30]", ((Parent)d).onResume());

		Child e = new Child();
		assertEquals("Parent: [10, 20, 30]", ((Parent)e).onResume());
	}

	public class Parent {
		protected String onCreate() {
			String s = "Parent: " + Arrays.toString(getItems().toArray());
			System.out.println(s);
			return s;
		}

		protected String onResume() {
			String s = "Parent: " + Arrays.toString(getItems().toArray());
			System.out.println(s);
			return s;
		}

		public List<Integer> getItems() {
			return new ArrayList<Integer>() {{ add(1); add(2);}};
		}
	}

	public class Child extends Parent {

		@Override
		public String onResume() {
			return super.onResume();
		}

		//@Override
		public List<Integer> getItems() {
			return new ArrayList<Integer>() {{ add(10); add(20); add(30); }};
		}
	}

}
