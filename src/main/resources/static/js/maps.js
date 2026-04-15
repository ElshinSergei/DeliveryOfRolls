/**
 * Карта для выбора адреса доставки (только Пермь)
 */

let clientMap;
let currentMarker = null;
let deliveryZones = [];
let pickupPoints = [];
let searchTimeout = null;
let selectedPickupPoint = null;
let isPickupMode = false;

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    console.log('Maps.js загружен');

    // Инициализируем вкладки
    initDeliveryTabs();

    isPickupMode = false;

    // Обработчик открытия модального окна
    const addressModal = document.getElementById('addressModal');
    if (addressModal) {
        addressModal.addEventListener('shown.bs.modal', function() {
            setTimeout(function() {
                if (clientMap) {
                    clientMap.container.fitToViewport();
                }
            }, 100);
            initClientMap();
        });
    }

    // Загружаем зоны доставки и точки самовывоза
    loadDeliveryZones();
    loadPickupPoints();

    // Инициализируем поиск с выпадающим списком
    initAddressSearch();

    // Кнопка подтверждения
    const confirmBtn = document.getElementById('confirmBtn');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', confirmAddress);
    }

    // Загружаем сохраненный адрес в форму
    loadSavedAddressToForm();
});

// Инициализация вкладок Доставка/Самовывоз
function initDeliveryTabs() {
    const deliveryTab = document.getElementById('deliveryTabBtn');
    const pickupTab = document.getElementById('pickupTabBtn');
    const addressSelector = document.querySelector('.search-wrapper');
    const mapWrapper = document.querySelector('.map-wrapper');
    const deliveryZoneInfo = document.getElementById('deliveryZoneInfo');
    const confirmBtn = document.getElementById('confirmBtn');
    const addressSearchInput = document.getElementById('addressSearchInput');
    const savedAddressesBlock = document.getElementById('savedAddressesBlock');
    const pickupPointsContainer = document.getElementById('pickupPointsContainer');

    if (!deliveryTab || !pickupTab) return;

    // Вкладка "Доставка"
    deliveryTab.addEventListener('click', function() {
        isPickupMode = false;
        deliveryTab.classList.add('active');
        pickupTab.classList.remove('active');

        if (addressSelector) addressSelector.style.display = 'block';
        if (mapWrapper) mapWrapper.style.display = 'block';
        if (addressSearchInput) addressSearchInput.disabled = false;
        if (deliveryZoneInfo) {
            // Показываем блок информации о зоне доставки только если есть контент
            const zoneStatus = document.getElementById('zoneStatus');
            const zoneMessage = document.getElementById('zoneMessage');
            if (zoneStatus && zoneStatus.textContent.trim() || zoneMessage && zoneMessage.textContent.trim()) {
                deliveryZoneInfo.style.display = 'block';
            } else {
                deliveryZoneInfo.style.display = 'none';
            }
        }
        if (pickupPointsContainer) pickupPointsContainer.style.display = 'none';
        if (savedAddressesBlock) {
            // Показываем блок сохраненных адресов только если есть адреса
            const select = document.getElementById('savedAddressesSelect');
            if (select && select.options.length > 1) {
                savedAddressesBlock.style.display = 'block';
            } else {
                savedAddressesBlock.style.display = 'none';
            }
        }
        if (confirmBtn) confirmBtn.disabled = true;

        window.selectedAddress = null;
        selectedPickupPoint = null;

        if (clientMap) {
            showDeliveryMode();
        }

        // ⭐ УБИРАЕМ ПЕРЕЗАГРУЗКУ
        console.log('Режим: Доставка');
    });

    // Вкладка "Самовывоз"
    pickupTab.addEventListener('click', function() {
        isPickupMode = true;
        pickupTab.classList.add('active');
        deliveryTab.classList.remove('active');

        if (addressSelector) addressSelector.style.display = 'none';
        if (addressSearchInput) addressSearchInput.disabled = true;
        if (deliveryZoneInfo) deliveryZoneInfo.style.display = 'none';
        if (pickupPointsContainer) pickupPointsContainer.style.display = 'block';
        if (savedAddressesBlock) savedAddressesBlock.style.display = 'none';
        if (confirmBtn) confirmBtn.disabled = true;

        if (clientMap) {
            showPickupMode();
        }

        if (pickupPoints.length > 0) {
            renderPickupPointsList();
        }

        // ⭐ УБИРАЕМ ПЕРЕЗАГРУЗКУ
        console.log('Режим: Самовывоз');
    });
}

// Показать режим доставки (зоны на карте)
function showDeliveryMode() {
    if (!clientMap) return;

    // Удаляем все маркеры точек самовывоза
    const toRemove = [];
    clientMap.geoObjects.each(function(obj) {
        const isPickupMarker = obj.properties && obj.properties.get('isPickupPoint');
        if (!isPickupMarker) {
            toRemove.push(obj);
        }
    });
    toRemove.forEach(obj => clientMap.geoObjects.remove(obj));

    // Показываем зоны доставки
    loadZonesOnMap();

    // ⭐ СКРЫВАЕМ КОНТЕЙНЕР С ТОЧКАМИ САМОВЫВОЗА
    const pickupContainer = document.getElementById('pickupPointsContainer');
    if (pickupContainer) pickupContainer.style.display = 'none';

    console.log('Карта переключена в режим доставки');
}

// Показать режим самовывоза (точки на карте)
function showPickupMode() {
    if (!clientMap) return;

    const toRemove = [];
    clientMap.geoObjects.each(function(obj) {
        toRemove.push(obj);
    });
    toRemove.forEach(obj => clientMap.geoObjects.remove(obj));

    loadPickupPointsOnMap();

    // ⭐ Показываем блок с точками самовывоза, скрываем зону
    const pickupContainer = document.getElementById('pickupPointsContainer');
    if (pickupContainer) pickupContainer.style.display = 'block';

    const zoneInfo = document.getElementById('deliveryZoneInfo');
    if (zoneInfo) zoneInfo.style.display = 'none';

    if (pickupPoints.length > 0 && pickupPoints[0].coordinates) {
        const parts = pickupPoints[0].coordinates.split(',').map(parseFloat);
        const coords = [parts[0], parts[1]];
        clientMap.setCenter(coords, 14);
    }

    console.log('Карта переключена в режим самовывоза');
}

