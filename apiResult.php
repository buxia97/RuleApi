<?php
error_reporting(0);

$version = "1.0.0 beta";
$versionIntro = "规则之树手机客户端";
$versionUrl = "";
$versionCode = 10;

//广告定义，预留了3个，可以参考我的格式自己添加,格式为“图片地址|链接”

//是否开启广告（改为1则显示广告，0不显示）
$isAds = 1;

$ad1 = "https://www.ruletree.club/app/app-ads1.jpg|https://curl.qcloud.com/IvR7A1sk";
$ad2 = "https://www.ruletree.club/app/app-ads2.jpg|https://v.douyin.com/NLVaeau/";
$ad3 = "https://www.ruletree.club/app/app-ads1.jpg|https://curl.qcloud.com/IvR7A1sk";

//自定义启动图广告
//图片地址（分辨率建议，宽度1080，长度1883）
$appStartPic="";
//跳转地址（支持app内部页面，和http链接）
$appStartUrl="";

if(isset($_GET['update'])){
	$result=array(
    'version'=>$version,
    'versionIntro'=>$versionIntro,
    'versionUrl'=>$versionUrl,
	'versionCode'=>$versionCode
   );
   //输出json
   echo json_encode($result);
}
if(isset($_GET['getAds'])){
	if($isAds==1){
		$result=array(
			"isAds"=>$isAds,
			'ad1'=>$ad1,
			'ad2'=>$ad2,
			'ad3'=>$ad3,
		);
	}else{
		$result=array(
			"isAds"=>$isAds,
		);
	}

   //输出json
   echo json_encode($result);
}
if(isset($_GET['appStart'])){
	$result=array(
		"appStartPic"=>$appStartPic,
		'appStartUrl'=>$appStartUrl,
	);

   //输出json
   echo json_encode($result);
}
?>