package com.pratham.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.pratham.smart.dao.ContactRepository;
import com.pratham.smart.dao.UserRepository;
import com.pratham.smart.entities.Contact;
import com.pratham.smart.entities.User;
import com.pratham.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContactRepository contactRepository;

    @ModelAttribute
    public void addCommonData(Model model, Principal principal) {
        String userName = principal.getName();
        System.out.println("USERNAME" + userName);

        User user = userRepository.getUserByUserName(userName);
        System.out.println("USER" + user);

        model.addAttribute("user", user);
    }

    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal) {
        return "normal/user_dashboard";
    }

    @GetMapping("/add-contact")
    public String openAddContactForm(Model model) {
        model.addAttribute("title", "Add Contact");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    @RequestMapping("/process-contact")
    public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
            Principal principal, HttpSession session) {
        try {
            String name = principal.getName();
            User user = this.userRepository.getUserByUserName(name);

            if (file.isEmpty()) {
               System.out.println("image is empty");
               contact.setImage("contact.png");
            }else {
                contact.setImage(file.getOriginalFilename());
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }

            contact.setUser(user);
            user.getContacts().add(contact);
            this.userRepository.save(user);

            System.out.println("Data: " + contact);
            System.out.println("Data added to the device");

            session.setAttribute("message", new Message("Your contact is added successfully", "success"));
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            session.setAttribute("message", new Message("Something went wrong", "danger"));
        }

        return "normal/add_contact_form";
    }
    @GetMapping("/show_contacts/{page}")
    public String showContacts(@PathVariable("page") Integer page,Model m,Principal principal) {
    	m.addAttribute("title", "Show User Contacts");
    	
    	String userName = principal.getName();
    	User user = this.userRepository.getUserByUserName(userName);
    	
    	Pageable pageable=PageRequest.of(page,5);
    	
    	Page<Contact> contacts=this.contactRepository.findContactsByUser(user.getId(),pageable);
    	
        m.addAttribute("contacts",contacts);
        m.addAttribute("currentPage",page);
        m.addAttribute("totalPages",contacts.getTotalPages());
    	
    	return "normal/show_contacts";
    }
    @RequestMapping("/{cId}/contact")
    public String showContactDetail(@PathVariable("cId")Integer cId,Model model,Principal principal) {
    	System.out.println("cId"+cId);
    	String userName = principal.getName();
    	User user =this.userRepository.getUserByUserName(userName);
    	Optional<Contact> contactOptional = this.contactRepository.findById(cId);
    	Contact contact = contactOptional.get();
    	if(user.getId()==contact.getUser().getId()) {
    		model.addAttribute("contact",contact);
    	}
    	return "normal/contact_detail";
    }
    @GetMapping("/delete/{cId}")
    public String deleteContact(@PathVariable("cId")Integer cId,Principal principal,HttpSession session) {
    	Optional<Contact> contactOptional = this.contactRepository.findById(cId);
    	Contact contact = this.contactRepository.findById(cId).get();
    	String userName = principal.getName();
    	User user =this.userRepository.getUserByUserName(userName);
    	
    	if(user.getId()==contact.getUser().getId()) {
    		user.getContacts().remove(contact);
    		this.userRepository.save(user);
    		this.contactRepository.delete(contact);
    		session.setAttribute("message", new Message("Contact deleted successfully....","success"));
    	}
    	return "redirect:/user/show_contacts/0";
    }
    @PostMapping("/show_contacts/user/update_contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid,Model m) {
    	Contact contact = this.contactRepository.findById(cid).get();
    	m.addAttribute("contact",contact);
    	m.addAttribute("title","Update Contact");
    	
    	return "normal/update_form";
    }
    @RequestMapping(value="/process-update",method= RequestMethod.POST)
    public String updateHandler(@ModelAttribute Contact contact,@RequestParam("profileImage")MultipartFile file,
    		Model m,HttpSession session,Principal principal) {
    	try {
    		Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();
    		if(!file.isEmpty()) {
    			
    			File deleteFile = new ClassPathResource("static/img").getFile();
    			File file1= new File(deleteFile,oldContactDetail.getImage());
    			file1.delete();
    			
    			 File saveFile = new ClassPathResource("static/img").getFile();
                 Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                 Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
    			 contact.setImage(file.getOriginalFilename());
    		}else {
    			contact.setImage(oldContactDetail.getImage());
    		}
    		User user = this.userRepository.getUserByUserName(principal.getName());
    		contact.setUser(user);
    		this.contactRepository.save(contact);
    		session.setAttribute("message", new Message("Your contact is updated","success"));
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	System.out.println("Contact name" + contact.getName());
    	System.out.println("Contact Id" + contact.getcId());
    	return "redirect:/user/"+contact.getcId()+"/contact";
    }
    @GetMapping("/profile")
    public String yourProfile(Model model) {
    	model.addAttribute("title","Your Profile");
    	return "normal/profile";
    	
    }
}

