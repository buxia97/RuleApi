(window.webpackJsonp=window.webpackJsonp||[]).push([[2],{437:function(t,e,o){},438:function(t,e,o){"use strict";o(437)},469:function(t,e,o){"use strict";o.r(e);o(35),o(75);var n={layout:"no",head:function(){return{title:"RuleApi",titleTemplate:"%s - 开启你的自由社区"}},data:function(){return{version:16,versionText:"V1.3.6",key:"",goSystem:!1,newVersion:"",newVersionCode:0,installStatus:0,goInstall:!1,installData:"",isTypecho:!1,goInstallTypecho:!1,name:"",password:"",isPro:0}},beforeDestroy:function(){},created:function(){this.isPro=this.$api.isPro()},mounted:function(){this.isUpdate(),this.isInstall()},methods:{isUpdate:function(){var t=this;t.$axios.$get(t.$api.apiNewVersion(),{progress:!1}).then((function(e){1==e.code&&(t.newVersion=e.data.ruleapiVersion,t.newVersionCode=e.data.ruleapiVersionCode)})).catch((function(e){console.log(e),t.$message({message:"网络异常！",type:"error"})}))},update:function(){var t=this;t.newVersionCode>t.version?t.$confirm("更新后将获得更高的安全性、更多的功能和更好的体验！","有新的版本推送！",{confirmButtonText:"前往升级",cancelButtonText:"我再想想",type:"success"}).then((function(){window.open()})).catch((function(){})):t.$message({message:"当前为最新版本！",type:"success"})},goHelp:function(){this.$message({message:"暂未开放！",type:"warning"})},isInstall:function(){var t=this;t.$axios.$get(t.$api.isInstall()).then((function(e){e.code,100==e.code&&t.$alert("Redis连接失败或未安装！请检查您的API配置或Redis安装状态。","API警告",{confirmButtonText:"我知道了",type:"warning"}),101==e.code&&(t.isTypecho=!0),102==e.code&&t.$alert("Mysql数据库连接失败或未安装，请检查您的API配置或Mysql安装状态。","API警告",{confirmButtonText:"我知道了",type:"warning"})})).catch((function(e){console.log(e),t.$message({message:"接口请求异常，请检查数据库连接或设备网络！",type:"error"})}))},goTypecho:function(){var t=this;t.installData="",t.installStatus=0,t.name="",t.password="",t.goInstallTypecho=!0},installTypecho:function(){var t=this;t.key;""!=t.name&&""!=t.password||t.$message({message:"请输入正确的参数",type:"error"});var data={webkey:t.key,name:t.name,password:t.password};t.installStatus=1,t.$axios.$post(t.$api.typechoInstall(),this.qs.stringify(data),{progress:!1}).then((function(e){t.installStatus=2,t.installData=e.msg})).catch((function(e){t.installStatus=2,t.installData="网络错误，请重试！"}))},Install:function(){this.installStatus=0,this.goInstall=!0},goInstallStart:function(){var t=this,data=(t.key,{webkey:t.key});t.installStatus=1,t.$axios.$post(t.$api.newInstall(),this.qs.stringify(data),{progress:!1}).then((function(e){t.installStatus=2,t.installData=e,1==t.isPro&&setTimeout((function(){t.installData="正在完成Pro版部分更新，请稍等...",t.goInstallPro()}),500)})).catch((function(e){t.installStatus=2,t.installData="常规更新异常，请重试！"}))},goInstallPro:function(){var t=this,data=(t.key,{webkey:t.key});t.$axios.$post(t.$api.proInstall(),this.qs.stringify(data),{progress:!1}).then((function(e){t.installData=e.msg})).catch((function(e){t.installData="Pro部分更新异常，请重试！"}))},loginSystem:function(){var t=this,data=(t.key,{webkey:t.key});t.$axios.$post(t.$api.isKey(),this.qs.stringify(data)).then((function(e){1==e.code?(t.$message({message:e.msg,type:"success"}),t.goSystem=!1,localStorage.setItem("webkey",t.key),t.$router.push({path:"/system/home"})):t.$message({message:e.msg,type:"error"})})).catch((function(e){console.log(e),t.$message({message:"接口请求异常，请检查网络！",type:"error"})}))}}},l=(o(438),o(26)),component=Object(l.a)(n,(function(){var t=this,e=t._self._c;return e("div",{staticClass:"container"},[e("div",{staticClass:"index-main"},[t._m(0),t._v(" "),e("div",{staticClass:"rule-version"},[e("p",[t._v("当前版本："+t._s(t.versionText)+" "),1==t.isPro?[t._v("PRO")]:t._e()],2),t._v(" "),0==t.isPro?e("p",{staticClass:"new-version"},[t._v("最新版本："),e("a",{attrs:{href:"javascript:;"},on:{click:t.update}},[t._v(t._s(t.newVersion))])]):t._e(),t._v(" "),1==t.isPro?e("p",{staticClass:"new-version"},[t._v("授权模式："),e("a",{attrs:{href:"javascript:;"}},[t._v("全局授权")])]):t._e()]),t._v(" "),e("div",{staticClass:"rule-btn"},[e("el-row",[e("el-button",{attrs:{type:"danger",icon:"el-icon-s-opportunity"},on:{click:function(e){return t.Install()}}},[t._v("安装及更新")]),t._v(" "),e("el-button",{attrs:{type:"primary",icon:"el-icon-s-tools"},on:{click:function(e){t.goSystem=!0}}},[t._v("管理中心")])],1),t._v(" "),e("el-row",[e("div",{staticClass:"protocol"},[e("a",{attrs:{href:"https://www.yuque.com/buxia97/ruleproject/gm1pzr6h0e1eqvvc",target:"_blank"}},[t._v("许可协议 / 免责申明")])])])],1)]),t._v(" "),e("a",{staticClass:"github-corner",attrs:{href:"https://github.com/buxia97/RuleApi","aria-label":"View source on GitHub"}},[e("svg",{staticStyle:{fill:"#151513",color:"#fff",position:"absolute",top:"0",border:"0",right:"0"},attrs:{width:"80",height:"80",viewBox:"0 0 250 250","aria-hidden":"true"}},[e("path",{attrs:{d:"M0,0 L115,115 L130,115 L142,142 L250,250 L250,0 Z"}}),e("path",{staticClass:"octo-arm",staticStyle:{"transform-origin":"130px 106px"},attrs:{d:"M128.3,109.0 C113.8,99.7 119.0,89.6 119.0,89.6 C122.0,82.7 120.5,78.6 120.5,78.6 C119.2,72.0 123.4,76.3 123.4,76.3 C127.3,80.9 125.5,87.3 125.5,87.3 C122.9,97.6 130.6,101.9 134.4,103.2",fill:"currentColor"}}),e("path",{staticClass:"octo-body",attrs:{d:"M115.0,115.0 C114.9,115.1 118.7,116.5 119.8,115.4 L133.7,101.6 C136.9,99.2 139.9,98.4 142.2,98.6 C133.8,88.0 127.5,74.4 143.8,58.0 C148.5,53.4 154.0,51.2 159.7,51.0 C160.3,49.4 163.2,43.6 171.4,40.1 C171.4,40.1 176.1,42.5 178.8,56.2 C183.1,58.6 187.2,61.8 190.9,65.4 C194.5,69.0 197.7,73.2 200.1,77.6 C213.8,80.2 216.3,84.9 216.3,84.9 C212.7,93.1 206.9,96.0 205.4,96.6 C205.1,102.4 203.0,107.8 198.3,112.5 C181.9,128.9 168.3,122.5 157.7,114.1 C157.9,116.9 156.7,120.9 152.7,124.9 L141.0,136.5 C139.8,137.7 141.6,141.9 141.8,141.8 Z",fill:"currentColor"}})])]),e("style",[t._v(".github-corner:hover .octo-arm{animation:octocat-wave 560ms ease-in-out}@keyframes octocat-wave{0%,100%{transform:rotate(0)}20%,60%{transform:rotate(-25deg)}40%,80%{transform:rotate(10deg)}}@media (max-width:500px){.github-corner:hover .octo-arm{animation:none}.github-corner .octo-arm{animation:octocat-wave 560ms ease-in-out}}")]),t._v(" "),e("el-dialog",{attrs:{title:"进入管理中心",visible:t.goSystem,"close-on-click-modal":!1,width:"400px"},on:{"update:visible":function(e){t.goSystem=e}}},[e("div",{staticClass:"dialog-form"},[e("el-input",{attrs:{placeholder:"请输入管理Key",type:"password"},model:{value:t.key,callback:function(e){t.key=e},expression:"key"}})],1),t._v(" "),e("span",{staticClass:"dialog-footer",attrs:{slot:"footer"},slot:"footer"},[e("el-button",{on:{click:function(e){t.goSystem=!1}}},[t._v("取 消")]),t._v(" "),e("el-button",{attrs:{type:"primary"},on:{click:function(e){return t.loginSystem()}}},[t._v("确 定")])],1)]),t._v(" "),e("el-dialog",{attrs:{title:"安装及更新",visible:t.goInstall,"close-on-click-modal":!1,width:"400px"},on:{"update:visible":function(e){t.goInstall=e}}},[0==t.installStatus?e("div",{staticClass:"dialog-form"},[e("p",{staticClass:"dialog-tips"},[t._v("在完成API安装或更新后，请在此处验证，并完成数据库升级！")]),t._v(" "),e("el-input",{attrs:{placeholder:"请输入管理Key",type:"password"},model:{value:t.key,callback:function(e){t.key=e},expression:"key"}})],1):t._e(),t._v(" "),1==t.installStatus?e("div",{staticClass:"install-loading"},[e("p",[t._v("正在执行，请勿关闭窗口或刷新页面！")])]):t._e(),t._v(" "),2==t.installStatus?e("div",{staticClass:"install-data"},[e("div",{staticClass:"install-data-text"},[t._v("\n        "+t._s(t.installData)+"\n      ")])]):t._e(),t._v(" "),e("span",{staticClass:"dialog-footer",attrs:{slot:"footer"},slot:"footer"},[0==t.installStatus?e("el-button",{on:{click:function(e){t.goInstall=!1}}},[t._v("取 消")]):t._e(),t._v(" "),0==t.installStatus?e("el-button",{attrs:{type:"primary"},on:{click:function(e){return t.goInstallStart()}}},[t._v("确 定")]):t._e(),t._v(" "),1==t.installStatus?e("el-button",{attrs:{type:"primary",loading:!0}},[t._v("执行中")]):t._e(),t._v(" "),2==t.installStatus?e("el-button",{attrs:{type:"danger"},on:{click:function(e){t.installStatus=0}}},[t._v("重 试")]):t._e(),t._v(" "),2==t.installStatus?e("el-button",{attrs:{type:"primary"},on:{click:function(e){t.goInstall=!1}}},[t._v("完 成")]):t._e()],1)]),t._v(" "),e("el-dialog",{attrs:{title:"Typecho未安装",visible:t.isTypecho,width:"400px","close-on-click-modal":!1},on:{"update:visible":function(e){t.isTypecho=e}}},[t.goInstallTypecho?t._e():[e("span",[t._v("Typecho未安装或者数据表前缀不正确，请检查您的API配置。")])],t._v(" "),t.goInstallTypecho?[0==t.installStatus?e("div",{staticClass:"dialog-form"},[e("p",{staticClass:"dialog-tips"},[t._v("此操作将放弃链接typecho数据库，让API独立运行！")]),t._v(" "),e("el-input",{attrs:{placeholder:"请输入管理Key",type:"password"},model:{value:t.key,callback:function(e){t.key=e},expression:"key"}}),t._v(" "),e("el-input",{attrs:{placeholder:"请输入管理用户账号",type:"text"},model:{value:t.name,callback:function(e){t.name=e},expression:"name"}}),t._v(" "),e("el-input",{attrs:{placeholder:"请输入管理用户密码",type:"text"},model:{value:t.password,callback:function(e){t.password=e},expression:"password"}})],1):t._e(),t._v(" "),1==t.installStatus?e("div",{staticClass:"install-loading"},[e("p",[t._v("正在执行，请勿关闭窗口或刷新页面！")])]):t._e(),t._v(" "),2==t.installStatus?e("div",{staticClass:"install-data"},[e("div",{staticClass:"install-data-text"},[t._v("\n          "+t._s(t.installData)+"\n        ")])]):t._e()]:t._e(),t._v(" "),e("span",{staticClass:"dialog-footer",attrs:{slot:"footer"},slot:"footer"},[t.goInstallTypecho?t._e():[e("el-button",{attrs:{type:"danger"},on:{click:function(e){return t.goTypecho()}}},[t._v("我不想安装Typecho")]),t._v(" "),e("el-button",{attrs:{type:"primary"},on:{click:function(e){t.isTypecho=!1}}},[t._v("我知道了")])],t._v(" "),t.goInstallTypecho?[0==t.installStatus?e("el-button",{on:{click:function(e){t.isTypecho=!1}}},[t._v("取 消")]):t._e(),t._v(" "),0==t.installStatus?e("el-button",{attrs:{type:"primary"},on:{click:function(e){return t.installTypecho()}}},[t._v("确 定")]):t._e(),t._v(" "),1==t.installStatus?e("el-button",{attrs:{type:"primary",loading:!0}},[t._v("执行中")]):t._e(),t._v(" "),2==t.installStatus?e("el-button",{attrs:{type:"danger"},on:{click:function(e){t.installStatus=0}}},[t._v("重 试")]):t._e(),t._v(" "),2==t.installStatus?e("el-button",{attrs:{type:"primary"},on:{click:function(e){t.isTypecho=!1,t.goInstall=!0,t.goInstallStart()}}},[t._v("继续安装")]):t._e()]:t._e()],2)],2)],1)}),[function(){var t=this,e=t._self._c;return e("div",{staticClass:"rule-logo"},[e("h1",[t._v("Rule"),e("span",[t._v("Api")])])])}],!1,null,null,null);e.default=component.exports}}]);