package com.lumos.ai.services;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理服务：启动时加载 + 提供 统计/分块列表/检索/新增/重载
 */
@Service
public class KnowledgeBaseService implements CommandLineRunner {

    private final VectorStore vectorStore;
    private List<Document> chunks = new ArrayList<>();

    public KnowledgeBaseService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /** 启动时自动加载知识库（替代原 SpringAiDemoApplication 里的 loadRagDocs） */
    @Override
    public void run(String... args) throws Exception {
        reload();
    }

    /** 从 rag/terms-of-service.txt 重新切片入库 */
    public void reload() {
        Resource resource = new ClassPathResource("rag/terms-of-service.txt");
        TextReader reader = new TextReader(resource);
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(250)           // 每块 250 token
                .withMinChunkSizeChars(100)   // 最短块 100 字符
                .withMinChunkLengthToEmbed(50) // 少于 50 字符不入库
                .withKeepSeparator(true)      // 保留分隔符
                .build();

        List<Document> docs = splitter.apply(reader.get());

        // 清空旧数据再入库（避免重复；首次启动 chunks 为空，跳过 delete 少一次 embedding 调用）
        if (!chunks.isEmpty()) {
            vectorStore.delete(chunks.stream().map(Document::getId).toList());
        }
        vectorStore.add(docs);
        chunks = docs;
        System.out.println(">>> RAG知识库加载完成，共" + docs.size() + "个分块");
    }

    /** 知识库统计 */
    public Map<String, Object> stats() {
        int totalChars = chunks.stream().mapToInt(d -> d.getText().length()).sum();
        return Map.of(
                "chunkCount", chunks.size(),
                "totalChars", totalChars
        );
    }

    /** 全部分块（管理后台列表用） */
    public List<Document> getChunks() {
        return chunks;
    }

    /** 检索测试：query → topK 个命中分块 */
    public List<Document> search(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        return vectorStore.similaritySearch(request);
    }

    /** 新增一条知识：入库 + 追加到知识库文件（持久化，重启后 reload 仍保留） */
    public void addEntry(String content) throws Exception {
        // 1) 追加到源文件（下次重启 reload 会包含它）
        Resource resource = new ClassPathResource("rag/terms-of-service.txt");
        Path file = resource.getFile().toPath();
        Files.writeString(file, "\n" + content, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        // 2) 立即入库
        Document doc = new Document(content);
        vectorStore.add(List.of(doc));
        chunks.add(doc);
    }
}