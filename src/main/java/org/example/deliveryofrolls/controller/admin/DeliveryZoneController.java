package org.example.deliveryofrolls.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/delivery-zones")
public class DeliveryZoneController {

    @GetMapping
    public String deliveryZonesPage(Model model) {
        model.addAttribute("pageTitle", "Управление зонами доставки");
        model.addAttribute("pageCss", "delivery-zones.css");
        return "admin/delivery-zones";
    }
}
