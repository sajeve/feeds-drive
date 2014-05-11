/*
 * Copyright (c) 2013 Fizz Buzz LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dh.newspaper.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import dagger.ObjectGraph;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

/**
 * Manages an ObjectGraph on behalf of a BroadcastReceiver.  This graph is created by extending the application-scope
 * graph with BroadcastReceiver-specific module(s).
 */
public class InjectingBroadcastReceiver
        extends BroadcastReceiver
        implements Injector {

    private Context mContext;
    private ObjectGraph mObjectGraph;

    /**
     * Creates an object graph for this BroadcastReceiver by extending the application-scope object graph with the
     * modules returned by {@link #getModules()}.
     * <p/>
     * Injects this BroadcastReceiver using the created graph.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;

        // extend the application-scope object graph with the modules for this broadcast receiver
        mObjectGraph = ((Injector) context.getApplicationContext()).getObjectGraph().plus(getModulesPlusSelf().toArray());

        // then inject ourselves
        mObjectGraph.inject(this);
    }

    /**
     * Gets this BroadcastReceiver's object graph.
     *
     * @return the object graph
     */
    @Override
    public ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }

    /**
     * Injects a target object using this BroadcastReceiver's object graph.
     *
     * @param target the target object
     */
    public void inject(Object target) {
        checkState(mObjectGraph != null, "object graph must be initialized prior to calling inject");
        mObjectGraph.inject(target);
    }

    /**
     * Returns the list of dagger modules to be included in this BroadcastReceiver's object graph.  Subclasses that
     * override this method should add to the list returned by super.getModules().
     *
     * @return the list of modules
     */
    protected List<Object> getModules() {
        return null;
    }

	private List<Object> getModulesPlusSelf() {
		List<Object> result = getModules();
		if (result == null) {
			result = new ArrayList<Object>();
		}
		result.add(new InjectingBroadcastReceiverModule(mContext, this, this));
		return result;
	}

}