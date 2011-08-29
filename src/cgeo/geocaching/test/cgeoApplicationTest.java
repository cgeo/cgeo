package cgeo.geocaching.test;

import java.util.HashMap;

import junit.framework.Assert;
import android.content.Context;
import android.content.SharedPreferences;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import cgeo.geocaching.ICache;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.test.mock.GC2CJPF;

/**
 * The c:geo application test.
 * It can be used for tests that require an application and/or context.
 */

public class cgeoApplicationTest extends ApplicationTestCase<cgeoapplication> {
	
	private cgSettings settings = null;
	private cgBase base = null; 

    public cgeoApplicationTest() {
        super(cgeoapplication.class);
      }

      @Override
      protected void setUp() throws Exception {
          super.setUp();
          
       	  // init environment
    	  createApplication();
    	  Context context = this.getContext();
    	  SharedPreferences prefs = context.getSharedPreferences(cgSettings.preferences, Context.MODE_PRIVATE);
    	  
    	  // create required c:geo objects
    	  settings = new cgSettings(context, prefs);
    	  base = new cgBase(this.getApplication(), settings, prefs);
      }

      /**
       * The name 'test preconditions' is a convention to signal that if this
       * test doesn't pass, the test case was not set up properly and it might
       * explain any and all failures in other tests.  This is not guaranteed
       * to run before other tests, as junit uses reflection to find the tests.
       */
      @SmallTest
      public void testPreconditions() {
      }

      /**
       * Test {@link cgBase#searchByGeocode(HashMap, int, boolean)}
       * @param base
       */
      @MediumTest
      public void testSearchByGeocode() {
    	  HashMap<String, String> params = new HashMap<String, String>();
    	  params.put("geocode", "GC1RMM2");
    	  
    	  Long id = base.searchByGeocode(params, 0, true);
    	  Assert.assertNotNull(id);
      }
      
      /**
       * Test {@link cgBase#parseCache(String, int) with "mocked" data
       * @param base
       */
      @MediumTest
      public void testParseCache() {
    	  ICache cache = new GC2CJPF();
    	  cgCacheWrap caches = base.parseCache(cache.getData(),0);
    	  cgCache cacheParsed = caches.cacheList.get(0);
    	  Assert.assertEquals(cacheParsed.geocode, cache.getGeocode());
    	  Assert.assertEquals(cacheParsed.type, cache.getType());
    	  Assert.assertEquals(cacheParsed.owner, cache.getOwner());
    	  Assert.assertEquals(cacheParsed.difficulty, cache.getDifficulty());
    	  Assert.assertEquals(cacheParsed.terrain, cache.getTerrain());
    	  Assert.assertEquals(cacheParsed.latitudeString, cache.getLatitute());
    	  Assert.assertEquals(cacheParsed.longitudeString, cache.getLongitude());
    	  Assert.assertEquals(cacheParsed.disabled, cache.isDisabled());
    	  Assert.assertEquals(cacheParsed.own, cache.isOwn());
    	  Assert.assertEquals(cacheParsed.archived, cache.isArchived());
    	  Assert.assertEquals(cacheParsed.members, cache.isMembersOnly());
      }

}