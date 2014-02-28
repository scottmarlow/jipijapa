package org.jipijapa.test.openjpa;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Employee
 *
 * @author Scott Marlow
 */
@Entity
public class Employee {

    @Id
    @GeneratedValue(generator="uuid-hex")
    String id;

    String name;
    String address;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
