项目部署说明:
1.项目基于  Maven 进行依赖管理
2.数据库    MySql5.7(8.0)  项目文件中有数据库结构yizhi_db.sql文件,创建库后导入即可
3.缓存模块  Redis 6.x
4.JDK 1.8  SpringBoot2.0

配置文件说明:
1.项目启动 默认加载 application-dev.yml
2.MySql Redis 本地环境运行,要修改连接配置

程序访问入口:
http://localhost:8080/index
平台登录帐号: admin 密码: 111111

模块:  学生管理模块

mybatis映射文件: 完成 mybits/student/StudentInfoMapper.xml
