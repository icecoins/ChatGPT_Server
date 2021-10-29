package com.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class errorController implements ErrorController {
    /**
     * redirect error pages
     * */
    @RequestMapping("/error")
    public String error(){
        return "It seems we meet some errors.<br>" +
                "Please contact with the web administrator.";
    }
}
