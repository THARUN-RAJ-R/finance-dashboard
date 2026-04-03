package com.finance.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Root controller to redirect the base URL to Swagger UI.
 * This provides a better user experience for anyone visiting the main Render URL.
 */
@Controller
public class RootController {

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui/index.html";
    }
}
