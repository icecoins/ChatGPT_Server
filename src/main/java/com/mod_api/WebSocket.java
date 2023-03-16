package com.mod_api;

import com.alibaba.fastjson.JSONObject;
import com.config.MySpringConfigurator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Slf4j
@ServerEndpoint(value = "/webSocket/{userId}", configurator = MySpringConfigurator.class)
public class WebSocket {
    private String marked_key = "marked_key"
            ,preset_api_key = "preset_api_key";
    private Session session;
    private String userId;
    private static CopyOnWriteArraySet<WebSocket> webSockets =new CopyOnWriteArraySet<>();
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

    @OnMessage
    public void onMessage(String message) {
        new Thread(()->{
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
                JSONObject msg = new JSONObject();
                List<JSONObject>list = new ArrayList<>();
                msg.put("role", "user");
                msg.put("content", jsonObject.getString("prompt"));
                list.add(msg);
                jsonObject.remove("prompt");
                jsonObject.put("messages", list);
                callApiNew(jsonObject, api_key);
            }else {
                callApiOld(jsonObject, api_key);
            }
        }).start();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("Error " + userId + " :\n"+error.toString());
        error.printStackTrace();
    }

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

    Call getOkhttpCall(String endpoint, JSONObject jsonObject, String key){
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .writeTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .build();
        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + key)
                .post(RequestBody.create(jsonObject.toString(),
                        MediaType.parse("application/json")))
                .build();
        return client.newCall(request);
    }

    void callApiOld(JSONObject jsonObject, String key){
        Call call = getOkhttpCall("https://api.openai.com/v1/completions",
                jsonObject, key);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    InputStream inputStream = response.body().byteStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    StringBuilder res = new StringBuilder();
                    try {
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
                            res.append(s);
                        }
                        reader.close();
                        // End Message
                        System.out.println("To" + userId + " :\n" + res + "\n");
                    }catch (IOException e) {
                        sendOneMessage(userId, e.getMessage());
                        e.printStackTrace();
                    }
                    sendOneMessage(userId, "///**1100101**///");
                } else {
                    sendOneMessage(userId, "Error while calling OpenAI's API.\n");
                    sendOneMessage(userId, response.message());
                    sendOneMessage(userId, response.code() + response.body().string());
                    sendOneMessage(userId, "///**1100101**///");
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                sendOneMessage(userId, "Failed to call OpenAI's API. MSG:\n");
                sendOneMessage(userId, e.getMessage());
                sendOneMessage(userId, "///**1100101**///");
            }
        });

    }
    void callApiNew(JSONObject jsonObject, String key){
        Call call = getOkhttpCall("https://api.openai.com/v1/chat/completions",
                jsonObject, key);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    InputStream inputStream = response.body().byteStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    StringBuilder res = new StringBuilder();
                    try {
                        while ((line = reader.readLine())!=null){
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
                                    res.append(s);
                                }
                            }
                        }
                        reader.close();
                        inputStream.close();
                        // End Message
                        System.out.println("To" + userId + " :\n" + res + "\n");
                    }catch (IOException e) {
                        sendOneMessage(userId, e.getMessage());
                        e.printStackTrace();
                    }
                    sendOneMessage(userId, "///**1100101**///");
                } else {
                    sendOneMessage(userId, "Error while calling OpenAI's API.\n");
                    sendOneMessage(userId, response.message());
                    sendOneMessage(userId, "Code: " + response.code() + "\n" + response.body().string());
                    sendOneMessage(userId, "///**1100101**///");
                }
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                sendOneMessage(userId, "Failed to call OpenAI's API. Plz try again later:\n");
                sendOneMessage(userId, "///**1100101**///");
            }
        });

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

}
