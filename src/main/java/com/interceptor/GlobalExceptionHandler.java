package com.interceptor;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
@ControllerAdvice
public class GlobalExceptionHandler {
    @ResponseBody
    @ExceptionHandler(Exception.class)
    public JSONObject handleException(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.equals("")) {
            msg = "遇到了意料之外的错误，请联系管理员解决问题";
        }
        JSONObject jsonObject = new JSONObject();
        e.printStackTrace();
        jsonObject.put("error", e.toString());
        jsonObject.put("message", msg);
        return jsonObject;
    }
}
