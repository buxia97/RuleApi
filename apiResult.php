<?php
error_reporting(0);

$version = "1.0.0 beta";
$versionIntro = "规则之树手机客户端";
$versionUrl = "";
$versionCode = 10;

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
if(isset($_GET['appStart'])){
	$result=array(
		"appStartPic"=>$appStartPic,
		'appStartUrl'=>$appStartUrl,
	);

   //输出json
   echo json_encode($result);
}
?>