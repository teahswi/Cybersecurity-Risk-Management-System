/* Live preview of Risk Score = Likelihood x Impact. The server always recalculates and stores the real value. */
(function () {
    'use strict';
    const l = document.getElementById('likelihood');
    const i = document.getElementById('impact');
    const out = document.getElementById('riskPreview');
    if (!l || !i || !out) { return; }
    function level(score) {
        if (score <= 4) { return ['Low', 'cs-low']; }
        if (score <= 9) { return ['Medium', 'cs-medium']; }
        if (score <= 16) { return ['High', 'cs-high']; }
        return ['Critical', 'cs-critical'];
    }
    function update() {
        const lv = parseInt(l.value, 10);
        const iv = parseInt(i.value, 10);
        if (!lv || !iv) { out.innerHTML = '<span class="cs-muted">Choose likelihood and impact</span>'; return; }
        const score = lv * iv;
        const [name, cls] = level(score);
        out.innerHTML = 'Score <strong>' + lv + ' &times; ' + iv + ' = ' + score + '</strong> &nbsp;<span class="badge cs-badge ' + cls + '">' + name + '</span>';
    }
    l.addEventListener('change', update);
    i.addEventListener('change', update);
    update();
})();
