# springboot-verify-token-raspi

原项目：https://github.com/JinBinPeng/springboot-jwt （致敬）

而本springboot项目原计划运行于树莓派4b（2g ram）

树莓派环境：宝塔面板 7.7，php 7.3, phpmyadmin 5.0, java openjdk 1.8， mysql 5.6，其余无关紧要

在idea中使用maven将代码package后上传到树莓派中，使用 java -jar xxx.jar 命令启动

当然也可以在idea中直接启动，请在配置文件中修改数据库相关信息

配套的android app：https://github.com/woyaogtmdttkp/TokenVerifier

和之前一样，是“有感而发”的代码

注意：bug非常多，很有可能一运行就报错，请不要过于惊讶

附：项目文件中resources目录下包含了sql文件，可以少手动录入六七条数据


# 注：all mappings information：
# 位于UserApi下
host/time ：  获取当前时间，yyyy-MM-dd HH:mm:ss

host/getUser/{name} ：  验证token，通过则返回message：200

host/getUser/{name}/{pwd} ：  验证用户名密码，通过则返回token和user的info

host/getProperty/{username} ：  验证token与username后，返回对应的property

host/getInfo/{username} ：  验证token与username后，返回对应的一些information

host/checkToken ： 验证token是否可用

host/checkIn/{username} ： 验证token与username后，在数据库中对该user增加一些property项，返回boolean

# 位于ErrorPage下
host/error  ： 错误页面

# 位于InfoController下
host/username ： 从token中获取username

host/token  ： 获取当前token
