/**
 * Управление зонами доставки
 */

// API URL
const deliveryZonesApiUrl = '/api/delivery-zones';

// Координаты Перми
const PERM_CENTER = [58.0105, 56.2294];
const PERM_ZOOM = 12;

// Глобальные переменные
let deliveryMap;
let currentDrawingPolygon = null;
let currentEditZoneId = null;
let deliveryZonesData = [];
let isDrawingMode = false;

// ========== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========
// Функция для упорядочивания точек по часовой стрелке
function orderPointsClockwise(points) {
    if (points.length < 3) return points;

    // Находим центр полигона
    const center = points.reduce((acc, point) => {
        return [acc[0] + point[0], acc[1] + point[1]];
    }, [0, 0]);
    center[0] /= points.length;
    center[1] /= points.length;

    // Сортируем точки по углу относительно центра
    return points.sort((a, b) => {
        const angleA = Math.atan2(a[1] - center[1], a[0] - center[0]);
        const angleB = Math.atan2(b[1] - center[1], b[0] - center[0]);
        return angleA - angleB;
    });
}

// Проверка и исправление порядка точек
function fixPolygonOrder(points) {
    if (!points || points.length < 3) return points;

    // Вычисляем знак площади
    let area = 0;
    for (let i = 0; i < points.length; i++) {
        const j = (i + 1) % points.length;
        area += points[i][0] * points[j][1];
        area -= points[j][0] * points[i][1];
    }

    // Если площадь отрицательная, разворачиваем
    if (area < 0) {
        return points.reverse();
    }

    return points;
}
// ==========================================

// Инициализация
document.addEventListener('DOMContentLoaded', function() {
    console.log('Delivery zones page loaded');
    setupEventListeners();

    if (typeof ymaps !== 'undefined') {
        ymaps.ready(initDeliveryMap);
    } else {
        console.error('Яндекс.Карты не загружены');
        showZoneToast('Ошибка загрузки карты', 'error');
    }
});

// Настройка обработчиков
function setupEventListeners() {
    const createBtn = document.getElementById('createZoneBtn');
    const refreshBtn = document.getElementById('refreshZonesBtn');
    const drawBtn = document.getElementById('drawPolygonBtn');
    const clearBtn = document.getElementById('clearDrawingBtn');
    const fitBtn = document.getElementById('fitMapBtn');
    const saveBtn = document.getElementById('saveZoneBtn');
    const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');

    if (createBtn) createBtn.addEventListener('click', openCreateModal);
    if (refreshBtn) refreshBtn.addEventListener('click', loadZones);
    if (drawBtn) drawBtn.addEventListener('click', enableDrawingMode);
    if (clearBtn) clearBtn.addEventListener('click', clearDrawing);
    if (fitBtn) fitBtn.addEventListener('click', fitMapToZones);
    if (saveBtn) saveBtn.addEventListener('click', saveZone);
    if (confirmDeleteBtn) confirmDeleteBtn.addEventListener('click', confirmDelete);

    // Синхронизация цветов
    const colorInput = document.getElementById('zoneColor');
    const colorText = document.getElementById('zoneColorText');
    const borderColorInput = document.getElementById('zoneBorderColor');
    const borderColorText = document.getElementById('zoneBorderColorText');
    const colorPreview = document.getElementById('colorPreview');

    if (colorInput && colorText) {
        colorInput.addEventListener('change', () => {
            colorText.value = colorInput.value;
            if (colorPreview) colorPreview.style.background = colorInput.value;
        });
        colorText.addEventListener('input', () => {
            if (/^#[0-9A-F]{6}$/i.test(colorText.value)) {
                colorInput.value = colorText.value;
                if (colorPreview) colorPreview.style.background = colorText.value;
            }
        });
    }

    if (borderColorInput && borderColorText) {
        borderColorInput.addEventListener('change', () => {
            borderColorText.value = borderColorInput.value;
        });
        borderColorText.addEventListener('input', () => {
            if (/^#[0-9A-F]{6}$/i.test(borderColorText.value)) {
                borderColorInput.value = borderColorText.value;
            }
        });
    }
}

