package org.jipijapa.test.eclipselink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.eclipse.persistence.exceptions.CommunicationException;
import org.eclipse.persistence.internal.sessions.coordination.RemoteConnection;
import org.eclipse.persistence.sessions.coordination.Command;
import org.eclipse.persistence.sessions.coordination.ServiceId;
import org.eclipse.persistence.sessions.coordination.TransportManager;
import org.junit.Test;

/**
 * ClusterInvalidationHookTest
 *
 * @author Scott Marlow
 */
public class ClusterInvalidationHookTest extends AbstractTestCase {

    @Test
    public void secondLevelCacheInvalidation() {
        /**
         * create two entity managers from separate entity manager factories (each EMF should have its own 2lc)
         */
        EntityManager entityManager1= createEntityManager("TransportManagerPU");
        EntityManager entityManager2= createEntityManager("TransportManagerPU");
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
        employee.setId(101);
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


public static class TransportManagerTestImpl extends TransportManager {

    public TransportManagerTestImpl() {
        initialize();
    }

    @Override
    public RemoteConnection createConnection(ServiceId serviceId) {
        System.out.println("TransportManagerTestImpl.createConnection " + serviceId);
        return new RemoteConnection() {

            @Override
            public Object executeCommand(Command command) throws CommunicationException {
                System.out.println("TransportManagerTestImpl$RemoteConnection.executeCommand " + command );
                return null;
            }

            @Override
            public Object executeCommand(byte[] command) throws CommunicationException {
                System.out.println("TransportManagerTestImpl$RemoteConnection.executeCommand " + command );
                return null;
            }
        };
    }

    @Override
    public void createLocalConnection() {
        // create local connection and pass it RemoteCommandManager rcm, save local connection in localConnection
        //         localConnection = new CORBARemoteCommandConnection((CORBAConnection)connectionImpl);
        //        localConnection.setServiceId(rcm.getServiceId());

        System.out.println("TransportManagerTestImpl.createLocalConnection ");
    }

    @Override
    public void removeLocalConnection() {
        System.out.println("TransportManagerTestImpl.removeLocalConnection ");

    }
}
}
