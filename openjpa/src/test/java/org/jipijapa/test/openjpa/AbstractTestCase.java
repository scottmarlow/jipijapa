package org.jipijapa.test.openjpa;

import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * AbstractTestCase
 *
 * @author Scott Marlow
 */
public class AbstractTestCase {

    public EntityManagerFactory createEntityManagerFactory(String unitName) {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(unitName);
        assertNotNull(entityManagerFactory);
        return entityManagerFactory;
    }

    public EntityManager createEntityManager(String unitName) {
        return createEntityManagerFactory(unitName).createEntityManager();

    }

}
