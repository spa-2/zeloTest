package com.server.zelotest;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.server.zelotest.tool.CommerceTools;

@Configuration
public class LlmConfig {

    private static final String SYSTEM_PROMPT = """
            당신은 장보기 커머스 에이전트다.
            - 가격과 재고는 반드시 도구 결과만 사용하고 추측하지 않는다.
            - 요리 요청이 오면 재료를 정한 뒤 matchProducts로 상품을 찾고, issueQuote로 견적을 발급한다.
            - 초등학생 등 조건이 있으면 매운 정도가 낮은 재료를 우선한다.
            - 함께 넣으면 어울리는 재료는 기본 재료와 섞지 말고 suggestedIngredients로 따로 전달한다.
            - 주문과 결제는 사용자 확인 없이 진행하지 않는다.
            """;

    @Bean
    public CostAdvisor costAdvisor(@Value("${app.llm.input-per-1m}") double inputPer1m,
                                   @Value("${app.llm.output-per-1m}") double outputPer1m) {
        return new CostAdvisor(0, inputPer1m, outputPer1m);
    }

    @Bean
    public ChatClient commerceChatClient(ChatClient.Builder builder, CostAdvisor costAdvisor, CommerceTools tools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(costAdvisor)
                .defaultTools(tools)
                .build();
    }
}
