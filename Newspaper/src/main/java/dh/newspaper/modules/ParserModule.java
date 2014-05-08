package dh.newspaper.modules;

import dagger.Module;
import dagger.Provides;
import dh.newspaper.MainActivity;
import dh.newspaper.parser.ContentParser;

import javax.inject.Singleton;

/**
 * Created by hiep on 8/05/2014.
 */
@Module (injects = {MainActivity.PlaceholderFragment.class})
public class ParserModule {

	@Provides @Singleton
	public ContentParser provideContentParser() {
		return new ContentParser();
	}
}
