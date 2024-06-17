package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.DetectData;
import com.example.demo.service.GitService;

@Controller
public class MainController {

    @Autowired
    private GitService gitService;

    @GetMapping("/")
    public String main(Model model) {
        model.addAttribute("detectData", new DetectData());
        model.addAttribute("repo", new String[]{null, null});
        model.addAttribute("detectCount", null);
        model.addAttribute("allCount", null);
        model.addAttribute("userUrl", null);
        return "index";
    }

    // @PostMapping("/scan")
    // public String scan(@RequestParam String userUrl, Model model) {
    //     String[] repoName = userUrl.replace("https://github.com/", "").split("/");
    //     List<String> codeUrls = gitService.gitParser(userUrl);
    //     List<List<String>> codeData = gitService.codeParser(userUrl, codeUrls);
    //     DetectData detectData = gitService.codeRegex(codeData, codeUrls);

    //     model.addAttribute("detectData", detectData);
    //     model.addAttribute("repo", repoName);
    //     model.addAttribute("detectCount", detectData.getDetectCount());
    //     model.addAttribute("allCount", detectData.getAllCount());
    //     model.addAttribute("userUrl", userUrl);

    //     return "index";
    // }
}