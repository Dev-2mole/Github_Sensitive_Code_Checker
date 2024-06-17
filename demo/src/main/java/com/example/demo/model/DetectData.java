package com.example.demo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetectData {
    private int detectCount;
    private int allCount;
    private List<Map<String, String>> detectedUrls = new ArrayList<>();
    private List<Map<String, String>> sensitiveData = new ArrayList<>();
    private List<Map<String, String>> reg = new ArrayList<>();
    private List<Map<String, String>> call = new ArrayList<>();
    private List<Map<String, String>> awsAccess = new ArrayList<>();
    private List<Map<String, String>> awsSecret = new ArrayList<>();
    private List<Map<String, String>> ip = new ArrayList<>();

    // 기존 생성자 및 메서드들

    public List<Map<String, String>> getReg() {
        return reg;
    }

    public void setReg(List<Map<String, String>> reg) {
        this.reg = reg;
    }

    public List<Map<String, String>> getCall() {
        return call;
    }

    public void setCall(List<Map<String, String>> call) {
        this.call = call;
    }

    public List<Map<String, String>> getAwsAccess() {
        return awsAccess;
    }

    public void setAwsAccess(List<Map<String, String>> awsAccess) {
        this.awsAccess = awsAccess;
    }

    public List<Map<String, String>> getAwsSecret() {
        return awsSecret;
    }

    public void setAwsSecret(List<Map<String, String>> awsSecret) {
        this.awsSecret = awsSecret;
    }

    public List<Map<String, String>> getIp() {
        return ip;
    }

    public void setIp(List<Map<String, String>> ip) {
        this.ip = ip;
    }

    public int getDetectCount() {
        return detectCount;
    }

    public void setDetectCount(int detectCount) {
        this.detectCount = detectCount;
    }

    public int getAllCount() {
        return allCount;
    }

    public void setAllCount(int allCount) {
        this.allCount = allCount;
    }

    public List<Map<String, String>> getDetectedUrls() {
        return detectedUrls;
    }

    public void setDetectedUrls(List<Map<String, String>> detectedUrls) {
        this.detectedUrls = detectedUrls;
    }

    public List<Map<String, String>> getSensitiveData() {
        return sensitiveData;
    }

    public void setSensitiveData(List<Map<String, String>> sensitiveData) {
        this.sensitiveData = sensitiveData;
    }

    public Map<String, String> getDetectedCodeMap() {
        Map<String, String> detectedCodeMap = new HashMap<>();
        for (Map<String, String> data : sensitiveData) {
            detectedCodeMap.put(data.get("filePath"), data.get("sensitiveInfo"));
        }
        return detectedCodeMap;
    }
}