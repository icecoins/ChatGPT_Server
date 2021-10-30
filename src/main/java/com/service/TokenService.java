package com.service;

import com.api.tokenApi;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.entity.User;
import org.springframework.stereotype.Service;
import java.util.*;


@Service("TokenService")
public class TokenService {
    public String getToken(User user) {
        // the token expires in 0.5 minutes, u should change it
        float expMinutes = 5;
        //now
        Date iatDate = new Date(),
                //time to expire
                exp = new Date(System.currentTimeMillis() + (int)(expMinutes*1000*60));

        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("alg", "HS256");
        headerMap.put("typ", "JWT");

        return JWT.create()
                .withHeader(headerMap) // u can skip this step
                .withAudience("app")// receiver, u can skip this step
                .withIssuedAt(iatDate) // sign time
                .withExpiresAt(exp) // expire time
                .withClaim("userId", user.getId()) //put some messages inside
                .withClaim("username", user.getUsername())
                //.withClaim("password", user.getPassword())  //u can skip this step
                .sign(Algorithm.HMAC256(tokenApi.getTokenSecret())); //set the key of token;
    }
}
