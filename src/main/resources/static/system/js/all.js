var vm = new Vue({
	el: '#app',
	data: {
		page: 1,
		webkey:"",
		isLogin:0,
		alert:0,
		alertText:"",
		isMenu:false,

		//基本信息
		webinfoTitle:"",
		webinfoUrl:"",
		webinfoKey:"",
		webinfoUsertime:"",
		webinfoUploadUrl:"",
		webinfoAvatar:"",
		pexelsKey:"",
		scale:"",

		//邮箱配置
		mailHost:'',
		mailUsername:'',
		mailPassword:'',
		//mysql配置
		dataUrl:"",
		dataUsername:"",
		dataPassword:"",
		dataPrefix:"",
		//Redis配置
		redisHost:'',
		redisPassword:'',
		redisPort:'',
		redisPrefix:'',
		//COS
		cosAccessKey:'',
		cosSecretKey:'',
		cosBucket:'',
		cosBucketName:'',
		cosPath:'',
		cosPrefix:'',
		//OSS
		aliyunEndpoint:'',
		aliyunAccessKeyId:'',
		aliyunAccessKeySecret:'',
		aliyunAucketName:'',
		aliyunUrlPrefix:'',
		aliyunFilePrefix:'',
		//支付宝
		alipayAppId:'',
		alipayPrivateKey:'',
		alipayPublicKey:'',
		alipayNotifyUrl:'',
		//微信支付
		wxpayAppId:'',
		wxpayMchId:'',
		wxpayKey:'',
		wxpayNotifyUrl:'',
		//配置信息
		applicationText:"",
	},
	created(){
		var that = this;
		if(localStorage.getItem('ruleApipage')){
			that.page = Number(localStorage.getItem('ruleApipage'));
		}
		if(localStorage.getItem('webkey')){
			that.webkey = localStorage.getItem('webkey');
			that.isLogin = 1;
		}else{
			that.isLogin = 0;
		}
		that.getAll();
		that.getApplicationText();
	},
	mounted(){
		var that = this;
	},
	methods: {
		toMenu(){
			var that = this;
			that.isMenu = !that.isMenu;
		},
		toAlert(text){
			var that = this;
			if(that.alert==1){
				return false;
			}
			that.alert=1;
			that.alertText = text;
			var timer = setTimeout(function() {
				that.alert=0;
				that.alertText = "";
			}, 2000)
		},
		toPage(i){
			var that = this;
			that.page = i;
			localStorage.setItem('ruleApipage',i);
		},
		outSystem(){
			var that = this;
			localStorage.removeItem('webkey');
			localStorage.removeItem('ruleApipage');
			that.isLogin = 0;
			that.webkey = "";
		},
		toSystem(){
			var that = this;
			var url = "/system/isKey"
			axios.get(url,{
				params:{
					"webkey":that.webkey,
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					localStorage.setItem('webkey',that.webkey);
					that.isLogin=1;
					that.getAll();
					that.getApplicationText();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		//移除数据中的空对象
		removeObjectEmptyKey(json) {
			var value;
			for (var key in json) {
				if (json.hasOwnProperty(key)) {
					value = json[key];
					if (value === undefined || value === '' || value === null) {
						delete json[key]
					}
				}
			}
			return json;
		},
		getAll(){
			var that = this;
			var url = "/system/allConfig"
			axios.get(url,{
			        params:{
						"webkey":that.webkey,
			        }
			}).then(function(res){
				if(res.data.code==1){
					that.webinfoTitle=res.data.data.webinfoTitle;
					that.webinfoUrl=res.data.data.webinfoUrl;
					that.webinfoKey=res.data.data.webinfoKey;
					that.webinfoUsertime=res.data.data.webinfoUsertime;
					that.webinfoUploadUrl=res.data.data.webinfoUploadUrl;
					that.webinfoAvatar=res.data.data.webinfoAvatar;
					that.pexelsKey = res.data.data.pexelsKey;
					that.scale = res.data.data.scale;

					//邮箱信息
					that.mailHost=res.data.data.mailHost;
					that.mailUsername=res.data.data.mailUsername;
					that.mailPassword=res.data.data.mailPassword;
					//mysql配置
					that.dataUrl=res.data.data.dataUrl;
					that.dataUsername=res.data.data.dataUsername;
					that.dataPassword=res.data.data.dataPassword;
					that.dataPrefix=res.data.data.dataPrefix;
					//Redis配置
					that.redisHost=res.data.data.redisHost;
					that.redisPassword=res.data.data.redisPassword;
					that.redisPort=res.data.data.redisPort;
					that.redisPrefix=res.data.data.redisPrefix;
					//COS
					that.cosAccessKey=res.data.data.cosAccessKey;
					that.cosSecretKey=res.data.data.cosSecretKey;
					that.cosBucket=res.data.data.cosBucket;
					that.cosBucketName=res.data.data.cosBucketName;
					that.cosPath=res.data.data.cosPath;
					that.cosPrefix=res.data.data.cosPrefix;
					//Oss
					that.aliyunEndpoint=res.data.data.aliyunEndpoint;
					that.aliyunAccessKeyId=res.data.data.aliyunAccessKeyId;
					that.aliyunAccessKeySecret=res.data.data.aliyunAccessKeySecret;
					that.aliyunAucketName=res.data.data.aliyunAucketName;
					that.aliyunUrlPrefix=res.data.data.aliyunUrlPrefix;
					that.aliyunFilePrefix=res.data.data.aliyunFilePrefix;
					//支付宝
					that.alipayAppId=res.data.data.alipayAppId;
					that.alipayPrivateKey=res.data.data.alipayPrivateKey;
					that.alipayPublicKey=res.data.data.alipayPublicKey;
					that.alipayNotifyUrl=res.data.data.alipayNotifyUrl;

					//微信支付
					that.wxpayAppId=res.data.data.wxpayAppId;
					that.wxpayMchId=res.data.data.wxpayMchId;
					that.wxpayKey=res.data.data.wxpayKey;
					that.wxpayNotifyUrl=res.data.data.wxpayNotifyUrl;
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		getApplicationText(){
			var that = this;
			var url = "/system/getConfig"
			axios.get(url,{
				params:{
					"webkey":that.webkey,
				}
			}).then(function(res){
				if(res.data.code==1){
					that.applicationText = unescape(res.data.data);
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		saveWebinfo(){
			var that = this;
			var url = "/system/setupWebInfo"
			var data={
				webinfoTitle:that.webinfoTitle,
				webinfoUrl:that.webinfoUrl,
				webinfoKey:that.webinfoKey,
				webinfoUsertime:that.webinfoUsertime,
				webinfoUploadUrl:that.webinfoUploadUrl,
				webinfoAvatar:that.webinfoAvatar,
				pexelsKey:that.pexelsKey,
				scale:that.scale
			}
			axios.get(url,{
				params:{
					"webkey":that.webkey,
					"params":JSON.stringify(that.removeObjectEmptyKey(data))
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					that.getAll();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		saveEmail(){
			var that = this;
			var url = "/system/setupEmail"
			var data={
				mailHost:that.mailHost,
				mailUsername:that.mailUsername,
				mailPassword:that.mailPassword,
			}
			axios.get(url,{
				params:{
					"webkey":that.webkey,
					"params":JSON.stringify(that.removeObjectEmptyKey(data))
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					that.getAll();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		saveMysql(){
			var that = this;
			var url = "/system/setupMysql"
			var data={
				dataUrl:that.dataUrl,
				dataUsername:that.dataUsername,
				dataPassword:that.dataPassword,
				dataPrefix:that.dataPrefix
			}
			axios.get(url,{
				params:{
					"webkey":that.webkey,
					"params":JSON.stringify(that.removeObjectEmptyKey(data))
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					that.getAll();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		saveRedis(){
			var that = this;
			var url = "/system/setupRedis"
			var data={
				redisHost:that.redisHost,
				redisPassword:that.redisPassword,
				redisPort:that.redisPort,
				redisPrefix:that.pexelsKey,
			}
			axios.get(url,{
				params:{
					"webkey":that.webkey,
					"params":JSON.stringify(that.removeObjectEmptyKey(data))
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					that.getAll();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		saveCos(){
			var that = this;
			var url = "/system/setupCos"
			var data={
				cosAccessKey:that.cosAccessKey,
				cosSecretKey:that.cosSecretKey,
				cosBucket:that.cosBucket,
				cosBucketName:that.cosBucketName,
				cosPath:that.cosPath,
				cosPrefix:that.cosPrefix,
			}
			axios.get(url,{
				params:{
					"webkey":that.webkey,
					"params":JSON.stringify(that.removeObjectEmptyKey(data))
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					that.getAll();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		saveOss(){
			var that = this;
			var url = "/system/setupOss"
			var data={
				aliyunEndpoint:that.aliyunEndpoint,
				aliyunAccessKeyId:that.aliyunAccessKeyId,
				aliyunAccessKeySecret:that.aliyunAccessKeySecret,
				aliyunAucketName:that.aliyunAucketName,
				aliyunUrlPrefix:that.aliyunUrlPrefix,
				aliyunFilePrefix:that.aliyunFilePrefix,
			}
			axios.get(url,{
				params:{
					"webkey":that.webkey,
					"params":JSON.stringify(that.removeObjectEmptyKey(data))
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					that.getAll();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		saveAlipay(){
			var that = this;
			var url = "/system/setupAlipay"
			var data={
				alipayAppId:that.alipayAppId,
				alipayPrivateKey:that.alipayPrivateKey,
				alipayPublicKey:that.alipayPublicKey,
				alipayNotifyUrl:that.alipayNotifyUrl,
			}
			axios.get(url,{
				params:{
					"webkey":that.webkey,
					"params":JSON.stringify(that.removeObjectEmptyKey(data))
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					that.getAll();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		saveWxpay(){
			var that = this;
			var url = "/system/setupWxpay"
			var data={
				wxpayAppId:that.wxpayAppId,
				wxpayMchId:that.wxpayMchId,
				wxpayKey:that.wxpayKey,
				wxpayNotifyUrl:that.wxpayNotifyUrl,
			}
			axios.get(url,{
				params:{
					"webkey":that.webkey,
					"params":JSON.stringify(that.removeObjectEmptyKey(data))
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					that.getAll();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		saveConfig(){
			var that = this;
			var url = "/system/setupConfig"
			axios.get(url,{
				params:{
					"webkey":that.webkey,
					"params":that.applicationText
				}
			}).then(function(res){
				that.toAlert(res.data.msg);
				if(res.data.code==1){
					that.getAll();
					that.getApplicationText();
				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		}
	}
})
