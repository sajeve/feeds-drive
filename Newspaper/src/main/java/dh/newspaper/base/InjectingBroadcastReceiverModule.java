package dh.newspaper.base;

import android.content.Context;
import dagger.Module;
import dagger.Provides;

import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The dagger module associated with {@link dh.newspaper.base.InjectingBroadcastReceiver}
 */
@Module(library = true)
public class InjectingBroadcastReceiverModule {
	Context mContext;
	android.content.BroadcastReceiver mReceiver;
	Injector mInjector;

	/**
	 * Class constructor.
	 *
	 * @param receiver the InjectingBroadcastReceiver with which this module is associated.
	 */
	public InjectingBroadcastReceiverModule(Context context, android.content.BroadcastReceiver receiver, Injector injector) {
		mContext = context;
		mReceiver = receiver;
		mInjector = injector;
	}

	/**
	 * Provides the Context for the BroadcastReceiver associated with this graph.
	 *
	 * @return the BroadcastReceiver Context
	 */
	@Provides
	@Singleton
	@BroadcastReceiver
	public Context provideBroadcastReceiverContext() {
		return mContext;
	}
	/**
	 * Provides the BroadcastReceiver
	 *
	 * @return the BroadcastReceiver
	 */
	@Provides
	@Singleton
	public android.content.BroadcastReceiver provideBroadcastReceiver() {
		return mReceiver;
	}

	/**
	 * Provides the Injector for the BroadcastReceiver-scope graph
	 *
	 * @return the Injector
	 */
	@Provides
	@Singleton
	@BroadcastReceiver
	public Injector provideBroadcastReceiverInjector() {
		return mInjector;
	}
	/**
	 * Defines a qualifier annotation which can be used in conjunction with a type to identify dependencies within
	 * the object graph.
	 *
	 * @see <a href="http://square.github.io/dagger/">the dagger documentation</a>
	 */
	@Qualifier
	@Target({FIELD, PARAMETER, METHOD})
	@Documented
	@Retention(RUNTIME)
	public @interface BroadcastReceiver {
	}
}
