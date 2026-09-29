const GITHUB_REPO = "rejsi-builds/rbal-monitor";

let currentServices = [];
let selectedEnv = "ALL";

async function fetchHealth() {
    try {
        const res = await fetch('http://localhost:8081/api/status');
        const data = await res.json();

        document.getElementById('timestamp').innerText = data.lastUpdate;
        currentServices = data.services;

        renderServices();
        updateCards();

    } catch (err) {
        console.error("Backend server is unreachable", err);
        document.getElementById('timestamp').innerText = "Backend offline";
        setClusterState("UNREACHABLE", "#f87171");
    }
}

function renderServices() {
    const body = document.getElementById('table-body');
    body.innerHTML = '';

    currentServices.forEach((srv, index) => {
        if (selectedEnv !== "ALL" && srv.env !== selectedEnv) {
            return;
        }

        let badgeClass = 'down';
        let badgeText = '○ OFFLINE';
        if (srv.status === 'UP') {
            badgeClass = 'up';
            badgeText = '● ONLINE';
        }

        const row = document.createElement('tr');
        row.onclick = () => openDetails(index);
        row.innerHTML = `
            <td><strong>${srv.name}</strong></td>
            <td><span class="env-tag">${srv.env}</span></td>
            <td><code style="color: #cbd5e1">${srv.url}</code></td>
            <td><span class="badge ${badgeClass}">${badgeText}</span></td>
        `;
        body.appendChild(row);
    });
}

function updateCards() {
    let healthy = 0;
    const environments = new Set();

    currentServices.forEach(srv => {
        environments.add(srv.env);
        if (srv.status === 'UP') {
            healthy++;
        }
    });

    document.getElementById('total-count').innerText = environments.size;
    document.getElementById('healthy-count').innerText = healthy + " / " + currentServices.length;

    if (healthy === currentServices.length) {
        setClusterState("STABLE", "#34d399");
    } else if (healthy > 0) {
        setClusterState("DEGRADED", "#f59e0b");
    } else {
        setClusterState("CRITICAL", "#f87171");
    }
}

function setClusterState(text, color) {
    const statusText = document.getElementById('system-status');
    statusText.innerText = text;
    statusText.style.color = color;
}

function showReleaseMessage(message) {
    document.getElementById('release-body').innerHTML =
        '<tr><td colspan="4">' + message + '</td></tr>';
}

async function fetchReleases() {
    try {
        const res = await fetch('https://api.github.com/repos/' + GITHUB_REPO + '/releases');
        if (!res.ok) {
            showReleaseMessage("Could not load releases. Check that the repository is public.");
            return;
        }

        const releases = await res.json();
        if (releases.length === 0) {
            showReleaseMessage("No releases yet. Push to main to run the pipeline.");
            return;
        }

        const body = document.getElementById('release-body');
        body.innerHTML = '';

        releases.slice(0, 8).forEach(release => {
            let imageCell = "-";
            if (release.assets.length > 0) {
                const asset = release.assets[0];
                imageCell = `<a href="${asset.browser_download_url}" style="color: var(--rbal-yellow)">${asset.name}</a>`;
            }

            const published = new Date(release.published_at).toLocaleString();

            const row = document.createElement('tr');
            row.innerHTML = `
                <td><code>${published}</code></td>
                <td><strong>${release.tag_name}</strong></td>
                <td>${imageCell}</td>
                <td><span class="badge up">✔ RELEASED</span></td>
            `;
            body.appendChild(row);
        });
    } catch (err) {
        console.error("GitHub is unreachable", err);
        showReleaseMessage("Could not reach GitHub.");
    }
}

function formatUptime(totalSeconds) {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return minutes + "m " + seconds + "s";
}

function openDetails(index) {
    const srv = currentServices[index];
    const statusBadge = document.getElementById('modal-service-status');

    document.getElementById('modal-service-name').innerText = srv.name + " (" + srv.env + ")";
    document.getElementById('modal-service-url').innerText = srv.url;

    if (srv.status === 'UP') {
        statusBadge.innerText = "ONLINE";
        statusBadge.className = "badge up";

        document.getElementById('modal-cpu').innerText = srv.metrics.cpu + "%";
        document.getElementById('modal-ram').innerText = srv.metrics.ram + "%";
        document.getElementById('modal-uptime').innerText = formatUptime(srv.metrics.uptimeSeconds);

        document.getElementById('modal-logs').innerText =
            "[INFO] GET /health returned 200 OK in " + srv.responseMs + "ms.\n" +
            "[SUCCESS] Service is running normally.";
    } else {
        statusBadge.innerText = "OFFLINE";
        statusBadge.className = "badge down";

        document.getElementById('modal-cpu').innerText = "0%";
        document.getElementById('modal-ram').innerText = "0%";
        document.getElementById('modal-uptime').innerText = "0m 0s";

        document.getElementById('modal-logs').innerText =
            "[CRITICAL] GET /health failed after " + srv.responseMs + "ms.\n" +
            "[ERROR] Service is not answering requests.\n" +
            "[WARN] Auto-healing alert triggered.";
    }

    document.getElementById('details-modal').style.display = "block";
}

document.getElementById('close-modal').onclick = () => {
    document.getElementById('details-modal').style.display = "none";
};

window.onclick = (event) => {
    const modal = document.getElementById('details-modal');
    if (event.target === modal) {
        modal.style.display = "none";
    }
};

document.querySelectorAll('.filter').forEach(button => {
    button.onclick = () => {
        selectedEnv = button.dataset.env;
        document.querySelectorAll('.filter').forEach(b => b.classList.remove('active'));
        button.classList.add('active');
        renderServices();
    };
});

fetchHealth();
fetchReleases();
setInterval(fetchHealth, 3000);
setInterval(fetchReleases, 120000);