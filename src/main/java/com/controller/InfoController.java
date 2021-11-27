package com.controller;


import com.api.TokenApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    /**
     * it's easy to understand what these mappings doing
     * maybe you should delete them
     * */
    @RequestMapping("/username")
    public String getName(){
        return TokenApi.getCurrentUsername();
    }


    @RequestMapping("/token")
    public String getToken(){
        return TokenApi.getCurrentToken();
    }
}
