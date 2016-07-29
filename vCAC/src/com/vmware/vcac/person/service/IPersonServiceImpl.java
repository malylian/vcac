package com.vmware.vcac.person.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.vmware.vcac.person.dao.PersonMapper;
import com.vmware.vcac.person.model.Person;



	@Service("personService")
	public class IPersonServiceImpl implements IPersonService {
		 @Autowired
	    private PersonMapper personMapper123;

	    public PersonMapper getPersonMapper() {
	        return personMapper123;
	    }
	   
	    public void setPersonMapper(PersonMapper personMapper) {
	        this.personMapper123 = personMapper;
	    }

	    @Override
	    public List<Person> loadPersons() {
	        return personMapper123.queryAll();
	    }

}
