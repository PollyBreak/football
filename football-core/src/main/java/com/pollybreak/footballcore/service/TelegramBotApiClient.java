package com.pollybreak.footballcore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pollybreak.footballcore.config.TelegramBotProperties;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class TelegramBotApiClient {

    private final TelegramBotProperties properties;
    private final ObjectMapper objectMapper;

    public JsonNode getMe() {
        return call("getMe", Map.of());
    }

    public JsonNode getChat(Long chatId) {
        return call("getChat", Map.of("chat_id", chatId));
    }

    public JsonNode getChatMember(Long chatId, Long userId) {
        return call("getChatMember", Map.of("chat_id", chatId, "user_id", userId));
    }

    public JsonNode sendMessage(Long chatId, String text, List<List<Map<String, String>>> inlineKeyboard) {
        return call("sendMessage", Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML",
                "disable_web_page_preview", true,
                "reply_markup", Map.of("inline_keyboard", inlineKeyboard)
        ));
    }

    public JsonNode editMessageText(Long chatId, Long messageId, String text, List<List<Map<String, String>>> inlineKeyboard) {
        return call("editMessageText", Map.of(
                "chat_id", chatId,
                "message_id", messageId,
                "text", text,
                "parse_mode", "HTML",
                "disable_web_page_preview", true,
                "reply_markup", Map.of("inline_keyboard", inlineKeyboard)
        ));
    }

    public @Nullable JsonNode tryEditMessageText(Long chatId, Long messageId, String text, List<List<Map<String, String>>> inlineKeyboard) {
        try {
            return editMessageText(chatId, messageId, text, inlineKeyboard);
        } catch (HttpClientErrorException exception) {
            if (exception.getResponseBodyAsString().contains("message is not modified")) {
                return objectMapper.createObjectNode();
            }
            return null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public void answerCallbackQuery(String callbackQueryId, String text, boolean showAlert, String url) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("callback_query_id", callbackQueryId);
        payload.put("text", text);
        payload.put("show_alert", showAlert);
        if (url != null && !url.isBlank()) {
            payload.put("url", url);
        }
        call("answerCallbackQuery", payload);
    }

    private JsonNode call(String method, Map<String, ?> payload) {
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            throw new IllegalStateException("Telegram bot token is not configured");
        }

        JsonNode response = RestClient.create()
                .post()
                .uri("https://api.telegram.org/bot" + properties.getToken() + "/" + method)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("ok").asBoolean(false)) {
            throw new IllegalStateException("Telegram Bot API error: " + objectMapper.valueToTree(response));
        }
        return response.path("result");
    }
}
