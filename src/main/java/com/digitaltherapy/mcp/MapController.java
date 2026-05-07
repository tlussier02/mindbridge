package com.digitaltherapy.mcp;

import com.digitaltherapy.service.AiService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {
    private final AiService aiService;

    public McpController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/tools")
    public List<McpTool> listTools() {
        return List.of(
            new McpTool("cbt_helper", "Accesses CBT techniques for burnout"),
            new McpTool("crisis_lookup", "Retrieves emergency safety protocols"),
            new McpTool("thought_analyzer", "Analyzes diary entries for cognitive distortions"),
            new McpTool("resource_locator", "Finds nearby mental health clinics and support groups")
        );
    }

    @GetMapping("/prompts")
    public List<McpPrompt> listPrompts() {
        return List.of(
            new McpPrompt("daily_checkin", "A starter for daily reflection", "How are you feeling today? Would you like to record a diary entry?"),
            new McpPrompt("distortion_check", "Help identifying negative thoughts", "I noticed some strong language in your last entry. Shall we look for cognitive distortions together?")
        );
    }

    @PostMapping("/tools/call")
    public Object callTool(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        Map<String, Object> args = (Map<String, Object>) request.get("arguments");
        
        if ("thought_analyzer".equals(name)) {
            return aiService.analyzeThought(args.get("text").toString());
        }
        return "Tool execution logic not found";
    }
}