// Загрузка точек самовывоза
async function loadPickupPoints() {
    try {
        const response = await fetch('/api/pickup-points/active');
        if (response.ok) {
            pickupPoints = await response.json();
            console.log('Загружено точек самовывоза:', pickupPoints.length);

            if (clientMap && isPickupMode) {
                loadPickupPointsOnMap();
            }
        } else {
            console.log('API точек самовывоза не доступен, используем стандартную');
            pickupPoints = [{
                id: 1,
                name: 'Главный ресторан',
                address: 'г. Пермь, ул. Ленина, 10',
                coordinates: '58.016248, 56.257102',
                workingHours: 'Ежедневно с 10:00 до 23:00',
                phone: '+7 (999) 123-45-67',
                description: 'Центральный ресторан, рядом с театром оперы и балета'
            }];
        }
    } catch (error) {
        console.error('Ошибка загрузки точек самовывоза:', error);
        pickupPoints = [{
            id: 1,
            name: 'Главный ресторан',
            address: 'г. Пермь, ул. Ленина, 10',
            coordinates: '58.016248, 56.257102',
            workingHours: 'Ежедневно с 10:00 до 23:00',
            phone: '+7 (999) 123-45-67'
        }];
    }
}

// Отображение точек самовывоза на карте
function loadPickupPointsOnMap() {
    if (!clientMap) return;

    pickupPoints.forEach(point => {
        if (point.coordinates) {
            let coords;
            const parts = point.coordinates.split(',').map(parseFloat);
            if (parts.length === 2) {
                // ⭐ МЕНЯЕМ МЕСТАМИ: было [parts[0], parts[1]] -> стало [parts[1], parts[0]]
                coords = [parts[1], parts[0]];
                console.log(`Точка "${point.name}" исправленные координаты:`, coords);
            } else {
                coords = [58.016248, 56.257102];
            }

            const placemark = new ymaps.Placemark(coords, {
                hintContent: point.name,
                balloonContent: `
                    <div style="padding: 5px;">
                        <b style="font-size: 16px;">🏪 ${escapeHtml(point.name)}</b><br><br>
                        📍 <b>Адрес:</b> ${escapeHtml(point.address)}<br>
                        🕐 <b>Часы работы:</b> ${escapeHtml(point.workingHours || '10:00 - 23:00')}<br>
                        📞 <b>Телефон:</b> ${escapeHtml(point.phone || '+7 (999) 123-45-67')}<br>
                        ${point.description ? `<br>📝 <b>Описание:</b> ${escapeHtml(point.description)}` : ''}
                    </div>
                `,
                isPickupPoint: true
            }, {
                preset: 'islands#blueStoreIcon',
                iconColor: '#0078ff',
                balloonMaxWidth: 300
            });

            clientMap.geoObjects.add(placemark);

            placemark.events.add('click', function() {
                selectPickupPoint(point);
                clientMap.setCenter(coords, 15);
            });
        }
    });

    if (pickupPoints.length > 0) {
        console.log(`На карте отображено ${pickupPoints.length} точек самовывоза`);
    }
}

// Отображение списка точек самовывоза
function renderPickupPointsList() {
    const container = document.getElementById('pickupPointsContainer');
    const list = document.getElementById('pickupPointsList');
    const confirmBtn = document.getElementById('confirmBtn');

    if (!container || !list) return;

    if (pickupPoints.length === 0) {
        container.style.display = 'none';
        return;
    }

    if (pickupPoints.length === 1) {
        const point = pickupPoints[0];

        // Автоматически выбираем единственную точку
        selectedPickupPoint = point;

        // Активируем кнопку
        if (confirmBtn) confirmBtn.disabled = false;

        // Показываем информацию о точке
        container.style.display = 'block';
        list.innerHTML = `
            <div class="pickup-single-info selected" data-point-id="${point.id}">
                <div class="pickup-single-icon">
                    <i class="bi bi-shop-check"></i>
                </div>
                <div class="pickup-single-details">
                    <strong>🏪 ${escapeHtml(point.name)}</strong>
                    <div><i class="bi bi-geo-alt"></i> ${escapeHtml(point.address)}</div>
                    <div><i class="bi bi-clock"></i> ${escapeHtml(point.workingHours || '10:00 - 23:00')}</div>
                    <div><i class="bi bi-telephone"></i> ${escapeHtml(point.phone || '+7 (999) 123-45-67')}</div>
                    ${point.description ? `<div><i class="bi bi-info-circle"></i> ${escapeHtml(point.description)}</div>` : ''}
                </div>
            </div>
        `;

        // Обновляем хедер и localStorage
        updateHeaderAddress(`🏪 ${point.name}`);
        localStorage.setItem('deliveryAddress', JSON.stringify({
            type: 'pickup',
            text: `Самовывоз: ${point.name}`,
            point: point
        }));

        // Центрируем карту на точке
        if (clientMap && point.coordinates) {
            const parts = point.coordinates.split(',').map(parseFloat);
            const coords = [parts[1], parts[0]];
            clientMap.setCenter(coords, 15);
            addMarker(coords);
        }

        console.log('Автоматически выбрана точка самовывоза:', point.name);

    } else {
        // Несколько точек - показываем список
        container.style.display = 'block';
        list.innerHTML = pickupPoints.map(point => `
            <div class="pickup-point-item ${selectedPickupPoint?.id === point.id ? 'selected' : ''}" 
                 data-point-id="${point.id}" 
                 onclick="window.selectPickupPointFromList(${point.id})">
                <div class="pickup-point-icon">
                    <i class="bi bi-shop"></i>
                </div>
                <div class="pickup-point-info">
                    <div class="pickup-point-name">${escapeHtml(point.name)}</div>
                    <div class="pickup-point-address">
                        <i class="bi bi-geo-alt-fill"></i>
                        ${escapeHtml(point.address)}
                    </div>
                    <div class="pickup-point-hours">
                        <i class="bi bi-clock"></i>
                        ${escapeHtml(point.workingHours || '10:00 - 23:00')}
                    </div>
                </div>
                <div class="pickup-point-arrow">
                    <i class="bi bi-chevron-right"></i>
                </div>
            </div>
        `).join('');
    }
}

