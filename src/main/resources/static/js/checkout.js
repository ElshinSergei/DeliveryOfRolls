// ========== ГЛОБАЛЬНЫЕ ПЕРЕМЕННЫЕ ==========
let currentDiscount = 0;
let originalTotal = 0;
let currentBonusDiscount = 0;

// ========== СИНХРОНИЗАЦИЯ С ХЕДЕРОМ ==========
function syncDeliveryTypeFromHeader() {
    const deliveryMode = localStorage.getItem('deliveryMode');
    const savedAddress = localStorage.getItem('deliveryAddress');

    const hiddenDeliveryType = document.getElementById('deliveryType');
    if (hiddenDeliveryType) {
        hiddenDeliveryType.value = deliveryMode ? deliveryMode.toUpperCase() : 'DELIVERY';
    }

    // Обновляем видимость адреса
    updateAddressVisibility();

    if (deliveryMode === 'pickup') {
        // Удаляем информацию о зоне
        document.querySelectorAll('.zone-info-card').forEach(el => el.remove());
        document.querySelectorAll('.min-order-warning-card').forEach(el => el.remove());

        // Добавляем информацию о самовывозе
        if (savedAddress) {
            try {
                const addressData = JSON.parse(savedAddress);
                if (addressData.point) {
                    addPickupInfoAfterLoad(addressData.point);
                }
            } catch(e) {}
        }
    } else {
        // Удаляем информацию о самовывозе
        document.querySelectorAll('.pickup-info').forEach(el => el.remove());
    }
}

// Функция обновления видимости адреса
function updateAddressVisibility() {
    const deliveryMode = localStorage.getItem('deliveryMode');
    const addressGroup = document.getElementById('addressGroup');
    const addressHint = document.getElementById('addressHint');
    const addressField = document.getElementById('deliveryAddress');

    // Добавьте проверку
    if (!addressGroup) return;

    if (deliveryMode === 'pickup') {
        if (addressGroup) addressGroup.style.display = 'none';
        if (addressField) addressField.removeAttribute('required');
        if (addressHint) {
            addressHint.innerHTML = '<i class="bi bi-info-circle"></i> Вы выбрали самовывоз. Заберите заказ по адресу:';
        }
        toggleAddressDetails(false);
    } else {
        if (addressGroup) addressGroup.style.display = 'block';
        if (addressField) addressField.setAttribute('required', 'required');
        if (addressHint) {
            addressHint.innerHTML = '<i class="bi bi-info-circle"></i> Адрес доставки можно изменить в шапке сайта';
        }
        toggleAddressDetails(true);
    }
}

// Слушаем изменения в localStorage
window.addEventListener('storage', function(e) {
    if (e.key === 'deliveryMode' || e.key === 'deliveryAddress') {
        syncDeliveryTypeFromHeader();
        loadAddressFromStorage();
        updateAddressVisibility();
    }
});


// ========== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========
function getOriginalTotal() {
    const subtotalElement = document.getElementById('subtotal');
    if (subtotalElement) {
        const text = subtotalElement.textContent;
        const match = text.match(/(\d+[\s]?\d*)/);
        if (match) {
            return parseFloat(match[0].replace(/\s/g, ''));
        }
    }

    const totalElement = document.getElementById('totalAmount');
    if (totalElement) {
        const text = totalElement.textContent;
        const match = text.match(/(\d+[\s]?\d*)/);
        if (match) {
            return parseFloat(match[0].replace(/\s/g, ''));
        }
    }

    return 0;
}

function togglePromo() {
    const section = document.querySelector('.promo-section');
    const content = document.getElementById('promoContent');

    if (section && content) {
        section.classList.toggle('expanded');
        content.style.display = content.style.display === 'none' ? 'block' : 'none';
    }
}

function toggleBonus() {
    const section = document.querySelector('.bonus-section');
    const content = document.getElementById('bonusContent');

    if (section && content) {
        section.classList.toggle('expanded');
        content.style.display = content.style.display === 'none' ? 'block' : 'none';
    }
}

function showMessage(text, type) {
    const messageEl = document.getElementById('promoMessage');
    if (messageEl) {
        messageEl.textContent = text;
        messageEl.className = 'promo-message ' + type;
        messageEl.style.display = 'block';

        setTimeout(() => {
            if (messageEl) {
                messageEl.style.display = 'none';
            }
        }, 5000);
    }
}

function showBonusMessage(text, type) {
    const messageEl = document.getElementById('bonusMessage');
    if (messageEl) {
        messageEl.textContent = text;
        messageEl.className = 'bonus-message ' + type;
        messageEl.style.display = 'block';

        setTimeout(() => {
            if (messageEl) {
                messageEl.style.display = 'none';
            }
        }, 5000);
    }
}

function updateTotalWithDiscount(discount, finalAmount) {
    const totalElement = document.getElementById('totalAmount');

    const mobileTotalElement = document.getElementById('mobileTotalAmount');
    const mobileTotalFinal = document.getElementById('mobileTotalFinal');
    const mobileDiscountRow = document.getElementById('mobileDiscountRow');
    const mobileDiscountAmount = document.getElementById('mobileDiscountAmount');

    const desktopDiscountRow = document.getElementById('desktopDiscountRow');
    const desktopDiscountAmount = document.getElementById('desktopDiscountAmount');

    const formattedFinal = Math.round(finalAmount).toLocaleString('ru-RU') + ' ₽';
    const formattedDiscount = '-' + Math.round(discount).toLocaleString('ru-RU') + ' ₽';

    // Обновляем итоговые суммы
    if (totalElement) totalElement.textContent = formattedFinal;
    if (mobileTotalElement) mobileTotalElement.textContent = formattedFinal;
    if (mobileTotalFinal) mobileTotalFinal.textContent = formattedFinal;

    if (discount > 0) {
        // Показываем строку со скидкой
        if (mobileDiscountAmount) mobileDiscountAmount.textContent = formattedDiscount;
        if (mobileDiscountRow) mobileDiscountRow.style.display = 'flex';
        if (desktopDiscountAmount) desktopDiscountAmount.textContent = formattedDiscount;
        if (desktopDiscountRow) desktopDiscountRow.style.display = 'flex';
    } else {
        // ✅ СКРЫВАЕМ строку (КАК У БОНУСОВ!)
        if (mobileDiscountRow) {
            mobileDiscountRow.style.display = 'none';
            mobileDiscountRow.style.setProperty('display', 'none', 'important');
        }
        if (desktopDiscountRow) {
            desktopDiscountRow.style.display = 'none';
            desktopDiscountRow.style.setProperty('display', 'none', 'important');
        }

        // Сбрасываем текст
        if (mobileDiscountAmount) mobileDiscountAmount.textContent = '-0 ₽';
        if (desktopDiscountAmount) desktopDiscountAmount.textContent = '-0 ₽';
    }
}

