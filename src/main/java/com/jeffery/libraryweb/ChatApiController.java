package com.jeffery.libraryweb;

import com.jeffery.libraryweb.dto.ChatRequest;
import com.jeffery.libraryweb.dto.ChatResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.AiHelpService;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {
    private final AiHelpService aiHelpService;

    public ChatApiController(AiHelpService aiHelpService) {
        this.aiHelpService = aiHelpService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest body, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"STUDENT".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String text = body != null && body.message() != null ? body.message() : "";
        String reply = aiHelpService.reply(user.getRole(), text);
        return ResponseEntity.ok(new ChatResponse(reply, aiHelpService.isAiEnabled()));
    }
}
