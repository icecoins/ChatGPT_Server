package com.api;

import com.alibaba.fastjson.JSONObject;
import com.annotation.UserLoginToken;
import com.entity.Property;
import com.entity.User;
import com.mapper.UserMapper;
import com.service.TokenService;
import com.service.UserService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;


@RestController
@RequestMapping("api")
public class UserApi {
    @Resource
    UserMapper userMapper;
    final
    UserService userService;
    final
    TokenService tokenService;

    public UserApi(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    /**
     * return the time, it seems useless
     * */
    @RequestMapping("/time")
    public static String getDate(){
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return sdf.format(d);
    }

     /**
      * decrypt the string, it's really sample,
      * but it corresponds to the content in the app,
      * so if you want to rebuild it, you should rebuild it also in the app.
      * */
    public String decrypt(String str) {
        char[] a = str.toCharArray();
        for (int i = 0; i < a.length; i++) {
            a[i] = (char) (a[i] ^ 't');
        }
        return new String(a);
    }

    /**
     * app or anyone else who has the token
     * can get a message as 200 by the mapping
     * it is used to check whether the token
     * is still available
     *
     * u must use it with the correct username,
     * otherwise this mapping will return null
     * */
    @UserLoginToken
    @PostMapping("/getUser/{name}")
    public JSONObject tokenUser(@PathVariable String name) {
        if(Objects.equals(name, tokenApi.getCurrentUsername())){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "200");
            return jsonObject;
        }
        return null;
    }


    /**
     * login with encrypted username and password
     * this mapping will decrypt them and check the database
     * if everything is alright, it will return a JSONObject
     * with token and userInfo inside
     *
     * notice: it is mainly used to verify username and password,
     * if you want to get more user information,
     * you should use other mappings
     * */
    @PostMapping("/getUser/{name}/{pwd}")
    public JSONObject getUser(@PathVariable String name, @PathVariable String pwd) {
        User user = new User();
        user.setUsername(decrypt(name));
        user.setPassword(decrypt(pwd));
        return login(user);
    }


    /**
     * this mapping is used to debug,
     * it will receive unencrypted username and password
     * and return the token
    */
    @RequestMapping("/getUser/debug/{name}")
    public JSONObject debug(@PathVariable String name) {
        return login(userService.findByUsername(name));
    }

    /**
     * login as a user
     * */
    public JSONObject login(User user) {
        JSONObject jsonObject = new JSONObject();
        User userForBase = userService.findByUser(user);
        if (userForBase == null) {
            jsonObject.put("message", "Login failed, user does not exist");
        } else {
            if (!userForBase.getPassword().equals(user.getPassword())) {
                jsonObject.put("message", "Login failed, password error");
            } else {
                String token = tokenService.getToken(userForBase);
                jsonObject.put("token", token);
                jsonObject.put("info", userForBase.getInfo());
            }
        }
        return jsonObject;
    }


    /**
     * this mapping try to return a user's property
     * as a JSONObject
     * such as coins, level, exp
     * notice: u must require them with token and correct username
     * */
    @UserLoginToken
    @RequestMapping("/getProperty/{username}")
    public JSONObject getProperty(@PathVariable String username) {
        if(misMatchUsername(username)){
            return null;
        }

        JSONObject jsonObject = new JSONObject();
        Property property = userMapper.getPropertyByUsername(username);
        jsonObject.put("username", property.getUsername());
        jsonObject.put("coin", property.getCoin());
        jsonObject.put("level", property.getLevel());
        jsonObject.put("exp", property.getExp());
        return jsonObject;
    }

    /**
     * this mapping try to return a user's information
     * as a JSONObject
     * such as username, id , info
     * notice: u must require them with token and correct username
     * */
    @UserLoginToken
    @RequestMapping("/getInfo/{username}")
    public JSONObject getInfo(@PathVariable String username) {
        if(misMatchUsername(username)){
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        User user = userMapper.findByUsername(username);
        jsonObject.put("username", user.getUsername());
        jsonObject.put("id", user.getId());
        jsonObject.put("info", user.getInfo());
        return jsonObject;
    }



    /**
     * this mapping will tell u whether your token is available
     * */
    @UserLoginToken
    @GetMapping("/checkToken")
    public String getMessage() {
        return "You have passed the verification";
    }

    public static boolean misMatchUsername(String username){
        return !username.equals(tokenApi.getCurrentUsername());
    }

    @UserLoginToken
    @RequestMapping("/checkIn/{username}")
    public boolean DailyCheckIn(@PathVariable String username) {
        if(misMatchUsername(username)){
            return false;
        }
        Property property = userMapper.getPropertyByUsername(username);
        int checked = userMapper.setPropertyByUsername(username, property.getCoin()+100,
                                        property.getLevel()+1, property.getExp()+5);
        return checked != 0;
    }


}
