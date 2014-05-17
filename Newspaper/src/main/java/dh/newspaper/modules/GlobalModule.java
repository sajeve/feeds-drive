package dh.newspaper.modules;

import dagger.Module;
import dagger.Provides;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.base.InjectingApplication;
import dh.newspaper.view.ArticleFragment;
import dh.newspaper.view.CategoriesFragment;
import dh.newspaper.view.FeedsFragment;
import dh.newspaper.parser.ContentParser;

import javax.inject.Singleton;

/**
 * Created by hiep on 8/05/2014.
 */
@Module (
	injects = {
			CategoriesFragment.class,
			FeedsFragment.class,
			ArticleFragment.class,
			MainActivity.class,
			MyApplication.class
	})
public class GlobalModule {

	@Provides @Singleton
	public ContentParser provideContentParser() {
		return new ContentParser();
	}

	@Provides @Singleton
	public CategoriesFragment provideCategoriesFragment() {
		return new CategoriesFragment();
	}

	@Provides @Singleton
	public AppBundle provideAppBundle() {
		return new AppBundle();
	}

//	@Provides @Singleton
//	public FeedsFragment provideFeedsFragment() {
//		return new FeedsFragment();
//	}
//
//	@Provides @Singleton
//	public ArticleFragment provideArticleFragment() {
//		return new ArticleFragment();
//	}
}
