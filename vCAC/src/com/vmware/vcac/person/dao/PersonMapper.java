package com.vmware.vcac.person.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import com.vmware.vcac.person.model.Person;


public interface PersonMapper {

	
	 /**
     * 插入一条记录
     * @param person
     */
    //void insert(Person person);
    
    /**
     * 查询所有
     * @return
     */
    List<Person> queryAll();
}
