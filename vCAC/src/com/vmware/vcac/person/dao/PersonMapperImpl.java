package com.vmware.vcac.person.dao;

import java.util.ArrayList;
import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.opensaml.xml.encryption.Public;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.vmware.vcac.person.model.Person;

@Repository("personMapper123")
public class PersonMapperImpl implements PersonMapper {
	@Autowired
	SqlSessionTemplate ssTemplate;
	@Override
	public List<Person> queryAll() {
		// TODO Auto-generated method stub
		List<Person> list =ssTemplate.selectList("Person.queryAll");
//		Person person = new Person();
//		person.setAge(18);
//		person.setName("242423");
//		List<Person> list = new ArrayList<Person>();
//		list.add(person);
		return list;
	}

}
