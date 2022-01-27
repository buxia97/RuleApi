<?php
error_reporting(0);

$version = "RuleTree App 1.0.0 beta";
$versionIntro = "规则之树手机客户端";
$versionUrl = "";
$versionCode = 10;

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

?>