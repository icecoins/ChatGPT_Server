package com.mod_api;

import com.alibaba.fastjson.JSONObject;
import com.config.MySpringConfigurator;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@ServerEndpoint(value = "/webSocket/{userId}", configurator = MySpringConfigurator.class)
public class WebSocket {
    private final String MARKED_KEY = "MARKED_KEY";
    private final String PRESET_KEY = "sk-Jc***7QyVN***GK3ZZL5***lbkFJn***1LDGU***G0Of***U";

    // 分别为 对话发送结束标记、文件发送结束标记、文件错误标记，可自行设置，与APP代码中的标记一致即可
    private final String SEND_END = "///**END_OF_SEND**///";
    private final String FILE_END = "///**END_OF_FILE**///";
    private final String FILE_ERROR = "///**FILE_ERROR**///";
    private final String GPT_3_5_URL = "https://api.openai.com/v1/chat/completions";
    private final String GPT_3_URL = "https://api.openai.com/v1/completions";

    // 调用vits模型进行语音合成的接口地址，根据你的python代码自行设置
    // 本人调用的vits接口返回值为 .mp3文件绝对路径 ，其他返回形式请自行修改代码
    private final String LOCAL_AUDIO_API_URL = "http://127.0.0.1:65432/getAudio";
    private Session session;
    private String userId;
    private static final int CALL_TYPE_NEW = 1;
    private static final int CALL_TYPE_OLD = 2;
    private static final int CALL_TYPE_SOUND = 3;
    private static final ConcurrentHashMap<String, Messenger> messengers = new ConcurrentHashMap<>();
    @OnOpen
    public void onOpen(Session session, @PathParam(value="userId")String userId) {
        try {
            this.session = session;
            this.userId = userId;
            messengers.put(userId, new Messenger(session));
            System.out.println(getDate().getString("time") + "  New: "+
                    userId + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose() {
        try {
            messengers.remove(userId);
            System.out.println(getDate().getString("time") + "  Close: " +
                    userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage(maxMessageSize =  1048576)
    public void onMessage(String message) {
        new Thread(()->{
            JSONObject jsonObject = JSONObject.parseObject(message);
            String uuid = jsonObject.getString("uuid");
            Messenger messenger = messengers.get(uuid);
            if (!(null == jsonObject.getString("type")) &&
                    jsonObject.getString("type").equals("sound")) {
                //callTtsApi(jsonObject, sessionPool.get(userId));
                messenger.sendMsg(uuid, jsonObject, "", CALL_TYPE_SOUND);
                return;
            }
            String api_key = jsonObject.getString("api_key");
            // 当传入的key为自定义的特殊标记时，用预设的key替换，以达到多个APP端共用一个key的效果
            // 我的openai账号的额度已经耗尽了，请使用自己的key
            if(api_key.equals("PicACG.2.2.3.3.4")){
                messenger.sendOneMsg("这个API KEY的可用额度已经耗尽，请使用其他KEY\n\nThe available quota for this API KEY has been exhausted. Please use another KEY.");
                return;
            }
            if(api_key.equals(MARKED_KEY)){
                api_key = PRESET_KEY;
            }
            jsonObject.remove("api_key");
            jsonObject.remove("uuid");
            if(jsonObject.getString("model").equals("gpt-3.5-turbo") ||
                    jsonObject.getString("model").equals("gpt-3.5-turbo-0301") ){
                JSONObject msg = new JSONObject();
                List<JSONObject> list = new ArrayList<>();
                msg.put("role", "user");
                msg.put("content", jsonObject.getString("prompt"));
                list.add(msg);
                jsonObject.remove("prompt");
                jsonObject.put("messages", list);
                messenger.sendMsg(uuid, jsonObject, api_key, CALL_TYPE_NEW);
                //callApiNew(jsonObject, api_key, sessionPool.get(userId));
            }else {
                messenger.sendMsg(uuid, jsonObject, api_key, CALL_TYPE_OLD);
                //callApiOld(jsonObject, api_key, sessionPool.get(userId));
            }
        }).start();
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("Error " + userId + " :\n"+error.toString());
        error.printStackTrace();
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
    
    class Messenger {
        Session session;
        int type;
        JSONObject jsonObject;
        String key;
        Messenger(Session session){
            this.session = session;
        }
        public void sendMsg(String id, JSONObject jsonObject, String key, int type){
            new Thread(()->{
                System.out.println("Run : " + id + "\ntype: " + type);
                this.jsonObject = jsonObject;
                this.key = key;
                this.type = type;
                switch (type){
                    case CALL_TYPE_NEW:
                        callApiNew(jsonObject, key);
                        break;
                    case CALL_TYPE_OLD:
                        callApiOld(jsonObject, key);
                        break;
                    case CALL_TYPE_SOUND:
                        callTtsApi(jsonObject);
                        break;
                }
            }).start();
        }
        Call getOkhttpCall(String endpoint, JSONObject jsonObject, String key){
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(Duration.ofSeconds(60))
                    .writeTimeout(Duration.ofSeconds(60))
                    .readTimeout(Duration.ofSeconds(60))
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
            Call call = getOkhttpCall(GPT_3_URL,
                    jsonObject, key);
            call.enqueue(new Callback() {
                @SneakyThrows
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
                                sendOneMessage(s);
                                res.append(s);
                            }
                            reader.close();
                            // End Message
                            System.out.println(getDate().getString("time") + "\nTo" + userId + " :\n" + res + "\n");
                        }catch (IOException e) {
                            sendOneMessage(e.getMessage());
                            e.printStackTrace();
                        }
                        sendOneMessage(SEND_END);
                    } else {
                        sendOneMessage("Error while calling OpenAI's API.\n");
                        sendOneMessage("Code: " + response.code() + "\n" + response.body().string());
                        sendOneMessage(SEND_END);
                    }
                }
                @SneakyThrows
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    sendOneMessage("Failed to call OpenAI's API. Plz try again later:\n");
                    sendOneMessage(SEND_END);
                }
            });

        }
        void callApiNew(JSONObject jsonObject, String key){
            Call call = getOkhttpCall(GPT_3_5_URL,
                    jsonObject, key);
            call.enqueue(new Callback() {
                @SneakyThrows
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
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
                                        sendOneMessage(s);
                                        res.append(s);
                                    }
                                }
                            }
                            reader.close();
                            inputStream.close();
                            // End Message
                            System.out.println(getDate().getString("time") + "\nTo" + userId + " :\n" + res + "\n");
                        }catch (IOException e) {
                            sendOneMessage(e.getMessage());
                            e.printStackTrace();
                        }
                        sendOneMessage(SEND_END);
                    } else {
                        sendOneMessage("Error while calling OpenAI's API.\n");
                        sendOneMessage("Code: " + response.code() + "\n" + response.body().string());
                        sendOneMessage(SEND_END);
                    }
                }
                @SneakyThrows
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    sendOneMessage("Failed to call OpenAI's API. Plz try again later:\n");
                    sendOneMessage(SEND_END);
                }
            });

        }
        void callTtsApi(JSONObject jsonObject){
            getOkhttpCall(LOCAL_AUDIO_API_URL, jsonObject, "").enqueue(new Callback() {
                @SneakyThrows
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    sendOneMessage("Failed to generate sound file.");
                    sendOneMessage(FILE_ERROR);
                    e.printStackTrace();
                }
                @SneakyThrows
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    if (response.isSuccessful()) {
                        InputStream inputStream = response.body().byteStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        StringBuilder path = new StringBuilder();
                        try {
                            while ((line = reader.readLine())!=null){
                                path.append(line);
                            }
                            reader.close();
                            inputStream.close();
                            sendSoundFile(path.toString());
                            // End Message
                            System.out.println(getDate().getString("time") + "\nTTS To: " +
                                    userId + " :\n" + path + "\n");
                        }catch (IOException e) {
                            sendOneMessage(e.getMessage());
                            e.printStackTrace();
                        }
                        sendOneMessage(FILE_END);
                    } else {
                        sendOneMessage(FILE_ERROR);
                        System.out.println("Error Code : " + response.code());
                    }
                }
            });
        }
        void sendOneMessage(String message) {
            if (session != null && session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        void sendSoundFile(String filePath) {
            if (session != null && session.isOpen()) {
                RemoteEndpoint.Basic endpoint = session.getBasicRemote();
                try {
                    byte[] data = new byte[1024];
                    FileInputStream inputStream = new FileInputStream(filePath);
                    while(inputStream.read(data) != -1){
                        endpoint.sendBinary(ByteBuffer.wrap(data));
                        data = new byte[1024];
                    }
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        public void sendOneMsg(String msg){
            sendOneMessage(msg);
            sendOneMessage(SEND_END);
        }
    }
}
