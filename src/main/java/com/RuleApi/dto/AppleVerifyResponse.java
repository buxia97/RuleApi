package com.RuleApi.dto;

import java.util.Map;

// 响应 DTO
public class AppleVerifyResponse {
    private int status;
    private Map<String, Object> receipt;
// other fields can be added as needed


    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public Map<String, Object> getReceipt() { return receipt; }
    public void setReceipt(Map<String, Object> receipt) { this.receipt = receipt; }
}