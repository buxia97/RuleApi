# RuleApi

一款支持搭建多功能综合内容平台的API后端代码。
接口文档地址（不全面，正在补充）：https://docs.apipost.cn/preview/12e2d0e7ab2f8738/9c7fd18771884cb2
开源不易，维护需要心血，欢迎赞赏支持本项目。
[赞助地址](https://www.ruletree.club/sponsor.html)
## 旧版下载

当前为2.x全新版本，1.x旧版本可在下方下载：
通过网盘分享的文件：1.X版备份
链接: https://pan.baidu.com/s/1Bk0PzQFu_Xwn-1dRnyg67Q?pwd=ibvj 提取码: ibvj

## 安装教程

[RuleProject全项目安装教程](https://www.yuque.com/buxia97/ruleproject)

## 更新教程

https://www.ruletree.club/archives/2824/

## 功能明细

1.用户模块：包括登录，注册，找回密码，基于邮箱、手机短信进行验证。拥有包括头衔体系，等级体系，VIP会员体系并配合支付模块，文章模块，商品模块，实现会员的充值提现，写作投稿，商品发布。

2.内容模块：包括随机，根据字段排行，推荐机制，redis缓存机制（文章，评论，标签分类），投稿邮件提醒机制在内的基础功能集成。同时支持文章挂载商品相互结合实现付费阅读。

3.商品模块：支持四种规范的商品发布，拥有完善的卖家和买家体系，实现卖出订单，买入订单等功能。并且商品可以根据类型挂载进文章。

4.支付模块：集成了支付宝当面付，微信APP支付，卡密充值等三种支付方式，并整合会员的积分系统，实现全站的收费体系，后续更多支付方式正在开发中。

5.其它功能：签到送积分，文章收藏，文章打赏，提现和提现审核机制，简易后台管理中心。可视化的接口端配置。

## V2新增功能。

1.增加手机短信，提供阿里，短信宝，飞鸽三个渠道短信发验证码。

2.优化机器人/注册机拦截。刷帖刷注册？不存在的，直接全部封杀。

3.新增Apple登录和Apple支付，并可以上架App Store商城（目前已支持支付宝，微信，卡密，易支付，apple pay整整五个支付渠道）。

4.新增S3上传渠道，支持对接cf cdn的s2对象存储（目前支持本地，ftp，cos，oss，七牛，s3），整整五个上传渠道。

5.新增文件分片上传，文章md5校验，上传重复文件不再消耗流量，并且大文件可以分片上传。

6.新增商品分类功能。

7.内置广告位新增轮播图广告位（内置广告位无需对接任何广告联盟，也可以帮助社区赚广告费）

8.新增举报，拉黑，屏蔽功能。

9.更多功能可以自行探索。

## 演示

演示只针对APP部署后的，而不是API

### 安卓端演示

[点击此处下载演示](https://www.pgyer.com/J9bd)

### IOS端演示


苹果端因为我没有开发者证书，所以采用的是H5网页封装模式，你们也可以感受一下这种免签约打包和原生的差异。
不过本源码是完全支持IOS原生打包和上架App store的，已通过用户协助验证和测试，可以放心打包。
演示请通过苹果内置Safari浏览器访问下载，不然会出现描述文件无法安装问题。

[使用Safari浏览器访问](https://www.ruletree.club/h5/ruletree.mobileconfig)

### Windows桌面版演示

Windows桌面版本是我自己另外打包的，需要的话可以来官方群找我帮忙。

[点击此处下载演示](https://workdrive.zohopublic.com.cn/external/31cc2b750591d7be39e5e2b398e6f5c15cd837d024332bb30efb3dbd87ba12de/download)

### h5网页端演示

[访问H5演示](https://www.ruletree.club/h5)

在安装使用过程遇到的一切问题，BUG的反馈，以及演示版本的下载，都可以加QQ群讨论：776176904。

或者访问语雀教程浏览更多信息：[RuleProject社区应用帮助文档](https://www.yuque.com/buxia97/ruleproject)

## 升级至PRO版

目前开源的版本已经集成了所有的收费体系功能，完全可以进行商业化运营，但如果你仍然觉得功能太少，想进一步拓展，欢迎选择功能进一步升级的PRO版本。
[升级至PRO](https://www.yuque.com/buxia97/ruleproject/xychnh7yxu2o4ere)

## 协议及申明

[许可协议 / 免责声明](https://www.yuque.com/buxia97/ruleproject/gm1pzr6h0e1eqvvc)