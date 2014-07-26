package dh.newspaper.model;

import java.io.Serializable;

/**
 * Created by hiep on 26/07/2014.
 */
public class CheckableString implements Serializable {
	private String text;
	private boolean checked = false;

	public CheckableString() {
	}

	public CheckableString(String text) {
		this.text = text;
	}

	public CheckableString(String text, boolean checked) {
		this.text = text;
		this.checked = checked;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}
}
