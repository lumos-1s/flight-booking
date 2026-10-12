package com.xs.ai;

// 阿里百炼 starter 已在 pom 中注释（教程改用 Spring AI 官方 deepseek/openai starter），相关 import 暂时停用
// import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgent;
// import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgentOptions;
// import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest
class SpringAiDemoApplicationTests {

    @Test
    void contextLoads(@Value("classpath:rag/terms-of-service.txt") Resource resource) {
        TextReader textReader = new TextReader(resource);

        List<Document> documents = textReader.read();
        for (Document document : documents) {
            System.out.println(document.getText());
        }
    }


    // 以下方法依赖阿里 DashScopeAgent，当前 pom 已移除阿里 starter，暂时停用，后续需要时再打开
    // @Test
    // public void testApplicationAgent(@Autowired DashScopeAgentApi dashscopeAgentApi
    //                                      )     {
    //     String message="规划长沙到武汉的驾车路线";
    //     String appId="97f814812b6543fca0aeeda0eee3c64f";
    //
    //     DashScopeAgent agent=new DashScopeAgent(dashscopeAgentApi);
    //
    //     Prompt prompt = new Prompt(message,
    //             DashScopeAgentOptions.builder()
    //                     .withIncrementalOutput(true)
    //                     .withAppId(appId).build());
    //
    //     Flux<ChatResponse> stream = agent.stream(prompt);
    //
    //
    //
    //     stream.subscribe(
    //             chatResponse -> System.out.println(chatResponse.getResult().getOutput().getText()),
    //             throwable -> System.out.println(throwable.getMessage()),
    //             () -> System.out.println("complete"));
    //
    //     stream.blockLast();
    // }

}
