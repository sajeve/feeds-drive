package dh.newspaper.services;

import android.app.Application;
import com.octo.android.robospice.SpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.persistence.exception.CacheCreationException;

/**
 * Created by hiep on 30/05/2014.
 */
public class MySpiceService extends SpiceService {
	@Override
	public CacheManager createCacheManager(Application application) throws CacheCreationException {
		return null;
	}
}
