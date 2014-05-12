package dh.newspaper.modules;

import dagger.Module;
import dagger.Provides;
import dh.newspaper.adapter.ArticlePreviewGridAdapter;
import dh.newspaper.view.CardsListFragment;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.view.ReaderFragment;

import javax.inject.Singleton;

/**
 * Created by hiep on 8/05/2014.
 */
@Module (
	injects = {
			CardsListFragment.class,
			ReaderFragment.class
	})
public class ParserModule {

	@Provides @Singleton
	public ContentParser provideContentParser() {
		return new ContentParser();
	}
}
