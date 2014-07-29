package dh.newspaper.event;

import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * Created by hiep on 11/05/2014.
 */
public class BaseEventOneArg<T> extends BaseEvent<T> {
	public String stringArg;
	public int intArg;
	public long longArg;
	public boolean boolArg;
	public Object objectArg;

	public BaseEventOneArg(T sender) {
		super(sender);
	}

	public BaseEventOneArg(T sender, String subject) {
		super(sender, subject);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(String.format("{EventOneArg sender=%s", this.getSender()));
		if (!Strings.isNullOrEmpty(getSubject())) {
			sb.append(" subject='"+this.getSubject()+"'");
		}
		if (!Strings.isNullOrEmpty(stringArg)) {
			sb.append(" stringArg='"+stringArg+"'");
		}
		if (objectArg!=null) {
			sb.append(" objectArg='"+objectArg+"'");
		}
		sb.append(" intArg="+intArg);
		sb.append(" longArg="+longArg);
		sb.append(" boolArg="+boolArg);
		sb.append("}");
		return sb.toString();
	}
}
