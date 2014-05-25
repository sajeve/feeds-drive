package dh.newspaper.parser;

import org.joda.time.format.DateTimeParser;
import org.joda.time.format.DateTimeParserBucket;

/**
 * Created by hiep on 26/05/2014.
 */
public class DefaultDateTimeParser implements DateTimeParser {
	@Override
	public int estimateParsedLength() {
		return 0;
	}

	@Override
	public int parseInto(DateTimeParserBucket dateTimeParserBucket, String s, int i) {

		return 0;
	}
}