function updateBonusDiscount(discount, finalAmount) {
    currentBonusDiscount = discount;

    if (finalAmount === undefined) {
        const subtotalElement = document.getElementById('subtotal');
        if (subtotalElement) {
            const text = subtotalElement.textContent;
            const match = text.match(/(\d+[\s]?\d*)/);
            if (match) {
                const originalTotalValue = parseFloat(match[0].replace(/\s/g, ''));
                finalAmount = originalTotalValue - discount;
            } else {
                return;
            }
        } else {
            return;
        }
    }

    const totalElement = document.getElementById('totalAmount');
    const mobileTotalElement = document.getElementById('mobileTotalAmount');
    const mobileTotalFinal = document.getElementById('mobileTotalFinal');

    const formattedFinal = finalAmount.toLocaleString('ru-RU') + ' ₽';

    if (totalElement) totalElement.textContent = formattedFinal;
    if (mobileTotalElement) mobileTotalElement.textContent = formattedFinal;
    if (mobileTotalFinal) mobileTotalFinal.textContent = formattedFinal;

    // ⭐ ПОКАЗЫВАЕМ СТРОКУ С БОНУСНОЙ СКИДКОЙ (если discount > 0) ⭐
    showBonusDiscountRow(discount);
}

function showBonusDiscountRow(discount) {

    const mobileBonusDiscountRow = document.getElementById('mobileBonusDiscountRow');
    const mobileBonusDiscountAmount = document.getElementById('mobileBonusDiscountAmount');
    const desktopBonusDiscountRow = document.getElementById('desktopBonusDiscountRow');
    const desktopBonusDiscountAmount = document.getElementById('desktopBonusDiscountAmount');

    if (discount > 0) {
        const formattedBonus = `-${discount.toLocaleString('ru-RU')} ₽`;

        if (mobileBonusDiscountAmount) mobileBonusDiscountAmount.textContent = formattedBonus;
        if (mobileBonusDiscountRow) mobileBonusDiscountRow.style.display = 'flex';
        if (desktopBonusDiscountAmount) desktopBonusDiscountAmount.textContent = formattedBonus;
        if (desktopBonusDiscountRow) desktopBonusDiscountRow.style.display = 'flex';
    } else {

        if (mobileBonusDiscountRow) mobileBonusDiscountRow.style.display = 'none';
        if (desktopBonusDiscountRow) desktopBonusDiscountRow.style.display = 'none';
        if (mobileBonusDiscountAmount) mobileBonusDiscountAmount.textContent = '-0 ₽';
        if (desktopBonusDiscountAmount) desktopBonusDiscountAmount.textContent = '-0 ₽';
    }
}

function updateDiscountDisplay() {
    const discountRows = document.querySelectorAll('.discount-row');
    discountRows.forEach(row => {
        const discountAmount = row.querySelector('.discount-amount')?.textContent;
        if (discountAmount === '-0 ₽' || !discountAmount || discountAmount === '0 ₽') {
            row.style.display = 'none';
        }
    });
}

