package dh.newspaper.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.widget.ArrayAdapter;

import java.util.Collection;
import java.util.List;

/**
 * Created by hiep on 7/06/2014.
 */
public class ArrayAdapterCompat<T> extends ArrayAdapter<T> {

	public ArrayAdapterCompat(Context context, int resource) {
		super(context, resource);
	}

	public ArrayAdapterCompat(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}

	public ArrayAdapterCompat(Context context, int resource, T[] objects) {
		super(context, resource, objects);
	}

	public ArrayAdapterCompat(Context context, int resource, int textViewResourceId, T[] objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public ArrayAdapterCompat(Context context, int resource, List<T> entries) {
		super(context, resource, entries);
	}

	public ArrayAdapterCompat(Context context, int resource, int textViewResourceId, List<T> objects) {
		super(context, resource, textViewResourceId, objects);
	}

	/**
	 * Add all elements in the collection to the end of the adapter.
	 * @param list to add all elements
	 */
	@SuppressLint("NewApi")
	public void addAll(Collection<? extends T> list) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			super.addAll(list);
		} else {
			for (T element : list) {
				super.add(element);
			}
		}
	}

	/**
	 * Add all elements in the array to the end of the adapter.
	 * @param array to add all elements
	 */
	@SuppressLint("NewApi")
	public void addAll(T... array) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			super.addAll(array);
		} else {
			for (T element : array) {
				super.add(element);
			}
		}
	}
}
