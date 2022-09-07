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

		webinfoUsertime:"",
		webinfoUploadUrl:"",
		webinfoAvatar:"",
		pexelsKey:"",
		scale:"",

		clock:"",
		vipPrice:"",
		vipDay:"",
		vipDiscount:"",
		isEmail:"",
		isInvite:"",

		//系统密钥
		webinfoKey:"",

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
		//缓存配置
		usertime:'',
		contentCache:'',
		contentInfoCache:'',
		CommentCache:'',
		userCache:'',
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
		//FTP
		ftpHost:'',
		ftpPort:'',
		ftpUsername:'',
		ftpPassword:'',
		ftpBasePath:'',
		//支付宝
		alipayAppId:'',
		alipayPrivateKey:'',
		alipayPublicKey:'',
		alipayNotifyUrl:'',
		//微信支付
		appletsAppid:'',
		appletsSecret:'',
		qqAppletsAppid:'',
		qqAppletsSecret:'',
		wxpayAppId:'',
		wxpayMchId:'',
		wxpayKey:'',
		wxpayNotifyUrl:'',
		//配置信息
		applicationText:"",
		//评论配置
		auditlevel:"",
		forbidden:"",
		//自定义字段
		fields:"",
		//付费广告
		pushAdsPrice:"",
		pushAdsNum:"",
		bannerAdsPrice:"",
		bannerAdsNum:"",
		startAdsPrice:"",
		startAdsNum:"",
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
		that.getConfigAll();
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
		getConfigAll(){
			var that = this;
			var url = "/system/getApiConfig"
			axios.get(url,{
				params:{
					"webkey":that.webkey,
				}
			}).then(function(res){
				if(res.data.code==1){
					that.webinfoTitle=res.data.data.webinfoTitle;
					that.webinfoUrl=res.data.data.webinfoUrl;
					that.webinfoUsertime=res.data.data.webinfoUsertime;
					that.webinfoUploadUrl=res.data.data.webinfoUploadUrl;
					that.webinfoAvatar=res.data.data.webinfoAvatar;
					that.pexelsKey = res.data.data.pexelsKey;


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
					//ftp
					that.ftpHost=res.data.data.ftpHost;
					that.ftpPort=res.data.data.ftpPort;
					that.ftpUsername=res.data.data.ftpUsername;
					that.ftpPassword=res.data.data.ftpPassword;
					that.ftpBasePath=res.data.data.ftpBasePath;
					//支付宝
					that.alipayAppId=res.data.data.alipayAppId;
					that.alipayPrivateKey=res.data.data.alipayPrivateKey;
					that.alipayPublicKey=res.data.data.alipayPublicKey;
					that.alipayNotifyUrl=res.data.data.alipayNotifyUrl;

					//微信支付
					that.appletsAppid=res.data.data.appletsAppid;
					that.appletsSecret=res.data.data.appletsSecret;
					that.qqAppletsAppid=res.data.data.qqAppletsAppid;
					that.qqAppletsSecret=res.data.data.qqAppletsSecret;
					that.wxpayAppId=res.data.data.wxpayAppId;
					that.wxpayMchId=res.data.data.wxpayMchId;
					that.wxpayKey=res.data.data.wxpayKey;
					that.wxpayNotifyUrl=res.data.data.wxpayNotifyUrl;

					//支付模块
					that.scale = res.data.data.scale;
					that.clock=res.data.data.clock;
					that.vipPrice=res.data.data.vipPrice;
					that.vipDay=res.data.data.vipDay;
					that.vipDiscount=res.data.data.vipDiscount;
					that.isEmail=res.data.data.isEmail;
					that.isInvite=res.data.data.isInvite;

					//评论模块
					that.auditlevel=res.data.data.auditlevel;
					that.forbidden=res.data.data.forbidden;
					//自定义字段
					that.fields=res.data.data.fields;
					//付费广告
					that.pushAdsPrice=res.data.data.pushAdsPrice;
					that.pushAdsNum=res.data.data.pushAdsNum;
					that.bannerAdsPrice=res.data.data.bannerAdsPrice;
					that.bannerAdsNum=res.data.data.bannerAdsNum;
					that.startAdsPrice=res.data.data.startAdsPrice;
					that.startAdsNum=res.data.data.startAdsNum;

				}else{
					that.outSystem();
				}
			}).catch(function (error) {
				console.log(error);
			});
		},
		getAll(){
			var that = this;
			that.getConfigAll();
			var url = "/system/allConfig"
			axios.get(url,{
			        params:{
						"webkey":that.webkey,
			        }
			}).then(function(res){
				if(res.data.code==1){

					that.webinfoKey=res.data.data.webinfoKey;


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
					//缓存配置
					that.usertime=res.data.data.usertime;
					that.contentCache=res.data.data.contentCache;
					that.contentInfoCache=res.data.data.contentCache;
					that.CommentCache=res.data.data.CommentCache;
					that.userCache=res.data.data.userCache;
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
		saveCache(){
			var that = this;
			var url = "/system/setupCache"
			var data={
				usertime:that.usertime,
				contentCache:that.contentCache,
				contentInfoCache:that.contentInfoCache,
				CommentCache:that.CommentCache,
				userCache:that.userCache,
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
		saveWebKey(){
			var that = this;
			var url = "/system/setupWebKey"
			var data={
				webinfoKey:that.webinfoKey,
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
		saveWebinfo(){
			var that = this;
			var url = "/system/apiConfigUpdate"
			var data={
				webinfoTitle:that.webinfoTitle,
				webinfoUrl:that.webinfoUrl,
				webinfoUsertime:that.webinfoUsertime,
				webinfoUploadUrl:that.webinfoUploadUrl,
				webinfoAvatar:that.webinfoAvatar,
				pexelsKey:that.pexelsKey,
				isEmail:that.isEmail,
				isInvite:that.isInvite,
				auditlevel:that.auditlevel,
				forbidden:that.forbidden,
				fields:that.fields
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
		saveAssets(){
			var that = this;
			var url = "/system/apiConfigUpdate"
			var data={
				scale:that.scale,
				clock:that.clock,
				vipPrice:that.vipPrice,
				vipDay:that.vipDay,
				vipDiscount:that.vipDiscount,
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
			var url = "/system/apiConfigUpdate"
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
			var url = "/system/apiConfigUpdate"
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

		saveFTP(){
			var that = this;
			var url = "/system/apiConfigUpdate"
			var data={
				ftpHost:that.ftpHost,
				ftpPort:that.ftpPort,
				ftpUsername:that.ftpUsername,
				ftpPassword:that.ftpPassword,
				ftpBasePath:that.ftpBasePath,
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
			var url = "/system/apiConfigUpdate"
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
			var url = "/system/apiConfigUpdate"
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
		saveApplets(){
			var that = this;
			var url = "/system/apiConfigUpdate"
			var data={
				appletsAppid:that.appletsAppid,
				appletsSecret:that.appletsSecret,
				qqAppletsAppid:that.qqAppletsAppid,
				qqAppletsSecret:that.qqAppletsSecret,
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
		saveAds(){
			var that = this;
			var url = "/system/apiConfigUpdate"
			var data={
				pushAdsPrice:that.pushAdsPrice,
				pushAdsNum:that.pushAdsNum,
				bannerAdsPrice:that.bannerAdsPrice,
				bannerAdsNum:that.bannerAdsNum,
				startAdsPrice:that.startAdsPrice,
				startAdsNum:that.startAdsNum,
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
