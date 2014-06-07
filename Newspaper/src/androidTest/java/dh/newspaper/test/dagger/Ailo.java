package dh.newspaper.test.dagger;

import dh.newspaper.parser.ContentParser;
import org.joda.time.DateTime;

import javax.inject.Inject;

/**
 * Created by hiep on 3/06/2014.
 */
public class Ailo {
	private DateTime creationTime;

	@Inject
	public Ailo() {
		creationTime = DateTime.now();
	}

	public DateTime getCreationTime() {
		return creationTime;
	}
}
