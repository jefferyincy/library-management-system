(function () {
  'use strict';

  var rows = document.querySelectorAll('tr.catalog-row');
  var panel = document.getElementById('book-detail-panel');
  var summaryEl = document.getElementById('book-detail-summary');
  var issueInput = document.getElementById('issueId');
  var useIdBtn = document.getElementById('book-detail-use-id');

  if (!rows.length || !panel) return;

  function fillPanel(tr) {
    var id = tr.getAttribute('data-book-id') || '';
    var title = tr.getAttribute('data-title') || '';
    var author = tr.getAttribute('data-author') || '';
    var isbn = tr.getAttribute('data-isbn') || '';
    var available = tr.getAttribute('data-available') || '';
    var summary = tr.getAttribute('data-summary') || '';

    document.getElementById('book-detail-title').textContent = title || '—';
    document.getElementById('book-detail-author').textContent = author || '—';
    document.getElementById('book-detail-isbn').textContent = isbn || '—';
    document.getElementById('book-detail-available').textContent = available || '—';
    document.getElementById('book-detail-id').textContent = id || '—';

    if (summary && summary.trim()) {
      summaryEl.textContent = summary.trim();
    } else {
      summaryEl.textContent =
        'No summary is on file for this book yet. Check the title and author above, or ask staff for more information.';
    }

    panel.classList.add('book-detail-panel--active');
    if (useIdBtn) useIdBtn.disabled = !id;
  }

  function selectRow(tr) {
    rows.forEach(function (r) {
      r.classList.remove('catalog-row--active');
      r.setAttribute('aria-selected', 'false');
    });
    tr.classList.add('catalog-row--active');
    tr.setAttribute('aria-selected', 'true');
    fillPanel(tr);
  }

  rows.forEach(function (tr) {
    tr.addEventListener('click', function () {
      selectRow(tr);
    });
    tr.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        selectRow(tr);
      }
    });
  });

  if (useIdBtn && issueInput) {
    useIdBtn.addEventListener('click', function () {
      var active = document.querySelector('tr.catalog-row--active');
      if (!active) return;
      var id = active.getAttribute('data-book-id');
      if (id) {
        issueInput.value = id;
        issueInput.focus();
      }
    });
  }
})();