// Инициализация карты
function initDeliveryMap() {
    console.log('Initializing map...');
    deliveryMap = new ymaps.Map('map', {
        center: PERM_CENTER,
        zoom: PERM_ZOOM,
        controls: ['zoomControl', 'fullscreenControl']
    });

    // Добавляем метку центра Перми
    const permCenterPlacemark = new ymaps.Placemark(PERM_CENTER, {
        hintContent: 'Пермь',
        balloonContent: 'Центр города Пермь'
    }, {
        preset: 'islands#redCircleIcon',
        iconColor: '#ff0000'
    });
    deliveryMap.geoObjects.add(permCenterPlacemark);

    // Инициализируем поиск
    initSearchControl();

    loadZones();
}

function initSearchControl() {
    const searchInput = document.getElementById('addressSearch');
    const searchBtn = document.getElementById('searchBtn');

    if (!searchInput || !searchBtn) return;

    async function searchAddress() {
        const query = searchInput.value.trim();
        if (!query) {
            showZoneToast('Введите адрес для поиска', 'warning');
            return;
        }

        showZoneToast('Поиск...', 'info');

        try {
            const response = await ymaps.geocode(query, { results: 1 });
            const firstGeoObject = response.geoObjects.get(0);

            if (firstGeoObject) {
                const coordinates = firstGeoObject.geometry.getCoordinates();
                deliveryMap.setCenter(coordinates, 15);

                const placemark = new ymaps.Placemark(coordinates, {
                    hintContent: firstGeoObject.getAddressLine(),
                    balloonContent: firstGeoObject.getAddressLine()
                }, {
                    preset: 'islands#redDotIcon',
                    iconColor: '#ff0000'
                });

                deliveryMap.geoObjects.add(placemark);
                setTimeout(() => deliveryMap.geoObjects.remove(placemark), 5000);
                showZoneToast(`Найден: ${firstGeoObject.getAddressLine()}`, 'success');
            } else {
                showZoneToast('Адрес не найден', 'error');
            }
        } catch (error) {
            console.error('Ошибка поиска:', error);
            showZoneToast('Ошибка поиска адреса', 'error');
        }
    }

    searchBtn.addEventListener('click', searchAddress);
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') searchAddress();
    });
}

// Функция для сохранения полигона в textarea
function savePolygonToTextarea() {
    if (!deliveryMap) return false;

    let polygonPoints = null;
    let polygonFound = false;

    deliveryMap.geoObjects.each(function(obj) {
        if (obj.geometry && obj.geometry.getType() === 'Polygon') {
            polygonPoints = obj.geometry.getCoordinates()[0];
            polygonFound = true;
        }
    });

    if (polygonFound && polygonPoints && polygonPoints.length >= 3) {
        const points = polygonPoints.map(coord => [coord[1], coord[0]]);
        const pointsJson = JSON.stringify(points);
        const pointsTextarea = document.getElementById('zonePoints');
        pointsTextarea.value = pointsJson;

        pointsTextarea.style.border = '3px solid green';
        pointsTextarea.style.backgroundColor = '#e8f5e9';
        setTimeout(() => {
            pointsTextarea.style.border = '';
            pointsTextarea.style.backgroundColor = '';
        }, 2000);

        return true;
    }
    return false;
}

