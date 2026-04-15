package org.example.deliveryofrolls.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPickupPointController {

    @GetMapping("/pickup-points")
    public String pickupPoints( Model model) {

        model.addAttribute("pageTitle", "Управление точками самовывоза");
        model.addAttribute("pageCss", "pickup-points.css");
        return "admin/pickup-points";
    }
}
