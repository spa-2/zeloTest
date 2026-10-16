package com.server.zelotest.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

// 실제 로직 없이 시나리오 고정 응답만 돌려주는 도구 목업. 도구 호출 여부와 인자 검증이 목적이다.
@Slf4j
@Component
public class CommerceTools {

    public record IngredientRequest(String name, String category, Integer amount, String unit) {
    }

    public record ProductMatch(String ingredient, String productId, String productName,
                               Integer price, Integer quantity, String reason) {
    }

    public record MatchProductsResult(List<ProductMatch> products, List<ProductMatch> suggestions) {
    }

    public record QuoteItemRequest(String productId, Integer quantity) {
    }

    public record QuoteLine(String productName, Integer quantity, Integer price) {
    }

    public record QuoteResult(String quoteId, List<QuoteLine> lines, Integer totalPrice, Integer expiresInMinutes) {
    }

    public record ToolCall(String name, Object args) {
    }

    private static final Map<String, ProductMatch> CATALOG = Map.of(
            "P-WHITE-KIMCHI", new ProductMatch("김치", "P-WHITE-KIMCHI", "백김치 1kg", 8900, 1, "맵지 않다는 리뷰가 많음"),
            "P-RICE-COOKED", new ProductMatch("밥", "P-RICE-COOKED", "즉석밥 4개", 5600, 1, "판매량 상위"),
            "P-GREEN-ONION", new ProductMatch("대파", "P-GREEN-ONION", "국산 대파 1단", 2800, 1, "국산 우선"),
            "P-EGG-10", new ProductMatch("계란", "P-EGG-10", "계란 10구", 4200, 1, "필요량 4개 → 10구 1개"),
            "P-RICE-2KG", new ProductMatch("쌀", "P-RICE-2KG", "쌀 2kg", 9900, 1, "선택 재료"),
            "P-TUNA", new ProductMatch("참치", "P-TUNA", "참치캔 3개", 6900, 1, "함께 넣으면 어울림"));

    private final List<ToolCall> calls = new CopyOnWriteArrayList<>();

    @Tool(description = "재료 목록을 실제 판매 상품으로 매칭한다. 품절·주문불가 상품은 제외하고, 필요량을 상품 수량으로 환산한다. "
            + "견적 발급 전에 반드시 먼저 호출해야 하며, 가격과 재고는 이 도구 결과만 사용한다.")
    public MatchProductsResult matchProducts(
            @ToolParam(description = "메뉴를 만들기 위한 기본 재료") List<IngredientRequest> ingredients,
            @ToolParam(description = "함께 넣으면 어울리는 추천 재료 후보. 기본 재료와 섞지 않는다", required = false)
            List<IngredientRequest> suggestedIngredients,
            @ToolParam(description = "사용자 조건. 예: 초등학생 동반, 예산 30000원", required = false) String conditions) {
        record(new ToolCall("matchProducts", List.of(ingredients, nullToEmpty(suggestedIngredients), String.valueOf(conditions))));
        List<ProductMatch> products = new ArrayList<>();
        for (String id : List.of("P-WHITE-KIMCHI", "P-RICE-COOKED", "P-GREEN-ONION", "P-EGG-10", "P-RICE-2KG")) {
            products.add(CATALOG.get(id));
        }
        return new MatchProductsResult(products, List.of(CATALOG.get("P-TUNA")));
    }

    @Tool(description = "매칭된 상품으로 견적을 발급한다. 현재 가격과 재고를 확인하고 합계를 계산한다. 주문 전에 사용자에게 견적 확인을 받는 용도다.")
    public QuoteResult issueQuote(
            @ToolParam(description = "matchProducts 결과의 productId와 수량") List<QuoteItemRequest> items) {
        record(new ToolCall("issueQuote", items));
        List<QuoteLine> lines = items.stream()
                .map(item -> {
                    ProductMatch product = CATALOG.get(item.productId());
                    return new QuoteLine(product.productName(), item.quantity(), product.price() * item.quantity());
                })
                .toList();
        int total = lines.stream().mapToInt(QuoteLine::price).sum();
        return new QuoteResult("Q1", lines, total, 10);
    }

    public List<ToolCall> calls() {
        return List.copyOf(calls);
    }

    public void clear() {
        calls.clear();
    }

    private void record(ToolCall call) {
        calls.add(call);
        log.info("도구 실행: {}, 인자: {}", call.name(), call.args());
    }

    private static List<IngredientRequest> nullToEmpty(List<IngredientRequest> list) {
        return list == null ? List.of() : list;
    }
}
