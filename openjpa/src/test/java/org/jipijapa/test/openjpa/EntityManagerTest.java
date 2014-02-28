package org.jipijapa.test.openjpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Ignore;
import org.junit.Test;

/**
 * EntityManagerTest
 *
 * @author Scott Marlow
 */
public class EntityManagerTest extends AbstractTestCase {
    @Ignore
    @Test
    public void createSimpleDatabaseTransaction() {
        EntityManager entityManager= createEntityManager("testpu");
        EntityTransaction entityTransaction = entityManager.getTransaction();
        assertNotNull(entityTransaction);

        entityTransaction.begin();
        entityTransaction.commit();

    }

    @Ignore
    @Test
    public void createEntity() {
        EntityManager entityManager= createEntityManager("testpu");
        EntityTransaction entityTransaction = entityManager.getTransaction();
        assertNotNull(entityTransaction);

        /**
         * persist new Employee
         */
        entityTransaction.begin();
        Employee employee = new Employee();
        employee.setName("Sharon");
        employee.setAddress("101 Main Street, here, here");
        // employee.setId(101);
        entityManager.persist(employee);
        entityTransaction.commit();

        assertNotNull(entityManager.find(Employee.class, 101));

        assertTrue(entityManager.getEntityManagerFactory().getCache().contains(Employee.class, 101));

        /**
         * clean up database
         */
        entityTransaction.begin();
        entityManager.remove(entityManager.find(Employee.class, 101));
        entityTransaction.commit();

        entityManager.close();
    }

    @Ignore
    @Test
    public void secondLevelCacheInvalidation() {
        /**
         * create two entity managers from separate entity manager factories (each EMF should have its own 2lc)
         */
        EntityManager entityManager1= createEntityManager("testpu");
        EntityManager entityManager2= createEntityManager("testpu");
        EntityTransaction entityTransaction1 = entityManager1.getTransaction();
        EntityTransaction entityTransaction2 = entityManager2.getTransaction();
        assertNotNull(entityTransaction1);
        assertNotNull(entityTransaction2);

        /**
         * persist new Employee
         */
        entityTransaction1.begin();
        Employee employee = new Employee();
        employee.setName("Sharon");
        employee.setAddress("101 Main Street, here, here");
//         employee.setId(101);
        entityManager1.persist(employee);
        entityTransaction1.commit();

        // verify that new employee is found in database
        assertNotNull(entityManager1.find(Employee.class, 101));

        // verify that different cache objects are returned
        assertNotEquals(entityManager1.getEntityManagerFactory().getCache(),entityManager2.getEntityManagerFactory().getCache());

        // check if the same entity manager factory returns the same cache twice.  this is exploratory only and impacts the previous test
        // as if the same emf returns a different cache every time, the previous test can be removed.
        assertEquals(entityManager1.getEntityManagerFactory().getCache(), entityManager1.getEntityManagerFactory().getCache());

        // verify that new employee is in both caches
        assertTrue(entityManager1.getEntityManagerFactory().getCache().contains(Employee.class, 101));
        assertTrue(entityManager2.getEntityManagerFactory().getCache().contains(Employee.class, 101));

        /**
         * clean up database
         */
        entityTransaction1.begin();
        entityManager1.remove(entityManager1.find(Employee.class, 101));
        entityTransaction1.commit();

        /**
         * verify that deleted object was removed from both 2lc's
         */
        assertFalse(entityManager1.getEntityManagerFactory().getCache().contains(Employee.class, 101));
        assertFalse(entityManager2.getEntityManagerFactory().getCache().contains(Employee.class, 101));

        entityManager1.close();
        entityManager2.close();

    }

}
