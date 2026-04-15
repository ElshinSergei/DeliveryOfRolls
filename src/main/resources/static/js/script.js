const contextPath = document.querySelector('meta[name="context-path"]')?.content || '';

// === ОСНОВНАЯ ИНИЦИАЛИЗАЦИЯ ===
document.addEventListener('DOMContentLoaded', function() {
    console.log('Script.js загружен');
    initHeaderEffect();
    initPromoSlider();
    initCartButtons();
    initDishModal();
});

// === ЗАГРУЗКА СОСТОЯНИЯ КОРЗИНЫ  ===
document.addEventListener('DOMContentLoaded', async function() {
    try {

        const response = await fetch(contextPath + '/cart/state?t=' + Date.now());
        const data = await response.json();

        if (data.status === 'success') {
            console.log('Состояние корзины загружено:', data);

            // Обновляем счетчик в шапке
            const cartCounter = document.getElementById('cartCount');
            if (cartCounter) {
                cartCounter.textContent = data.totalCount;
                console.log('Счетчик обновлен:', data.totalCount);
            }
            
            // Обновляем мобильный счетчик
            const mobileCartBadge = document.querySelector('.mobile-cart-badge');
            if (mobileCartBadge) {
                mobileCartBadge.textContent = data.totalCount;
            }

            // Получаем информацию о товарах в корзине
            const itemsInCart = data.items || {};

            // Проходим по всем карточкам блюд
            document.querySelectorAll('.my-dish-card').forEach(card => {
                const addBtn = card.querySelector('.add-to-cart-btn');
                if (!addBtn) return;

                const dishId = addBtn.dataset.dishId;
                const quantity = itemsInCart[dishId];

                if (quantity) {
                    // Товар уже есть в корзине
                    addBtn.style.display = 'none';

                    // Находим или создаем счетчик
                    let qtyDiv = document.getElementById(`quantity-${dishId}`);
                    if (!qtyDiv) {
                        // Если счетчика нет, создаем его
                        createQuantityControl(dishId, addBtn);
                        qtyDiv = document.getElementById(`quantity-${dishId}`);
                    }

                    if (qtyDiv) {
                        qtyDiv.style.display = 'flex';
                        const qtySpan = document.getElementById(`qty-${dishId}`);
                        if (qtySpan) qtySpan.textContent = quantity;
                    }
                }
            });
        }
    } catch (error) {
        console.error('Ошибка загрузки состояния корзины:', error);
    }
});

// === ЭФФЕКТ ДЛЯ HEADER ===
function initHeaderEffect() {
    const navbar = document.querySelector('.navbar');
    if (navbar) {
        window.addEventListener('scroll', function() {
            if (window.scrollY > 50) {
                navbar.classList.add('scrolled');
            } else {
                navbar.classList.remove('scrolled');
            }
        });
    }
}

