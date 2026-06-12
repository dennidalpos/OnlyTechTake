// OnlyTechTake Landing Page Script

document.addEventListener('DOMContentLoaded', () => {
    // Smooth scrolling for navigation links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.getAttribute('href');
            if (targetId === '#') return;

            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                targetElement.scrollIntoView({
                    behavior: 'smooth'
                });
            }
        });
    });

    // Form submission handling (basic prevention of empty fields)
    const form = document.querySelector('.contact-form');
    if (form) {
        form.addEventListener('submit', (e) => {
            const nome = form.querySelector('input[name="nome"]').value;
            const email = form.querySelector('input[name="email"]').value;

            if (!nome || !email) {
                e.preventDefault();
                alert('Per favore, compila tutti i campi obbligatori.');
            }
        });
    }
});
