// для горизонтального скролла
document.addEventListener('DOMContentLoaded', function() {
    const cards = document.getElementById('cards');
    const prevBtn = document.querySelector('.sushi-scroll-btn.prev');
    const nextBtn = document.querySelector('.sushi-scroll-btn.next');

    if (prevBtn && nextBtn && cards) {
        prevBtn.addEventListener('click', () => {
            cards.scrollBy({
                left: -300,
                behavior: 'smooth'
            });
        });

        nextBtn.addEventListener('click', () => {
            cards.scrollBy({
                left: 300,
                behavior: 'smooth'
            });
        });

        // Прокрутка мышкой
        let isDown = false;
        let startX;
        let scrollLeft;

        cards.addEventListener('mousedown', (e) => {
            isDown = true;
            startX = e.pageX - cards.offsetLeft;
            scrollLeft = cards.scrollLeft;
        });

        cards.addEventListener('mouseleave', () => {
            isDown = false;
        });

        cards.addEventListener('mouseup', () => {
            isDown = false;
        });

        cards.addEventListener('mousemove', (e) => {
            if (!isDown) return;
            e.preventDefault();
            const x = e.pageX - cards.offsetLeft;
            const walk = (x - startX) * 2;
            cards.scrollLeft = scrollLeft - walk;
        });
    }
});



