/**
 * Управление точками самовывоза
 */

const apiUrl = '/api/pickup-points';
let currentEditId = null;
let pointsData = [];

// Инициализация
document.addEventListener('DOMContentLoaded', function() {
    loadPickupPoints();
});

// Загрузка точек
async function loadPickupPoints() {
    try {
        const response = await fetch(apiUrl + '/admin', {
            credentials: 'include'
        });

        if (!response.ok) throw new Error('Ошибка загрузки');

        pointsData = await response.json();
        renderPointsList();
    } catch (error) {
        console.error('Error:', error);
        showToast('Ошибка загрузки', 'error');
    }
}

// Отображение списка
function renderPointsList() {
    const container = document.getElementById('pickupPointsList');

    if (!pointsData || pointsData.length === 0) {
        container.innerHTML = '<tr><td colspan="7" class="text-center p-4 text-muted">Нет созданных точек самовывоза</td></tr>';
        return;
    }

    container.innerHTML = pointsData.map(point => `
        <tr>
            <td><strong>${escapeHtml(point.name)}</strong></td>
            <td>${escapeHtml(point.address)}</td>
            <td>${escapeHtml(point.workingHours || '—')}</td>
            <td>${escapeHtml(point.phone || '—')}</td>
            <td>
                <span class="status-badge ${point.active ? 'status-active' : 'status-inactive'}">
                    ${point.active ? 'Активна' : 'Неактивна'}
                </span>
            </td>
            <td>${point.displayOrder || 0}</td>
            <td>
                <div class="action-buttons">
                    <button class="btn btn-sm btn-outline-primary" onclick="editPoint(${point.id})">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="openDeleteModal(${point.id})">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// Открытие модального окна создания
function openCreateModal() {
    currentEditId = null;
    document.getElementById('modalTitle').innerText = 'Добавление точки самовывоза';
    document.getElementById('pickupPointForm').reset();
    document.getElementById('pointActive').value = 'true';
    document.getElementById('pointDisplayOrder').value = '0';

    const modal = new bootstrap.Modal(document.getElementById('pickupPointModal'));
    modal.show();
}

// Редактирование точки
function editPoint(id) {
    const point = pointsData.find(p => p.id === id);
    if (!point) return;

    currentEditId = id;
    document.getElementById('modalTitle').innerText = 'Редактирование точки';
    document.getElementById('pointName').value = point.name;
    document.getElementById('pointAddress').value = point.address;
    document.getElementById('pointCoordinates').value = point.coordinates || '';
    document.getElementById('pointPhone').value = point.phone || '';
    document.getElementById('pointWorkingHours').value = point.workingHours || '';
    document.getElementById('pointDescription').value = point.description || '';
    document.getElementById('pointDisplayOrder').value = point.displayOrder || 0;
    document.getElementById('pointActive').value = point.active;

    const modal = new bootstrap.Modal(document.getElementById('pickupPointModal'));
    modal.show();
}

// Сохранение точки
async function savePickupPoint() {
    const pointData = {
        name: document.getElementById('pointName').value,
        address: document.getElementById('pointAddress').value,
        coordinates: document.getElementById('pointCoordinates').value,
        phone: document.getElementById('pointPhone').value,
        workingHours: document.getElementById('pointWorkingHours').value,
        description: document.getElementById('pointDescription').value,
        displayOrder: parseInt(document.getElementById('pointDisplayOrder').value) || 0,
        active: document.getElementById('pointActive').value === 'true'
    };

    if (!pointData.name) {
        showToast('Введите название', 'error');
        return;
    }

    if (!pointData.address) {
        showToast('Введите адрес', 'error');
        return;
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    const headers = {
        'Content-Type': 'application/json'
    };
    if (csrfToken) headers[csrfHeader] = csrfToken;

    try {
        let response;
        if (currentEditId) {
            response = await fetch(`${apiUrl}/admin/${currentEditId}`, {
                method: 'PUT',
                headers: headers,
                credentials: 'include',
                body: JSON.stringify(pointData)
            });
        } else {
            response = await fetch(`${apiUrl}/admin`, {
                method: 'POST',
                headers: headers,
                credentials: 'include',
                body: JSON.stringify(pointData)
            });
        }

        if (!response.ok) throw new Error('Ошибка сохранения');

        bootstrap.Modal.getInstance(document.getElementById('pickupPointModal')).hide();
        await loadPickupPoints();
        showToast(currentEditId ? 'Точка обновлена' : 'Точка создана', 'success');

    } catch (error) {
        console.error('Error:', error);
        showToast('Ошибка сохранения', 'error');
    }
}

// Открытие модального окна удаления
function openDeleteModal(id) {
    const point = pointsData.find(p => p.id === id);
    if (!point) return;

    currentEditId = id;
    document.getElementById('deletePointName').innerText = point.name;
    const modal = new bootstrap.Modal(document.getElementById('deleteModal'));
    modal.show();
}

// Подтверждение удаления
async function confirmDelete() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    const headers = {};
    if (csrfToken) headers[csrfHeader] = csrfToken;

    try {
        const response = await fetch(`${apiUrl}/admin/${currentEditId}`, {
            method: 'DELETE',
            headers: headers,
            credentials: 'include'
        });

        if (!response.ok) throw new Error('Ошибка удаления');

        bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
        await loadPickupPoints();
        showToast('Точка удалена', 'success');

    } catch (error) {
        console.error('Error:', error);
        showToast('Ошибка удаления', 'error');
    }
}

// Показ уведомления
function showToast(message, type = 'success') {
    const toastElement = document.getElementById('toast');
    const toastBody = document.getElementById('toastMessage');

    if (!toastElement || !toastBody) {
        alert(message);
        return;
    }

    toastBody.innerText = message;
    toastElement.classList.remove('bg-success', 'bg-danger', 'bg-warning');

    switch(type) {
        case 'error':
            toastElement.classList.add('bg-danger', 'text-white');
            break;
        case 'warning':
            toastElement.classList.add('bg-warning');
            break;
        default:
            toastElement.classList.add('bg-success', 'text-white');
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