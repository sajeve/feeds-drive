//package dh.newspaper.test.dagger;
//
//import android.test.ActivityInstrumentationTestCase2;
//import dagger.ObjectGraph;
//import dh.newspaper.MainActivity;
//import dh.newspaper.MyApplication;
//import dh.newspaper.cache.ModelHelper;
//import org.joda.time.DateTime;
//
//import javax.inject.Inject;
//
///**
// * Created by hiep on 3/06/2014.
// */
//public class DaggerTest extends ActivityInstrumentationTestCase2<MainActivity> {
//
//	public DaggerTest() {
//		super(MainActivity.class);
//	}
//
//	public void testInjection() {
////		ContentParser d1 = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(ContentParser.class);
////		A1 a1 = new A1(((MyApplication)this.getActivity().getApplication()).getObjectGraph());
////		ContentParser cp = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(ContentParser.class);
//		ModelHelper mModelHelper = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(ModelHelper.class);
//		Ailo a2=                     ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(Ailo.class);
//		//assertEquals(d1.getCreationTime(), a1.getCreationTimeD1());
//	}
//
//	static class D2 {
//		private DateTime creationTime;
//		@Inject
//		public D2() {
//			creationTime = DateTime.now();
//		}
//
//		public DateTime getCreationTime() {
//			return creationTime;
//		}
//	}
//
//	static class A1 {
//		@Inject
//		Doofi d1;
//
//		@Inject
//		public A1(ObjectGraph og) {
//			og.inject(this);
//		}
//
//		public DateTime getCreationTimeD1() {
//			return d1.getCreationTime();
//		}
//	}
//}
