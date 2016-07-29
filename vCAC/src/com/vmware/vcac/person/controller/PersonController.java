package com.vmware.vcac.person.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vmware.vcac.person.model.Person;
import com.vmware.vcac.person.service.IPersonService;

@Controller
@RequestMapping("/personController")
public class PersonController {
	@Autowired
    public IPersonService personService;
    
    public IPersonService getPersonService() {
        return personService;
    }

  
    public void setPersonService(IPersonService personService) {
        this.personService = personService;
    }

    @RequestMapping("/showPerson")
    public String showPersons(Model model){
        List<Person> persons = personService.loadPersons();
        model.addAttribute("persons", persons);
        return "showPerson";
    }
}