// Включение режима рисования
function enableDrawingMode() {
    if (isDrawingMode) {
        showZoneToast('Режим рисования уже активен', 'info');
        return;
    }

    clearDrawing();
    isDrawingMode = true;

    const color = document.getElementById('zoneColor').value || '#0d6efd';
    const borderColor = document.getElementById('zoneBorderColor').value || '#0b5ed7';
    const opacity = parseFloat(document.getElementById('zoneFillOpacity').value) || 0.3;

    currentDrawingPolygon = new ymaps.Polygon([[]], {}, {
        editorDrawingCursor: 'crosshair',
        editorMaxPoints: 100,
        fillColor: color + Math.floor(opacity * 255).toString(16).padStart(2, '0'),
        strokeColor: borderColor,
        strokeWidth: 3,
        opacity: opacity
    });

    deliveryMap.geoObjects.add(currentDrawingPolygon);
    currentDrawingPolygon.editor.startDrawing();

    currentDrawingPolygon.editor.events.add('drawingend', function() {
        isDrawingMode = false;
        const coordinates = currentDrawingPolygon.geometry.getCoordinates()[0];

        if (coordinates && coordinates.length >= 3) {
            const points = coordinates.map(coord => [coord[1], coord[0]]);
            const orderedPoints = orderPointsClockwise(points);
            const pointsJson = JSON.stringify(orderedPoints);

            const pointsTextarea = document.getElementById('zonePoints');
            pointsTextarea.value = pointsJson;

            pointsTextarea.style.border = '3px solid green';
            pointsTextarea.style.backgroundColor = '#e8f5e9';
            setTimeout(() => {
                pointsTextarea.style.border = '';
                pointsTextarea.style.backgroundColor = '';
            }, 2000);

            showZoneToast(`✅ Полигон создан! (${orderedPoints.length} точек)`, 'success');
        } else {
            showZoneToast('Полигон должен иметь минимум 3 точки', 'warning');
            if (currentDrawingPolygon) {
                deliveryMap.geoObjects.remove(currentDrawingPolygon);
                currentDrawingPolygon = null;
            }
        }
    });

    showZoneToast('🎨 Нажмите на карте 3+ раза, затем дважды кликните', 'info');
}

// Очистка рисования
function clearDrawing() {
    isDrawingMode = false;
    if (currentDrawingPolygon) {
        deliveryMap.geoObjects.remove(currentDrawingPolygon);
        currentDrawingPolygon = null;
    }
    if (deliveryMap && deliveryMap.editor) {
        deliveryMap.editor.stopEditing();
    }
    const pointsInput = document.getElementById('zonePoints');
    if (pointsInput) {
        pointsInput.value = '';
        pointsInput.style.border = '';
        pointsInput.style.backgroundColor = '';
    }
    const pointsInfo = document.getElementById('pointsInfo');
    if (pointsInfo) pointsInfo.remove();
}

// Загрузка зон
async function loadZones() {
    try {
        const response = await fetch(deliveryZonesApiUrl + '/admin', {
            method: 'GET',
            credentials: 'include'
        });

        if (response.status === 404) {
            deliveryZonesData = [];
            renderZonesList();
            return;
        }

        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        deliveryZonesData = await response.json();
        console.log('Загружено зон:', deliveryZonesData.length);

        renderZonesList();
        renderZonesOnMap();
    } catch (error) {
        console.error('Error:', error);
        deliveryZonesData = [];
        renderZonesList();
    }
}

// Отображение списка зон
function renderZonesList() {
    const container = document.getElementById('zonesList');
    if (!container) return;

    if (!deliveryZonesData || deliveryZonesData.length === 0) {
        container.innerHTML = `
            <div class="text-center p-4 text-muted">
                <i class="bi bi-geo-alt fs-1"></i>
                <p class="mt-2">Нет созданных зон доставки</p>
                <button class="btn btn-sm btn-primary mt-2" onclick="window.openCreateModal()">
                    <i class="bi bi-plus-circle"></i> Создать первую зону
                </button>
            </div>
        `;
        return;
    }

    container.innerHTML = deliveryZonesData.map(zone => `
        <div class="zone-item" onclick="window.selectZone(${zone.id})" data-id="${zone.id}">
            <div class="zone-name">
                <span class="zone-color-badge" style="background: ${zone.color}; border: 2px solid ${zone.borderColor};"></span>
                ${escapeHtml(zone.name)}
                <span class="zone-status ${zone.active ? 'status-active' : 'status-inactive'}">
                    ${zone.active ? 'Активна' : 'Неактивна'}
                </span>
            </div>
            <div class="zone-details">
                <i class="bi bi-calculator"></i> Мин. заказ: ${zone.minOrder} ₽
            </div>
            <div class="zone-details">
                <i class="bi bi-clock"></i> Время: ${escapeHtml(zone.deliveryTime)}
            </div>
            <div class="zone-actions">
                <button class="btn btn-sm btn-outline-primary" onclick="event.stopPropagation(); window.editZone(${zone.id})">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="event.stopPropagation(); window.openDeleteModal(${zone.id})">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>
    `).join('');
}

