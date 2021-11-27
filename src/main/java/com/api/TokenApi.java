package com.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;


public class TokenApi {
    /**
     * return current token
     * */
    public static String getCurrentToken() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                .getRequest();
        return request.getHeader("Via");
    }

    /**
     * return current username
     * */
    public static String getCurrentUsername(){
        String token = getCurrentToken();
        Map<String, Claim> claims = verifyToken(token);
        Claim username = claims.get("username");
        if (null == username || username.asString().isEmpty()) {
            // there's no username in the token
            return null;
        }
        return username.asString();
    }

    /**
     * the key of token, u should change it
     * */
    public static String getTokenSecret(){
        return "Th-is-Is_Th-eT_oke_nS=tri_n+g";
    }

    /**
     * verify token and return things inside as a Map<String, Claim>
     * */
    public static Map<String, Claim> verifyToken(String token) {
        DecodedJWT jwt;
        try {
            JWTVerifier verifier = JWT.require(
                    Algorithm.HMAC256(getTokenSecret())).build();
            jwt = verifier.verify(token);
        } catch (JWTVerificationException e) {
            e.printStackTrace();
            throw new RuntimeException("Verify error. Your token may have expired.");
        }
        return jwt.getClaims();
    }
}
