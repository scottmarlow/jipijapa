package org.jipijapa.eclipselink;

import java.util.Properties;

import org.jipijapa.cache.spi.Classification;
import org.jipijapa.event.impl.internal.Notification;

/**
 * SecondLevelCache
 *
 * @author Scott Marlow
 */
public class SecondLevelCache {

    public static void addSecondLevelCacheDependencies(Properties mutableProperties, String scopedPersistenceUnitName) {
        Notification.addCacheDependencies(Classification.SHAREDCLUSTER, mutableProperties);
    }

}
