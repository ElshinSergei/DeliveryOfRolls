package org.example.deliveryofrolls.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.deliveryofrolls.dto.PickupPointRequest;
import org.example.deliveryofrolls.dto.PickupPointResponse;
import org.example.deliveryofrolls.entity.PickupPoint;
import org.example.deliveryofrolls.repository.PickupPointRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pickup-points")
@RequiredArgsConstructor
public class PickupPointController {

    private final PickupPointRepository pickupPointRepository;

    /**
     * Получить все активные точки самовывоза для клиента
     */
    @GetMapping("/active")
    public ResponseEntity<List<PickupPointResponse>> getActivePickupPoints() {
        log.info("Запрос на получение активных точек самовывоза");

        List<PickupPoint> points = pickupPointRepository.findByActiveTrueOrderByDisplayOrderAsc();

        List<PickupPointResponse> response = points.stream()
                .map(this::mapToResponse)
                .toList();

        log.info("Найдено активных точек: {}", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Получить все точки самовывоза для админки
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PickupPointResponse>> getAllPickupPoints() {
        log.info("Запрос на получение всех точек самовывоза (админ)");

        List<PickupPoint> points = pickupPointRepository.findAllByOrderByDisplayOrderAsc();

        List<PickupPointResponse> response = points.stream()
                .map(this::mapToResponse)
                .toList();

        log.info("Всего точек: {}", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Получить точку самовывоза по ID
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PickupPointResponse> getPickupPointById(@PathVariable Long id) {
        log.info("Запрос на получение точки с id: {}", id);

        PickupPoint point = findPickupPointById(id);

        return ResponseEntity.ok(mapToResponse(point));
    }

    /**
     * Создать новую точку самовывоза
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PickupPointResponse> createPickupPoint(@Valid @RequestBody PickupPointRequest request) {
        log.info("Запрос на создание новой точки: {}", request.getName());

        // Проверяем, что точка с таким именем не существует
        if (pickupPointRepository.findAll().stream()
                .anyMatch(point -> point.getName().equalsIgnoreCase(request.getName()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Точка с названием '" + request.getName() + "' уже существует");
        }

        PickupPoint point = new PickupPoint();
        point.setName(request.getName());
        point.setAddress(request.getAddress());
        point.setCoordinates(request.getCoordinates());
        point.setWorkingHours(request.getWorkingHours());
        point.setPhone(request.getPhone());
        point.setActive(request.getActive() != null ? request.getActive() : true);
        point.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        point.setDescription(request.getDescription());

        PickupPoint saved = pickupPointRepository.save(point);
        log.info("Точка успешно создана с id: {}", saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(saved));
    }

    /**
     * Обновить точку самовывоза
     */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PickupPointResponse> updatePickupPoint(
            @PathVariable Long id,
            @Valid @RequestBody PickupPointRequest request) {
        log.info("Запрос на обновление точки с id: {}", id);

        PickupPoint point = findPickupPointById(id);

        // Проверяем, что новое имя не конфликтует с другими
        if (!point.getName().equalsIgnoreCase(request.getName())) {
            boolean nameExists = pickupPointRepository.findAll().stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(request.getName()));
            if (nameExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Точка с названием '" + request.getName() + "' уже существует");
            }
        }

        point.setName(request.getName());
        point.setAddress(request.getAddress());
        point.setCoordinates(request.getCoordinates());
        point.setWorkingHours(request.getWorkingHours());
        point.setPhone(request.getPhone());
        point.setActive(request.getActive());
        point.setDisplayOrder(request.getDisplayOrder());
        point.setDescription(request.getDescription());

        PickupPoint updated = pickupPointRepository.save(point);
        log.info("Точка с id: {} успешно обновлена", id);

        return ResponseEntity.ok(mapToResponse(updated));
    }

    /**
     * Обновить статус активности точки
     */
    @PatchMapping("/admin/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PickupPointResponse> togglePickupPointActive(@PathVariable Long id) {
        log.info("Запрос на переключение статуса точки с id: {}", id);

        PickupPoint point = findPickupPointById(id);
        point.setActive(!point.getActive());

        PickupPoint updated = pickupPointRepository.save(point);
        log.info("Статус точки с id: {} изменен на active={}", id, updated.getActive());

        return ResponseEntity.ok(mapToResponse(updated));
    }

    /**
     * Обновить порядок отображения точек
     */
    @PatchMapping("/admin/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderPickupPoints(@RequestBody List<Long> pointIds) {
        log.info("Запрос на изменение порядка точек: {}", pointIds);

        for (int i = 0; i < pointIds.size(); i++) {
            PickupPoint point = findPickupPointById(pointIds.get(i));
            point.setDisplayOrder(i);
            pickupPointRepository.save(point);
        }

        log.info("Порядок точек успешно обновлен");
        return ResponseEntity.ok().build();
    }

    /**
     * Удалить точку самовывоза
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePickupPoint(@PathVariable Long id) {
        log.info("Запрос на удаление точки с id: {}", id);

        PickupPoint point = findPickupPointById(id);

        if (point.getActive()) {
            log.warn("Попытка удалить активную точку с id: {}", id);
        }

        pickupPointRepository.deleteById(id);
        log.info("Точка с id: {} успешно удалена", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Вспомогательный метод для поиска точки
     */
    private PickupPoint findPickupPointById(Long id) {
        return pickupPointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Точка самовывоза с id " + id + " не найдена"
                ));
    }

    /**
     * Маппинг для ответа
     */
    private PickupPointResponse mapToResponse(PickupPoint point) {
        return new PickupPointResponse(
                point.getId(),
                point.getName(),
                point.getAddress(),
                point.getCoordinates(),
                point.getWorkingHours(),
                point.getPhone(),
                point.getActive(),
                point.getDisplayOrder(),
                point.getDescription()
        );
    }
}
