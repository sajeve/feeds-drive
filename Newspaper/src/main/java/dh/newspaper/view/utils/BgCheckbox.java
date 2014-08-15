package dh.newspaper.view.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;
import dh.newspaper.R;

import java.util.concurrent.Executor;

/**
 * Checkbox, when client click, display a progressbar, and do job in background
 * A non null tag is required (to know when the view is ready)
 * Created by hiep on 14/08/2014.
 */
public class BgCheckbox extends ViewSwitcher {
	private static final String TAG = BgCheckbox.class.getName();
	private CheckBox checkBox;
	private ProgressBar progressBar;
	private ICheckedAction checkedAction;
	private Executor executor;

	public BgCheckbox(final Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.bg_check_box, this, true);
		checkBox = (CheckBox)findViewById(R.id.checkbox);
		progressBar = (ProgressBar)findViewById(R.id.progress);

		checkBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if (BgCheckbox.this.getTag()==null) {
						return;
					}

					boolean isChecked = ((CheckBox)v).isChecked();

					//Toast.makeText(BgCheckbox.this.getContext(), Boolean.toString(isChecked), Toast.LENGTH_SHORT).show();

					setRefreshing(true);

					AsyncTask task = new AsyncTask<Object, Object, Boolean>() {
						@Override
						protected Boolean doInBackground(Object... params) {
							try {
								if (checkedAction == null) {
									return true;
								}
								return checkedAction.performActionInBackground((Boolean) (params[0]), params[1]);
							}
							catch (Exception ex) {
								Log.wtf(TAG, ex);
								return false;
							}
						}

						@Override
						protected void onPostExecute(Boolean success) {
							try {
								setRefreshing(false);
								if (checkedAction!=null) {
									checkedAction.onFinished(BgCheckbox.this, success);
								}
							}
							catch (Exception ex) {
								Log.wtf(TAG, ex);
							}
						}
					};

					if (executor == null) {
						task.execute(isChecked, BgCheckbox.this.getTag());
					}
					else {
						task.executeOnExecutor(executor, isChecked, BgCheckbox.this.getTag());
					}
				}
				catch (Exception ex) {
					Log.wtf(TAG, ex);
				}
			}
		});

		/*checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

			}
		});*/


	}

	public boolean isRefreshing() {
		return getNextView() == checkBox;
	}

	public void setRefreshing(boolean b) {
		if ((getNextView() == checkBox) != b) {
			showNext();
		}
	}

	public boolean isChecked() {
		return checkBox.isChecked();
	}

	public void setChecked(boolean b) {
		checkBox.setChecked(b);
	}

	public void setOnCheckedChangeListener(ICheckedAction action) {
		checkedAction = action;
	}

	public void setExecutor(Executor exec) {
		executor = exec;
	}

	/**
	 * Action to perform in background thread
	 */
	public static interface ICheckedAction {
		/**
		 * tag is the tag object of the component sent to the background thread
		 * return true if success
		 * @param isChecked
		 */
		public boolean performActionInBackground(boolean isChecked, Object senderTag);
		public void onFinished(Object sender, boolean success);
	}
}
