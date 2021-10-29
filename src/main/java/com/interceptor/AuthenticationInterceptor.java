package com.interceptor;

import com.api.tokenApi;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.annotation.PassToken;
import com.annotation.UserLoginToken;
import com.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;


public class AuthenticationInterceptor implements HandlerInterceptor {
    @Autowired
    UserService userService;
    /**
     * Fetch the token from the HTTP request header and check it
     * */
    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) {
        //Fetch the token from the HTTP request header,
        //you can set the name of token, like "Via" below
        String token = httpServletRequest.getHeader("Via");

        //If not mapped to a method, directly through
        if(!(object instanceof HandlerMethod)){
            return true;
        }
        HandlerMethod handlerMethod=(HandlerMethod)object;
        Method method=handlerMethod.getMethod();

        //Check whether there is a @PassToken comment. If yes, skip authentication
        if (method.isAnnotationPresent(PassToken.class)) {
            PassToken passToken = method.getAnnotation(PassToken.class);
            if (passToken.required()) {
                return true;
            }
        }

        //Check whether there is a @UserLoginToken comment. If yes, execute certification
        if (method.isAnnotationPresent(UserLoginToken.class)) {
            UserLoginToken userLoginToken = method.getAnnotation(UserLoginToken.class);
            if (userLoginToken.required()) {
                //execute certification
                if (token == null) {
                    throw new RuntimeException("Couldn't find token, please login again");
                }

                //Validate token
                try {
                    JWTVerifier verifier = JWT.require(
                            Algorithm.HMAC256(tokenApi.getTokenSecret())
                    ).build();
                    verifier.verify(token);
                } catch (JWTVerificationException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Verify error. Your token may have expired.");
                }
                return true;
            }
        }
        return true;
    }




    @Override
    public void postHandle(HttpServletRequest httpServletRequest,
                           HttpServletResponse httpServletResponse,
                           Object o,
                           ModelAndView modelAndView) {

    }
    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest,
                                HttpServletResponse httpServletResponse,
                                Object o,
                                Exception e) {

    }
}
