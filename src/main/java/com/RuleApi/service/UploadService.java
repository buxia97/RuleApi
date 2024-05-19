package com.RuleApi.service;

import com.RuleApi.entity.TypechoApiconfig;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    String base64Upload(String base64Img, String  dataprefix, TypechoApiconfig apiconfig,Integer uid);
    String cosUpload(MultipartFile file, String  dataprefix, TypechoApiconfig apiconfig,Integer uid);
    String localUpload(MultipartFile file, String  dataprefix, TypechoApiconfig apiconfig,Integer uid);
    String ossUpload(MultipartFile file, String  dataprefix, TypechoApiconfig apiconfig,Integer uid);
    String qiniuUpload(MultipartFile file, String  dataprefix, TypechoApiconfig apiconfig,Integer uid);
    String ftpUpload(MultipartFile file, String  dataprefix, TypechoApiconfig apiconfig,Integer uid);
}