// === СЛАЙДЕР АКЦИЙ С ПОДДЕРЖКОЙ ДРАГА И ИНЕРЦИЕЙ ===
function initPromoSlider() {
    const track = document.getElementById('promoSlider');
    const prevBtn = document.querySelector('.promo-slider-container .prev');
    const nextBtn = document.querySelector('.promo-slider-container .next');

    if (!track) return;

    // Переменные для драга
    let isDown = false;
    let startX;
    let scrollLeft;
    let velocity = 0;
    let lastX = 0;
    let lastTime = 0;
    let animationFrame;

    // Функция плавной прокрутки с инерцией
    function smoothScroll() {
        if (Math.abs(velocity) < 0.5) {
            cancelAnimationFrame(animationFrame);
            return;
        }

        velocity *= 0.95; // Затухание
        track.scrollLeft -= velocity;
        animationFrame = requestAnimationFrame(smoothScroll);
    }

    function startDrag(clientX) {
        // Останавливаем текущую анимацию
        if (animationFrame) {
            cancelAnimationFrame(animationFrame);
        }

        document.body.style.userSelect = 'none';
        isDown = true;
        track.style.cursor = 'grabbing';
        startX = clientX - track.offsetLeft;
        scrollLeft = track.scrollLeft;
        track.classList.add('dragging');

        // Сбрасываем скорость
        velocity = 0;
        lastX = clientX;
        lastTime = Date.now();
    }

    function endDrag() {
        document.body.style.userSelect = '';
        isDown = false;
        track.style.cursor = 'grab';
        track.classList.remove('dragging');

        // Запускаем инерцию, если скорость достаточная
        if (Math.abs(velocity) > 1) {
            animationFrame = requestAnimationFrame(smoothScroll);
        }
    }

    function moveDrag(clientX) {
        if (!isDown) return;

        const x = clientX - track.offsetLeft;
        const walk = (x - startX);
        track.scrollLeft = scrollLeft - walk;

        // Вычисляем скорость для инерции
        const now = Date.now();
        const dt = Math.max(16, now - lastTime); // минимум 16ms
        const newVelocity = (clientX - lastX) / dt * 20; // скорость в пикселях/секунду
        velocity = velocity * 0.7 + newVelocity * 0.3; // сглаживание
        lastX = clientX;
        lastTime = now;
    }

    // Предотвращаем перетаскивание картинок
    track.querySelectorAll('img').forEach(img => {
        img.addEventListener('dragstart', (e) => {
            e.preventDefault();
            return false;
        });
    });

    // Обработчики для трека
    track.addEventListener('mousedown', (e) => {
        if (e.target.closest('.slider-nav')) return;
        startDrag(e.pageX);
    });

    document.addEventListener('mouseup', endDrag);
    document.addEventListener('mousemove', (e) => {
        if (!isDown) return;
        e.preventDefault();
        moveDrag(e.pageX);
    });

    // Для тач-экранов
    track.addEventListener('touchstart', (e) => {
        if (e.target.closest('.slider-nav')) return;
        startDrag(e.touches[0].pageX);
    });

    track.addEventListener('touchmove', (e) => {
        if (!isDown) return;
        e.preventDefault();
        moveDrag(e.touches[0].pageX);
    });

    track.addEventListener('touchend', endDrag);

    // Кнопки навигации с плавной прокруткой
    if (prevBtn && nextBtn) {
        const scrollAmount = 270;

        prevBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();

            // Останавливаем текущую инерцию
            if (animationFrame) {
                cancelAnimationFrame(animationFrame);
            }

            track.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
        });

        nextBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();

            if (animationFrame) {
                cancelAnimationFrame(animationFrame);
            }

            track.scrollBy({ left: scrollAmount, behavior: 'smooth' });
        });
    }

    // Устанавливаем курсор
    track.style.cursor = 'grab';
}

