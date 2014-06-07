package dh.newspaper.test.dagger;

import org.joda.time.DateTime;

import javax.inject.Inject;

/**
 * Created by hiep on 3/06/2014.
 */
public class Doofi {
	private DateTime creationTime;

	@Inject
	public Doofi() {
		creationTime = DateTime.now();
	}

	public DateTime getCreationTime() {
		return creationTime;
	}
}
