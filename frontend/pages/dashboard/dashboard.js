/* dashboard.js  MVP */
const user = JSON.parse(localStorage.getItem('userData') || '{}');
const token  = localStorage.getItem('authToken');

/* --- приветствие --- */
document.querySelector('.userName').textContent = user.firstName || 'Gast';
if (!token) location.href = '../login/login.html';

/* --- logout --- */
window.logout = () => {
    localStorage.clear();
    location.href = '../login/login.html';
};

/* --- МОИ КУРСЫ (пока захардкожено) --- */
const myCourses = [
    {title: 'Theater-Grundlagen', progress: 75, nextLesson: '15.12.2025'},
    {title: 'Tanz-Labor',        progress: 30, nextLesson: '18.12.2025'},
    {title: 'Stimme & Präsenz',  progress: 100, nextLesson: null}
];

const coursesList = document.getElementById('myCoursesList');
coursesList.innerHTML = myCourses.map(c => `
  <li>
      <span class="course-title">${c.title}</span>
      <div class="course-meta">
          <span class="progress">${c.progress}%</span>
          <span class="next-lesson">${c.nextLesson || 'abgeschlossen'}</span>
      </div>
      <div class="progress-bar"><div style="width:${c.progress}%"></div></div>
  </li>`).join('');