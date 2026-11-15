/*
 * Quiz page: countdown clock, progress rail and answered tally.
 * The server enforces the deadline; this only keeps the student informed
 * and submits automatically at zero.
 */
(() => {
    const form = document.querySelector('[data-quiz-form]');
    if (!form) return;

    const clock = document.querySelector('[data-clock]');
    const tally = document.querySelector('[data-tally]');
    const bar = document.querySelector('[data-progress]');
    const total = Number(form.dataset.questions);
    let left = Number(clock.dataset.seconds);
    let leaving = false;

    const answeredIds = () => new Set(
        [...form.querySelectorAll('input[type=radio]:checked')].map(r => r.name.replace('answer-', ''))
    );

    const paint = () => {
        const mins = Math.floor(left / 60);
        const secs = String(left % 60).padStart(2, '0');
        clock.textContent = `${mins}:${secs}`;
        clock.classList.toggle('low', left <= 60);

        const done = answeredIds();
        tally.textContent = `${done.size} of ${total} answered`;
        bar.style.width = `${total ? (done.size / total) * 100 : 0}%`;
        document.querySelectorAll('[data-rail]').forEach(dot => {
            dot.classList.toggle('done', done.has(dot.dataset.rail));
        });
    };

    form.addEventListener('change', paint);

    form.addEventListener('submit', (event) => {
        const done = answeredIds().size;
        if (left > 0 && done < total &&
            !window.confirm(`You've answered ${done} of ${total}. Submit anyway?`)) {
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