function convertTimeToLocalDateTime(timeStr) {
    if (!timeStr || timeStr === '') {
        return '';
    }

    const now = new Date();
    const [hours, minutes] = timeStr.split(':');
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}:00`;
}

function restoreDeliveryTime() {
    const deliveryTimeSelect = document.getElementById('deliveryTimeSelect');
    const deliveryTimeHidden = document.getElementById('deliveryTime');

    if (deliveryTimeSelect && deliveryTimeHidden) {
        const savedValue = deliveryTimeHidden.value;
        if (savedValue) {
            const match = savedValue.match(/(\d{2}):(\d{2})/);
            if (match) {
                const timeStr = `${match[1]}:${match[2]}`;
                if (Array.from(deliveryTimeSelect.options).some(opt => opt.value === timeStr)) {
                    deliveryTimeSelect.value = timeStr;
                } else {
                    deliveryTimeSelect.value = '';
                }
            }
        }
    }
}

// ========== ЛОГИКА ДЛЯ ПРОМОКОДА ==========
async function applyPromoCode() {
    const usedBonuses = document.getElementById('usedBonuses')?.value;
    if (usedBonuses && parseInt(usedBonuses) > 0) {
        showMessage('Нельзя применить промокод вместе с бонусами', 'error');
        return;
    }

    const input = document.getElementById('promoCodeInput');
    const code = input ? input.value.trim().toUpperCase() : '';
    const applyBtn = document.getElementById('applyPromoBtn');

    if (!code) {
        showMessage('Введите промокод', 'error');
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    if (applyBtn) {
        applyBtn.disabled = true;
        applyBtn.textContent = 'Проверка...';
    }

    try {
        const headers = {
            'Content-Type': 'application/json',
        };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch('/api/promo/apply', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ code: code })
        });

        const data = await response.json();

        if (data.valid) {
            showMessage('✓ ' + data.message, 'success');
            const appliedPromoField = document.getElementById('appliedPromoCode');
            if (appliedPromoField) appliedPromoField.value = code;

            let discountText = data.discountText;
            if (!discountText && data.discountAmount) {
                discountText = data.discountAmount < 100
                    ? Math.round(data.discountAmount) + '%'
                    : Math.round(data.discountAmount) + ' ₽';
            }

            const discountTextEl = document.getElementById('discountText');
            if (discountTextEl) discountTextEl.textContent = discountText || '';
            const mobileDiscountText = document.getElementById('mobileDiscountText');
            if (mobileDiscountText) mobileDiscountText.textContent = discountText || '';
            const desktopDiscountText = document.getElementById('desktopDiscountText');
            if (desktopDiscountText) desktopDiscountText.textContent = discountText || '';

            updateTotalWithDiscount(data.discountAmount, data.finalAmount);

            if (input) input.disabled = true;
            if (applyBtn) {
                applyBtn.textContent = '✓ Применен';
                applyBtn.disabled = true;
            }

            // Блокируем бонусы
            const bonusInput = document.getElementById('bonusInput');
            const applyBonusBtn = document.getElementById('applyBonusBtn');
            if (bonusInput) bonusInput.disabled = true;
            if (applyBonusBtn) applyBonusBtn.disabled = true;

            // ========== НОВАЯ ЛОГИКА: ПОКАЗЫВАЕМ БЛОК ИНФОРМАЦИИ ==========
            // Показываем блок информации о примененном промокоде
            const promoAppliedInfo = document.querySelector('.promo-applied-info');
            const appliedPromoDisplay = document.getElementById('appliedPromoCodeDisplay');

            if (promoAppliedInfo && appliedPromoDisplay) {
                appliedPromoDisplay.textContent = code;
                promoAppliedInfo.style.display = 'flex';
            }

            // Скрываем форму ввода промокода
            const promoInputGroup = document.querySelector('.promo-input-group');
            if (promoInputGroup) {
                promoInputGroup.style.display = 'none';
            }

            // Скрываем сообщение, если оно было
            const promoMessage = document.getElementById('promoMessage');
            if (promoMessage) {
                promoMessage.style.display = 'none';
            }

        } else {
            showMessage('✗ ' + data.message, 'error');
            const appliedPromoField = document.getElementById('appliedPromoCode');
            if (appliedPromoField) appliedPromoField.value = '';

            const discountText = document.getElementById('discountText');
            if (discountText) discountText.textContent = '';
            const mobileDiscountText = document.getElementById('mobileDiscountText');
            if (mobileDiscountText) mobileDiscountText.textContent = '';
            const desktopDiscountText = document.getElementById('desktopDiscountText');
            if (desktopDiscountText) desktopDiscountText.textContent = '';

            const originalTotalValue = getOriginalTotal();
            updateTotalWithDiscount(0, originalTotalValue);

            if (applyBtn) {
                applyBtn.disabled = false;
                applyBtn.textContent = 'Применить';
            }
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showMessage('Ошибка при проверке промокода', 'error');
        if (applyBtn) {
            applyBtn.disabled = false;
            applyBtn.textContent = 'Применить';
        }
    }
}

async function restorePromoCodeFromSession() {
    const savedPromo = document.getElementById('appliedPromoCode')?.value;
    if (savedPromo) {
        const input = document.getElementById('promoCodeInput');
        const btn = document.getElementById('applyPromoBtn');

        if (input) {
            input.value = savedPromo;
            input.disabled = true;
        }
        if (btn) {
            btn.textContent = '✓ Применен';
            btn.disabled = true;
        }

        // Блокируем бонусы
        const bonusInput = document.getElementById('bonusInput');
        const applyBonusBtn = document.getElementById('applyBonusBtn');
        if (bonusInput) bonusInput.disabled = true;
        if (applyBonusBtn) applyBonusBtn.disabled = true;

        // Показываем блок информации о примененном промокоде
        const promoAppliedInfo = document.querySelector('.promo-applied-info');
        const appliedPromoDisplay = document.getElementById('appliedPromoCodeDisplay');

        if (promoAppliedInfo && appliedPromoDisplay) {
            appliedPromoDisplay.textContent = savedPromo;
            promoAppliedInfo.style.display = 'flex';
        }

        // Скрываем форму ввода промокода
        const promoInputGroup = document.querySelector('.promo-input-group');
        if (promoInputGroup) {
            promoInputGroup.style.display = 'none';
        }

        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

            const headers = {
                'Content-Type': 'application/json',
            };
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const response = await fetch('/api/promo/apply', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ code: savedPromo })
            });

            const data = await response.json();
            if (data.valid) {
                const discountTextEl = document.getElementById('discountText');
                if (discountTextEl) discountTextEl.textContent = data.discountText || '';

                const mobileDiscountText = document.getElementById('mobileDiscountText');
                if (mobileDiscountText) mobileDiscountText.textContent = data.discountText || '';

                const desktopDiscountText = document.getElementById('desktopDiscountText');
                if (desktopDiscountText) desktopDiscountText.textContent = data.discountText || '';

                updateTotalWithDiscount(data.discountAmount, data.finalAmount);
            }
        } catch (error) {
            console.error('Ошибка восстановления промокода:', error);
        }
    }
}

function updatePromoDiscount(promoCode) {
    if (!promoCode) return;

    fetch('/api/promo/apply', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ code: promoCode })
    })
        .then(response => response.json())
        .then(data => {
            if (data.valid) {
                const discountText = document.getElementById('discountText');
                if (discountText) discountText.textContent = data.discountText || '';

                const mobileDiscountText = document.getElementById('mobileDiscountText');
                if (mobileDiscountText) mobileDiscountText.textContent = data.discountText || '';

                const desktopDiscountText = document.getElementById('desktopDiscountText');
                if (desktopDiscountText) desktopDiscountText.textContent = data.discountText || '';

                updateTotalWithDiscount(data.discountAmount, data.finalAmount);
            }
        })
        .catch(error => console.error('Ошибка восстановления промокода:', error));
}

// ========== ЛОГИКА ДЛЯ БОНУСОВ ==========
async function applyBonuses() {
    const appliedPromo = document.getElementById('appliedPromoCode')?.value;
    const promoInput = document.getElementById('promoCodeInput');

    if (appliedPromo || (promoInput && promoInput.value && !promoInput.disabled)) {
        showBonusMessage('Нельзя применить бонусы вместе с промокодом', 'error');
        return;
    }

    const bonusInput = document.getElementById('bonusInput');
    const usedBonuses = parseInt(bonusInput?.value || 0);
    const maxSpendable = parseInt(bonusInput?.getAttribute('max') || 0);
    const applyBtn = document.getElementById('applyBonusBtn');
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    if (usedBonuses <= 0) {
        showBonusMessage('Введите количество бонусов', 'error');
        return;
    }

    if (usedBonuses > maxSpendable) {
        showBonusMessage(`Можно использовать не более ${maxSpendable} бонусов`, 'error');
        return;
    }

    if (applyBtn) {
        applyBtn.disabled = true;
        applyBtn.textContent = 'Применяем...';
    }

    try {
        const headers = {
            'Content-Type': 'application/json',
        };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch('/api/bonus/calculate', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ usedBonuses: usedBonuses })
        });

        const data = await response.json();

        if (data.valid) {
            showBonusMessage('✓ Бонусы применены', 'success');
            const usedBonusesField = document.getElementById('usedBonuses');
            if (usedBonusesField) usedBonusesField.value = usedBonuses;
            updateBonusDiscount(data.discountAmount, data.finalAmount);

            if (applyBtn) {
                applyBtn.textContent = '✓ Применено';
                applyBtn.disabled = true;
            }

            if (bonusInput) bonusInput.disabled = true;

            // Блокируем промокод
            const promoCodeInput = document.getElementById('promoCodeInput');
            const applyPromoBtn = document.getElementById('applyPromoBtn');
            if (promoCodeInput) promoCodeInput.disabled = true;
            if (applyPromoBtn) applyPromoBtn.disabled = true;

            // ========== НОВАЯ ЛОГИКА: ПОКАЗЫВАЕМ БЛОК ИНФОРМАЦИИ ==========
            // Показываем блок информации о примененных бонусах
            const bonusAppliedInfo = document.querySelector('.bonus-applied-info');
            const appliedBonusAmount = document.getElementById('appliedBonusAmount');

            if (bonusAppliedInfo && appliedBonusAmount) {
                appliedBonusAmount.textContent = usedBonuses;
                bonusAppliedInfo.style.display = 'flex';
            }

            // Скрываем форму ввода бонусов
            const bonusInputGroup = document.querySelector('.bonus-input-group');
            if (bonusInputGroup) {
                bonusInputGroup.style.display = 'none';
            }

            // Скрываем сообщение, если оно было
            const bonusMessage = document.getElementById('bonusMessage');
            if (bonusMessage) {
                bonusMessage.style.display = 'none';
            }

            // Скрываем подсказку (опционально)
            const bonusHint = document.querySelector('.bonus-hint');
            if (bonusHint) {
                bonusHint.style.display = 'none';
            }

        } else {
            showBonusMessage('✗ ' + data.message, 'error');
            const usedBonusesField = document.getElementById('usedBonuses');
            if (usedBonusesField) usedBonusesField.value = 0;
            const originalTotalValue = getOriginalTotal();
            updateBonusDiscount(0, originalTotalValue);
            if (applyBtn) {
                applyBtn.disabled = false;
                applyBtn.textContent = 'Применить';
            }
        }
    } catch (error) {
        console.error('Ошибка:', error);
        showBonusMessage('Ошибка при применении бонусов', 'error');
        if (applyBtn) {
            applyBtn.disabled = false;
            applyBtn.textContent = 'Применить';
        }
    }
}

function restoreBonuses() {
    const usedBonusesField = document.getElementById('usedBonuses');
    const bonusInput = document.getElementById('bonusInput');
    const totalElement = document.getElementById('totalAmount');
    const mobileTotalElement = document.getElementById('mobileTotalAmount');
    const mobileTotalFinal = document.getElementById('mobileTotalFinal');
    const subtotalElement = document.getElementById('subtotal');

    const appliedPromo = document.getElementById('appliedPromoCode')?.value;
    if (appliedPromo) {
        console.log('Активен промокод, бонусы не восстанавливаем');
        return;
    }

    if (usedBonusesField && bonusInput && usedBonusesField.value && parseInt(usedBonusesField.value) > 0) {
        const usedBonuses = parseInt(usedBonusesField.value);

        let originalTotalValue = 0;
        if (subtotalElement) {
            const text = subtotalElement.textContent;
            const match = text.match(/(\d+[\s]?\d*)/);
            if (match) {
                originalTotalValue = parseFloat(match[0].replace(/\s/g, ''));
            }
        }

        if (originalTotalValue > 0 && originalTotalValue > usedBonuses) {
            const finalAmount = originalTotalValue - usedBonuses;

            if (totalElement) totalElement.textContent = finalAmount.toLocaleString('ru-RU') + ' ₽';
            if (mobileTotalElement) mobileTotalElement.textContent = finalAmount.toLocaleString('ru-RU') + ' ₽';
            if (mobileTotalFinal) mobileTotalFinal.textContent = finalAmount.toLocaleString('ru-RU') + ' ₽';

            showBonusDiscountRow(usedBonuses);

            bonusInput.value = usedBonuses;
            bonusInput.disabled = true;

            const applyBonusBtn = document.getElementById('applyBonusBtn');
            if (applyBonusBtn) {
                applyBonusBtn.textContent = '✓ Применено';
                applyBonusBtn.disabled = true;
            }

            // Блокируем промокод
            const promoInput = document.getElementById('promoCodeInput');
            const applyPromoBtn = document.getElementById('applyPromoBtn');
            if (promoInput) promoInput.disabled = true;
            if (applyPromoBtn) applyPromoBtn.disabled = true;

            // ========== НОВАЯ ЛОГИКА: ПОКАЗЫВАЕМ БЛОК ИНФОРМАЦИИ ==========
            // Показываем блок информации о примененных бонусах
            const bonusAppliedInfo = document.querySelector('.bonus-applied-info');
            const appliedBonusAmount = document.getElementById('appliedBonusAmount');

            if (bonusAppliedInfo && appliedBonusAmount) {
                appliedBonusAmount.textContent = usedBonuses;
                bonusAppliedInfo.style.display = 'flex';
            }

            // Скрываем форму ввода бонусов
            const bonusInputGroup = document.querySelector('.bonus-input-group');
            if (bonusInputGroup) {
                bonusInputGroup.style.display = 'none';
            }

            // Скрываем подсказку
            const bonusHint = document.querySelector('.bonus-hint');
            if (bonusHint) {
                bonusHint.style.display = 'none';
            }

        } else {
            console.error('Ошибка: сумма заказа меньше или равна бонусам');
            usedBonusesField.value = 0;
            if (bonusInput) bonusInput.value = '';
            showBonusDiscountRow(0);
        }
    }
}

async function cancelPromo() {
    // Проверяем, что промокод действительно был применен
    const appliedPromoField = document.getElementById('appliedPromoCode');
    if (!appliedPromoField || !appliedPromoField.value) {
        showMessage('Нет активного промокода для отмены', 'error');
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    const cancelBtn = document.querySelector('.btn-cancel-promo');
    const originalText = cancelBtn?.innerHTML;
    if (cancelBtn) {
        cancelBtn.disabled = true;
        cancelBtn.innerHTML = '<i class="bi bi-hourglass-split"></i> Отмена...';
    }

    try {
        const headers = {
            'Content-Type': 'application/json',
        };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch('/api/promo/cancel', {
            method: 'POST',
            headers: headers,
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            performLocalPromoCancel();
            showMessage(data.message || 'Промокод отменен', 'success');

            // ✅ Дополнительное скрытие через 100ms
            setTimeout(forceHideAllDiscountRows, 100);
        } else {
            const data = await response.json();
            showMessage(data.message || 'Ошибка при отмене промокода', 'error');

            if (appliedPromoField && appliedPromoField.value) {
                performLocalPromoCancel();
                showMessage('Промокод отменен локально', 'warning');
            }
        }
    } catch (error) {
        console.error('Ошибка отмены промокода:', error);
        showMessage('Ошибка соединения при отмене промокода', 'error');

        if (appliedPromoField && appliedPromoField.value) {
            performLocalPromoCancel();
            showMessage('Промокод отменен локально (ошибка соединения)', 'warning');
        }
    } finally {
        if (cancelBtn) {
            cancelBtn.disabled = false;
            cancelBtn.innerHTML = originalText;
        }
    }
}

async function cancelBonuses() {
    // Проверяем, что бонусы действительно были применены
    const usedBonusesField = document.getElementById('usedBonuses');
    if (!usedBonusesField || parseInt(usedBonusesField.value) === 0) {
        showBonusMessage('Нет активных бонусов для отмены', 'error');
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    // Блокируем кнопку отмены на время запроса
    const cancelBtn = document.querySelector('.btn-cancel-bonus');
    const originalText = cancelBtn?.innerHTML;
    if (cancelBtn) {
        cancelBtn.disabled = true;
        cancelBtn.innerHTML = '<i class="bi bi-hourglass-split"></i> Отмена...';
    }

    try {
        const headers = {
            'Content-Type': 'application/json',
        };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch('/api/bonus/cancel', {
            method: 'POST',
            headers: headers,
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            performLocalBonusCancel();
            showBonusMessage(data.message || 'Бонусы отменены', 'success');

            // ✅ МНОГОКРАТНОЕ ПРИНУДИТЕЛЬНОЕ СКРЫТИЕ
            const hideBonusRows = () => {
                const bonusRows = document.querySelectorAll('#bonusDiscountRow, #mobileBonusDiscountRow, #desktopBonusDiscountRow, .discount-row');
                bonusRows.forEach(row => {
                    const amountSpan = row.querySelector('.discount-amount');
                    if (amountSpan && (amountSpan.textContent === '-0 ₽' || amountSpan.textContent === '0 ₽')) {
                        row.style.display = 'none';
                        row.style.setProperty('display', 'none', 'important');
                    }
                });
            };

            // Скрываем сразу
            hideBonusRows();

            // Скрываем через 100ms
            setTimeout(hideBonusRows, 100);

            // Скрываем через 300ms (после возможных других обновлений)
            setTimeout(hideBonusRows, 300);

            // Скрываем через 500ms (на всякий случай)
            setTimeout(hideBonusRows, 500);

        } else {
            const data = await response.json();
            showBonusMessage(data.message || 'Ошибка при отмене бонусов', 'error');

            if (usedBonusesField && parseInt(usedBonusesField.value) > 0) {
                performLocalBonusCancel();
                showBonusMessage('Бонусы отменены локально', 'warning');
            }
        }
    } catch (error) {
        console.error('Ошибка отмены бонусов:', error);
        showBonusMessage('Ошибка соединения при отмене бонусов', 'error');

        if (usedBonusesField && parseInt(usedBonusesField.value) > 0) {
            performLocalBonusCancel();
            showBonusMessage('Бонусы отменены локально (ошибка соединения)', 'warning');
        }
    } finally {
        // Восстанавливаем кнопку
        if (cancelBtn) {
            cancelBtn.disabled = false;
            cancelBtn.innerHTML = originalText;
        }
    }
}

// Сохранение деталей адреса в localStorage при изменении
function initAddressDetailsSaving() {
    const detailsFields = ['deliveryEntrance', 'deliveryFloor', 'deliveryApartment', 'deliveryIntercom'];

    detailsFields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field) {
            field.addEventListener('change', function() {
                saveAddressDetailsToStorage();
            });
            field.addEventListener('blur', function() {
                saveAddressDetailsToStorage();
            });
        }
    });
}

// Сохранение деталей адреса в localStorage
function saveAddressDetailsToStorage() {
    const address = document.getElementById('deliveryAddress')?.value;
    if (!address) return;

    const entrance = document.getElementById('deliveryEntrance')?.value || '';
    const floor = document.getElementById('deliveryFloor')?.value || '';
    const apartment = document.getElementById('deliveryApartment')?.value || '';
    const intercom = document.getElementById('deliveryIntercom')?.value || '';

    const addressDetails = {
        address: address,
        entrance: entrance,
        floor: floor,
        apartment: apartment,
        intercom: intercom,
        savedAt: new Date().toISOString()
    };

    localStorage.setItem('addressDetails', JSON.stringify(addressDetails));
    console.log('✅ Детали адреса сохранены в localStorage');
}

// Загрузка деталей адреса из localStorage
function loadAddressDetailsFromStorage() {
    const savedDetails = localStorage.getItem('addressDetails');
    if (savedDetails) {
        try {
            const details = JSON.parse(savedDetails);
            const entranceField = document.getElementById('deliveryEntrance');
            const floorField = document.getElementById('deliveryFloor');
            const apartmentField = document.getElementById('deliveryApartment');
            const intercomField = document.getElementById('deliveryIntercom');

            if (entranceField && details.entrance) entranceField.value = details.entrance;
            if (floorField && details.floor) floorField.value = details.floor;
            if (apartmentField && details.apartment) apartmentField.value = details.apartment;
            if (intercomField && details.intercom) intercomField.value = details.intercom;

            console.log('✅ Детали адреса загружены из localStorage');
        } catch(e) {
            console.error('Ошибка загрузки деталей адреса:', e);
        }
    }
}

// Обновление деталей адреса при выборе адреса через карту
function updateAddressDetailsFromMap(details) {
    const entranceField = document.getElementById('deliveryEntrance');
    const floorField = document.getElementById('deliveryFloor');
    const apartmentField = document.getElementById('deliveryApartment');
    const intercomField = document.getElementById('deliveryIntercom');

    if (entranceField && details.entrance) entranceField.value = details.entrance;
    if (floorField && details.floor) floorField.value = details.floor;
    if (apartmentField && details.apartment) apartmentField.value = details.apartment;
    if (intercomField && details.intercom) intercomField.value = details.intercom;

    // Сохраняем в localStorage
    saveAddressDetailsToStorage();
}

// ========== ЛОГИКА ДЛЯ АДРЕСА ==========
function updateCheckoutForm(addressData) {
    if (!addressData) return;

    const addressInput = document.getElementById('deliveryAddress');

    if (addressInput) {
        if (addressData.type === 'pickup') {
            addressInput.value = 'Самовывоз';
            // При самовывозе скрываем детали адреса
            toggleAddressDetails(false);
        } else {
            addressInput.value = addressData.text || '';
            // При доставке показываем детали адреса
            toggleAddressDetails(true);
        }
    }

    // Если пришли детали адреса из maps.js, обновляем их
    if (addressData.type === 'delivery' && addressData.details) {
        updateAddressDetailsFromMap(addressData.details);
    }

    // Сохраняем детали
    saveAddressDetailsToStorage();

    console.log('✅ Форма обновлена из localStorage:', addressData);
}

function loadAddressFromStorage() {
    const savedAddress = localStorage.getItem('deliveryAddress');
    if (savedAddress) {
        try {
            const addressData = JSON.parse(savedAddress);
            updateCheckoutForm(addressData);

            // Обновляем информацию о зоне после загрузки адреса
            if (addressData.type === 'delivery' && addressData.zone) {
                updateZoneInfoDisplay(addressData.zone);
                toggleAddressDetails(true);  // Показываем детали
            } else if (addressData.type === 'pickup') {
                loadDeliveryInfoFromCart();
                toggleAddressDetails(false); // Скрываем детали
            }

            // Загружаем детали адреса
            loadAddressDetailsFromStorage();

        } catch(e) {
            console.error('Ошибка загрузки адреса:', e);
        }
    } else {
        loadAddressDetailsFromStorage();
    }
}

// Функция для отображения информации о зоне
function updateZoneInfoDisplay(zone) {
    const addressGroup = document.getElementById('addressGroup');
    if (!addressGroup) return;

    // Удаляем старую информацию о зоне
    document.querySelectorAll('.zone-info-card').forEach(el => el.remove());

    // Добавляем новую информацию о зоне
    const zoneInfoCard = document.createElement('div');
    zoneInfoCard.className = 'zone-info-card';
    zoneInfoCard.innerHTML = `
        <i class="bi bi-geo-alt-fill"></i>
        <div class="zone-content">
            <div class="zone-title">
                <span>📍 Зона доставки: ${escapeHtml(zone.name)}</span>
            </div>
            <div class="zone-details">
                <span><i class="bi bi-clock"></i> ${escapeHtml(zone.deliveryTime)}</span>
                <span><i class="bi bi-calculator"></i> от ${zone.minOrder ? zone.minOrder.toLocaleString('ru-RU') : '0'} ₽</span>
            </div>
        </div>
    `;

    addressGroup.insertAdjacentElement('afterend', zoneInfoCard);

    // Проверяем минимальную сумму
    const subtotalElement = document.getElementById('subtotal');
    let currentTotal = 0;
    if (subtotalElement) {
        const match = subtotalElement.textContent.match(/(\d+[\s]?\d*)/);
        if (match) {
            currentTotal = parseFloat(match[0].replace(/\s/g, ''));
        }
    }

    if (zone.minOrder && currentTotal < zone.minOrder) {
        showMinOrderWarning(zone.name, zone.minOrder, currentTotal);
    } else {
        document.querySelectorAll('.min-order-info, .min-order-warning-card').forEach(w => w.remove());
    }
}

// Функция для отображения информации о минимальной сумме (зеленый)
function showMinOrderWarning(zoneName, minOrder, currentTotal) {
    const addressGroup = document.getElementById('addressGroup');
    if (!addressGroup) return;

    // Удаляем только старые предупреждения, но не трогаем другие элементы
    document.querySelectorAll('.min-order-info, .min-order-warning-card').forEach(w => w.remove());

    const needToAdd = minOrder - currentTotal;
    const percent = (currentTotal / minOrder) * 100;

    const warningDiv = document.createElement('div');
    warningDiv.className = 'min-order-warning-card';
    warningDiv.innerHTML = `
        <div class="warning-header">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <span class="warning-title">До заказа не хватает ${needToAdd} ₽</span>
        </div>
        <div class="warning-progress">
            <div class="progress-bar">
                <div class="progress-fill" style="width: ${percent}%;"></div>
            </div>
            <div class="progress-labels">
                <span>${currentTotal.toLocaleString('ru-RU')} ₽</span>
                <span>${minOrder.toLocaleString('ru-RU')} ₽</span>
            </div>
        </div>
        <div class="warning-actions">
            <button type="button" class="btn-add-more" onclick="window.location.href='/'">
                <i class="bi bi-cart-plus"></i> Добавить товары
            </button>
        </div>
    `;

    addressGroup.insertAdjacentElement('afterend', warningDiv);
}

async function loadDeliveryInfoFromCart() {
    try {
        const response = await fetch('/cart/delivery-info', {
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            console.log('Информация о доставке из корзины:', data);

            if (data.hasDeliveryInfo) {
                const deliveryTypeSelect = document.getElementById('deliveryType');
                const addressInput = document.getElementById('deliveryAddress');
                const addressGroup = document.getElementById('addressGroup');
                // const addressRequired = document.getElementById('addressRequired');
                const deliveryTimeSelect = document.getElementById('deliveryTimeSelect');
                const deliveryTimeHidden = document.getElementById('deliveryTime');

                const deliveryType = data.deliveryType?.toUpperCase();

                if (deliveryType === 'DELIVERY' && deliveryTypeSelect) {
                    deliveryTypeSelect.value = 'DELIVERY';

                    if (addressGroup) addressGroup.style.display = 'block';
                    // if (addressRequired) addressRequired.style.display = 'inline';

                    toggleAddressDetails(true);

                    if (addressInput && data.address && !addressInput.value) {
                        addressInput.value = data.address;
                        saveAddressDetailsToStorage();
                        console.log('✅ Адрес установлен:', data.address);
                    }

                    if (deliveryTimeSelect && data.deliveryTime) {
                        const optionExists = Array.from(deliveryTimeSelect.options).some(opt => opt.value === data.deliveryTime);
                        if (optionExists) {
                            deliveryTimeSelect.value = data.deliveryTime;
                            if (deliveryTimeHidden) deliveryTimeHidden.value = data.deliveryTime;
                        }
                    }

                    // ========== ОБНОВЛЕНИЕ ИНФОРМАЦИИ О ЗОНЕ ==========
                    // Удаляем старые предупреждения и информацию о зоне
                    document.querySelectorAll('.min-order-info').forEach(w => w.remove());
                    document.querySelectorAll('.zone-info-card').forEach(el => el.remove());

                    // Добавляем информацию о зоне (если есть)
                    if (data.zoneName && addressGroup) {
                        updateZoneInfoDisplay({
                            name: data.zoneName,
                            deliveryTime: data.deliveryTime || '30-45 мин',
                            minOrder: data.minOrderRequired || 0
                        });
                    }

                    // Добавляем предупреждение о минимальной сумме (ТОЛЬКО ОДИН РАЗ)
                    if (data.minOrderRequired && data.currentTotal < data.minOrderRequired && addressGroup) {
                        showMinOrderWarning(data.zoneName, data.minOrderRequired, data.currentTotal);
                    } else {
                        // Если сумма достаточна - удаляем предупреждение
                        document.querySelectorAll('.min-order-info, .min-order-warning-card').forEach(w => w.remove());
                    }

                } else if (deliveryType === 'PICKUP' && deliveryTypeSelect) {
                    deliveryTypeSelect.value = 'PICKUP';
                    if (addressGroup) addressGroup.style.display = 'none';
                    // if (addressRequired) addressRequired.style.display = 'none';

                    toggleAddressDetails(false);

                    document.querySelectorAll('.pickup-info').forEach(info => info.remove());
                    document.querySelectorAll('.min-order-info').forEach(w => w.remove());
                    document.querySelectorAll('.zone-info-card').forEach(el => el.remove());

                    const infoDiv = document.createElement('div');
                    infoDiv.className = 'pickup-info';
                    infoDiv.innerHTML = `
        <i class="bi bi-shop"></i>
        <div class="pickup-content">
            <div class="pickup-title">✅ Вы выбрали самовывоз</div>
            <div class="pickup-details">
                <span><i class="bi bi-geo-alt"></i> <strong>${escapeHtml(data.pointName || 'Ресторан')}</strong></span>
                <span><i class="bi bi-pin-map"></i> ${escapeHtml(data.address || 'г. Пермь, ул. Ленина, 10')}</span>
                <span><i class="bi bi-clock"></i> Готовность: 15-20 минут</span>
            </div>
        </div>
    `;

                    // ⭐ ВСТАВЛЯЕМ ПОСЛЕ addressGroup (как и другие уведомления)
                    if (addressGroup) {
                        addressGroup.insertAdjacentElement('afterend', infoDiv);
                    } else {
                        const form = document.querySelector('.checkout-form');
                        if (form) form.insertBefore(infoDiv, form.querySelector('.btn-checkout'));
                    }
                }

                if (deliveryTypeSelect) deliveryTypeSelect.dispatchEvent(new Event('change'));
                updateSummaryByDeliveryType(deliveryType);
            }
        }
    } catch (error) {
        console.error('Ошибка загрузки информации о доставке:', error);
    }
}

function updateSummaryByDeliveryType(deliveryType) {
    const summaryRows = document.querySelectorAll('.summary-row');
    summaryRows.forEach(row => {
        const label = row.querySelector('.summary-label');
        const value = row.querySelector('.summary-value');
        if (label && value) {
            const labelText = label.textContent.trim();
            if (labelText === 'Доставка:' || labelText.includes('Доставка')) {
                if (deliveryType === 'DELIVERY') {
                    value.textContent = 'Бесплатно';
                } else if (deliveryType === 'PICKUP') {
                    value.textContent = 'Самовывоз';
                }
            }
        }
    });
}

function escapeHtml(str) {
    if (!str) return str;
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ========== МАСКА ДЛЯ ТЕЛЕФОНА ==========
function initPhoneMask() {
    const phoneInput = document.getElementById('customerPhone');

    if (phoneInput) {
        phoneInput.addEventListener('input', function(e) {
            let input = e.target;
            let value = input.value;

            let numbers = value.replace(/\D/g, '');

            if (numbers.length > 11) {
                numbers = numbers.substring(0, 11);
            }

            let formatted = '';
            if (numbers.length > 0) {
                if (numbers.startsWith('8')) {
                    numbers = '7' + numbers.substring(1);
                } else if (!numbers.startsWith('7')) {
                    numbers = '7' + numbers;
                }

                if (numbers.length <= 1) {
                    formatted = '+7';
                } else if (numbers.length <= 4) {
                    formatted = '+7 (' + numbers.substring(1, 4);
                } else if (numbers.length <= 7) {
                    formatted = '+7 (' + numbers.substring(1, 4) + ') ' + numbers.substring(4, 7);
                } else if (numbers.length <= 9) {
                    formatted = '+7 (' + numbers.substring(1, 4) + ') ' + numbers.substring(4, 7) + '-' + numbers.substring(7, 9);
                } else {
                    formatted = '+7 (' + numbers.substring(1, 4) + ') ' + numbers.substring(4, 7) + '-' + numbers.substring(7, 9) + '-' + numbers.substring(9, 11);
                }
            }

            input.value = formatted;

            let position = input.value.length;
            input.setSelectionRange(position, position);
        });

        phoneInput.addEventListener('focus', function() {
            this.setSelectionRange(this.value.length, this.value.length);
        });

        phoneInput.addEventListener('blur', function() {
            let numbers = this.value.replace(/\D/g, '');
            if (numbers.length < 11 && this.value.length > 0) {
                this.classList.add('is-invalid');
                let feedback = this.nextElementSibling;
                if (!feedback || !feedback.classList.contains('invalid-feedback')) {
                    feedback = document.createElement('div');
                    feedback.className = 'invalid-feedback';
                    this.parentNode.insertBefore(feedback, this.nextSibling);
                }
                feedback.textContent = 'Введите полный номер телефона';
            } else {
                this.classList.remove('is-invalid');
            }
        });

        if (phoneInput.value) {
            let event = new Event('input', { bubbles: true });
            phoneInput.dispatchEvent(event);
        }
    }
}

// Функция для управления видимостью деталей адреса
function toggleAddressDetails(show) {
    const detailsContainer = document.getElementById('addressDetailsContainer');
    if (detailsContainer) {
        detailsContainer.style.display = show ? 'block' : 'none';
    }
}

// Обновление страницы при выборе самовывоза (только сохраняем данные)
window.updateCheckoutPageAfterPickup = function(point) {
    console.log('Подготовка страницы для самовывоза:', point);

    // Сохраняем в localStorage для восстановления после перезагрузки
    const addressData = {
        type: 'pickup',
        text: `Самовывоз: ${point.name}`,
        point: point
    };
    localStorage.setItem('deliveryAddress', JSON.stringify(addressData));
    localStorage.setItem('deliveryMode', 'pickup');

    // Не трогаем DOM, перезагрузка будет в maps.js
};

// Обновление страницы при выборе доставки (только сохраняем данные)
window.updateCheckoutPageAfterDelivery = function(addressData) {
    console.log('Подготовка страницы для доставки:', addressData);

    // Сохраняем в localStorage для восстановления после перезагрузки
    localStorage.setItem('deliveryAddress', JSON.stringify(addressData));
    localStorage.setItem('deliveryMode', 'delivery');
    localStorage.removeItem('selectedPickupPoint');

    // Не трогаем DOM, перезагрузка будет в maps.js
};

// Добавление уведомления о самовывозе после загрузки страницы
function addPickupInfoAfterLoad(point) {
    // Удаляем старые уведомления
    const oldPickupInfo = document.querySelector('.pickup-info');
    if (oldPickupInfo) oldPickupInfo.remove();

    const zoneInfoCard = document.querySelector('.zone-info-card');
    if (zoneInfoCard) zoneInfoCard.remove();

    const minOrderWarning = document.querySelector('.min-order-info');
    if (minOrderWarning) minOrderWarning.remove();

    // Добавляем новое уведомление
    const addressGroup = document.getElementById('addressGroup');
    if (addressGroup && addressGroup.parentNode) {
        const infoDiv = document.createElement('div');
        infoDiv.className = 'pickup-info';
        infoDiv.innerHTML = `
            <i class="bi bi-shop"></i>
            <div class="pickup-content">
                <div class="pickup-title">✅ Вы выбрали самовывоз</div>
                <div class="pickup-details">
                    <span><i class="bi bi-geo-alt"></i> ${escapeHtml(point.name)}</span>
                    <span><i class="bi bi-pin-map"></i> ${escapeHtml(point.address)}</span>
                    <span><i class="bi bi-clock"></i> Готовность: 15-20 мин</span>
                </div>
            </div>
        `;

        addressGroup.insertAdjacentElement('afterend', infoDiv);
    }
}

// ========== ФУНКЦИЯ ЛОКАЛЬНОЙ ОТМЕНЫ БОНУСОВ ==========
function performLocalBonusCancel() {
    console.log('Выполняется локальная отмена бонусов...');

    // 1. Сбрасываем скрытое поле использованных бонусов
    const usedBonusesField = document.getElementById('usedBonuses');
    let usedBonuses = 0;
    if (usedBonusesField) {
        usedBonuses = parseInt(usedBonusesField.value) || 0;
        usedBonusesField.value = 0;
    }

    // 2. Сбрасываем поле ввода бонусов
    const bonusInput = document.getElementById('bonusInput');
    if (bonusInput) {
        bonusInput.value = '';
        bonusInput.disabled = false;
    }

    // 3. Восстанавливаем кнопку применения бонусов
    const applyBonusBtn = document.getElementById('applyBonusBtn');
    if (applyBonusBtn) {
        applyBonusBtn.disabled = false;
        applyBonusBtn.textContent = 'Применить';
    }

    // 4. Сбрасываем глобальную переменную
    currentBonusDiscount = 0;

    // 5. Разблокируем промокод
    const promoCodeInput = document.getElementById('promoCodeInput');
    const applyPromoBtn = document.getElementById('applyPromoBtn');
    if (promoCodeInput) promoCodeInput.disabled = false;
    if (applyPromoBtn) applyPromoBtn.disabled = false;

    // 6. Показываем форму ввода бонусов
    const bonusInputGroup = document.querySelector('.bonus-input-group');
    if (bonusInputGroup) bonusInputGroup.style.display = 'flex';

    // 7. Показываем подсказку
    const bonusHint = document.querySelector('.bonus-hint');
    if (bonusHint) bonusHint.style.display = 'block';

    // 8. Скрываем блок информации о примененных бонусах
    const bonusAppliedInfo = document.querySelector('.bonus-applied-info');
    if (bonusAppliedInfo) bonusAppliedInfo.style.display = 'none';

    // ========== ВОССТАНАВЛИВАЕМ СУММУ ==========

    // Получаем базовую сумму товаров из subtotal (если есть)
    const subtotalElement = document.getElementById('subtotal');
    let subtotal = 0;
    if (subtotalElement) {
        const text = subtotalElement.textContent;
        const match = text.match(/(\d+[\s]?\d*)/);
        if (match) {
            subtotal = parseFloat(match[0].replace(/\s/g, ''));
        }
    }

    // Если subtotal нет, получаем из totalAmount и прибавляем бонусы
    let finalAmount = subtotal;
    if (finalAmount === 0) {
        const totalElement = document.getElementById('totalAmount');
        if (totalElement) {
            const text = totalElement.textContent;
            const match = text.match(/(\d+[\s]?\d*)/);
            if (match) {
                const currentTotal = parseFloat(match[0].replace(/\s/g, ''));
                finalAmount = currentTotal + usedBonuses;
            }
        }
    }

    // Если есть активный промокод, вычитаем его скидку
    if (currentDiscount > 0) {
        finalAmount = finalAmount - currentDiscount;
    }

    console.log('Базовая сумма (subtotal):', subtotal);
    console.log('Возвращаем бонусов:', usedBonuses);
    console.log('Итоговая сумма:', finalAmount);

    // Обновляем отображение суммы в правой колонке (десктоп)
    const totalElement = document.getElementById('totalAmount');
    if (totalElement) {
        totalElement.textContent = finalAmount.toLocaleString('ru-RU') + ' ₽';
    }

    // Обновляем мобильные суммы
    const mobileTotalElement = document.getElementById('mobileTotalAmount');
    const mobileTotalFinal = document.getElementById('mobileTotalFinal');
    if (mobileTotalElement) {
        mobileTotalElement.textContent = finalAmount.toLocaleString('ru-RU') + ' ₽';
    }
    if (mobileTotalFinal) {
        mobileTotalFinal.textContent = finalAmount.toLocaleString('ru-RU') + ' ₽';
    }

    // Скрываем строки с бонусной скидкой
    const mobileBonusDiscountRow = document.getElementById('mobileBonusDiscountRow');
    const desktopBonusDiscountRow = document.getElementById('desktopBonusDiscountRow');

    if (mobileBonusDiscountRow) {
        mobileBonusDiscountRow.style.display = 'none';
        mobileBonusDiscountRow.style.setProperty('display', 'none', 'important');
    }
    if (desktopBonusDiscountRow) {
        desktopBonusDiscountRow.style.display = 'none';
        desktopBonusDiscountRow.style.setProperty('display', 'none', 'important');
    }

    // Обновляем отображение
    updateDiscountDisplay();

    console.log('✅ Бонусы полностью отменены локально');
}

function forceHideAllDiscountRows() {
    const allDiscountRows = document.querySelectorAll('.discount-row');
    allDiscountRows.forEach(row => {
        row.style.display = 'none';
        row.style.setProperty('display', 'none', 'important');
    });
    console.log('✅ Все строки со скидками скрыты');
}

// ========== ФУНКЦИЯ ЛОКАЛЬНОЙ ОТМЕНЫ ПРОМОКОДА ==========
function performLocalPromoCancel() {
    console.log('Выполняется локальная отмена промокода...');

    // Сбрасываем промокод
    const appliedPromoField = document.getElementById('appliedPromoCode');
    if (appliedPromoField) appliedPromoField.value = '';

    const promoCodeInput = document.getElementById('promoCodeInput');
    if (promoCodeInput) {
        promoCodeInput.value = '';
        promoCodeInput.disabled = false;
    }

    const applyPromoBtn = document.getElementById('applyPromoBtn');
    if (applyPromoBtn) {
        applyPromoBtn.disabled = false;
        applyPromoBtn.textContent = 'Применить';
    }

    // Сбрасываем скидку
    currentDiscount = 0;

    // Разблокируем бонусы
    const bonusInput = document.getElementById('bonusInput');
    const applyBonusBtn = document.getElementById('applyBonusBtn');
    if (bonusInput) bonusInput.disabled = false;
    if (applyBonusBtn) applyBonusBtn.disabled = false;

    // Показываем форму ввода промокода
    const promoInputGroup = document.querySelector('.promo-input-group');
    if (promoInputGroup) {
        promoInputGroup.style.display = 'flex';
    }

    // Скрываем блок информации о примененном промокоде
    const promoAppliedInfo = document.querySelector('.promo-applied-info');
    if (promoAppliedInfo) {
        promoAppliedInfo.style.display = 'none';
    }

    // Очищаем текст скидки
    const discountTextEl = document.getElementById('discountText');
    if (discountTextEl) discountTextEl.textContent = '';
    const mobileDiscountText = document.getElementById('mobileDiscountText');
    if (mobileDiscountText) mobileDiscountText.textContent = '';
    const desktopDiscountText = document.getElementById('desktopDiscountText');
    if (desktopDiscountText) desktopDiscountText.textContent = '';

    // Восстанавливаем исходную сумму
    const originalTotal = getOriginalTotal();

    // Обновляем итоговую сумму
    const totalElement = document.getElementById('totalAmount');
    const mobileTotalElement = document.getElementById('mobileTotalAmount');
    const mobileTotalFinal = document.getElementById('mobileTotalFinal');

    if (totalElement) totalElement.textContent = originalTotal.toLocaleString('ru-RU') + ' ₽';
    if (mobileTotalElement) mobileTotalElement.textContent = originalTotal.toLocaleString('ru-RU') + ' ₽';
    if (mobileTotalFinal) mobileTotalFinal.textContent = originalTotal.toLocaleString('ru-RU') + ' ₽';

    // ✅ ПРИНУДИТЕЛЬНО СКРЫВАЕМ ВСЕ СТРОКИ СО СКИДКАМИ
    forceHideAllDiscountRows();

    console.log('✅ Промокод полностью отменен локально');
}

// ========== ОСНОВНАЯ ЛОГИКА ПРИ ЗАГРУЗКЕ ==========
document.addEventListener('DOMContentLoaded', function() {
    // 1. Базовая синхронизация
    syncDeliveryTypeFromHeader();
    initPhoneMask();

    // 2. Инициализация деталей адреса
    initAddressDetailsSaving();

    // 3. Загрузка данных
    loadAddressFromStorage();
    loadDeliveryInfoFromCart();

    // 4. Восстановление промокода и бонусов
    restorePromoCodeFromSession();
    restoreBonuses();

    // 5. Обновление отображения
    updateDiscountDisplay();
    originalTotal = getOriginalTotal();

    // 6. Настройка времени доставки
    const timeSelect = document.getElementById('deliveryTimeSelect');
    const deliveryTime = document.getElementById('deliveryTime');

    if (timeSelect && deliveryTime) {
        timeSelect.addEventListener('change', function() {
            const timeValue = this.value;
            if (timeValue) {
                const formatted = convertTimeToLocalDateTime(timeValue);
                deliveryTime.value = formatted;
            } else {
                deliveryTime.value = '';
            }
        });
        restoreDeliveryTime();
    }

    // 7. Мобильное меню сводки
    const summaryToggle = document.getElementById('mobileSummaryToggle');
    if (summaryToggle) {
        summaryToggle.addEventListener('click', function() {
            const card = this.closest('.checkout-summary-card');
            if (card) {
                card.classList.toggle('collapsed');
            }
        });
    }

    // 8. Кнопка изменения адреса
    const changeAddressBtn = document.getElementById('changeAddressBtn');
    if (changeAddressBtn) {
        changeAddressBtn.addEventListener('click', function() {
            if (typeof window.openAddressModal === 'function') {
                window.openAddressModal();
            } else {
                const addressSelector = document.getElementById('addressSelector');
                if (addressSelector) {
                    addressSelector.click();
                }
            }
        });
    }

    // 9. Промокод
    const applyPromoBtn = document.getElementById('applyPromoBtn');
    if (applyPromoBtn) {
        applyPromoBtn.addEventListener('click', applyPromoCode);
    }

    const promoInput = document.getElementById('promoCodeInput');
    if (promoInput) {
        promoInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                applyPromoCode();
            }
        });
    }

    // 10. Бонусы
    const applyBonusBtn = document.getElementById('applyBonusBtn');
    if (applyBonusBtn) {
        applyBonusBtn.addEventListener('click', applyBonuses);
    }

    const bonusInput = document.getElementById('bonusInput');
    if (bonusInput) {
        bonusInput.addEventListener('input', function() {
            this.value = this.value.replace(/[^\d]/g, '');
        });
    }

    // 11. Скрываем пустые строки со скидками
    setTimeout(function() {
        const allDiscountRows = document.querySelectorAll('.discount-row');
        allDiscountRows.forEach(row => {
            const amountSpan = row.querySelector('.discount-amount');
            if (amountSpan) {
                const amountText = amountSpan.textContent.trim();
                if (amountText === '-0 ₽' || amountText === '0 ₽' || amountText === '0') {
                    row.style.display = 'none';
                    row.style.setProperty('display', 'none', 'important');
                }
            }
        });
        console.log('✅ Все пустые строки со скидками скрыты');
    }, 100);

    // 12. Слушаем изменения localStorage
    window.addEventListener('storage', function(e) {
        if (e.key === 'deliveryAddress') {
            loadAddressFromStorage();
        }
    });
});

const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            opacity: 0;
            transform: translateX(100px);
        }
        to {
            opacity: 1;
            transform: translateX(0);
        }
    }
    @keyframes slideOutRight {
        from {
            opacity: 1;
            transform: translateX(0);
        }
        to {
            opacity: 0;
            transform: translateX(100px);
        }
    }
`;
document.head.appendChild(style);

window.updateZoneInfoDisplay = updateZoneInfoDisplay;
window.showMinOrderWarning = showMinOrderWarning;
window.saveAddressDetailsToStorage = saveAddressDetailsToStorage;
window.toggleAddressDetails = toggleAddressDetails;