(() => {
  'use strict';

  const menu = document.querySelector('.mobile-menu');
  menu?.querySelectorAll('nav a').forEach(link => {
    link.addEventListener('click', () => menu.removeAttribute('open'));
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') menu?.removeAttribute('open');
  });
  document.addEventListener('click', event => {
    if (menu?.open && !menu.contains(event.target)) menu.removeAttribute('open');
  });

  const dialog = document.querySelector('#screen-dialog');
  const image = document.querySelector('#dialog-image');
  const title = document.querySelector('#dialog-title');
  let opener;
  document.querySelectorAll('[data-screen]').forEach(button => {
    button.addEventListener('click', () => {
      if (!dialog || !image || !title) return;
      opener = button;
      image.src = button.dataset.screen;
      image.alt = button.querySelector('img').alt;
      title.textContent = button.dataset.title;
      dialog.showModal();
      document.body.classList.add('dialog-open');
    });
  });
  document.querySelector('.dialog-close')?.addEventListener('click', () => dialog.close());
  dialog?.addEventListener('click', event => {
    if (event.target === dialog) {
      const bounds = dialog.getBoundingClientRect();
      if (event.clientX < bounds.left || event.clientX > bounds.right ||
          event.clientY < bounds.top || event.clientY > bounds.bottom) dialog.close();
    }
  });
  dialog?.addEventListener('close', () => {
    document.body.classList.remove('dialog-open');
    opener?.focus({preventScroll: true});
  });

  document.querySelectorAll('[data-copy]').forEach(button => {
    button.addEventListener('click', async () => {
      const value = document.getElementById(button.dataset.copy)?.textContent.trim();
      const status = document.querySelector('#copy-status');
      if (!value || !status) return;
      try {
        if (!navigator.clipboard) throw new Error('Clipboard unavailable');
        await navigator.clipboard.writeText(value);
        status.textContent = 'SHA-256 copied';
      } catch {
        status.textContent = 'Select and copy the checksum above.';
      }
    });
  });
})();
