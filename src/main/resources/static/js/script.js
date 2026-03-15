const contextPath = document.querySelector('meta[name="context-path"]')?.content || '';

// === ОСНОВНАЯ ИНИЦИАЛИЗАЦИЯ ===
document.addEventListener('DOMContentLoaded', function() {
    console.log('Script.js загружен');

    initCategoryHighlight();
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

                // Получаем информацию о товарах в корзине
                const itemsInCart = data.items || {};

                // Проходим по всем карточкам блюд
                document.querySelectorAll('.dish-card').forEach(card => {
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

// === ПОДСВЕТКА КАТЕГОРИЙ ===
function initCategoryHighlight() {
    const navLinks = document.querySelectorAll('.category-link');

    window.addEventListener('scroll', function() {
        const scrollPosition = window.scrollY + 200;
        let currentCategory = 'all';

        document.querySelectorAll('.category-block').forEach(block => {
            const blockTop = block.offsetTop;
            const blockBottom = blockTop + block.offsetHeight;

            if (scrollPosition >= blockTop && scrollPosition < blockBottom) {
                currentCategory = block.id;
            }
        });

        navLinks.forEach(link => {
            link.classList.remove('active');
            if (link.dataset.category === currentCategory) {
                link.classList.add('active');
            }
        });
    });
}

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

// === СЛАЙДЕР АКЦИЙ ===
function initPromoSlider() {
    const track = document.getElementById('promoSlider');
    const prevBtn = document.querySelector('.promo-slider-container .prev');
    const nextBtn = document.querySelector('.promo-slider-container .next');

    if (!track || !prevBtn || !nextBtn) return;

    const scrollAmount = 270;

    prevBtn.addEventListener('click', () => {
        track.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
    });

    nextBtn.addEventListener('click', () => {
        track.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    });
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

                    if (cartCounter) cartCounter.textContent = data.cartCount;
                    showNotification('success', '✅ Добавлено в корзину', dishName);
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
                        document.querySelector(`.add-to-cart-btn[data-dish-id="${dishId}"]`).style.display = 'block';
                    } else {
                        qtySpan.textContent = currentQty;
                    }

                    // Обновляем счетчик в шапке
                    if (cartCounter) cartCounter.textContent = data.cartCount;
                    showNotification('info', '➖ Количество уменьшено', dishName);
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
    const dishCards = document.querySelectorAll('.dish-card');

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
}

// === ФУНКЦИИ УПРАВЛЕНИЯ МОДАЛКОЙ ===
function openCustomModal() {
    const modal = document.getElementById('myCustomModal');
    if (modal) {
        modal.style.display = 'block';
        document.body.style.overflow = 'hidden';
    }
}

function closeCustomModal() {
    const modal = document.getElementById('myCustomModal');
    if (modal) {
        modal.style.display = 'none';
        document.body.style.overflow = '';
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
    const oldNotification = document.querySelector('.cart-notification');
    if (oldNotification) oldNotification.remove();

    const notification = document.createElement('div');
    notification.className = `cart-notification cart-notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <strong>${title}</strong>
            <p>${message}</p>
        </div>
        <button class="notification-close" onclick="this.parentElement.remove()">×</button>
    `;

    document.body.appendChild(notification);

    setTimeout(() => notification.classList.add('show'), 10);
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}