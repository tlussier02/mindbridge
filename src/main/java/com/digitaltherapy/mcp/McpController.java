package com.digitaltherapy.mcp;

import com.digitaltherapy.service.AiService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
            new McpTool("crisis_lookup", "Retrieves emergency safety protocols")
        );
    }
<<<<<<< HEAD

    public record McpTool(String name, String description) {}
=======
>>>>>>> 2bb2ef62b9902fd4c36412ff39432e6f45bb2bf3
}