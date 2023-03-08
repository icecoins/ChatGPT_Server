package com.mod_api;

import com.alibaba.fastjson.JSONObject;
import com.config.MySpringConfigurator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.net.ssl.HttpsURLConnection;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
@Component
@Slf4j
@ServerEndpoint(value = "/webSocket/{userId}", configurator = MySpringConfigurator.class)
public class WebSocketApi {
    private String marked_key = "whatever"
            ,preset_api_key = "the api key you want to preset";
    private Session session;
    private String userId;
    private static CopyOnWriteArraySet<WebSocketApi> webSockets =new CopyOnWriteArraySet<>();
    private static ConcurrentHashMap<String,Session> sessionPool = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam(value="userId")String userId) {
        try {
            this.session = session;
            this.userId = userId;
            webSockets.add(this);
            sessionPool.put(userId, session);
            System.out.println("New: "+ userId + "\nCount:"+webSockets.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose() {
        try {
            webSockets.remove(this);
            sessionPool.remove(this.userId);
            System.out.println("Close: " + userId + "\nCount:"+webSockets.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    HttpsURLConnection getConnection(String url, String api_key) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + api_key);
        connection.connect();
        return connection;
    }
    void newModel(JSONObject jsonObject, String api_key) {
        try{
            JSONObject msg = new JSONObject();
            List<JSONObject>list = new ArrayList<>();
            msg.put("role", "user");
            msg.put("content", jsonObject.getString("prompt"));
            list.add(msg);
            jsonObject.remove("prompt");
            jsonObject.put("messages", list);
            HttpsURLConnection connection = getConnection("https://api.openai.com/v1/chat/completions", api_key);

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
            outputStreamWriter.write(jsonObject.toString());
            outputStreamWriter.flush();
            outputStreamWriter.close();
            System.out.println(connection.getResponseMessage());
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = "", res = "";
            while ((line = reader.readLine())!=null){
                System.out.println(line);
                if(line.length()<40){
                    continue;
                }
                JSONObject object = JSONObject.parseObject(line.substring(6));
                JSONObject choices = JSONObject.parseObject(object.getString("choices")
                        .replace('[',' ')
                        .replace(']',' '));
                JSONObject content;
                if(jsonObject.getBoolean("stream")){
                    content = JSONObject.parseObject(choices.getString("delta"));
                }else{
                    content = JSONObject.parseObject(choices.getString("message"));
                }
                if(!(null == content)){
                    String s = content.getString("content");
                    if(!(null == s)){
                        sendOneMessage(userId, s);
                        res += s;
                    }
                }
            }
            reader.close();
            // End Message
            sendOneMessage(userId, "///**1100101**///");
            System.out.println("To " + userId + " :\n" + res + "\n");
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    void ordinaryModel(JSONObject jsonObject, String api_key) throws IOException {
        HttpsURLConnection connection = getConnection("https://api.openai.com/v1/completions", api_key);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
        outputStreamWriter.write(jsonObject.toString());
        outputStreamWriter.flush();
        outputStreamWriter.close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line = "", res = "";
        System.out.println(reader.lines().toString());
        while ((line = reader.readLine())!=null){
            if(line.length()<50){
                continue;
            }
            JSONObject object = JSONObject.parseObject(line.substring(6));
            JSONObject text = JSONObject.parseObject(object.getString("choices")
                    .replace('[',' ')
                    .replace(']',' '));
            String s = text.getString("text");
            sendOneMessage(userId, s);
            res += s;
        }
        reader.close();
        // End Message
        sendOneMessage(userId, "///**1100101**///");
        System.out.println("To" + userId + " :\n" + res + "\n");
    }

    @OnMessage
    public void onMessage(String message) {
        new Thread(()->{
            try {
                JSONObject jsonObject = JSONObject.parseObject(message);
                String api_key = jsonObject.getString("api_key");
                System.out.println("user: " + userId + "\nKEY :" + api_key + "\nmsg: " + jsonObject);
                // 当传入的key为自定义的特殊标记时，用预设的key替换，以达到多个APP端共用一个key的效果
                if(api_key.equals(marked_key)){
                    api_key = preset_api_key;
                }
                jsonObject.remove("api_key");
                if(jsonObject.getString("model").equals("gpt-3.5-turbo") ||
                        jsonObject.getString("model").equals("gpt-3.5-turbo-0301") ){
                    newModel(jsonObject, api_key);
                }else {
                    ordinaryModel(jsonObject, api_key);
                }
            }catch (IOException e){
                sendOneMessage(userId, "Server Error, please try again later\n");
                sendOneMessage(userId, "///**1100101**///");
                e.printStackTrace();
            }
        }).start();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("Error " + userId + " :\n"+error.getMessage());
        error.printStackTrace();
    }

/*    public void sendAllMessage(String message) {
        System.out.println("BroadCast: "+message);
        for(WebSocketApi webSocket : webSockets) {
            try {
                if(webSocket.session.isOpen()) {
                    webSocket.session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }*/

    public void sendOneMessage(String userId, String message) {
        Session session = sessionPool.get(userId);
        if (session != null && session.isOpen()) {
            synchronized (session){
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

/*    public void sendMoreMessage(String[] userIds, String message) {
        for(String userId:userIds) {
            Session session = sessionPool.get(userId);
            if (session != null&&session.isOpen()) {
                synchronized (session){
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }*/

    @RequestMapping("/time")
    public static JSONObject getDate() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        Date d = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("time", sdf.format(d));
        return jsonObject;
    }
}