// Выбор точки самовывоза из списка
window.selectPickupPointFromList = function(pointId) {
    const point = pickupPoints.find(p => p.id === pointId);
    if (point) {
        // ⭐ ВЫЗЫВАЕМ ОБЩУЮ ФУНКЦИЮ, КОТОРАЯ УЖЕ ЦЕНТРИРУЕТ КАРТУ
        selectPickupPoint(point);

        // Подсветка элемента списка
        document.querySelectorAll('.pickup-point-item').forEach(item => {
            item.style.background = '';
            item.style.borderColor = '#e0e0e0';
        });

        // Подсвечиваем текущий элемент
        if (typeof event !== 'undefined' && event?.currentTarget) {
            event.currentTarget.style.background = '#e8f5e9';
            event.currentTarget.style.borderColor = '#4caf50';
        }

        // ⭐ ЦЕНТРИРОВАНИЕ КАРТЫ УЖЕ ЕСТЬ В selectPickupPoint
        // Дополнительно не нужно
    }
};

// Выбор точки самовывоза
function selectPickupPoint(point) {
    selectedPickupPoint = point;
    const confirmBtn = document.getElementById('confirmBtn');
    if (confirmBtn) confirmBtn.disabled = false;

    // Подсвечиваем выбранную точку в списке
    document.querySelectorAll('.pickup-point-item').forEach(item => {
        const itemId = item.getAttribute('data-point-id');
        if (itemId == point.id) {
            item.classList.add('selected');
        } else {
            item.classList.remove('selected');
        }
    });

    // Центрируем карту
    if (clientMap && point.coordinates) {
        const parts = point.coordinates.split(',').map(parseFloat);
        const coords = [parts[1], parts[0]];
        clientMap.setCenter(coords, 15);
        if (currentMarker) {
            clientMap.geoObjects.remove(currentMarker);
        }
        addMarker(coords);
    }

    // Обновляем хедер
    updateHeaderAddress(`🏪 ${point.name}`);

    // Сохраняем в localStorage
    localStorage.setItem('deliveryAddress', JSON.stringify({
        type: 'pickup',
        text: `Самовывоз: ${point.name}`,
        point: point
    }));

    // Обновляем форму
    if (typeof window.updateCheckoutForm === 'function') {
        window.updateCheckoutForm({
            type: 'pickup',
            text: `Самовывоз: ${point.name}`,
            point: point
        });
    }

    console.log('Выбрана точка самовывоза:', point.name);
}

// Инициализация карты
function initClientMap() {
    const mapContainer = document.getElementById('addressMap');
    if (!mapContainer) {
        console.log('Контейнер карты не найден');
        return;
    }

    if (clientMap && clientMap.destroy) {
        clientMap.destroy();
        clientMap = null;
    }

    const defaultCenter = [58.0105, 56.2294];

    try {
        clientMap = new ymaps.Map('addressMap', {
            center: defaultCenter,
            zoom: 12,
            controls: ['zoomControl', 'fullscreenControl']
        });

        const permBounds = [
            [57.9, 55.8],
            [58.2, 56.6]
        ];
        clientMap.options.set('restrictMapArea', permBounds);

        if (isPickupMode) {
            showPickupMode();
        } else {
            showDeliveryMode();
        }

        console.log('Карта инициализирована');

    } catch (error) {
        console.error('Ошибка инициализации карты:', error);
    }
}

// Инициализация поиска с выпадающим списком
function initAddressSearch() {
    const searchInput = document.getElementById('addressSearchInput');
    if (!searchInput) {
        console.log('Элемент addressSearchInput не найден');
        return;
    }

    const dropdown = document.getElementById('addressSuggestions');
    if (!dropdown) {
        console.log('Элемент addressSuggestions не найден');
        return;
    }

    async function searchAddresses() {
        const query = searchInput.value.trim();

        if (searchTimeout) clearTimeout(searchTimeout);

        if (query.length < 3) {
            dropdown.style.display = 'none';
            return;
        }

        searchTimeout = setTimeout(async () => {
            try {
                // Проверка, что ymaps.geocode существует
                if (typeof ymaps === 'undefined' || !ymaps.geocode) {
                    console.error('ymaps.geocode не доступен');
                    dropdown.style.display = 'none';
                    return;
                }

                const searchQuery = query + ', Пермь';
                const response = await ymaps.geocode(searchQuery, {
                    results: 8
                });

                // Проверка, что response и geoObjects существуют
                if (!response || !response.geoObjects) {
                    console.warn('Нет результатов геокодирования');
                    dropdown.style.display = 'none';
                    return;
                }

                const geoObjects = response.geoObjects;
                const count = geoObjects.getLength();

                if (count > 0) {
                    dropdown.innerHTML = '';
                    let hasResults = false;

                    for (let i = 0; i < count; i++) {
                        const geoObject = geoObjects.get(i);
                        const address = geoObject.getAddressLine();
                        const coordinates = geoObject.geometry.getCoordinates();

                        if (address && address.toLowerCase().includes('пермь')) {
                            hasResults = true;
                            const item = document.createElement('div');
                            item.className = 'suggestion-item';

                            let displayAddress = address;
                            if (displayAddress.startsWith('Пермь, ')) {
                                displayAddress = displayAddress.substring(6);
                            }
                            if (displayAddress.length > 50) {
                                displayAddress = displayAddress.substring(0, 47) + '...';
                            }

                            item.innerHTML = `
                            <i class="bi bi-geo-alt-fill"></i>
                            <div class="suggestion-address">
                                <div class="suggestion-main">${escapeHtml(displayAddress)}</div>
                                <div class="suggestion-city">Пермь</div>
                            </div>
                        `;

                            item.onclick = () => {
                                selectAddressFromSearch(address, coordinates);
                                dropdown.style.display = 'none';
                                searchInput.value = address;
                            };

                            dropdown.appendChild(item);
                        }
                    }

                    dropdown.style.display = hasResults ? 'block' : 'none';
                    if (!hasResults) {
                        dropdown.innerHTML = '<div class="suggestion-item text-muted">Ничего не найдено в Перми</div>';
                        dropdown.style.display = 'block';
                        setTimeout(() => {
                            dropdown.style.display = 'none';
                        }, 2000);
                    }
                } else {
                    dropdown.innerHTML = '<div class="suggestion-item text-muted">Ничего не найдено</div>';
                    dropdown.style.display = 'block';
                    setTimeout(() => {
                        dropdown.style.display = 'none';
                    }, 2000);
                }
            } catch (error) {
                console.error('Ошибка поиска адресов:', error);
                console.error('Текст ошибки:', error.message);
                dropdown.style.display = 'none';

                // Показываем понятное сообщение пользователю
                if (query.length >= 3) {
                    showMessage('Ошибка поиска адресов. Проверьте подключение к интернету.', 'warning');
                }
            }
        }, 300);
    }

    searchInput.addEventListener('input', searchAddresses);
    searchInput.addEventListener('focus', searchAddresses);
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            const query = searchInput.value.trim();
            if (query) {
                searchAddress(query);
                dropdown.style.display = 'none';
            }
        }
    });

    document.addEventListener('click', function(e) {
        const wrapper = document.querySelector('.search-wrapper');
        if (dropdown && wrapper && !wrapper.contains(e.target)) {
            dropdown.style.display = 'none';
        }
    });
}

