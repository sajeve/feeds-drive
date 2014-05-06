package dh.newspaper.parser;

import java.text.ParseException;

/**
 * Created by hiep on 7/05/2014.
 */
public class RssParserException extends ParseException {
	/**
	 * Constructs a new instance of this class with its stack trace, detail
	 * message and the location of the error filled in.
	 *
	 * @param detailMessage the detail message for this exception.
	 * @param location
	 */
	public RssParserException(String detailMessage, int location) {
		super(detailMessage, location);
	}
}