// === КНОПКИ КОРЗИНЫ И СЧЕТЧИК ===
function initCartButtons() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    const cartCounter = document.getElementById('cartCount');

    // Кнопки "В корзину"
    document.querySelectorAll('.add-to-cart-btn').forEach(btn => {
        btn.addEventListener('click', async function(e) {
            e.preventDefault();
            e.stopPropagation();

            const dishId = this.dataset.dishId;
            const dishName = this.dataset.dishName;

            try {
                const response = await fetch(contextPath + `/cart/add/${dishId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        [csrfHeader]: csrfToken
                    },
                    body: new URLSearchParams({ 'quantity': 1 })
                });

                const data = await response.json();

                if (data.status === 'success') {
                    // Прячем кнопку, показываем счетчик
                    this.style.display = 'none';
                    const qtyDiv = document.getElementById(`quantity-${dishId}`);
                    if (qtyDiv) {
                        qtyDiv.style.display = 'flex';
                        const qtySpan = document.getElementById(`qty-${dishId}`);
                        if (qtySpan) qtySpan.textContent = '1';
                    }

                    // Добавляем класс has-in-cart к карточке для мобильных стилей
                    const card = this.closest('.my-dish-card');
                    if (card) {
                        card.classList.add('has-in-cart');
                    }

                    // Обновляем оба счетчика - десктопный и мобильный
                    if (cartCounter) cartCounter.textContent = data.cartCount;
                    const mobileCartBadge = document.querySelector('.mobile-cart-badge');
                    if (mobileCartBadge) mobileCartBadge.textContent = data.cartCount;
                    
                    showNotification('success', 'Добавлено в корзину', dishName);
                }
            } catch (error) {
                console.error('Error:', error);
                showNotification('error', '❌ Ошибка', 'Не удалось добавить товар');
            }
        });
    });

    // Кнопки "+" (увеличение)
    document.querySelectorAll('.quantity-plus').forEach(btn => {
        btn.addEventListener('click', async function(e) {
            if (window.location.pathname === '/cart') {
                return; // Не обрабатываем на странице корзины
            }
            e.preventDefault();
            e.stopPropagation();

            const dishId = this.dataset.dishId;
            const dishName = this.dataset.dishName;
            const qtySpan = document.getElementById(`qty-${dishId}`);
            let currentQty = parseInt(qtySpan.textContent);

            try {
                const response = await fetch(contextPath + `/cart/add/${dishId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        [csrfHeader]: csrfToken
                    },
                    body: new URLSearchParams({ 'quantity': 1 })
                });

                if (!response.ok) {
                    throw new Error('Ошибка сервера');
                }

                const data = await response.json();

                if (data.status === 'success') {
                    currentQty++;
                    qtySpan.textContent = currentQty;
                    if (cartCounter) cartCounter.textContent = data.cartCount;
                    const mobileCartBadge = document.querySelector('.mobile-cart-badge');
                    if (mobileCartBadge) mobileCartBadge.textContent = data.cartCount;
                    showNotification('success', '➕ Добавлено еще', dishName);
                }
            } catch (error) {
                console.error('Error:', error);
                showNotification('error', '❌ Ошибка', 'Не удалось добавить товар');
            }
        });
    });

    // Кнопки "-" (уменьшение)
    document.querySelectorAll('.quantity-minus').forEach(btn => {
        btn.addEventListener('click', async function(e) {
            if (window.location.pathname === '/cart') {
                return; // Не обрабатываем на странице корзины
            }
            e.preventDefault();
            e.stopPropagation();

            const dishId = this.dataset.dishId;
            const dishName = this.dataset.dishName;
            const qtySpan = document.getElementById(`qty-${dishId}`);

            try {
                const response = await fetch(contextPath + `/cart/decrease-dish/${dishId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        [csrfHeader]: csrfToken
                    },
                    body: new URLSearchParams() // Пустой body
                });

                if (!response.ok) {
                    throw new Error('Ошибка сервера');
                }

                const data = await response.json();

                if (data.status === 'success') {
                    let currentQty = parseInt(qtySpan.textContent) - 1;

                    if (currentQty <= 0) {
                        // Если 0 - показываем кнопку "В корзину"
                        document.getElementById(`quantity-${dishId}`).style.display = 'none';
                        const addBtn = document.querySelector(`.add-to-cart-btn[data-dish-id="${dishId}"]`);
                        if (addBtn) {
                            addBtn.style.display = 'block';
                        }

                        // Удаляем класс has-in-cart с карточки
                        const card = addBtn?.closest('.my-dish-card');
                        if (card) {
                            card.classList.remove('has-in-cart');
                        }
                    } else {
                        qtySpan.textContent = currentQty;
                    }

                    // Обновляем счетчик в шапке
                    if (cartCounter) cartCounter.textContent = data.cartCount;
                    const mobileCartBadge = document.querySelector('.mobile-cart-badge');
                    if (mobileCartBadge) mobileCartBadge.textContent = data.cartCount;
                    showNotification('info', 'Количество уменьшено', dishName);
                }
            } catch (error) {
                console.error('Error:', error);
                showNotification('error', '❌ Ошибка', 'Не удалось уменьшить количество');
            }
        });
    });
}

// === МОДАЛЬНОЕ ОКНО ===
function initDishModal() {
    const dishCards = document.querySelectorAll('.my-dish-card');

    dishCards.forEach(card => {
        card.addEventListener('click', function(e) {
            // Не открываем модалку при клике на кнопки
            if (e.target.closest('.add-to-cart-btn') ||
                e.target.closest('.add-more-btn') ||
                e.target.closest('.quantity-simple')) {
                return;
            }

            // Собираем данные
            const dishId = this.querySelector('.add-to-cart-btn')?.dataset.dishId;
            const dishName = this.querySelector('.card-title')?.textContent || '';
            const dishCategory = this.querySelector('.small.text-muted span')?.textContent || '';
            const dishPrice = this.querySelector('.dish-price')?.textContent || '';
            const dishDescription = this.querySelector('.full-description')?.value || '';
            const dishIngredients = this.querySelector('.full-ingredients')?.value || '—';
            const dishWeight = this.querySelector('.d-flex.justify-content-between.mb-3 span:first-child')?.textContent.trim() || '—';
            const dishCalories = this.querySelector('.d-flex.justify-content-between.mb-3 span:last-child')?.textContent.trim() || '—';
            const dishImage = this.querySelector('.dish-img')?.src || '';

            // Заполняем модалку
            document.getElementById('customModalName').textContent = dishName;
            document.getElementById('customModalCategory').textContent = dishCategory;
            document.getElementById('customModalPrice').textContent = dishPrice;
            document.getElementById('customModalDescription').textContent = dishDescription;
            document.getElementById('customModalIngredients').textContent = dishIngredients;
            document.getElementById('customModalWeight').textContent = dishWeight;
            document.getElementById('customModalCalories').textContent = dishCalories;
            document.getElementById('customModalImage').src = dishImage;
            document.getElementById('customModalAddToCart').setAttribute('data-dish-id', dishId);

            openCustomModal();
        });
    });

    // Кнопка в корзину в модалке
    const modalAddBtn = document.getElementById('customModalAddToCart');
    if (modalAddBtn) {
        modalAddBtn.addEventListener('click', function() {
            const dishId = this.getAttribute('data-dish-id');
            const btn = document.querySelector(`.add-to-cart-btn[data-dish-id="${dishId}"]`);
            if (btn) {
                btn.click();
            }
            closeCustomModal();
        });
    }

    // Закрытие по клику на затемнение
    const modalOverlay = document.getElementById('modalOverlay');
    if (modalOverlay) {
        modalOverlay.addEventListener('click', closeCustomModal);
    }

    // Закрытие по кнопке "Закрыть" в модалке
    const closeModalBtn = document.querySelector('#myCustomModal .btn-secondary');
    if (closeModalBtn) {
        closeModalBtn.addEventListener('click', closeCustomModal);
    }
}

// === ФУНКЦИИ УПРАВЛЕНИЯ МОДАЛКОЙ ===
function openCustomModal() {
    const modal = document.getElementById('myCustomModal');
    if (modal) {
        modal.style.display = 'flex';
        modal.style.visibility = 'visible';
        modal.style.pointerEvents = 'auto';
        document.body.style.overflow = 'hidden';
        document.body.style.pointerEvents = 'auto';
    }
}

function closeCustomModal() {
    const modal = document.getElementById('myCustomModal');
    if (modal) {
        modal.style.display = 'none';
        modal.style.visibility = 'hidden';
        modal.style.pointerEvents = 'none';
        document.body.style.overflow = 'auto';
        document.body.style.pointerEvents = 'auto';
    }
}

// Закрытие по Escape
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        const modal = document.getElementById('myCustomModal');
        if (modal && modal.style.display === 'block') {
            closeCustomModal();
        }
    }
});

// === УВЕДОМЛЕНИЯ ===
function showNotification(type, title, message) {
    // Удаляем старое уведомление, если есть
    const oldNotification = document.querySelector('.modern-notification');
    if (oldNotification) oldNotification.remove();

    // Создаем контейнер
    const notification = document.createElement('div');
    notification.className = `modern-notification modern-notification-${type}`;

    // Иконки для разных типов
    const icons = {
        success: '✓',
        error: '✗',
        info: 'ℹ',
        warning: '⚠'
    };

    notification.innerHTML = `
        <div class="notification-icon">${icons[type] || icons.success}</div>
        <div class="notification-content">
            <div class="notification-title">${title}</div>
            <div class="notification-message">${message}</div>
        </div>
        <button class="notification-close">×</button>
        <div class="notification-progress"></div>
    `;

    document.body.appendChild(notification);

    // Анимация появления
    setTimeout(() => notification.classList.add('show'), 10);

    // Закрытие по кнопке
    const closeBtn = notification.querySelector('.notification-close');
    closeBtn.addEventListener('click', () => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    });

    // Автоматическое закрытие через 3 секунды
    setTimeout(() => {
        if (notification && notification.parentElement) {
            notification.classList.remove('show');
            setTimeout(() => notification.remove(), 300);
        }
    }, 3000);
}

// === АКСЕССУАРЫ В КОРЗИНЕ ===
function initCartAccessories() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    function getItemsText(count) {
        if (count % 10 === 1 && count % 100 !== 11) return 'товар';
        if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) return 'товара';
        return 'товаров';
    }

    async function updateCartTotal() {
        try {
            const response = await fetch(contextPath + '/cart/state?t=' + Date.now());
            const data = await response.json();

            if (data.status === 'success') {
                let totalAmount = data.totalAmount ?? data.totalPrice ?? 0;

                if (totalAmount === 0 && data.items) {
                    totalAmount = Object.values(data.items).reduce((sum, item) => sum + (item.totalPrice || 0), 0);
                }

                const subtotalEl = document.getElementById('subtotal-value');
                if (subtotalEl) subtotalEl.textContent = totalAmount.toLocaleString('ru-RU') + ' ₽';

                const cartTotalEl = document.getElementById('cart-total');
                if (cartTotalEl) cartTotalEl.textContent = totalAmount.toLocaleString('ru-RU') + ' ₽';

                const cartSubtitle = document.querySelector('.cart-subtitle');
                if (cartSubtitle && data.totalCount > 0) {
                    const itemsText = getItemsText(data.totalCount);
                    cartSubtitle.innerHTML = `<i class="bi bi-basket me-2"></i>${data.totalCount} ${itemsText} на сумму <span class="cart-total-amount">${totalAmount.toLocaleString('ru-RU')} ₽</span>`;
                }
            }
        } catch (error) {
            console.error('Ошибка обновления корзины:', error);
        }
    }

    // ✅ ИСПРАВЛЕННЫЙ ПЛЮС (с правильным обходом всех кнопок)
    document.querySelectorAll('.accessory-plus').forEach(btn => {
        // Удаляем старый обработчик, чтобы не дублировать
        btn.removeEventListener('click', btn._handler);

        const handler = async function() {
            const dishId = this.dataset.dishId;
            const accessoryItem = this.closest('.accessory-item');
            const dishName = accessoryItem?.querySelector('.accessory-name')?.textContent || 'Аксессуар';
            const qtySpan = document.getElementById(`accessory-qty-${dishId}`);
            const currentQty = qtySpan ? parseInt(qtySpan.textContent) : 0;

            try {
                const response = await fetch(contextPath + `/cart/add/${dishId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        [csrfHeader]: csrfToken
                    },
                    body: new URLSearchParams({ 'quantity': 1 })
                });

                const data = await response.json();

                if (data.status === 'success') {
                    if (qtySpan) qtySpan.textContent = currentQty + 1;
                    await updateCartTotal();
                    showNotification('success', 'Добавлено', `${dishName} добавлен в корзину`);
                } else {
                    showNotification('error', '❌ Ошибка', `Не удалось добавить ${dishName}`);
                }
            } catch (error) {
                console.error('Ошибка:', error);
                showNotification('error', '❌ Ошибка сервера', `Попробуйте позже`);
            }
        };

        btn._handler = handler;
        btn.addEventListener('click', handler);
    });

    // ✅ ИСПРАВЛЕННЫЙ МИНУС
    document.querySelectorAll('.accessory-minus').forEach(btn => {
        btn.removeEventListener('click', btn._handler);

        const handler = async function() {
            const dishId = this.dataset.dishId;
            const accessoryItem = this.closest('.accessory-item');
            const dishName = accessoryItem?.querySelector('.accessory-name')?.textContent || 'Аксессуар';
            const qtySpan = document.getElementById(`accessory-qty-${dishId}`);
            const currentQty = qtySpan ? parseInt(qtySpan.textContent) : 0;

            if (currentQty <= 0) {
                showNotification('warning', '⚠️ Внимание', `${dishName} уже нет в корзине`);
                return;
            }

            try {
                const response = await fetch(contextPath + `/cart/decrease-dish/${dishId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        [csrfHeader]: csrfToken
                    },
                    body: new URLSearchParams()
                });

                const data = await response.json();

                if (data.status === 'success') {
                    if (qtySpan) qtySpan.textContent = currentQty - 1;
                    await updateCartTotal();
                    showNotification('info', 'Удалено', `${dishName} удален из корзины`);
                } else {
                    showNotification('error', '❌ Ошибка', `Не удалось удалить ${dishName}`);
                }
            } catch (error) {
                console.error('Ошибка:', error);
                showNotification('error', '❌ Ошибка сервера', `Попробуйте позже`);
            }
        };

        btn._handler = handler;
        btn.addEventListener('click', handler);
    });
}

// Инициализация аксессуаров
document.addEventListener('DOMContentLoaded', function() {
    if (document.querySelector('.cart-accessories')) {
        initCartAccessories();
    }
});

// ========== МОБИЛЬНЫЕ УЛУЧШЕНИЯ ==========
document.addEventListener('DOMContentLoaded', function() {
    // Обработка активного состояния кнопок на мобильных
    const allButtons = document.querySelectorAll('button, .btn, .category-link');

    allButtons.forEach(btn => {
        btn.addEventListener('touchstart', function() {
            this.style.transform = 'scale(0.98)';
        });

        btn.addEventListener('touchend', function() {
            this.style.transform = '';
        });

        btn.addEventListener('touchcancel', function() {
            this.style.transform = '';
        });
    });
});

// Обработка динамической высоты на мобильных (для iOS)
function handleMobileViewport() {
    let vh = window.innerHeight * 0.01;
    document.documentElement.style.setProperty('--vh', `${vh}px`);
}

window.addEventListener('resize', handleMobileViewport);
window.addEventListener('orientationchange', handleMobileViewport);
handleMobileViewport();

// ========== КАТЕГОРИИ - ПОЛНОСТЬЮ ИСПРАВЛЕННАЯ ВЕРСИЯ ==========
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(function() {
        const categoryLinks = document.querySelectorAll('.category-link');
        const categoryBlocks = document.querySelectorAll('.category-block');

        if (categoryLinks.length === 0) return;

        function getScrollOffset() {
            const header = document.querySelector('header .navbar');
            const stickyNav = document.querySelector('.categories-sticky-nav');
            let offset = 0;
            if (header) offset += header.offsetHeight;
            if (stickyNav) offset += stickyNav.offsetHeight;
            return offset;
        }

        function scrollToCategory(targetElement) {
            if (!targetElement) return;
            const offset = getScrollOffset();
            const rect = targetElement.getBoundingClientRect();
            const scrollTop = window.pageYOffset;
            const targetPosition = rect.top + scrollTop - offset;
            window.scrollTo({ top: targetPosition, behavior: 'smooth' });
        }

        categoryLinks.forEach(link => {
            const newLink = link.cloneNode(true);
            link.parentNode.replaceChild(newLink, link);

            newLink.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                const targetId = this.getAttribute('href');
                if (!targetId || targetId === '#') return;
                const targetElement = document.querySelector(targetId);
                if (!targetElement) return;
                scrollToCategory(targetElement);
                document.querySelectorAll('.category-link').forEach(l => l.classList.remove('active'));
                this.classList.add('active');
            });
        });

        function updateActiveCategory() {
            const offset = getScrollOffset() + 50;
            const scrollPosition = window.scrollY + offset;
            let activeCategory = null;
            categoryBlocks.forEach(block => {
                if (block.offsetTop <= scrollPosition) {
                    activeCategory = block.id;
                }
            });
            if (!activeCategory && categoryBlocks.length > 0) {
                activeCategory = categoryBlocks[0].id;
            }
            categoryLinks.forEach(link => {
                if (link.dataset.category === activeCategory) {
                    link.classList.add('active');
                } else {
                    link.classList.remove('active');
                }
            });
        }

        window.addEventListener('scroll', function() {
            requestAnimationFrame(updateActiveCategory);
        });
        updateActiveCategory();
    }, 100);
});