// Выбор адреса из выпадающего списка
function selectAddressFromSearch(address, coordinates) {
    console.log('Выбран адрес:', address);
    console.log('Координаты (lat, lng):', coordinates);

    if (clientMap) {
        clientMap.setCenter(coordinates, 15);
        addMarker(coordinates);
    }

    checkDeliveryZone(coordinates, address);
}

// Загрузка зон доставки
async function loadDeliveryZones() {
    try {
        const response = await fetch('/api/delivery-zones/active');
        if (response.ok) {
            deliveryZones = await response.json();
            console.log('Загружено зон:', deliveryZones.length);

            if (clientMap && !isPickupMode) {
                loadZonesOnMap();
            }
        } else {
            console.error('Ошибка загрузки зон:', response.status);
        }
    } catch (error) {
        console.error('Ошибка загрузки зон:', error);
    }
}

// Отображение зон на карте
function loadZonesOnMap() {
    if (!clientMap || !deliveryZones.length) return;

    deliveryZones.forEach(zone => {
        if (zone.points && zone.points.length >= 3) {
            const coordinates = zone.points.map(point => [point[1], point[0]]);

            const polygon = new ymaps.Polygon([coordinates], {
                hintContent: zone.name,
                balloonContent: `
                    <b>${escapeHtml(zone.name)}</b><br>
                    Мин. заказ: ${zone.minOrder} ₽<br>
                    Время: ${zone.deliveryTime}
                `
            }, {
                fillColor: zone.color + '40',
                strokeColor: zone.borderColor,
                strokeWidth: 2,
                fillOpacity: 0.3,
                cursor: 'pointer'
            });

            clientMap.geoObjects.add(polygon);

            polygon.events.add('click', function() {
                showZoneInfo(zone);
            });
        }
    });
}

// Показ информации о зоне
function showZoneInfo(zone) {
    const zoneInfo = document.getElementById('deliveryZoneInfo');
    const zoneStatus = document.getElementById('zoneStatus');
    const zoneMessage = document.getElementById('zoneMessage');

    if (zoneInfo && zoneStatus && zoneMessage) {
        zoneStatus.innerHTML = '<i class="bi bi-info-circle-fill text-info"></i> Информация о зоне';
        zoneMessage.innerHTML = `
            <strong>${escapeHtml(zone.name)}</strong><br>
            <i class="bi bi-clock"></i> Время доставки: ${zone.deliveryTime}<br>
            <i class="bi bi-calculator"></i> Мин. заказ: ${zone.minOrder} ₽
        `;
        zoneInfo.style.display = 'block';
        zoneInfo.style.borderLeftColor = '#17a2b8';

        setTimeout(() => {
            if (document.getElementById('confirmBtn')?.disabled !== false) {
                zoneInfo.style.display = 'none';
            }
        }, 3000);
    }
}

// Поиск адреса
async function searchAddress(query) {
    if (!query) return;

    let searchQuery = query;
    if (!query.toLowerCase().includes('пермь')) {
        searchQuery = query + ', Пермь';
    }

    try {
        const response = await ymaps.geocode(searchQuery, {
            results: 1,
            boundedBy: [[57.9, 55.8], [58.2, 56.6]],
            strictBounds: false
        });
        const firstGeoObject = response.geoObjects.get(0);

        if (firstGeoObject) {
            const coordinates = firstGeoObject.geometry.getCoordinates();
            const address = firstGeoObject.getAddressLine();

            const lat = coordinates[0];
            const lng = coordinates[1];

            if (lat < 57.9 || lat > 58.2 || lng < 55.8 || lng > 56.6) {
                showMessage('Адрес должен быть в городе Пермь', 'warning');
                return;
            }

            console.log('Найден адрес:', address);
            console.log('Координаты (lat, lng):', coordinates);

            clientMap.setCenter(coordinates, 15);
            addMarker(coordinates);
            checkDeliveryZone(coordinates, address);

        } else {
            showMessage('Адрес не найден в Перми', 'error');
        }
    } catch (error) {
        console.error('Ошибка поиска:', error);
        showMessage('Ошибка поиска', 'error');
    }
}

