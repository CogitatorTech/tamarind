// Basic file import helper using existing /api/v1/datasources/upload endpoint
(function(){
  if (window.TamarindFileImport) return;
  function upload(file, tableName){
    return new Promise((resolve,reject)=>{
      if(!file){ reject(new Error('No file')); return; }
      const fd = new FormData();
      fd.append('file', file);
      if (tableName) fd.append('tableName', tableName);
      const token = localStorage.getItem('tamarind_token')||'';
      fetch('/api/v1/datasources/upload',{method:'POST', headers:{'Authorization':'Bearer '+token}, body:fd})
        .then(r=>r.json().then(j=>({ok:r.ok, j})))
        .then(({ok,j})=> ok? resolve(j): reject(new Error(j.error?.message||'Upload failed')))
        .catch(reject);
    });
  }
  window.TamarindFileImport={ upload };
})();

