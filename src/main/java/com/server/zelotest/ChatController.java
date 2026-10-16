package com.server.zelotest;

import com.server.zelotest.tool.CommerceTools;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 프롬프트를 바꿔가며 도구 호출 시나리오를 수동으로 확인하는 개발용 엔드포인트. chat.http로 호출한다.
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;
    private final CommerceTools tools;

    public record ChatRequest(@NotBlank String message) {
    }

    public record ChatResponse(String answer, List<CommerceTools.ToolCall> toolCalls) {
    }

    // 도구 호출 기록이 인스턴스 하나에 쌓이므로 요청을 동시에 보내면 기록이 섞인다.
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        tools.clear();
        String answer = chatClient.prompt().user(request.message()).call().content();
        return ResponseEntity.ok(new ChatResponse(answer, tools.calls()));
    }
}