// Добавление маркера
function addMarker(coordinates) {
    if (currentMarker) {
        clientMap.geoObjects.remove(currentMarker);
    }

    currentMarker = new ymaps.Placemark(coordinates, {
        hintContent: 'Выбранный адрес'
    }, {
        preset: 'islands#redDotIcon',
        draggable: true
    });

    clientMap.geoObjects.add(currentMarker);

    currentMarker.events.add('dragend', function() {
        const newCoords = currentMarker.geometry.getCoordinates();
        checkDeliveryZone(newCoords);
    });
}

// Проверка зоны доставки
function checkDeliveryZone(coordinates, addressText = null) {
    console.log('=== ПРОВЕРКА АДРЕСА ===');
    console.log('Координаты (lat, lng):', coordinates);

    const point = [coordinates[1], coordinates[0]];
    console.log('Точка для проверки (lng, lat):', point);

    let foundZone = null;

    for (const zone of deliveryZones) {
        const isInside = isPointInPolygon(point, zone.points);
        console.log(`Зона "${zone.name}": внутри = ${isInside}`);

        if (isInside) {
            foundZone = zone;
            break;
        }
    }

    const zoneInfo = document.getElementById('deliveryZoneInfo');
    const zoneStatus = document.getElementById('zoneStatus');
    const zoneMessage = document.getElementById('zoneMessage');
    const confirmBtn = document.getElementById('confirmBtn');

    if (foundZone) {
        const addressDetails = parseAddressDetails(addressText);

        window.selectedAddress = {
            text: addressText || `Адрес: ${coordinates[0]}, ${coordinates[1]}`,
            coordinates: coordinates,
            zone: foundZone,
            details: addressDetails
        };

        if (zoneStatus) zoneStatus.innerHTML = '<i class="bi bi-check-circle-fill text-success"></i> ✅ Доставка доступна';
        if (zoneMessage) zoneMessage.innerHTML = `
            <strong>📍 ${escapeHtml(foundZone.name)}</strong><br>
            <i class="bi bi-clock"></i> ⏱️ Время: ${foundZone.deliveryTime}<br>
            <i class="bi bi-calculator"></i> 💰 Мин. заказ: ${foundZone.minOrder} ₽
        `;
        if (zoneInfo) zoneInfo.style.display = 'block';
        if (confirmBtn) confirmBtn.disabled = false;

        console.log('✅ Адрес в зоне доставки!');
    } else {
        window.selectedAddress = null;
        if (zoneStatus) zoneStatus.innerHTML = '<i class="bi bi-exclamation-triangle-fill text-warning"></i> ⚠️ Доставка недоступна';
        if (zoneMessage) zoneMessage.innerHTML = 'К сожалению, этот адрес не входит в зону доставки. Попробуйте выбрать другой адрес.';
        if (zoneInfo) zoneInfo.style.display = 'block';
        if (confirmBtn) confirmBtn.disabled = true;

        console.log('❌ Адрес не в зоне доставки');
    }
}

// Проверка точки в полигоне
function isPointInPolygon(point, polygon) {
    const x = point[0];
    const y = point[1];
    let inside = false;

    for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
        const xi = polygon[i][0];
        const yi = polygon[i][1];
        const xj = polygon[j][0];
        const yj = polygon[j][1];

        const intersect = ((yi > y) != (yj > y)) &&
            (x < (xj - xi) * (y - yi) / (yj - yi) + xi);

        if (intersect) inside = !inside;
    }

    return inside;
}

// Парсинг деталей адреса из строки
function parseAddressDetails(addressText) {
    const details = {
        entrance: '',
        floor: '',
        apartment: '',
        intercom: ''
    };

    if (!addressText) return details;

    const entranceMatch = addressText.match(/подъезд\s*(\d+)/i);
    const floorMatch = addressText.match(/этаж\s*(\d+)/i);
    const apartmentMatch = addressText.match(/кв\.?\s*(\d+)/i);
    const intercomMatch = addressText.match(/домофон\s*(\d+)/i);

    if (entranceMatch) details.entrance = entranceMatch[1];
    if (floorMatch) details.floor = floorMatch[1];
    if (apartmentMatch) details.apartment = apartmentMatch[1];
    if (intercomMatch) details.intercom = intercomMatch[1];

    return details;
}

