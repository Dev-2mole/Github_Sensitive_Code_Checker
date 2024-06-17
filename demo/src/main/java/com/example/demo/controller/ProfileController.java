package com.example.demo.controller;

import com.example.demo.model.DetectData;
import com.example.demo.service.GitService;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private GitService gitService;

    @Value("${github.token}")
    private String githubToken;

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("repoList", new ArrayList<>());
        model.addAttribute("profileUrl", "");
        return "profile";
    }

    @PostMapping("/profile")
    public String scanProfile(@RequestParam String profileUrl, Model model) throws JsonProcessingException {
        List<Map<String, String>> repoList = gitService.getRepositories(profileUrl);
        List<Map<String, Object>> scanResults = new ArrayList<>();

        for (Map<String, String> repo : repoList) {
            String repoName = repo.get("name");
            String repoUrl = repo.get("url");

            logger.info("Scanning repository: {}", repoName);

            List<String> codeUrls = gitService.gitParser(repoUrl);
            DetectData detectData = gitService.processRepository(repoUrl, codeUrls);

            Map<String, Object> scanResult = new HashMap<>();
            scanResult.put("name", repoName);
            scanResult.put("totalFiles", codeUrls.size());
            scanResult.put("totalLines", detectData.getAllCount());
            scanResult.put("detectCount", detectData.getDetectCount());
            scanResult.put("detectedUrls", detectData.getDetectedUrls());
            scanResult.put("detectedCodeMap", detectData.getDetectedCodeMap());
            scanResult.put("sensitiveData", detectData.getSensitiveData());

            logger.info("Scan result for {}: {}", repoName, scanResult);

            scanResults.add(scanResult);
        }

        logger.info("Final scan results: {}", scanResults);
        model.addAttribute("scanResults", scanResults);
        model.addAttribute("profileUrl", profileUrl);
        return "profile";
    }


    @PostMapping("/scan")
    @ResponseBody
    public List<Map<String, Object>> scanRepositories(@RequestParam String profileUrl) throws JsonProcessingException {
        List<Map<String, String>> repoList = gitService.getRepositories(profileUrl);
        List<Map<String, Object>> scanResults = new ArrayList<>();

        for (Map<String, String> repo : repoList) {
            String repoName = repo.get("name");
            String repoUrl = repo.get("url");

            logger.info("Scanning repository: {}", repoName);

            List<String> codeUrls = gitService.gitParser(repoUrl);
            DetectData detectData = gitService.processRepository(repoUrl, codeUrls);

            Map<String, Object> scanResult = new HashMap<>();
            scanResult.put("name", repoName);
            scanResult.put("totalFiles", codeUrls.size());
            scanResult.put("totalLines", detectData.getAllCount());
            scanResult.put("detectCount", detectData.getDetectCount());
            scanResult.put("detectedUrls", detectData.getDetectedUrls());
            scanResult.put("detectedCodeMap", detectData.getDetectedCodeMap());
            scanResult.put("sensitiveData", detectData.getSensitiveData());

            logger.info("Scan result for {}: {}", repoName, scanResult);

            scanResults.add(scanResult);
        }

        return scanResults;
    }

    @PostMapping("/code")
    @ResponseBody
    public String getCodeSnippet(@RequestParam String repoName, @RequestParam String filePath) {
        String repoUrl = "https://github.com/your-username/" + repoName; // 실제 레포지토리 URL로 대체해야 합니다.
        String apiUrl = repoUrl.replace("https://github.com", "https://api.github.com/repos") + "/contents/" + filePath;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

        Map<String, String> codeMap = response.getBody();
        String codeContent = codeMap.get("content");
        byte[] decodedBytes = Base64.getDecoder().decode(codeContent);
        return new String(decodedBytes);
    }

    private String highlightSensitiveInfo(String codeSnippet, String sensitiveInfo) {
        return codeSnippet.replace(sensitiveInfo, "<span style='color: red;'>" + sensitiveInfo + "</span>");
    }
}