// Отображение зон на карте
function renderZonesOnMap() {
    if (!deliveryMap) return;

    const toRemove = [];
    deliveryMap.geoObjects.each(function(obj) {
        const isPermPlacemark = obj.geometry &&
            obj.geometry.getType() === 'Point' &&
            obj.geometry.getCoordinates()[0] === PERM_CENTER[0] &&
            obj.geometry.getCoordinates()[1] === PERM_CENTER[1];

        if (obj !== currentDrawingPolygon &&
            obj.geometry &&
            obj.geometry.getType() === 'Polygon' &&
            !isPermPlacemark) {
            toRemove.push(obj);
        }
    });
    toRemove.forEach(obj => deliveryMap.geoObjects.remove(obj));

    let polygonsCount = 0;

    deliveryZonesData.forEach(zone => {
        if (zone.points && zone.points.length >= 3) {
            polygonsCount++;
            const coordinates = zone.points.map(point => [point[1], point[0]]);

            const polygon = new ymaps.Polygon([coordinates], {
                hintContent: zone.name,
                balloonContent: `
                    <b>${escapeHtml(zone.name)}</b><br>
                    Мин. заказ: ${zone.minOrder} ₽<br>
                    Время: ${zone.deliveryTime}<br>
                    Статус: ${zone.active ? 'Активна' : 'Неактивна'}
                `
            }, {
                fillColor: zone.color + Math.floor(zone.fillOpacity * 255).toString(16).padStart(2, '0'),
                strokeColor: zone.borderColor,
                strokeWidth: 3,
                fillOpacity: zone.fillOpacity,
                cursor: 'pointer'
            });
            deliveryMap.geoObjects.add(polygon);
            polygon.events.add('click', () => selectZone(zone.id));
        }
    });

    console.log(`Отображено полигонов на карте: ${polygonsCount}`);

    if (deliveryZonesData.length > 0 && deliveryZonesData[0].points && deliveryZonesData[0].points.length > 0) {
        const firstZone = deliveryZonesData[0];
        const center = firstZone.points.reduce((acc, point) => {
            return [acc[0] + point[1], acc[1] + point[0]];
        }, [0, 0]);
        center[0] /= firstZone.points.length;
        center[1] /= firstZone.points.length;
        deliveryMap.setCenter([center[0], center[1]], 13);
    }
}

// Выбор зоны
window.selectZone = function(zoneId) {
    const zone = deliveryZonesData.find(z => z.id === zoneId);
    if (!zone) return;

    document.querySelectorAll('.zone-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.id == zoneId) item.classList.add('active');
    });

    if (zone.points && zone.points.length > 0) {
        const center = zone.points.reduce((acc, point) => {
            return [acc[0] + point[1], acc[1] + point[0]];
        }, [0, 0]);
        center[0] /= zone.points.length;
        center[1] /= zone.points.length;
        deliveryMap.setCenter([center[0], center[1]], 13);
    }
};