// Подтверждение адреса/способа получения
async function confirmAddress() {
    const isPickup = document.getElementById('pickupTabBtn').classList.contains('active');

    let deliveryInfo;
    let addressDataForForm = {};

    if (isPickup) {
        if (!selectedPickupPoint && pickupPoints.length > 0) {
            selectedPickupPoint = pickupPoints[0];
        }

        if (!selectedPickupPoint) {
            showMessage('Выберите точку самовывоза', 'error');
            return;
        }

        deliveryInfo = {
            type: 'pickup',
            point: selectedPickupPoint
        };

        addressDataForForm = {
            type: 'pickup',
            text: `Самовывоз: ${selectedPickupPoint.name}`,
            point: selectedPickupPoint
        };

        localStorage.setItem('deliveryMode', 'pickup');
        localStorage.setItem('deliveryAddress', JSON.stringify({
            type: 'pickup',
            text: `Самовывоз: ${selectedPickupPoint.name}`,
            point: selectedPickupPoint
        }));

        updateHeaderAddress(`🏪 ${selectedPickupPoint.name}`);
        updateCheckoutForm(addressDataForForm);

        // Очистка информации о зоне при самовывозе
        const zoneInfoContainer = document.getElementById('zoneInfoContainer');
        if (zoneInfoContainer) zoneInfoContainer.innerHTML = '';
        document.querySelectorAll('.min-order-warning').forEach(w => w.remove());

        // ⭐ ПЕРЕЗАГРУЗКА ПОСЛЕ ВЫБОРА САМОВЫВОЗА
        if (window.location.pathname.includes('/checkout') ||
            window.location.pathname.includes('/order/confirmation')) {
            window.location.reload();
        }

    } else {
        if (!window.selectedAddress) {
            showMessage('Выберите адрес в зоне доставки', 'error');
            return;
        }

        const addressDetails = window.selectedAddress.details || parseAddressDetails(window.selectedAddress.text);

        deliveryInfo = {
            type: 'delivery',
            text: window.selectedAddress.text,
            zone: window.selectedAddress.zone
        };

        addressDataForForm = {
            type: 'delivery',
            text: window.selectedAddress.text,
            zone: window.selectedAddress.zone,
            details: addressDetails
        };

        localStorage.setItem('deliveryMode', 'delivery');
        localStorage.setItem('deliveryAddress', JSON.stringify({
            type: 'delivery',
            text: window.selectedAddress.text,
            zone: window.selectedAddress.zone,
            details: addressDetails
        }));

        updateHeaderAddress(window.selectedAddress.text);
        updateCheckoutForm(addressDataForForm);

        // Обновление информации о зоне через глобальную функцию из checkout.js
        if (window.selectedAddress.zone && typeof window.updateZoneInfoDisplay === 'function') {
            window.updateZoneInfoDisplay(window.selectedAddress.zone);
        }

        // ⭐ ПЕРЕЗАГРУЗКА ПОСЛЕ ВЫБОРА ДОСТАВКИ
        if (window.location.pathname.includes('/checkout') ||
            window.location.pathname.includes('/order/confirmation')) {
            window.location.reload();
        }
    }

    // Отправляем на сервер
    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

        const headers = {
            'Content-Type': 'application/json'
        };
        if (csrfToken) headers[csrfHeader] = csrfToken;

        const response = await fetch('/cart/delivery-info', {
            method: 'POST',
            headers: headers,
            credentials: 'include',
            body: JSON.stringify(deliveryInfo)
        });

        if (response.ok) {
            const data = await response.json();
            console.log('Информация о доставке сохранена:', data);
        }

    } catch (error) {
        console.error('Ошибка сохранения информации о доставке:', error);
    }

    const modal = bootstrap.Modal.getInstance(document.getElementById('addressModal'));
    if (modal) {
        modal.hide();
    } else {
        // Fallback: закрываем модальное окно вручную
        const modalElement = document.getElementById('addressModal');
        if (modalElement) {
            modalElement.style.display = 'none';
            modalElement.classList.remove('show');
            document.body.classList.remove('modal-open');
            document.body.style.overflow = '';
        }
    }

    // После выбора адреса проверяем, на какой странице мы находимся
    setTimeout(() => {
        const currentPath = window.location.pathname;

        // Если мы на странице корзины - автоматически переходим к оформлению
        if (currentPath === '/cart') {
            console.log('Адрес выбран на странице корзины, переходим к оформлению...');
            window.location.href = '/order/checkout';
        }
        // Если мы на странице оформления - просто обновляем данные
        else if (currentPath === '/order/checkout') {
            console.log('Адрес обновлен на странице оформления, обновляем страницу...');
            window.location.reload();
        }
    }, 100);

}

// Обновление адреса в хедере
function updateHeaderAddress(addressText) {
    const desktopSpan = document.getElementById('selectedAddressDesktop');
    const mobileSpan = document.getElementById('selectedAddressMobile');

    let displayText = addressText;

    if (displayText === 'Самовывоз') {
        displayText = '🏪 Самовывоз';
    } else {
        // Функция для извлечения только улицы и дома
        displayText = formatShortAddress(displayText);

        // Если всё еще длинный, обрезаем
        if (displayText.length > 30) {
            displayText = displayText.substring(0, 27) + '...';
        }
    }

    if (desktopSpan) desktopSpan.textContent = displayText;
    if (mobileSpan) mobileSpan.textContent = displayText;

    console.log('✅ Хедер обновлен:', displayText);
}

// Форматирование короткого адреса для отображения в хедере
function formatShortAddress(fullAddress) {
    if (!fullAddress) return '';

    let shortAddress = fullAddress;

    // 1. Убираем город
    shortAddress = shortAddress.replace(/^(г\.?\s*)?Пермь,\s*/i, '');
    shortAddress = shortAddress.replace(/^Пермь\s*/i, '');

    // 2. Убираем все типы улиц (полный список)
    shortAddress = shortAddress.replace(/^(улица|ул\.|проспект|пр\.|переулок|пер\.|бульвар|б-р\.|шоссе|ш\.|площадь|пл\.|набережная|наб\.|аллея|ал\.|линия|лин\.|проезд|пр-д|тупик|туп\.)\s*/i, '');

    // 3. Убираем "дом", "д.", "строение", "стр." и т.д.
    shortAddress = shortAddress.replace(/\s+(дом|д\.|строение|стр\.|корпус|к\.|литера|лит\.)\s*/gi, ' ');

    // 4. Убираем лишние пробелы
    shortAddress = shortAddress.replace(/\s+/g, ' ').trim();

    // 5. Приводим к нормальному виду (первая буква заглавная)
    if (shortAddress.length > 0) {
        shortAddress = shortAddress.charAt(0).toUpperCase() + shortAddress.slice(1);
    }

    // Если строка пустая, возвращаем исходный адрес
    return shortAddress || fullAddress;
}

