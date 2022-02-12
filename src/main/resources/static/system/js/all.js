var vm = new Vue({
	el: '#app',
	data: {
		page: 1,
		webinfoTitle:"",
		webinfoUrl:"",
		webinfoKey:"",
		webinfoUsertime:"",
		webinfoUploadUrl:"",
		webinfoAvatar:"",
	},
	created(){
		var that = this;
		that.getAll();
	},
	mounted(){
		var that = this;
	},
	methods: {
		toPage(i){
			var that = this;
			that.page = i;
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
			axios.get(url,{       // 还可以直接把参数拼接在url后边
			        params:{
						"webkey":"123456"
			        }
			}).then(function(res){
					console.log(JSON.stringify(res))
				if(res.data.code==1){
					that.webinfoTitle=res.data.data.webinfoTitle;
					that.webinfoUrl=res.data.data.webinfoUrl;
					that.webinfoKey=res.data.data.webinfoKey;
					that.webinfoUsertime=res.data.data.webinfoUsertime;
					that.webinfoUploadUrl=res.data.data.webinfoUploadUrl;
					that.webinfoAvatar=res.data.data.webinfoAvatar;
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
			}
			axios.get(url,{       // 还可以直接把参数拼接在url后边
				params:{
					"webkey":"123456",
					"params":JSON.stringify(that.removeObjectEmptyKey(data))
				}
			}).then(function(res){
				console.log(JSON.stringify(res))
				if(res.data.code==1){
					that.getAll();
				}
			}).catch(function (error) {
				console.log(error);
			});
		}
	}
})
