/* Builds the dashboard charts from /api/dashboard/charts (all values come from MySQL). */
(function () {
    'use strict';
    if (!window.Chart) { return; }
    const charts = {};
    let data = null;

    const palettes = {
        vulnerabilitySeverity: ['#dc3545', '#fd7e14', '#ffc107', '#0dcaf0'],
        incidentStatus: ['#2f6fed', '#ffc107', '#0dcaf0', '#198754', '#6c757d'],
        assetDistribution: ['#2f6fed', '#22d3ee', '#8b5cf6', '#f59e0b', '#10b981', '#ef4444', '#64748b', '#ec4899'],
        riskDistribution: ['#198754', '#ffc107', '#fd7e14', '#dc3545'],
        remediationProgress: ['#6c757d', '#0dcaf0', '#2f6fed', '#ffc107', '#198754', '#fd7e14']
    };
    const types = {
        vulnerabilitySeverity: 'bar', incidentStatus: 'doughnut', assetDistribution: 'bar',
        riskDistribution: 'doughnut', remediationProgress: 'bar'
    };

    function css(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    function render() {
        if (!data) { return; }
        const text = css('--cs-chart-text');
        const grid = css('--cs-chart-grid');
        Object.entries(data).forEach(([key, chart]) => {
            const canvas = document.getElementById('chart-' + key);
            if (!canvas) { return; }
            if (charts[key]) { charts[key].destroy(); }
            const type = types[key] || 'bar';
            const total = chart.values.reduce((a, b) => a + b, 0);
            const empty = canvas.parentElement.querySelector('[data-chart-empty]');
            if (empty) { empty.classList.toggle('d-none', total > 0); }
            canvas.classList.toggle('d-none', total === 0);
            const colors = palettes[key] || ['#2f6fed'];
            charts[key] = new Chart(canvas, {
                type: type,
                data: {
                    labels: chart.labels,
                    datasets: [{
                        label: chart.title, data: chart.values, borderWidth: type === 'doughnut' ? 2 : 0,
                        backgroundColor: chart.labels.map((_, i) => colors[i % colors.length]),
                        borderColor: css('--cs-surface'), borderRadius: type === 'bar' ? 6 : 0, maxBarThickness: 42
                    }]
                },
                options: {
                    responsive: true, maintainAspectRatio: false,
                    plugins: {
                        legend: { display: type === 'doughnut', position: 'bottom', labels: { color: text, boxWidth: 12 } },
                        tooltip: { enabled: true }
                    },
                    scales: type === 'doughnut' ? {} : {
                        x: { ticks: { color: text }, grid: { display: false } },
                        y: { beginAtZero: true, ticks: { color: text, precision: 0 }, grid: { color: grid } }
                    },
                    cutout: type === 'doughnut' ? '62%' : undefined
                }
            });
        });
    }

    csFetch('/api/dashboard/charts').then(json => { data = json; render(); })
        .catch(() => document.querySelectorAll('[data-chart-empty]').forEach(e => {
            e.textContent = 'Chart data could not be loaded.'; e.classList.remove('d-none');
        }));
    document.addEventListener('cs:themechange', render);
})();
