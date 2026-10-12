package com.lumos.ai.controller;

import com.lumos.ai.services.KnowledgeBaseService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库管理后台 REST 接口
 */
@RestController
@CrossOrigin
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    public KnowledgeBaseController(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    /** 知识库统计 */
    @GetMapping("/kb/stats")
    public Map<String, Object> stats() {
        return kbService.stats();
    }

    /** 全部分块列表 */
    @GetMapping("/kb/documents")
    public List<Map<String, Object>> documents() {
        return kbService.getChunks().stream()
                .map(d -> Map.<String, Object>of("id", d.getId(), "content", d.getText()))
                .toList();
    }

    /** 检索测试 */
    @GetMapping("/kb/search")
    public List<Map<String, Object>> search(@RequestParam("q") String q,
                                            @RequestParam(value = "topK", defaultValue = "3") int topK) {
        return kbService.search(q, topK).stream()
                .map(d -> Map.<String, Object>of("id", d.getId(), "content", d.getText()))
                .toList();
    }

    /** 新增知识 */
    @PostMapping("/kb/add")
    public Map<String, Object> add(@RequestBody Map<String, String> body) throws Exception {
        kbService.addEntry(body.get("content"));
        return Map.of("ok", true);
    }
}