// Обновление формы оформления заказа
function updateCheckoutForm(addressData) {
    const deliveryAddressField = document.getElementById('deliveryAddress');
    const entranceField = document.getElementById('deliveryEntrance');
    const floorField = document.getElementById('deliveryFloor');
    const apartmentField = document.getElementById('deliveryApartment');
    const intercomField = document.getElementById('deliveryIntercom');

    if (deliveryAddressField) {
        if (addressData.type === 'pickup') {
            deliveryAddressField.value = 'Самовывоз';
        } else {
            deliveryAddressField.value = addressData.text || '';
        }
        deliveryAddressField.dispatchEvent(new Event('change', { bubbles: true }));
        deliveryAddressField.dispatchEvent(new Event('input', { bubbles: true }));
    }

    if (addressData.type === 'delivery' && addressData.details) {
        if (entranceField && addressData.details.entrance) {
            entranceField.value = addressData.details.entrance;
            entranceField.dispatchEvent(new Event('change', { bubbles: true }));
        }
        if (floorField && addressData.details.floor) {
            floorField.value = addressData.details.floor;
            floorField.dispatchEvent(new Event('change', { bubbles: true }));
        }
        if (apartmentField && addressData.details.apartment) {
            apartmentField.value = addressData.details.apartment;
            apartmentField.dispatchEvent(new Event('change', { bubbles: true }));
        }
        if (intercomField && addressData.details.intercom) {
            intercomField.value = addressData.details.intercom;
            intercomField.dispatchEvent(new Event('change', { bubbles: true }));
        }
    }

    if (typeof window.saveAddressDetailsToStorage === 'function') {
        window.saveAddressDetailsToStorage();
    }

    console.log('✅ Форма обновлена:', addressData);
}

// Загрузка сохраненного адреса при загрузке страницы
function loadSavedAddressToForm() {
    const savedAddress = localStorage.getItem('deliveryAddress');
    if (savedAddress) {
        try {
            const addressData = JSON.parse(savedAddress);
            updateCheckoutForm(addressData);
            console.log('✅ Адрес загружен из localStorage при загрузке страницы');

            // Обновляем информацию о зоне, если это доставка
            if (addressData.type === 'delivery' && addressData.zone && typeof window.updateZoneInfoDisplay === 'function') {
                window.updateZoneInfoDisplay(addressData.zone);
            }
        } catch(e) {
            console.error('Ошибка загрузки адреса:', e);
        }
    }
}

// Показ сообщения (зеленые уведомления)
function showMessage(message, type = 'info') {
    let toastContainer = document.getElementById('clientToastContainer');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'clientToastContainer';
        toastContainer.className = 'position-fixed bottom-0 end-0 p-3';
        toastContainer.style.zIndex = '1100';
        document.body.appendChild(toastContainer);
    }

    const toast = document.createElement('div');
    // ⭐ ИСПРАВЛЕНО: warning тоже делаем зеленым
    let bgClass = 'success';
    if (type === 'error') {
        bgClass = 'danger';
    } else {
        bgClass = 'success'; // и warning, и info, и success - все зеленые
    }
    toast.className = `toast align-items-center text-white bg-${bgClass} border-0`;
    toast.role = 'alert';
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">${escapeHtml(message)}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;

    toastContainer.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast, { delay: 3000 });
    bsToast.show();

    toast.addEventListener('hidden.bs.toast', () => toast.remove());
}

// Экранирование HTML
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Функция для теста адреса
window.testPermAddress = async function() {
    const query = 'Пермь, Ленина 10';
    console.log('=== ТЕСТ АДРЕСА ЛЕНИНА 10 ===');

    try {
        const response = await ymaps.geocode(query, { results: 1 });
        const firstGeoObject = response.geoObjects.get(0);

        if (firstGeoObject) {
            const coordinates = firstGeoObject.geometry.getCoordinates();
            const address = firstGeoObject.getAddressLine();

            console.log('Найден адрес:', address);
            console.log('Координаты (lat, lng):', coordinates);

            const point = [coordinates[1], coordinates[0]];

            if (deliveryZones.length === 0) {
                await loadDeliveryZones();
            }

            let isInsideAny = false;
            deliveryZones.forEach(zone => {
                const isInside = isPointInPolygon(point, zone.points);
                console.log(`Зона "${zone.name}": внутри = ${isInside}`);
                if (isInside) isInsideAny = true;
            });

            console.log('Результат:', isInsideAny ? '✅ АДРЕС В ЗОНЕ!' : '❌ АДРЕС НЕ В ЗОНЕ');
            return { address, coordinates, isInside: isInsideAny };
        }
    } catch (error) {
        console.error('Ошибка:', error);
    }
};

