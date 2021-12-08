<?php
error_reporting(0);

$version = "RuleTree App 1.0.0 beta";
$versionIntro = "规则之树手机客户端";
$versionUrl = "";
$versionCode = 10;

require './var/PasswordHash.php';
$hasher = new PasswordHash(8, true);
//加密密码
if(isset($_GET['pw'])){
	$pw = $_GET['pw'];
	echo $hasher->HashPassword($pw);
}
//用于登陆时验证密码，如果返回值等于newpw，则密码正确
if(isset($_GET['oldpw'])&&isset($_GET['newpw'])){
	$hashValidate = $hasher->crypt_private($_GET['oldpw'], $_GET['newpw']);
	echo $hashValidate;
}
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