/*
 * Quiz clock: counts down the seconds the server says are left, keeps an
 * "answered" tally, and submits the form automatically at zero. The server
 * still enforces the deadline, so this is a convenience, not a guard.
 */
(() => {
    const form = document.querySelector('[data-quiz-form]');
    if (!form) return;

    const clock = document.querySelector('[data-clock]');
    const tally = document.querySelector('[data-tally]');
    const total = Number(form.dataset.questions);
    let left = Number(clock.dataset.seconds);
    let leaving = false;

    const answered = () => new Set(
        [...form.querySelectorAll('input[type=radio]:checked')].map(r => r.name)
    ).size;

    const paint = () => {
        const mins = Math.floor(left / 60);
        const secs = String(left % 60).padStart(2, '0');
        clock.textContent = `${mins}:${secs}`;
        clock.classList.toggle('low', left <= 60);
        tally.textContent = `${answered()} of ${total} answered`;
    };

    form.addEventListener('change', paint);

    form.addEventListener('submit', (event) => {
        if (left > 0 && answered() < total &&
            !window.confirm('Some questions are unanswered. Submit anyway?')) {
            event.preventDefault();
            return;
        }
        leaving = true;
    });

    window.addEventListener('beforeunload', (event) => {
        if (!leaving) event.preventDefault();
    });

    paint();
    const tick = setInterval(() => {
        left = Math.max(0, left - 1);
        paint();
        if (left === 0) {
            clearInterval(tick);
            leaving = true;
            form.submit();
        }
    }, 1000);
})();
