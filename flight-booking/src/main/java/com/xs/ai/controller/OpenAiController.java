package com.xs.ai.controller;

import com.xs.ai.services.BookingTools;
import com.xs.ai.services.MyLoggingAdvisor;
import org.springaicommunity.agent.tools.*;
import org.springaicommunity.agent.utils.AgentEnvironment;
import org.springaicommunity.agent.utils.CommandLineQuestionHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

/**
 * @author 李佳俊
 * @version 1.0
 * @description: 图灵航空智能客服系统
 */
@RestController
@CrossOrigin
public class OpenAiController {

    private final ChatClient chatClient;



    public OpenAiController(
            ChatMemory chatMemory,
            // 内部 tools
            BookingTools bookingTools,
            // 外部 mcp tools
            ToolCallbackProvider mcpTools,
            DeepSeekChatModel chatModel,
            VectorStore vectorStore,
            @Value("${agent.skills.dirs:Unknown}") List<Resource> agentSkillsDirs,
            @Value("${agent.memory.dir}") String memoryDir
    ) {

        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                          ##角色
                          您是“图灵”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                          您正在通过在线聊天系统与客户互动。
                          ##要求
                          1.在提供有关预订航班或取消预订的信息之前，您必须始终从用户处获取以下信息：预订号、客户姓名。
                          2.在涉及增删改（除了查询）function-call前，必须等用户回复“确认”后再调用tool。
                          3.一定记得在处理了长期记忆后记得同时处理MEMORY.md
                          4.请讲中文。
                        5.当用户请求不在航空业务范围内（如 GitHub、代码、开发者工具），如果可用工具能完成该请求，请直接调用工具协助，不必拘泥于航空客服角色。
                        
                          今天的日期是 {current_date}.
                        """)
                .defaultSystem(p -> p.param("MEMORIES_ROOT_DIERCTORY", memoryDir))
                .defaultAdvisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .order(BaseAdvisor.HIGHEST_PRECEDENCE + 100)
                                .build(),
                        ToolCallingAdvisor.builder()
                                .advisorOrder(BaseAdvisor.HIGHEST_PRECEDENCE + 300).build(),
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .order(BaseAdvisor.HIGHEST_PRECEDENCE + 400).build(),
                        MyLoggingAdvisor.builder()
                                .order(BaseAdvisor.HIGHEST_PRECEDENCE + 500)
                                .showAvailableTools(true)
                                .showSystemMessage(true)
                                .build()
                )
                .defaultTools(bookingTools,

                        AutoMemoryTools.builder()
                                .memoriesDir(memoryDir)          // from application.properties
                                .build(),
                            // Skills tool
                            SkillsTool.builder().addSkillsResources(agentSkillsDirs).build(),

                            //Bash execution tool
                            ShellTools.builder().build(),// built-in shell tools
                            // Read, Write and Edit files tool
                            FileSystemTools.builder().build(),// built-in file system tools
                            // Smart web fetch tool
                            SmartWebFetchTool.builder(chatClientBuilder.clone().build()).build()
                            // Brave web search tool
                            // BraveWebSearchTool.builder(System.getenv("BRAVE_API_KEY")).resultCount(15).build()


                         )
                .defaultToolCallbacks(mcpTools)
				 // 老版本.defaultFunctions("getBookingDetails", "changeBooking", "cancelBooking") // FUNCTION CALLING
				.build();
	}


    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(@RequestParam(value = "message", defaultValue = "讲个笑话") String message,
                                               @RequestParam(value = "userId", required = false, defaultValue = "default") String userId) {

        Flux<String> content = chatClient.prompt()
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .user(message)

                .stream()
                .content();

        return  content
                .concatWith(Flux.just("[complete]"));

    }



}
