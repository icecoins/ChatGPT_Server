# ChatGPT_Server
使用springboot+websocket与APP进行即时通讯，中转 https://api.openai.com 的流量

配套APP项目地址： https://github.com/icecoins/ChatGPT_Android

自定义运行端口、运行路径，建议自行修改代码，或自行搜索方法

启动流程：

maven打包springboot项目，上传jar到服务器

设置Nginx等前台进行反向代理

    location /webSocket  #对应springboot websocket监听路径 
    {
        proxy_pass http://127.0.0.1:8848; # 对应springboot运行端口
        proxy_redirect off;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $http_host;
    }

安装java

    sudo apt install default-jre
    
在jar存放目录下，单次启动

    java -jar test-1.0-SNAPSHOT.jar
    
编写脚本spring.sh后台启动，内填

    nohup java -jar test-1.0-SNAPSHOT.jar >> log/spring.out  &
    
在jar存放目录下，运行脚本

    sudo bash spring.sh
   

# 关于语音转换：
引用自 https://github.com/SayaSS/vits-finetuning
