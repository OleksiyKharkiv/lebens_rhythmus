/* global toggleMobileMenu */

async function loadPartials() {
    const [head, foot] = await Promise.all([
        fetch('../../partials/header.html').then(r => r.text()),
        fetch('../../partials/footer.html').then(r => r.text())
    ]);
    document.body.insertAdjacentHTML('afterbegin', head);
    document.body.insertAdjacentHTML('beforeend', foot);
}

loadPartials().catch(console.error);