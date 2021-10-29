package com.mapper;

import com.entity.Property;
import com.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    /**
     * You can understand it at a glance
     * */
    User findByUsername(@Param("username") String username);
    User findUserById(@Param("Id") String Id);
    Property getPropertyByUsername(@Param("username") String username);
    Property getPropertyById(@Param("Id") String Id);
}