// Фит карты
function fitMapToZones() {
    if (!deliveryMap) return;
    const allPoints = [];
    deliveryZonesData.forEach(zone => {
        if (zone.points) {
            zone.points.forEach(point => {
                allPoints.push([point[1], point[0]]);
            });
        }
    });
    if (allPoints.length > 0) {
        const bounds = ymaps.geoQuery(allPoints).getBounds();
        if (bounds) {
            deliveryMap.setBounds(bounds, { checkZoomRange: true });
        }
    } else {
        deliveryMap.setCenter(PERM_CENTER, PERM_ZOOM);
        showZoneToast('Нет зон для отображения', 'info');
    }
}

// Открытие модального окна создания
window.openCreateModal = function() {
    savePolygonToTextarea();

    const pointsTextarea = document.getElementById('zonePoints');
    const existingPointsStr = pointsTextarea.value;

    let hasPolygon = false;
    let pointsCount = 0;

    if (existingPointsStr && existingPointsStr !== '[]' && existingPointsStr !== '') {
        try {
            const points = JSON.parse(existingPointsStr);
            pointsCount = points.length;
            if (points.length >= 3) {
                hasPolygon = true;
            }
        } catch(e) {
            console.error('Ошибка парсинга:', e);
        }
    }

    if (!hasPolygon) {
        showZoneToast('❌ Сначала нарисуйте полигон!\n\n1. Нажмите "Рисовать полигон"\n2. Кликните 3+ раза на карте\n3. Дважды кликните', 'error');
        return;
    }

    currentEditZoneId = null;
    document.getElementById('modalTitle').innerText = 'Создание зоны доставки';
    document.getElementById('zoneName').value = '';
    document.getElementById('zoneColor').value = '#0d6efd';
    document.getElementById('zoneColorText').value = '#0d6efd';
    document.getElementById('zoneBorderColor').value = '#0b5ed7';
    document.getElementById('zoneBorderColorText').value = '#0b5ed7';
    document.getElementById('zoneFillOpacity').value = '0.3';
    document.getElementById('zoneMinOrder').value = '500';
    document.getElementById('zoneDeliveryTime').value = '30 мин';
    document.getElementById('zoneActive').value = 'true';

    const existingInfo = document.getElementById('pointsInfo');
    if (existingInfo) existingInfo.remove();

    const pointsInfo = document.createElement('div');
    pointsInfo.id = 'pointsInfo';
    pointsInfo.className = 'alert alert-success mt-2';
    pointsInfo.innerHTML = `<i class="bi bi-check-circle"></i> ✅ Полигон готов (${pointsCount} точек)`;
    pointsTextarea.parentNode.insertBefore(pointsInfo, pointsTextarea.nextSibling);

    const modal = new bootstrap.Modal(document.getElementById('zoneModal'));
    modal.show();
};

