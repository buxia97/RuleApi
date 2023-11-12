(window.webpackJsonp=window.webpackJsonp||[]).push([[18],{454:function(e,t,o){"use strict";o.r(t);var l={layout:"layout",head:function(){return{title:"易支付配置"}},data:function(){return{key:"",form:{epayUrl:"",epayPid:"",epayKey:"",epayNotifyUrl:""}}},beforeDestroy:function(){},created:function(){},mounted:function(){var e=this;localStorage.getItem("webkey")?e.key=localStorage.getItem("webkey"):(e.$message({message:"认证失效！",type:"error"}),e.$router.push({path:"/"})),e.getConfig()},methods:{save:function(){var e=this,t=e.form,data={webkey:e.key,params:JSON.stringify(t)},o=this.$loading({lock:!0,text:"Loading",spinner:"el-icon-loading",background:"rgba(0, 0, 0, 0.7)"});e.$axios.$post(e.$api.apiConfigUpdate(),this.qs.stringify(data)).then((function(t){o.close(),1==t.code?(e.$message({message:t.msg,type:"success"}),e.getConfig()):e.$message({message:t.msg,type:"error"})})).catch((function(t){o.close(),console.log(t),e.$message({message:"接口请求异常，请检查网络！",type:"error"})}))},getConfig:function(){var e=this,data={webkey:e.key};e.$axios.$post(e.$api.getApiConfig(),this.qs.stringify(data)).then((function(t){1==t.code&&(e.form.epayUrl=t.data.epayUrl,e.form.epayPid=t.data.epayPid,e.form.epayKey=t.data.epayKey,e.form.epayNotifyUrl=t.data.epayNotifyUrl)})).catch((function(t){console.log(t),e.$message({message:"接口请求异常，请检查网络！",type:"error"})}))}}},r=o(26),component=Object(r.a)(l,(function(){var e=this,t=e._self._c;return t("div",{staticClass:"page-container"},[t("el-row",{attrs:{gutter:15}},[t("el-col",{attrs:{span:24}},[t("div",{staticClass:"data-box"},[t("div",{staticClass:"page-title"},[t("h4",[e._v("易支付配置")]),e._v(" "),t("p",[e._v("易支付接口可支持所有核心程序为彩虹易支付的第三方支付平台。不过，为了财产安全，对于非官方的支付渠道，请谨慎选择。可能不支持部分关闭API支付模式或者魔改的易支付程序。")])]),e._v(" "),t("div",{staticClass:"page-form"},[t("el-form",{ref:"form",attrs:{model:e.form,"label-position":"top","label-width":"80px"}},[t("el-form-item",[t("p",{staticClass:"form-label",attrs:{slot:"label"},slot:"label"},[e._v("易支付接口地址"),t("span",[e._v("输入易支付平台的接口地址")])]),e._v(" "),t("el-input",{attrs:{placeholder:"请输入易支付接口地址"},model:{value:e.form.epayUrl,callback:function(t){e.$set(e.form,"epayUrl",t)},expression:"form.epayUrl"}})],1),e._v(" "),t("el-form-item",[t("p",{staticClass:"form-label",attrs:{slot:"label"},slot:"label"},[e._v("易支付商户ID")]),e._v(" "),t("el-input",{attrs:{placeholder:"请输入易支付商户ID"},model:{value:e.form.epayPid,callback:function(t){e.$set(e.form,"epayPid",t)},expression:"form.epayPid"}})],1),e._v(" "),t("el-form-item",[t("p",{staticClass:"form-label",attrs:{slot:"label"},slot:"label"},[e._v("易支付商户密钥")]),e._v(" "),t("el-input",{attrs:{placeholder:"请输入易支付商户密钥"},model:{value:e.form.epayKey,callback:function(t){e.$set(e.form,"epayKey",t)},expression:"form.epayKey"}})],1),e._v(" "),t("el-form-item",[t("p",{staticClass:"form-label",attrs:{slot:"label"},slot:"label"},[e._v("易支付回调地址"),t("span",[e._v("根据您的接口域名和访问协议进行填写，如：http://127.0.0.1/pay/EPayNotify")])]),e._v(" "),t("el-input",{attrs:{placeholder:"请输入易支付回调地址"},model:{value:e.form.epayNotifyUrl,callback:function(t){e.$set(e.form,"epayNotifyUrl",t)},expression:"form.epayNotifyUrl"}})],1),e._v(" "),t("el-form-item",[t("el-button",{attrs:{type:"primary"},on:{click:function(t){return e.save()}}},[e._v("保存设置")])],1)],1)],1)])])],1)],1)}),[],!1,null,null,null);t.default=component.exports}}]);