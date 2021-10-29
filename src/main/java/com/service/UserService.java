package com.service;

import com.entity.User;
import com.mapper.UserMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;


@Service("UserService")
public class UserService {

    /**
     * Some ways to get a user
     * */
    //@Autowired
    @Resource
    UserMapper userMapper;
    public User findByUser(User user){
        return userMapper.findByUsername(user.getUsername());
    }

    public User findByUsername(String username){
        return userMapper.findByUsername(username);
    }

    public User findUserById(String userId) {
        return userMapper.findUserById(userId);
    }

}
