<?php
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
?>