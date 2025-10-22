// Mobile menu management
function toggleMobileMenu() {
    const menu = document.getElementById('mobileMenu');
    menu.style.display = (menu.style.display === 'flex') ? 'none' : 'flex';
}

// smooth scroll through sections
function scrollToSection(id) {
    const el = document.getElementById(id);
    if (el) {
        el.scrollIntoView({behavior: 'smooth'});
    }
}

// Close menu after navigation
const menu = document.getElementById('mobileMenu');
if (menu) menu.style.display = 'none';