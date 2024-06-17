package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.demo.model.DetectData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GitService {
    private static final String GIT_URL = "https://github.com";
    private static final String GIT_RAW_URL = "https://raw.githubusercontent.com";
    private static final Logger logger = LoggerFactory.getLogger(GitService.class);

    @Value("${github.token}")
    private String githubToken;

    public List<String> gitParser(String repoUrl) {
        String[] urlParts = repoUrl.split("/");
        String owner = urlParts[urlParts.length - 2];
        String repo = urlParts[urlParts.length - 1];

        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/contents";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Object[]> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Object[].class);

        Object[] objects = response.getBody();
        List<String> paths = new ArrayList<>();
        List<String> excludedExtensions = Arrays.asList(".pdf", ".hwp", ".ppt", ".md", ".lock");

        for (Object object : objects) {
            Map<String, Object> fileMap = (Map<String, Object>) object;
            String filePath = fileMap.get("path").toString();
            String fileType = fileMap.get("type").toString();

            if (fileType.equals("dir")) {
                String subUrl = apiUrl + "/" + filePath;
                try {
                    List<String> subPaths = gitParser(subUrl);
                    paths.addAll(subPaths);
                } catch (HttpClientErrorException.NotFound e) {
                    logger.info("Skipping inaccessible directory: {}", filePath);
                }
            } else if (fileType.equals("file")) {
                int lastDotIndex = filePath.lastIndexOf(".");
                if (lastDotIndex != -1) {
                    String fileExtension = filePath.substring(lastDotIndex).toLowerCase();
                    if (!excludedExtensions.contains(fileExtension)) {
                        paths.add(filePath);
                        logger.info("Parsing file: {}", filePath);
                    }
                }
            }
        }
        return paths;
    }

    public List<List<String>> codeParser(String repoUrl, List<String> codeUrls) {
        List<List<String>> totalCode = new ArrayList<>();
        repoUrl = repoUrl.replace("https://github.com", "");

        RestTemplate restTemplate = new RestTemplate();
        for (String codeUrl : codeUrls) {
            String fullUrl = GIT_RAW_URL + repoUrl + "/master/" + codeUrl;
            String code = restTemplate.getForObject(fullUrl, String.class);
            code = code.replace(" ", "");
            totalCode.add(Arrays.asList(code.split("\n")));
        }

        return totalCode;
    }

    public DetectData codeRegex(String repoName, List<List<String>> codeData, List<String> codeUrls) {
        DetectData detectData = new DetectData();
        int count = codeData.stream().mapToInt(List::size).sum();
        int detectCount = 0;
        List<Map<String, String>> detectedUrls = new ArrayList<>(); // 감지된 URL을 저장할 리스트

        String regNumRegex = "(?:[0-9]{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[1,2][0-9]|3[0,1]))-[1-4][0-9]{6}";
        String callNumRegex = "01[016789]\\D\\d{3,4}\\D\\d{4}";
        String awsAccessKeyRegex = "(?<![A-Z0-9])[A-Z0-9]{20}(?![A-Z0-9])";
        String awsSecretAccessKeyRegex = "(?<![A-Za-z0-9/+=])[A-Za-z0-9/+=]{40}(?![A-Za-z0-9/+=])";
        String ipRegex = "(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])";

        for (int i = 0; i < codeData.size(); i++) {
            List<String> codeLines = codeData.get(i);
            String codeUrl = codeUrls.get(i);

            for (String codeLine : codeLines) {
                detectCount += detectSensitiveInfo(codeLine, regNumRegex, "Resident Registration Number", repoName, codeUrl, detectData.getReg(), detectedUrls);
                detectCount += detectSensitiveInfo(codeLine, callNumRegex, "Phone Number", repoName, codeUrl, detectData.getCall(), detectedUrls);
                detectCount += detectSensitiveInfo(codeLine, awsAccessKeyRegex, "AWS Access Key", repoName, codeUrl, detectData.getAwsAccess(), detectedUrls);
                detectCount += detectSensitiveInfo(codeLine, awsSecretAccessKeyRegex, "AWS Secret Key", repoName, codeUrl, detectData.getAwsSecret(), detectedUrls);
                detectCount += detectSensitiveInfo(codeLine, ipRegex, "IP Address", repoName, codeUrl, detectData.getIp(), detectedUrls);
            }
        }

        detectData.setDetectCount(detectCount);
        detectData.setAllCount(count);
        detectData.setDetectedUrls(detectedUrls); // 감지된 URL 리스트 설정

        logger.info("Code analysis completed for repo: {}. Total lines: {}, Detected count: {}", repoName, count, detectCount);

        return detectData;
    }

    private int detectSensitiveInfo(String codeLine, String regex, String infoType, String repoUrl, String filePath, List<Map<String, String>> detectList, List<Map<String, String>> detectedUrls) {
        int count = 0;
        Matcher matcher = Pattern.compile(regex).matcher(codeLine);
        while (matcher.find()) {
            String sensitiveInfo = matcher.group();
            
            Map<String, String> detectInfo = new HashMap<>();
            detectInfo.put("sensitiveInfo", sensitiveInfo);
            detectInfo.put("repoUrl", repoUrl);
            detectInfo.put("filePath", filePath);
            
            if (!detectList.contains(detectInfo)) {
                detectList.add(detectInfo);
                detectedUrls.add(detectInfo); // 감지된 URL을 리스트에 추가
                logger.info("Detected {} in file {}: {}", infoType, filePath, sensitiveInfo);
                count++;
            }
        }
        return count;
    }

    public List<Map<String, String>> getRepositories(String profileUrl) {
        String apiUrl = profileUrl.replace("https://github.com/", "https://api.github.com/users/") + "/repos";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, List.class);

        List<Map<String, String>> repoList = new ArrayList<>();
        for (Object object : response.getBody()) {
            Map<String, Object> repoMap = (Map<String, Object>) object;
            String repoName = (String) repoMap.get("name");
            String repoUrl = (String) repoMap.get("html_url");
            
            Map<String, String> repo = new HashMap<>();
            repo.put("name", repoName);
            repo.put("url", repoUrl);
            repoList.add(repo);
        }

        logger.info("Fetched repositories for profile URL {}: {}", profileUrl, repoList);

        return repoList;
    }

    public String getCodeForFile(String repoUrl, String filePath) {
        String apiUrl = repoUrl.replace("https://github.com", "https://api.github.com/repos") + "/contents/" + filePath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

        Map<String, String> codeMap = response.getBody();
        if (codeMap == null || codeMap.get("content") == null) {
            return null; // 코드 콘텐츠가 null인 경우 null 반환
        }

        String codeContent = codeMap.get("content");

        // Base64 디코딩 전에 코드 콘텐츠를 전처리합니다.
        codeContent = codeContent.replaceAll("[\\s]", ""); // 공백 문자 제거
        codeContent = codeContent.replaceAll("[^A-Za-z0-9+/=]", ""); // Base64 문자열이 아닌 문자 제거

        byte[] decodedBytes = Base64.getDecoder().decode(codeContent);
        return new String(decodedBytes);
    }

    public DetectData processRepository(String repoUrl, List<String> codeUrls) {
        List<List<String>> codeData = codeParser(repoUrl, codeUrls);
        String repoName = repoUrl.substring(repoUrl.lastIndexOf('/') + 1);
        return codeRegex(repoName, codeData, codeUrls);
    }
}
