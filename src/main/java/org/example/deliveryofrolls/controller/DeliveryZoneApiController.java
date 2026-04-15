package org.example.deliveryofrolls.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.ClientDeliveryZoneResponse;
import org.example.deliveryofrolls.dto.DeliveryZoneRequest;
import org.example.deliveryofrolls.dto.DeliveryZoneResponse;
import org.example.deliveryofrolls.entity.DeliveryZone;
import org.example.deliveryofrolls.repository.DeliveryZoneRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/delivery-zones")
@RequiredArgsConstructor
public class DeliveryZoneApiController {

    private final DeliveryZoneRepository deliveryZoneRepository;

    // ========== ПУБЛИЧНЫЕ ЭНДПОИНТЫ ДЛЯ КЛИЕНТА ==========

    /**
     * Получить все активные зоны для клиентской части
     */
    @GetMapping("/active")
    public ResponseEntity<List<ClientDeliveryZoneResponse>> getActiveZones() {
        log.info("Запрос на получение активных зон доставки");

        List<DeliveryZone> zones = deliveryZoneRepository.findByActiveTrueOrderByDisplayOrderAsc();

        return ResponseEntity.ok(zones.stream()
                .map(this::mapToClientResponse)
                .toList());
    }

    /**
     * Проверка вхождения точки в зону доставки
     */
    @PostMapping("/check")
    public ResponseEntity<DeliveryZoneResponse> checkPoint(@RequestBody PointCheckRequest request) {
        log.info("Проверка точки: lat={}, lng={}", request.getLat(), request.getLng());

        try {
            List<DeliveryZone> zones = deliveryZoneRepository.findByActiveTrueOrderByDisplayOrderAsc();
            double[] point = new double[]{request.getLng(), request.getLat()};

            for (DeliveryZone zone : zones) {
                if (zone.getPoints() != null && zone.getPoints().size() >= 3) {
                    if (isPointInPolygon(point, zone.getPoints())) {
                        log.info("Точка в зоне: {}", zone.getName());
                        return ResponseEntity.ok(mapToResponse(zone));
                    }
                }
            }

            log.info("Точка вне зон доставки");
            return ResponseEntity.ok(null);

        } catch (Exception e) {
            log.error("Ошибка проверки точки: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ========== АДМИНСКИЕ ЭНДПОИНТЫ ==========

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryZoneResponse>> getAllZones() {
        log.info("Запрос всех зон (админ)");

        List<DeliveryZone> zones = deliveryZoneRepository.findAllByOrderByDisplayOrderAsc();

        return ResponseEntity.ok(zones.stream()
                .map(this::mapToResponse)
                .toList());
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryZoneResponse> getZoneById(@PathVariable Long id) {
        log.info("Запрос зоны id: {}", id);
        return ResponseEntity.ok(mapToResponse(findZoneById(id)));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryZoneResponse> createZone(@Valid @RequestBody DeliveryZoneRequest request) {
        log.info("Создание зоны: {}", request.getName());

        // Проверка уникальности имени
        if (deliveryZoneRepository.findAll().stream()
                .anyMatch(zone -> zone.getName().equalsIgnoreCase(request.getName()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Зона с названием '" + request.getName() + "' уже существует");
        }

        DeliveryZone zone = new DeliveryZone();
        zone.setName(request.getName());
        zone.setColor(request.getColor());
        zone.setBorderColor(request.getBorderColor());
        zone.setFillOpacity(request.getFillOpacity());
        zone.setMinOrder(request.getMinOrder());
        zone.setDeliveryTime(request.getDeliveryTime());
        zone.setPoints(request.getPoints());
        zone.setActive(true);

        // Устанавливаем порядок отображения
        Integer maxOrder = deliveryZoneRepository.findAll().stream()
                .map(DeliveryZone::getDisplayOrder)
                .max(Integer::compareTo)
                .orElse(0);
        zone.setDisplayOrder(maxOrder + 1);

        DeliveryZone savedZone = deliveryZoneRepository.save(zone);
        log.info("Зона создана id: {}", savedZone.getId());

        return ResponseEntity.ok(mapToResponse(savedZone));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryZoneResponse> updateZone(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryZoneRequest request) {
        log.info("Обновление зоны id: {}", id);

        DeliveryZone zone = findZoneById(id);

        // Проверка уникальности имени
        if (!zone.getName().equalsIgnoreCase(request.getName())) {
            boolean nameExists = deliveryZoneRepository.findAll().stream()
                    .anyMatch(z -> z.getName().equalsIgnoreCase(request.getName()));
            if (nameExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Зона с названием '" + request.getName() + "' уже существует");
            }
        }

        zone.setName(request.getName());
        zone.setColor(request.getColor());
        zone.setBorderColor(request.getBorderColor());
        zone.setFillOpacity(request.getFillOpacity());
        zone.setMinOrder(request.getMinOrder());
        zone.setDeliveryTime(request.getDeliveryTime());
        zone.setPoints(request.getPoints());
        if (request.getActive() != null) {
            zone.setActive(request.getActive());
        }

        DeliveryZone updatedZone = deliveryZoneRepository.save(zone);
        log.info("Зона id: {} обновлена", id);

        return ResponseEntity.ok(mapToResponse(updatedZone));
    }

    @PatchMapping("/admin/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryZoneResponse> toggleZoneActive(@PathVariable Long id) {
        log.info("Переключение статуса зоны id: {}", id);

        DeliveryZone zone = findZoneById(id);
        zone.setActive(!zone.getActive());

        DeliveryZone updatedZone = deliveryZoneRepository.save(zone);
        log.info("Статус зоны id: {} изменен на active={}", id, updatedZone.getActive());

        return ResponseEntity.ok(mapToResponse(updatedZone));
    }

    @PatchMapping("/admin/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderZones(@RequestBody List<Long> zoneIds) {
        log.info("Изменение порядка зон: {}", zoneIds);

        for (int i = 0; i < zoneIds.size(); i++) {
            DeliveryZone zone = findZoneById(zoneIds.get(i));
            zone.setDisplayOrder(i);
            deliveryZoneRepository.save(zone);
        }

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        log.info("Удаление зоны id: {}", id);

        DeliveryZone zone = findZoneById(id);

        if (zone.getActive()) {
            log.warn("Попытка удалить активную зону id: {}", id);
        }

        deliveryZoneRepository.deleteById(id);
        log.info("Зона id: {} удалена", id);

        return ResponseEntity.noContent().build();
    }

    // ========== ПРИВАТНЫЕ МЕТОДЫ ==========

    private DeliveryZone findZoneById(Long id) {
        return deliveryZoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Зона доставки с id " + id + " не найдена"
                ));
    }

    private boolean isPointInPolygon(double[] point, List<List<Double>> polygon) {
        double x = point[0];
        double y = point[1];
        boolean inside = false;

        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i).get(0);
            double yi = polygon.get(i).get(1);
            double xj = polygon.get(j).get(0);
            double yj = polygon.get(j).get(1);

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

            if (intersect) inside = !inside;
        }

        return inside;
    }

    private ClientDeliveryZoneResponse mapToClientResponse(DeliveryZone zone) {
        return new ClientDeliveryZoneResponse(
                zone.getName(),
                zone.getColor(),
                zone.getBorderColor(),
                zone.getFillOpacity(),
                zone.getMinOrder(),
                zone.getDeliveryTime(),
                zone.getPoints()
        );
    }

    private DeliveryZoneResponse mapToResponse(DeliveryZone zone) {
        return new DeliveryZoneResponse(
                zone.getId(),
                zone.getName(),
                zone.getColor(),
                zone.getBorderColor(),
                zone.getFillOpacity(),
                zone.getMinOrder(),
                zone.getDeliveryTime(),
                zone.getPoints(),
                zone.getActive(),
                zone.getDisplayOrder()
        );
    }

    // ========== DTO ==========

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointCheckRequest {
        private Double lat;
        private Double lng;
    }
}