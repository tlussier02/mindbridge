package com.digitaltherapy.mcp;

import com.digitaltherapy.service.AiService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {
    private final AiService aiService;

    public McpController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/tool/execute")
    public Map<String, Object> executeTool(@RequestBody Map<String, Object> request) {
        // Implement logic to route to specific AiService methods
        return Map.of("status", "success");
    }
}