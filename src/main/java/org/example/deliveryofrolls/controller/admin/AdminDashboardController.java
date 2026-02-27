package org.example.deliveryofrolls.controller.admin;

import lombok.RequiredArgsConstructor;
import org.example.deliveryofrolls.dto.DashboardStats;
import org.example.deliveryofrolls.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public String dashboard(Model model) {

        DashboardStats stats = dashboardService.getStats();

        model.addAttribute("stats", stats);
        model.addAttribute("pageTitle", "Панель управления");

        return "admin/dashboard";
    }
}
