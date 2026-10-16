package com.server.zelotest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class CostAdvisor implements BaseAdvisor {

    private final int order;
    private final double inputPer1m;
    private final double outputPer1m;

    // 요청이 나갈 때
    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        return request;
    }

    // 응답이 들어올 때
    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        // 사용량을 주지 않을 때 넘어감
        var chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return  response;
        }
        Usage usage = chatResponse.getMetadata().getUsage();
        if (usage instanceof EmptyUsage) {
            return response;
        }

        double cost = (usage.getPromptTokens() * inputPer1m
                + usage.getCompletionTokens() * outputPer1m) / 1_000_000.0;

        log.info("LLM 호출 — 입력={}, 출력={}, 합={}, 비용=${}",
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens(),
                String.format("%.6f", cost));

        var output = Objects.requireNonNull(response.chatResponse().getResult()).getOutput();

        if (output.hasToolCalls()){
            output.getToolCalls().forEach(toolCall -> {
                log.info("LLM이 사용한 도구 이름: {}, 인자: {} "
                        , toolCall.name(), toolCall.arguments());
            });
        }
        return response;
    }

    @Override
    public int getOrder() {return order;}
}
