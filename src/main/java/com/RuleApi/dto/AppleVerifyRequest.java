package com.RuleApi.dto;

public class AppleVerifyRequest {
    private String receipt;
    private String productId;
    private String transactionId;
    private String userId;
    private Integer payType; //0为普通支付，1VIP套餐购买
    // getters & setters
    public String getReceipt() { return receipt; }
    public void setReceipt(String receipt) { this.receipt = receipt; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getPayType() { return payType; }
    public void setPayType(Integer payType) { this.payType = payType; }
}