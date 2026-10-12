package com.xs.ai;

import com.xs.ai.services.BookingTools;
import com.xs.ai.services.MyLoggingAdvisor;
import org.springaicommunity.agent.tools.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * @author xushu
 * @version 1.0.0
 * @description 图灵航空智能客服系统
 */
@SpringBootApplication
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }



    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
    }



    CommandLineRunner commandLineRunner( ChatMemory chatMemory,
                                         // 内部 tools
                                         BookingTools bookingTools,
                                         // 外部 mcp tools
                                         ToolCallbackProvider mcpTools,
                                         DeepSeekChatModel chatModel,
                                         @Value("${agent.skills.dirs:Unknown}") List<Resource> agentSkillsDirs,
                                         @Value("${agent.memory.dir}") String memoryDir
    ) {

        return args -> {
            ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
            ChatClient chatClient = chatClientBuilder
                        .defaultSystem("""
                          ##角色
                          您是“图灵”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                          您正在通过在线聊天系统与客户互动。
                          ##要求
                          1.在提供有关预订航班或取消预订的信息之前，您必须始终从用户处获取以下信息：预订号、客户姓名。
                          2.在涉及增删改（除了查询）function-call前，必须等用户回复“确认”后再调用tool。
                          3.优先读取{MEMORIES_ROOT_DIERCTORY}记忆
                          4.请讲中文。
                          
                          今天的日期是 {current_date}.
                        """)
                        .defaultSystem(p -> p.param("MEMORIES_ROOT_DIERCTORY", memoryDir))
                        .defaultAdvisors(

                                ToolCallingAdvisor.builder().build(),
                                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                MyLoggingAdvisor.builder()
                                        .showAvailableTools(true)
                                        .showSystemMessage(true)
                                        .build()
                        )
                        .defaultTools(bookingTools,
                                // Skills tool
                                SkillsTool.builder().addSkillsResources(agentSkillsDirs).build(),

                                //Bash execution tool
                                ShellTools.builder().build(),// built-in shell tools
                                // Read, Write and Edit files tool
                                FileSystemTools.builder().build(),// built-in file system tools
                                // Smart web fetch tool
                                SmartWebFetchTool.builder(chatClientBuilder.clone().build()).build(),
                                // Brave web search tool
                                // BraveWebSearchTool.builder(System.getenv("BRAVE_API_KEY")).resultCount(15).build()


                                AutoMemoryTools.builder()
                                        .memoriesDir(memoryDir)          // from application.properties
                                        .build()
                        )
                        .defaultToolCallbacks(mcpTools)
                        // 老版本.defaultFunctions("getBookingDetails", "changeBooking", "cancelBooking") // FUNCTION CALLING
                        .build();



        };

    }
    @Bean
    public VectorStore vectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel){
        return SimpleVectorStore.builder(embeddingModel).build();
    }
    @Bean
    public CommandLineRunner loadRagDocs(VectorStore vectorStore){
        return args -> {
            Resource resource = new ClassPathResource("rag/terms-of-service.txt");
            TextReader reader = new TextReader(resource);
            TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                    .withChunkSize(250)          // 每块 250 token
                    .withMinChunkSizeChars(100)  // 最短块 100 字符
                    .withMinChunkLengthToEmbed(50) // 少于 50 字符不入库

                    .withKeepSeparator(true)     // 保留分隔符
                    .build();


            List<Document> docs = tokenTextSplitter.apply(reader.get());
            vectorStore.add(docs);
            System.out.println(">>> RAG知识库加载完成，共" + docs.size() + "个分块");
        };
    }


}
