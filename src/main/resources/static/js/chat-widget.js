(function () {
  'use strict';

  var panel = document.getElementById('library-chat-panel');
  var toggle = document.getElementById('library-chat-toggle');
  var closeBtn = document.getElementById('library-chat-close');
  var form = document.getElementById('library-chat-form');
  var input = document.getElementById('library-chat-input');
  var messages = document.getElementById('library-chat-messages');
  var badge = document.getElementById('library-chat-badge');
  var backdrop = document.getElementById('library-chat-backdrop');

  if (!panel || !toggle || !form || !input || !messages) return;

  var showedFallbackHint = false;

  function scrollBottom() {
    messages.scrollTop = messages.scrollHeight;
  }

  function addBubble(text, who) {
    var div = document.createElement('div');
    div.className = 'library-chat__bubble library-chat__bubble--' + who;
    div.textContent = text;
    messages.appendChild(div);
    scrollBottom();
  }

  function setOpen(open) {
    panel.classList.toggle('library-chat__panel--open', open);
    panel.setAttribute('aria-hidden', open ? 'false' : 'true');
    toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    if (backdrop) {
      backdrop.classList.toggle('library-chat__backdrop--visible', open);
      backdrop.setAttribute('aria-hidden', open ? 'false' : 'true');
    }
    if (open) {
      input.focus();
      if (badge) badge.hidden = true;
    }
  }

  toggle.addEventListener('click', function () {
    setOpen(!panel.classList.contains('library-chat__panel--open'));
  });

  if (closeBtn) {
    closeBtn.addEventListener('click', function () {
      setOpen(false);
    });
  }

  if (backdrop) {
    backdrop.addEventListener('click', function () {
      setOpen(false);
    });
  }

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') setOpen(false);
  });

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    var text = (input.value || '').trim();
    if (!text) return;
    addBubble(text, 'user');
    input.value = '';

    var thinking = document.createElement('div');
    thinking.className = 'library-chat__bubble library-chat__bubble--bot library-chat__thinking';
    thinking.textContent = '…';
    messages.appendChild(thinking);
    scrollBottom();

    fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ message: text }),
    })
      .then(function (res) {
        thinking.remove();
        if (res.status === 401) {
          addBubble('Please sign in again to use help.', 'bot');
          return null;
        }
        if (res.status === 403) {
          addBubble('Help chat is only available on the student dashboard.', 'bot');
          return null;
        }
        if (!res.ok) {
          addBubble('Something went wrong. Try again in a moment.', 'bot');
          return null;
        }
        return res.json();
      })
      .then(function (data) {
        if (!data || !data.reply) return;
        addBubble(data.reply, 'bot');
        if (data.aiPowered === false && !showedFallbackHint) {
          showedFallbackHint = true;
          var hint = document.createElement('div');
          hint.className = 'library-chat__hint';
          hint.textContent = 'Tip: add an OpenAI API key (library.ai.openai.api-key) for full AI answers.';
          messages.appendChild(hint);
          scrollBottom();
        }
      })
      .catch(function () {
        thinking.remove();
        addBubble('Network error. Check your connection and try again.', 'bot');
      });
  });

  addBubble(
    "Hi! I'm your library helper. Ask how to borrow, return, due dates, fines, or anything about this site.",
    'bot'
  );
})();
