// Table explorer module: lists tables, preview, schema (typed), and filtering
(function(){
  // Avoid clobbering if already defined
  if (window.__TamarindTablesExplorerLoaded) return;
  window.__TamarindTablesExplorerLoaded = true;

  function escapeHtmlLocal(text){
    const div = document.createElement('div');
    div.textContent = text == null ? '' : String(text);
    return div.innerHTML;
  }
  function escapeRegexLocal(string){ return String(string).replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }

  const DataPreview = {
    timeout: null,
    cache: {},
    tooltip: null,
    currentTable: null,
    async fetchPreview(tableName){
      if (this.cache[tableName]) return this.cache[tableName];
      const token = localStorage.getItem('tamarind_token');
      const sql = `SELECT * FROM ${tableName} LIMIT 5`;
      const response = await fetch('/api/v1/query', { method: 'POST', headers: { 'Content-Type':'application/json', 'Authorization': `Bearer ${token}` }, body: JSON.stringify({ sql }) });
      if (!response.ok) throw new Error('Failed to fetch preview');
      const data = await response.json();
      const preview = { columns: data.data.columns || [], rows: data.data.rows || [], rowCount: data.metadata?.rowCount || (data.data.rows||[]).length };
      this.cache[tableName] = preview; setTimeout(()=>{ delete this.cache[tableName]; }, 5*60*1000);
      return preview;
    },
    show(tableName, element){
      clearTimeout(this.timeout); this.currentTable = tableName;
      this.timeout = setTimeout(async ()=>{ if (this.currentTable!==tableName) return; try { const preview = await this.fetchPreview(tableName); if (this.currentTable!==tableName) return; this.render(preview, element, tableName); } catch(_){} }, 500);
    },
    hide(){ clearTimeout(this.timeout); this.currentTable=null; if (this.tooltip){ this.tooltip.remove(); this.tooltip=null; }},
    render(preview, element, tableName){
      this.hide();
      this.tooltip = document.createElement('div'); this.tooltip.className='data-preview-tooltip';
      const { columns, rows, rowCount } = preview;
      let html = `<div class="preview-header"><strong>${escapeHtmlLocal(tableName)}</strong><span class="preview-count">${rowCount.toLocaleString()} rows</span></div>`;
      if (rows.length>0){
        html += `<div class="preview-table-wrapper"><table class="preview-table"><thead><tr>${columns.map(c=>`<th>${escapeHtmlLocal(c)}</th>`).join('')}</tr></thead><tbody>`;
        html += rows.map(row=>`<tr>${columns.map(col=>{ const v=row[col]; if (v==null) return '<td><span class="null-value">NULL</span></td>'; if (typeof v==='object') return '<td><span class="json-value">{...}</span></td>'; const s=String(v); return `<td title="${escapeHtmlLocal(s)}">${escapeHtmlLocal(s.length>50?s.slice(0,47)+'...':s)}</td>`; }).join('')}</tr>`).join('');
        html += `</tbody></table></div>`;
      } else { html += '<div class="preview-empty">No data</div>'; }
      html += `<div class="preview-footer"><span>Click to insert query</span></div>`;
      this.tooltip.innerHTML = html;
      const rect = element.getBoundingClientRect(); const sidebarRect = element.closest('.sidebar').getBoundingClientRect();
      this.tooltip.style.position='fixed'; this.tooltip.style.left=(sidebarRect.right+10)+'px'; this.tooltip.style.top=Math.max(10, rect.top)+'px';
      document.body.appendChild(this.tooltip);
      if (!document.getElementById('data-preview-styles')){
        const style = document.createElement('style'); style.id='data-preview-styles'; style.textContent = `
          .data-preview-tooltip{background:white;border:1px solid #ddd;border-radius:8px;box-shadow:0 4px 20px rgba(0,0,0,0.15);max-width:600px;max-height:400px;overflow:hidden;z-index:10000;}
          .preview-header{display:flex;justify-content:space-between;align-items:center;padding:12px 16px;border-bottom:1px solid #e9ecef;background:#f8f9fa;border-radius:8px 8px 0 0}
          .preview-count{font-size:12px;color:#666;background:white;padding:2px 8px;border-radius:12px;border:1px solid #dee2e6}
          .preview-table-wrapper{max-height:280px;overflow:auto}
          .preview-table{width:100%;border-collapse:collapse;font-size:13px}
          .preview-table th{position:sticky;top:0;background:#f8f9fa;padding:8px 12px;text-align:left;font-weight:600;color:#495057;border-bottom:2px solid #dee2e6;white-space:nowrap}
          .preview-table td{padding:6px 12px;border-bottom:1px solid #f1f3f5;color:#495057;max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
          .preview-table tbody tr:hover{background:#f8f9fa}
          .preview-table .null-value{color:#adb5bd;font-style:italic;font-size:12px}
          .preview-table .json-value{color:#667eea;font-family:monospace;font-size:12px}
          .preview-empty{padding:40px;text-align:center;color:#adb5bd;font-size:13px}
          .preview-footer{padding:8px 16px;border-top:1px solid #e9ecef;background:#f8f9fa;text-align:center;font-size:12px;color:#6c757d;border-radius:0 0 8px 8px}
        `; document.head.appendChild(style);
      }
    }
  };

  async function loadTables(){
    try {
      const token = localStorage.getItem('tamarind_token');
      const resp = await fetch('/api/v1/datasources/tables', { headers: { 'Authorization': `Bearer ${token}` } });
      const tableList = document.getElementById('tableList');
      if (resp.status===401){ localStorage.removeItem('tamarind_token'); localStorage.removeItem('tamarind_user'); window.location.href='/index.html'; return; }
      if (!resp.ok){ tableList.innerHTML = '<li style="padding:10px;color:#c33;">Error loading tables</li>'; return; }
      const payload = await resp.json(); const tables = (payload && payload.data) || [];
      tableList.innerHTML = tables.length? '': '<li style="padding:10px;color:#999;">No tables found</li>';
      window.__tamarind_tables = {}; tables.forEach(t => window.__tamarind_tables[t]=[]);
      if (window.editorInstances){ Object.values(window.editorInstances).forEach(e=>{ if (e.cm) e.cm.setOption('hintOptions', { tables: window.__tamarind_tables }); }); }
      tables.forEach(table => {
        const li = document.createElement('li'); li.className='table-item';
        const nameSpan = document.createElement('span'); nameSpan.className='tbl-name'; nameSpan.style.display='block'; nameSpan.style.overflow='hidden'; nameSpan.style.textOverflow='ellipsis'; nameSpan.style.whiteSpace='nowrap'; nameSpan.textContent = table; li.appendChild(nameSpan);
        li.addEventListener('click', async () => {
          window.lastSelectedTable = table;
          try {
            const cols = await window.ColumnsCache.getNames(table);
            if (!window.__tamarind_tables) window.__tamarind_tables = {};
            window.__tamarind_tables[table] = cols;
            if (window.editorInstances){ Object.values(window.editorInstances).forEach(e=>{ if (e.cm) e.cm.setOption('hintOptions', { tables: window.__tamarind_tables }); }); }
          } catch(_){}
          if (typeof window.insertQuery === 'function') window.insertQuery(`SELECT *\nFROM ${table} LIMIT 100`);
        });
        li.addEventListener('mouseenter', ()=>DataPreview.show(table, li));
        li.addEventListener('mouseleave', ()=>DataPreview.hide());
        tableList.appendChild(li);
      });
    } catch (e) {
      const tableList = document.getElementById('tableList'); if (tableList) tableList.innerHTML = '<li style="padding:10px;color:#c33;">Connection error</li>';
    }
  }

  function filterTables(query){
    const tableList = document.getElementById('tableList'); if (!tableList) return;
    const tableItems = tableList.querySelectorAll('.table-item'); const searchInfo = document.getElementById('tableSearchInfo');
    const lowerQuery = String(query||'').toLowerCase().trim(); if (!lowerQuery){ tableItems.forEach(i=>i.classList.remove('hidden','highlight')); if (searchInfo) searchInfo.style.display='none'; return; }
    let visibleCount=0; tableItems.forEach(item=>{ const name = item.textContent.toLowerCase(); const matches = name.includes(lowerQuery); item.classList.toggle('hidden', !matches); item.classList.toggle('highlight', matches); const span=item.querySelector('.tbl-name'); if (span){ const regex = new RegExp(`(${escapeRegexLocal(query)})`, 'gi'); const orig = span.textContent; span.innerHTML = matches? orig.replace(regex,'<mark>$1</mark>'): orig; } if (matches) visibleCount++; });
    if (!searchInfo) return; if (visibleCount===0){ searchInfo.textContent=`No tables match "${query}"`; searchInfo.className='table-search-info no-results'; searchInfo.style.display='block'; } else { const total=tableItems.length; if (visibleCount===total){ searchInfo.style.display='none'; } else { searchInfo.textContent=`Showing ${visibleCount} of ${total} tables`; searchInfo.className='table-search-info'; searchInfo.style.display='block'; } }
  }

  function previewTable(table, autorun){
    const cells = document.querySelectorAll('.cell'); if (cells.length===0 && typeof window.addCell==='function') window.addCell();
    const lastCell = document.querySelectorAll('.cell')[document.querySelectorAll('.cell').length - 1];
    const ed = window.editorInstances && window.editorInstances[lastCell.id]; const sql = `SELECT *\nFROM ${table} LIMIT 100`;
    if (ed && ed.setValue) { ed.setValue(sql); ed.focus && ed.focus(); } else { const ta = lastCell && lastCell.querySelector('.sql-input'); if (ta) { ta.value = sql; ta.focus(); } }
    if (autorun && typeof window.runSqlOnCell==='function') window.runSqlOnCell(lastCell.id, sql);
  }

  async function showTableSchema(table){
    const modal = document.getElementById('schemaModal'); const content = document.getElementById('schemaContent'); if (!modal||!content) return; content.innerHTML='Loading...'; modal.classList.add('active');
    try {
      let typed=[]; try { typed = await window.ColumnsCache.getTyped(table); } catch(_) { typed = []; }
      if (typed.length){
        let html = '<table style="width:100%; border-collapse:collapse; font-size:13px;">';
        html += '<thead><tr><th style="text-align:left; padding:6px; border-bottom:1px solid #eee;">Column</th><th style="text-align:left; padding:6px; border-bottom:1px solid #eee;">Type</th></tr></thead><tbody>';
        typed.forEach(c=>{ const n = c && c.name ? String(c.name):''; const t = c && c.type ? String(c.type):'UNKNOWN'; html += `<tr><td style="padding:6px; border-bottom:1px solid #f2f2f2;">${escapeHtmlLocal(n)}</td><td style="padding:6px; border-bottom:1px solid #f2f2f2; color:#666;">${escapeHtmlLocal(t)}</td></tr>`; });
        html += '</tbody></table>'; content.innerHTML = html;
        try { const names = typed.map(t=>t.name).filter(Boolean); window.__tamarind_tables ||= {}; window.__tamarind_tables[table] = names; if (window.editorInstances){ Object.values(window.editorInstances).forEach(e=>{ if (e.cm) e.cm.setOption('hintOptions', { tables: window.__tamarind_tables }); }); } } catch(_){ }
        return;
      }
      let cols=[]; try { cols = await window.ColumnsCache.getNames(table); } catch(_) { cols = []; }
      if (cols.length){ let html = '<table style="width:100%; border-collapse:collapse; font-size:13px;"><thead><tr><th style="text-align:left; padding:6px; border-bottom:1px solid #eee;">Column</th></tr></thead><tbody>' + cols.map(c=>`<tr><td style="padding:6px; border-bottom:1px solid #f2f2f2;">${escapeHtmlLocal(String(c))}</td></tr>`).join('') + '</tbody></table>'; content.innerHTML = html; window.__tamarind_tables ||= {}; window.__tamarind_tables[table] = cols; if (window.editorInstances){ Object.values(window.editorInstances).forEach(e=>{ if (e.cm) e.cm.setOption('hintOptions', { tables: window.__tamarind_tables }); }); } return; }
      const token = localStorage.getItem('tamarind_token'); const resp = await fetch('/api/v1/query', { method: 'POST', headers: { 'Content-Type': 'application/json','Authorization': `Bearer ${token}` }, body: JSON.stringify({ sql: `DESCRIBE ${table}` }) }); const txt = await resp.text(); let payload=null; try { payload = JSON.parse(txt); } catch(_){}
      if (!resp.ok || !payload || !payload.data){ content.innerHTML = '<div style="color:#c33;">Failed to load schema</div>'; return; }
      const rows = Array.isArray(payload.data.rows)?payload.data.rows:[]; const headers = Array.isArray(payload.data.columns)?payload.data.columns:(rows[0]?Object.keys(rows[0]):[]);
      let html = '<table style="width:100%; border-collapse:collapse; font-size:13px;">\n<thead><tr>' + headers.map(h=>`<th style="text-align:left; padding:6px; border-bottom:1px solid #eee;">${escapeHtmlLocal(h)}</th>`).join('') + '</tr></thead><tbody>' + rows.map(r=>'<tr>'+headers.map(h=>`<td style="padding:6px; border-bottom:1px solid #f2f2f2; color:#666;">${escapeHtmlLocal(String(r[h]))}</td>`).join('')+'</tr>').join('') + '</tbody></table>';
      content.innerHTML = html;
    } catch (e) { content.innerHTML = '<div style="color:#c33;">Error loading schema</div>'; }
  }

  function describeSelectedTable(){ if (window.lastSelectedTable) { showTableSchema(window.lastSelectedTable); } else if (typeof window.showToast==='function') { window.showToast('Select a table first', 'error'); } }

  // Expose globals (back-compat with notebook.html)
  window.DataPreview = DataPreview;
  window.loadTables = loadTables;
  window.filterTables = filterTables;
  window.previewTable = previewTable;
  window.showTableSchema = showTableSchema;
  window.describeSelectedTable = describeSelectedTable;

  // Namespaced helper
  window.TamarindTablesExplorer = {
    refresh: loadTables,
    filter: filterTables,
    preview: (t)=>previewTable(t,true),
    schema: showTableSchema
  };
})();
