package com.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * the user's basic information
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User{
    String Id;
    String username;
    String password;
    String Info;
}
