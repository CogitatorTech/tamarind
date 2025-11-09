// Placeholder explorer helper hooking into inline notebook.html logic
(function(){
  if (window.TamarindTablesExplorer) return;
  window.TamarindTablesExplorer = {
    refresh: function(){ if (typeof loadTables === 'function') loadTables(); },
    filter: function(q){ if (typeof filterTables === 'function') filterTables(q); },
    preview: function(t){ if (typeof previewTable === 'function') previewTable(t,true); }
  };
})();

