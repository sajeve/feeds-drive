package dh.newspaper.modules;

import dagger.Module;
import dagger.Provides;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.parser.SubscriptionFactory;
import dh.newspaper.view.ArticleFragment;
import dh.newspaper.view.TagsFragment;
import dh.newspaper.view.FeedsFragment;
import dh.newspaper.parser.ContentParser;

import javax.inject.Singleton;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by hiep on 8/05/2014.
 */
@Module (
	injects = {
			TagsFragment.class,
			FeedsFragment.class,
			ArticleFragment.class,
			MainActivity.class,
			MyApplication.class,
			SubscriptionFactory.class
	})
public class GlobalModule {

	@Provides @Singleton
	public ContentParser provideContentParser() {
		return new ContentParser();
	}

	@Provides @Singleton
	public TagsFragment provideCategoriesFragment() {
		return new TagsFragment();
	}

	@Provides @Singleton
	public AppBundle provideAppBundle() {
		return new AppBundle();
	}

	//@Provides @Singleton
	public MessageDigest provideMessageDigest() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return  null;
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
