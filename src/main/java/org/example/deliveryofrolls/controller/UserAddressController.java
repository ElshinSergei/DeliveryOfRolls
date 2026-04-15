package org.example.deliveryofrolls.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.UserAddressDTO;
import org.example.deliveryofrolls.entity.User;
import org.example.deliveryofrolls.service.UserAddressService;
import org.example.deliveryofrolls.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/profile/addresses")
@RequiredArgsConstructor
@Slf4j
public class UserAddressController {

    private final UserAddressService addressService;
    private final UserService userService;

    @GetMapping
    public String addressesPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails);

        model.addAttribute("user", user);
        model.addAttribute("addresses", addressService.getUserAddresses(user));
        model.addAttribute("pageTitle", "Мои адреса");
        model.addAttribute("pageCss", "profile.css");
        model.addAttribute("pageCss", "addresses.css");
        return "profile/addresses";
    }

    @GetMapping("/new")
    public String newAddressForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getCurrentUser(userDetails);

        model.addAttribute("user", user);
        model.addAttribute("address", new UserAddressDTO());
        model.addAttribute("pageTitle", "Новый адрес");
        model.addAttribute("pageCss", "addresses.css");
        return "profile/address-form";
    }

    @GetMapping("/{id}/edit")
    public String editAddressForm(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        User user = userService.getCurrentUser(userDetails);
        model.addAttribute("user", user);
        model.addAttribute("address", addressService.getAddress(id, user));
        model.addAttribute("pageTitle", "Редактирование адреса");
        model.addAttribute("pageCss", "addresses.css");
        return "profile/address-form";
    }

    @PostMapping("/save")
    public String saveAddress(@ModelAttribute UserAddressDTO addressDTO,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser(userDetails);
            addressService.saveAddress(addressDTO, user);
            redirectAttributes.addFlashAttribute("success", "Адрес сохранен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/profile/addresses";
    }

    @PostMapping("/{id}/delete")
    public String deleteAddress(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser(userDetails);
            addressService.deleteAddress(id, user);
            redirectAttributes.addFlashAttribute("success", "Адрес удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/profile/addresses";
    }

    @PostMapping("/{id}/set-default")
    public String setDefaultAddress(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getCurrentUser(userDetails);
            addressService.setDefaultAddress(id, user);
            redirectAttributes.addFlashAttribute("success", "Основной адрес обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/profile/addresses";
    }

    /**
     * Получить все адреса пользователя (для checkout.js)
     */
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<?> getUserAddressesApi(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(List.of());
        }

        try {
            User user = userService.getCurrentUser(userDetails);
            List<UserAddressDTO> addresses = addressService.getUserAddresses(user);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            log.error("Ошибка загрузки адресов", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Сохранить адрес из корзины/оформления заказа
     */
    @PostMapping("/api/save")
    @ResponseBody
    public ResponseEntity<?> saveAddressFromCheckout(@RequestBody Map<String, Object> request,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Необходимо авторизоваться"));
        }

        try {
            User user = userService.getCurrentUser(userDetails);

            String address = (String) request.get("address");
            String fullAddress = (String) request.get("fullAddress");
            String entrance = (String) request.getOrDefault("entrance", "");
            String floor = (String) request.getOrDefault("floor", "");
            String apartment = (String) request.getOrDefault("apartment", "");
            String comment = (String) request.getOrDefault("comment", "");
            Boolean isDefault = (Boolean) request.getOrDefault("isDefault", false);

            // Используем полный адрес, если передан, иначе address
            String finalAddress = fullAddress != null ? fullAddress : address;

            if (finalAddress == null || finalAddress.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Адрес обязателен"));
            }

            UserAddressDTO addressDTO = new UserAddressDTO();
            addressDTO.setAddress(finalAddress);
            addressDTO.setEntrance(entrance);
            addressDTO.setFloor(floor);
            addressDTO.setApartment(apartment);
            addressDTO.setComment(comment);
            addressDTO.setDefault(isDefault);

            addressService.saveAddress(addressDTO, user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Адрес сохранен в избранное"
            ));

        } catch (Exception e) {
            log.error("Ошибка сохранения адреса", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Проверить, существует ли адрес в избранном
     */
    @GetMapping("/api/check")
    @ResponseBody
    public ResponseEntity<?> checkAddressExists(@RequestParam String address,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("exists", false));
        }

        try {
            User user = userService.getCurrentUser(userDetails);
            boolean exists = addressService.addressExists(user, address);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("exists", false));
        }
    }
}
