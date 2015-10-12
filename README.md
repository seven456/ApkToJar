# Apk2Jar
将Apk转换成Jar的工具

#说明
    1、Build android's layout drawable to jar
    2、构建Android布局、图片资源到jar包中
    3、适用于用来封装带界面的jar包SDK

#使用方法
    1、Core/build.gradle -> buildAndCopyJar // 编译资源格式工具jar包
    2、DemoLib/build.gradle -> buildJar // 编译DemoLib成jar包
    3、Demo/build.gradle -> installDebug(Shift+F10) // 编译并安装包含DemoLib.jar的Demo到手机