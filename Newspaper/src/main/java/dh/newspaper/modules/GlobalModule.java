package dh.newspaper.modules;

import dagger.Module;
import dagger.Provides;
import dh.newspaper.DetailActivity;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.parser.SubscriptionFactory;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.services.FeedsDownloaderService;
import dh.newspaper.view.*;
import dh.newspaper.workflow.SaveSubscriptionWorkflow;
import dh.newspaper.workflow.SearchFeedsWorkflow;
import dh.newspaper.workflow.SelectArticleWorkflow;
import dh.newspaper.workflow.SelectTagWorkflow;

import javax.inject.Singleton;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by hiep on 8/05/2014.
 */
@Module (
	includes = AppContextModule.class,
	injects = {
			MyApplication.class,
			MainActivity.class,
			DetailActivity.class,
			SubscriptionActivity.class,
			TagsFragment.class,
			FeedsFragment.class,
			ArticleFragment.class,
			SubscriptionDialog.class,
			SubscriptionFactory.class,
			SelectArticleWorkflow.class,
			SelectTagWorkflow.class,
			SearchFeedsWorkflow.class,
			SaveSubscriptionWorkflow.class,
			ContentParser.class,
			BackgroundTasksManager.class,
			FeedsDownloaderService.class,
			ManageSubscriptionActivity.class,
			SettingsActivity.class
	},
	library = true
)
public class GlobalModule {

	@Provides @Singleton
	public MessageDigest provideMessageDigest() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return  null;
	}

	@Provides @Singleton
	public ContentParser provideContentParser() {
		return new ContentParser();
	}

	/*@Provides @Singleton
	public AppBundle provideAppBundle() {
		return new AppBundle();
	}*/
}