// Редактирование зоны
window.editZone = function(zoneId) {
    console.log('=== РЕДАКТИРОВАНИЕ ЗОНЫ ===');

    const zone = deliveryZonesData.find(z => z.id === zoneId);
    if (!zone) {
        console.error('Зона не найдена:', zoneId);
        return;
    }

    console.log('Название:', zone.name);
    console.log('Точек:', zone.points?.length);

    currentEditZoneId = zone.id;

    // Сохраняем данные зоны в переменные
    const zoneData = {
        name: zone.name,
        color: zone.color,
        borderColor: zone.borderColor,
        fillOpacity: zone.fillOpacity,
        minOrder: zone.minOrder,
        deliveryTime: zone.deliveryTime.includes('-') ? zone.deliveryTime.split('-')[0].trim() : zone.deliveryTime,
        active: zone.active,
        points: zone.points
    };

    // Функция для установки точек в textarea (будет вызываться несколько раз)
    function setPointsToTextarea() {
        const pointsTextarea = document.getElementById('zonePoints');
        if (!pointsTextarea) return;

        if (zoneData.points && zoneData.points.length >= 3) {
            const pointsJson = JSON.stringify(zoneData.points);
            pointsTextarea.value = pointsJson;

            console.log(`🔄 Установка точек (${zoneData.points.length} шт): ${pointsJson.substring(0, 80)}...`);

            // Визуальное подтверждение
            pointsTextarea.style.border = '2px solid green';
            pointsTextarea.style.backgroundColor = '#e8f5e9';

            // Добавляем информационную метку
            const existingInfo = document.getElementById('pointsInfo');
            if (existingInfo) existingInfo.remove();

            const pointsInfo = document.createElement('div');
            pointsInfo.id = 'pointsInfo';
            pointsInfo.className = 'alert alert-success mt-2';
            pointsInfo.innerHTML = `<i class="bi bi-check-circle"></i> ✅ Полигон загружен (${zoneData.points.length} точек)`;
            pointsTextarea.parentNode.insertBefore(pointsInfo, pointsTextarea.nextSibling);

            return true;
        }
        return false;
    }

    // Открываем модальное окно
    const modalElement = document.getElementById('zoneModal');
    const modal = new bootstrap.Modal(modalElement);

    // Устанавливаем значения ПОСЛЕ того, как модалка полностью откроется
    modalElement.addEventListener('shown.bs.modal', function() {
        console.log('Модалка открыта, заполняем поля...');

        // Заполняем поля формы
        document.getElementById('modalTitle').innerText = 'Редактирование зоны';
        document.getElementById('zoneName').value = zoneData.name;
        document.getElementById('zoneColor').value = zoneData.color;
        document.getElementById('zoneColorText').value = zoneData.color;
        document.getElementById('zoneBorderColor').value = zoneData.borderColor;
        document.getElementById('zoneBorderColorText').value = zoneData.borderColor;
        document.getElementById('zoneFillOpacity').value = zoneData.fillOpacity;
        document.getElementById('zoneMinOrder').value = zoneData.minOrder;
        document.getElementById('zoneDeliveryTime').value = zoneData.deliveryTime;
        document.getElementById('zoneActive').value = zoneData.active;

        // Первая попытка установить точки
        setPointsToTextarea();

        // Вторая попытка через 50ms (на случай если что-то очистило textarea)
        setTimeout(() => {
            const currentValue = document.getElementById('zonePoints').value;
            if (!currentValue || currentValue === '') {
                console.log('🔄 Вторая попытка установить точки...');
                setPointsToTextarea();
            }
        }, 50);

        // Третья попытка через 200ms
        setTimeout(() => {
            const currentValue = document.getElementById('zonePoints').value;
            if (!currentValue || currentValue === '') {
                console.log('🔄 Третья попытка установить точки...');
                setPointsToTextarea();
            }
            const finalValue = document.getElementById('zonePoints').value;
            console.log('Финальное значение textarea:', finalValue ? `${finalValue.length} символов` : 'пусто');
            if (finalValue && finalValue !== '[]') {
                try {
                    const pts = JSON.parse(finalValue);
                    console.log(`✅ В textarea ${pts.length} точек`);
                } catch(e) {}
            }
        }, 200);

    }, { once: true });

    modal.show();

    // Также устанавливаем точки ДО открытия модалки (на всякий случай)
    setTimeout(() => {
        const pointsTextarea = document.getElementById('zonePoints');
        if (zoneData.points && zoneData.points.length >= 3 && (!pointsTextarea.value || pointsTextarea.value === '')) {
            const pointsJson = JSON.stringify(zoneData.points);
            pointsTextarea.value = pointsJson;
            console.log('📌 Предварительная установка точек ДО открытия модалки');
        }
    }, 10);
};

