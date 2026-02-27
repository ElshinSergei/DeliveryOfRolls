<!-- Изменение статуса заказа через модальное окно -->
document.addEventListener('DOMContentLoaded', function() {
    const statusModal = document.getElementById('statusModal');
    if (!statusModal) return; // Если модалки нет на странице - выходим
    let currentOrderId = null; // переменная для хранения ID

    statusModal.addEventListener('show.bs.modal', function(event) {
        // Кнопка, которая открыла модалку
        const button = event.relatedTarget;
        // Получаем данные из data-атрибутов
        const orderId = button.getAttribute('data-order-id');
        const currentStatus = button.getAttribute('data-current-status');

        currentOrderId = orderId; // Сохраняем ID заказа
        document.getElementById('modalOrderId').textContent = orderId; // Обновляем заголовок

        // Выбираем текущий статус в селекте
        const select = document.getElementById('modalStatusSelect');
        Array.from(select.options).forEach(option => {
            if (option.value === currentStatus) {
                option.selected = true;
            }
        });
    });

    // Отправка формы
    document.getElementById('statusForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const orderId = currentOrderId;
        const status = document.getElementById('modalStatusSelect').value;
        const submitBtn = this.querySelector('button[type="submit"]');
        const originalText = submitBtn.innerHTML;

        if (!orderId) {
            alert('❌ Ошибка: ID заказа не найден');
            return;
        }

        submitBtn.disabled = true;
        submitBtn.innerHTML = '⏳';

        try {
            const csrfToken = document.querySelector('input[name="_csrf"]')?.value;
            const response = await fetch(`/admin/orders/${orderId}/status?status=${status}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-CSRF-TOKEN': csrfToken
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();

            if (data.success) {
                // Закрываем модалку
                const modal = bootstrap.Modal.getInstance(statusModal);
                if (modal) modal.hide();

                // Определяем, на какой мы странице
                const isDetailPage = window.location.pathname.includes('/admin/orders/') &&
                    window.location.pathname !== '/admin/orders';

                if (isDetailPage) {
                    // Детальная страница - перезагружаем
                    location.reload();
                } else {
                    // Страница списка - обновляем таблицу
                    const row = document.querySelector(`#order-${orderId}`);
                    if (row) {
                        const statusCell = row.querySelector('td:nth-child(8)');
                        const statusMap = {
                            'PENDING': { text: 'Ожидает', class: 'status-pending' },
                            'CONFIRMED': { text: 'Подтвержден', class: 'status-confirmed' },
                            'PREPARING': { text: 'Готовится', class: 'status-preparing' },
                            'READY_FOR_DELIVERY': { text: 'Готов к выдаче', class: 'status-ready' },
                            'ON_THE_WAY': { text: 'В пути', class: 'status-ontheway' },
                            'DELIVERED': { text: 'Доставлен', class: 'status-delivered' },
                            'COMPLETED': { text: 'Завершен', class: 'status-completed' },
                            'CANCELLED': { text: 'Отменен', class: 'status-cancelled' }
                        };
                        const statusInfo = statusMap[status];
                        statusCell.innerHTML = `<span class="${statusInfo.class}">${statusInfo.text}</span>`;
                    }
                }

                alert('✅ Статус обновлен!');
            } else {
                alert('❌ ' + data.message);
            }
        } catch (error) {
            console.error('Ошибка:', error);
            alert('❌ Ошибка соединения: ' + error.message);
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    });
});

