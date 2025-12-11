package com.RuleApi.service;

import com.RuleApi.entity.TypechoApiconfig;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UploadService {

    String base64Upload(String base64Img, String  dataprefix, Map apiconfig, Integer uid,FilesService filesService);
    String cosUpload(MultipartFile file, String  dataprefix, Map apiconfig,Integer uid);
    String localUpload(MultipartFile file, String  dataprefix, Map apiconfig,Integer uid);
    String ossUpload(MultipartFile file, String  dataprefix, Map apiconfig,Integer uid);
    String qiniuUpload(MultipartFile file, String  dataprefix, Map apiconfig,Integer uid);
    String ftpUpload(MultipartFile file, String  dataprefix, Map apiconfig,Integer uid);

    String s3Upload(MultipartFile file, String  dataprefix, Map apiconfig,Integer uid);
}