// Загрузка сохраненных адресов пользователя
async function loadSavedAddresses() {
    console.log('loadSavedAddresses вызвана');

    // Если режим самовывоза - не загружаем адреса
    if (isPickupMode) {
        console.log('Режим самовывоза, сохраненные адреса не загружаем');
        const block = document.getElementById('savedAddressesBlock');
        if (block) block.style.display = 'none';
        return;
    }

    // ⭐ ПРОВЕРЯЕМ, АВТОРИЗОВАН ЛИ ПОЛЬЗОВАТЕЛЬ ⭐
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    if (!csrfToken) {
        console.log('Пользователь не авторизован, сохраненные адреса не загружаем');
        const block = document.getElementById('savedAddressesBlock');
        if (block) block.style.display = 'none';
        return;
    }

    try {
        const response = await fetch('/profile/addresses/api', {
            credentials: 'include',
            headers: {
                'X-CSRF-TOKEN': csrfToken
            }
        });

        console.log('Ответ от /profile/addresses/api:', response.status);

        if (response.ok) {
            // Проверяем Content-Type перед парсингом JSON
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                console.log('Ответ не является JSON, пользователь не авторизован');
                const block = document.getElementById('savedAddressesBlock');
                if (block) block.style.display = 'none';
                return;
            }

            const addresses = await response.json();
            console.log('Загружено сохраненных адресов:', addresses.length);

            const block = document.getElementById('savedAddressesBlock');
            const select = document.getElementById('savedAddressesSelect');

            if (addresses.length > 0 && block && select) {
                block.style.display = 'block';

                // Формируем опции для выпадающего списка
                select.innerHTML = '<option value="">-- Выберите сохраненный адрес --</option>' +
                    addresses.map(addr => {
                        // Формируем короткий текст для отображения
                        let displayText = addr.address;
                        if (displayText.length > 50) {
                            displayText = displayText.substring(0, 47) + '...';
                        }
                        return `
                            <option value='${JSON.stringify(addr)}'>
                                ${escapeHtml(displayText)} ${addr.isDefault ? '(по умолчанию)' : ''}
                            </option>
                        `;
                    }).join('');

                // Обработчик выбора адреса
                select.onchange = async function() {
                    const selectedValue = this.value;
                    if (selectedValue) {
                        const addressData = JSON.parse(selectedValue);
                        await selectSavedAddress(addressData, null);
                    }
                };
            } else if (block) {
                block.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('Ошибка загрузки сохраненных адресов:', error);
        const block = document.getElementById('savedAddressesBlock');
        if (block) block.style.display = 'none';
    }
}

// Выбор сохраненного адреса
async function selectSavedAddress(addressData, clickEvent) {
    const fullAddress = addressData.address;
    console.log('Выбран сохраненный адрес:', fullAddress);
    console.log('Детали адреса:', addressData);

    try {
        const response = await ymaps.geocode(fullAddress + ', Пермь', { results: 1 });
        const geoObject = response.geoObjects.get(0);

        if (geoObject) {
            const coordinates = geoObject.geometry.getCoordinates();

            if (clientMap) {
                clientMap.setCenter(coordinates, 15);
                addMarker(coordinates);
            }

            // ⭐ СОХРАНЯЕМ ДЕТАЛИ ИЗ СОХРАНЕННОГО АДРЕСА ⭐
            const addressDetails = {
                entrance: addressData.entrance || '',
                floor: addressData.floor || '',
                apartment: addressData.apartment || '',
                intercom: addressData.intercom || addressData.comment || ''
            };

            // Проверяем зону доставки
            checkDeliveryZone(coordinates, fullAddress);

            // Обновляем window.selectedAddress с деталями
            window.selectedAddress = {
                text: fullAddress,
                coordinates: coordinates,
                zone: window.selectedAddress?.zone || null,
                details: addressDetails
            };

            // Обновляем форму (через глобальную функцию из checkout.js)
            if (typeof window.updateCheckoutForm === 'function') {
                window.updateCheckoutForm({
                    type: 'delivery',
                    text: fullAddress,
                    details: addressDetails
                });
            }

            // Подсвечиваем выбранный адрес в списке
            document.querySelectorAll('.saved-address-item').forEach(item => {
                item.classList.remove('active');
            });
            if (clickEvent && clickEvent.currentTarget) {
                clickEvent.currentTarget.classList.add('active');
            }

            showMessage('Адрес выбран. Детали заполнены.', 'success');

        } else {
            showMessage('Не удалось определить координаты адреса', 'error');
        }
    } catch (error) {
        console.error('Ошибка геокодирования:', error);
        showMessage('Ошибка при выборе адреса', 'error');
    }
}

// Вызываем загрузку сохраненных адресов при открытии модалки
const addressModal = document.getElementById('addressModal');
if (addressModal) {
    addressModal.addEventListener('shown.bs.modal', function() {
        setTimeout(function() {
            if (clientMap) {
                clientMap.container.fitToViewport();
            }
        }, 100);
        initClientMap();

        // ⭐ Загружаем сохраненные адреса только если режим доставки И пользователь авторизован ⭐
        if (!isPickupMode) {
            // Проверяем авторизацию по наличию CSRF токена
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            if (csrfToken) {
                loadSavedAddresses();
            } else {
                console.log('Пользователь не авторизован, сохраненные адреса не загружаем');
                const block = document.getElementById('savedAddressesBlock');
                if (block) block.style.display = 'none';
            }
        } else {
            const block = document.getElementById('savedAddressesBlock');
            if (block) block.style.display = 'none';
        }
    });
}

window.formatShortAddress = formatShortAddress;

window.openAddressModal = function() {
    const modalElement = document.getElementById('addressModal');
    const modal = new bootstrap.Modal(modalElement, {
        backdrop: true,
        keyboard: true,
        focus: true
    });

    // Force proper positioning
    setTimeout(() => {
        const modalDialog = modalElement.querySelector('.modal-dialog');
        if (modalDialog) {
            modalDialog.style.position = 'fixed';
            modalDialog.style.top = '50%';
            modalDialog.style.left = '50%';
            modalDialog.style.transform = 'translate(-50%, -50%)';
            modalDialog.style.margin = '0';
            modalDialog.style.zIndex = '1060';
        }
    }, 100);

    modal.show();
};

// Добавляем функции в глобальную область
window.selectPickupPointFromList = selectPickupPointFromList;

// ========== МОБИЛЬНОЕ МЕНЮ - УЛУЧШЕНИЯ ==========
document.addEventListener('DOMContentLoaded', function() {
    // Автоматическое закрытие меню после клика по ссылке
    const navbarCollapse = document.getElementById('navbarNav');
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link, .navbar-nav .dropdown-item, .navbar-nav .btn');

    if (navbarCollapse) {
        navLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                // Не закрываем если это кнопка с формой или выпадающее меню
                if (link.tagName === 'BUTTON' || link.closest('.dropdown')) {
                    return;
                }

                // Закрываем меню
                const bsCollapse = bootstrap.Collapse.getInstance(navbarCollapse);
                if (bsCollapse) {
                    bsCollapse.hide();
                }
            });
        });

        // Закрытие меню при клике на оверлей
        document.addEventListener('click', function(e) {
            const navbar = document.querySelector('.navbar');
            const toggler = document.querySelector('.navbar-toggler');

            if (navbarCollapse.classList.contains('show')) {
                if (!navbar.contains(e.target) || toggler?.contains(e.target)) {
                    // Не закрываем если клик на toggler
                    return;
                }

                const bsCollapse = bootstrap.Collapse.getInstance(navbarCollapse);
                if (bsCollapse) {
                    bsCollapse.hide();
                }
            }
        });
    }

    // Предотвращаем зум на iOS при фокусе на input
    const inputs = document.querySelectorAll('input, select, textarea');
    inputs.forEach(input => {
        input.addEventListener('focus', function() {
            if (window.innerWidth <= 768) {
                this.style.fontSize = '16px';
            }
        });

        input.addEventListener('blur', function() {
            this.style.fontSize = '';
        });
    });
});