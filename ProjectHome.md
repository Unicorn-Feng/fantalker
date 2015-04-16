## 介绍 ##

FanTalker是部署在Google App Engine云计算平台上的基于xmpp的Google Talk饭否机器人，使用JAVA开发。支持饭否的发送消息、查看时间线、回复消息等功能，更多功能即将上线。命令参考自[TwitalkerPlus](http://code.google.com/p/twitalkerplus/)和[fanfoutalk](http://code.google.com/p/fanfoutalk/)。


## 使用方法 ##

在Google Talk客户端中添加 fantalker@appspot.com为好友，而后使用-oauth命令绑定账号。更多命令请输入-help获取帮助。


## 命令列表 ##

所有命令均以-开头。若无-，则视为消息，fantalker机器人会自动将消息发送至绑定的饭否账号。

  * -?/-h/-help： 显示本帮助
  * -ho/-home： 查看首页时间线
  * -@/-r-/-reply：查看提到我的消息及 回复消息
  * -tl/-timeline：查看指定用户已发送的消息
  * -fav/-favorite： 查看已收藏的消息
  * -rt：转发消息
  * -m/-msg： 查看指定消息及其上下文
  * -s/-search: 搜索指定消息
  * -u： 查看用户信息
  * -fo/-follow: 将指定用户加为好友
  * -unfo/-unfollow: 删除指定好友
  * -del/-delete: 删除指定消息
  * -on: 开启定时提醒新的@提到我的消息
  * -off: 关闭定时提醒新的@提到我的消息
  * -oauth： 开始OAuth认证或进行XAuth认证
  * -bind： 绑定PIN码完成认证
  * -remove： 解除关联
  * -help COMMAND: 显示COMMAND的具体帮助信息。


## 已实现功能 ##

  * 通过OAuth方式绑定饭否账号
  * 通过XAuth方式绑定饭否账号
  * 发送消息
  * 查看时间线
  * 随便看看
  * 查看@提到我的消息
  * 查看已发送的消息
  * 查看已收藏的消息
  * 回复消息
  * 转发消息
  * 查看用户信息
  * 查看指定消息及其上下文
  * 删除指定消息
  * 添加与删除好友
  * 定时提醒新的@提到我的消息
  * 设置是否开启定时提醒新的@提到我的消息
  * 短ID
  * 搜索功能


## 计划中的功能 ##

  1. 设置定时提醒时间间隔
  1. 收藏消息
  1. 查看关注者及好友
  1. 发送、查看、删除私信
  1. 加入、删除黑名单


## 其他说明 ##

本程序不保存用户的密码信息，仅保存OAuth或XAuth后返回的Access Token及Access Token Secret。
本程序依照GPLv3协议开放源代码。

若有任何问题您可以在饭否 [@烽麒](http://fanfou.com/weili27)，也可以发送邮件或GT联系 [fengqiwl@gmail.com](mailto:fengqiwl@gmail.com) ，谢谢。