// Сохранение зоны
async function saveZone() {
    let pointsStr = document.getElementById('zonePoints').value;
    let points = [];

    if (pointsStr && pointsStr !== '[]' && pointsStr !== '') {
        try {
            points = JSON.parse(pointsStr);
        } catch (e) {
            console.error('Ошибка парсинга точек:', e);
        }
    }

    let deliveryTimeValue = document.getElementById('zoneDeliveryTime').value;
    if (!deliveryTimeValue) deliveryTimeValue = '30 мин';

    const zoneData = {
        name: document.getElementById('zoneName').value,
        color: document.getElementById('zoneColor').value,
        borderColor: document.getElementById('zoneBorderColor').value,
        fillOpacity: parseFloat(document.getElementById('zoneFillOpacity').value),
        minOrder: parseInt(document.getElementById('zoneMinOrder').value),
        deliveryTime: deliveryTimeValue,
        active: document.getElementById('zoneActive').value === 'true',
        points: points
    };

    if (!zoneData.name) {
        showZoneToast('Введите название зоны', 'error');
        return;
    }

    if (!zoneData.points || zoneData.points.length < 3) {
        showZoneToast(`❌ Нужно нарисовать полигон! Сейчас точек: ${zoneData.points?.length || 0}`, 'error');
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const headers = { 'Content-Type': 'application/json' };
    if (csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const url = currentEditZoneId
            ? `${deliveryZonesApiUrl}/admin/${currentEditZoneId}`
            : `${deliveryZonesApiUrl}/admin`;

        const response = await fetch(url, {
            method: currentEditZoneId ? 'PUT' : 'POST',
            headers: headers,
            credentials: 'include',
            body: JSON.stringify(zoneData)
        });

        if (!response.ok) throw new Error(`HTTP ${response.status}`);

        const modal = bootstrap.Modal.getInstance(document.getElementById('zoneModal'));
        if (modal) modal.hide();

        document.getElementById('zonePoints').value = '';
        const pointsInfo = document.getElementById('pointsInfo');
        if (pointsInfo) pointsInfo.remove();

        if (currentDrawingPolygon) {
            deliveryMap.geoObjects.remove(currentDrawingPolygon);
            currentDrawingPolygon = null;
        }

        await loadZones();
        showZoneToast(currentEditZoneId ? '✅ Зона обновлена' : '✅ Зона создана', 'success');

    } catch (error) {
        console.error('Ошибка сохранения:', error);
        showZoneToast('Ошибка сохранения', 'error');
    }
}

// Удаление зоны
window.openDeleteModal = function(zoneId) {
    const zone = deliveryZonesData.find(z => z.id === zoneId);
    if (!zone) return;
    currentEditZoneId = zoneId;
    document.getElementById('deleteZoneName').innerText = zone.name;
    const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
    modal.show();
};

async function confirmDelete() {
    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
        const headers = {};
        if (csrfToken) headers[csrfHeader] = csrfToken;

        const response = await fetch(`${deliveryZonesApiUrl}/admin/${currentEditZoneId}`, {
            method: 'DELETE',
            headers: headers,
            credentials: 'include'
        });

        if (!response.ok) throw new Error('Ошибка удаления');

        bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
        await loadZones();
        showZoneToast('Зона удалена', 'success');
    } catch (error) {
        console.error('Error:', error);
        showZoneToast('Ошибка удаления', 'error');
    }
}

// Показ уведомления
function showZoneToast(message, type = 'success') {
    const toastElement = document.getElementById('toast');
    const toastBody = document.getElementById('toastMessage');
    if (!toastElement || !toastBody) {
        alert(message);
        return;
    }
    toastBody.innerText = message;
    toastElement.classList.remove('bg-success', 'bg-danger', 'bg-warning', 'bg-info', 'text-white');
    switch(type) {
        case 'error': toastElement.classList.add('bg-danger', 'text-white'); break;
        case 'warning': toastElement.classList.add('bg-warning'); break;
        case 'info': toastElement.classList.add('bg-info', 'text-white'); break;
        default: toastElement.classList.add('bg-success', 'text-white');
    }
    new bootstrap.Toast(toastElement).show();
}

// Экранирование HTML
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Делаем функции глобальными
window.selectZone = selectZone;
window.editZone = editZone;
window.openDeleteModal = openDeleteModal;
window.openCreateModal = openCreateModal;