package com.vmware.vcac.person.service;

import java.util.List;

import com.vmware.vcac.person.model.Person;

public interface IPersonService {

    /**
     * 加载全部的person
     * @return
     */
    List<Person> loadPersons();
}
