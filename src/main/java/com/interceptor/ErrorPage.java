package com.interceptor;
import com.alibaba.fastjson.JSONObject;
import com.mod_api.WebSocket;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class ErrorPage implements org.springframework.boot.web.servlet.error.ErrorController {
    @RequestMapping("/error")
    public JSONObject error(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Date", WebSocket.getDate());
        jsonObject.put("Message","本站点为应用后端接口站点，请使用应用进行接口请求。");
        return jsonObject;
    }
}
