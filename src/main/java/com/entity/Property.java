package com.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * the user's property
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Property {
    String username;
    int coin;
    int level;
    int exp;
}
