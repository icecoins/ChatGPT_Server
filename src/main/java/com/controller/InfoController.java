package com.controller;


import com.api.tokenApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    /**
     * it's easy to understand what the mappings doing
     * maybe you should delete them
     * */
    @RequestMapping("/username")
    public String getName(){
        return tokenApi.getCurrentUsername();
    }


    @RequestMapping("/token")
    public String getToken(){
        return tokenApi.getCurrentToken();
    }
}
