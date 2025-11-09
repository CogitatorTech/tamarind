// Columns cache with TTL in localStorage for autocomplete/schema
(function(){
  if (window.ColumnsCache) return;
  const KEY = 'tamarind_columns_cache_v1';
  const DEFAULT_TTL_MS = 10 * 60 * 1000; // 10 minutes

  function now(){ return Date.now(); }
  function load(){ try { return JSON.parse(localStorage.getItem(KEY) || '{}'); } catch(_) { return {}; } }
  function save(obj){ try { localStorage.setItem(KEY, JSON.stringify(obj)); } catch(_) {} }

  async function fetchColumns(table, includeTypes){
    const token = localStorage.getItem('tamarind_token') || '';
    const url = `/api/v1/datasources/columns/${encodeURIComponent(table)}${includeTypes?`?includeTypes=true`:''}`;
    const resp = await fetch(url, { headers: { 'Authorization': `Bearer ${token}` } });
    if (!resp.ok) throw new Error('Columns fetch failed');
    const data = await resp.json();
    return (data && data.data) || [];
  }

  async function getRaw(table, { ttlMs = DEFAULT_TTL_MS, includeTypes = false } = {}){
    const cache = load();
    const key = includeTypes ? `${table}::typed` : table;
    const entry = cache[key];
    if (entry && entry.expiresAt && entry.expiresAt > now() && Array.isArray(entry.columns)) {
      return entry.columns;
    }
    const cols = await fetchColumns(table, includeTypes);
    cache[key] = { columns: cols, expiresAt: now() + ttlMs };
    save(cache);
    return cols;
  }

  async function getNames(table, opts){
    const raw = await getRaw(table, { ...(opts||{}), includeTypes: false });
    // If typed objects were returned unexpectedly, map them
    if (raw.length && typeof raw[0] === 'object' && raw[0] !== null) {
      return raw.map(r => r.name).filter(Boolean);
    }
    return raw;
  }

  async function getTyped(table, opts){
    const raw = await getRaw(table, { ...(opts||{}), includeTypes: true });
    // If plain strings were returned, convert to objects with unknown type
    if (raw.length && typeof raw[0] === 'string') {
      return raw.map(n => ({ name: n, type: 'UNKNOWN' }));
    }
    return raw;
  }

  window.ColumnsCache = { getNames, getTyped };
})();
