// Simple theme toggle (light and dark) using data-theme attribute
(function(){
  if (window.TamarindTheme) return;
  const KEY='tamarind_theme';
  function apply(theme){ document.documentElement.setAttribute('data-theme', theme); localStorage.setItem(KEY, theme); }
  function toggle(){ const cur = localStorage.getItem(KEY)||'light'; apply(cur==='light'?'dark':'light'); }
  function init(){ const saved = localStorage.getItem(KEY)||'light'; apply(saved); }
  window.TamarindTheme={ toggle, apply, init };
  document.addEventListener('DOMContentLoaded', init);
})();

