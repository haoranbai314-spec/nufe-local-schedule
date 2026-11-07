(function () {
  window.__nufeLocalImport = {state: 'waiting'};
  (async function () {
    try {
      if (location.protocol !== 'https:' || location.hostname !== 'jwxt.nufe.edu.cn') throw Error('请在学校教务课表页面读取');
      let semester = typeof currentSemester !== 'undefined' ? currentSemester : null;
      if (!semester && window.currentSemester) semester = window.currentSemester;
      const select = document.querySelector('#allSemesters');
      const selected = select && select.selectedOptions[0];
      let id = selected ? Number(selected.value) : Number(semester && semester.id);
      if (!Number.isSafeInteger(id) || id < 1) {
        // currentSemester 可能位于闭包；只解析其对象字面量，不执行页面文本。
        for (const script of document.scripts) {
          const source = script.textContent;
          const hit = /\bcurrentSemester\s*=\s*(\{[\s\S]*?\})\s*[;,]/.exec(source);
          if (hit) { try { const s = JSON.parse(hit[1]); if (Number.isSafeInteger(Number(s.id))) { semester = s; id = Number(s.id); break; } } catch (_) {} }
        }
      }
      if (!Number.isSafeInteger(id) || id < 1) throw Error('未找到学期，请确认已打开“我的课表”');
      async function get(path, optional) {
        const controller = new AbortController(); const timer = setTimeout(() => controller.abort(), 15000);
        try {
          const r = await fetch(path, {credentials: 'same-origin', redirect: 'error', signal: controller.signal, headers: {'Accept':'application/json, text/plain, */*','X-Requested-With':'XMLHttpRequest'}});
          if (!r.ok) throw Error('学校返回 ' + r.status);
          const text = await r.text(); if (text.length > 2 * 1024 * 1024) throw Error('课表数据过大');
          return JSON.parse(text);
        } catch (e) { if (optional) return null; throw e; } finally { clearTimeout(timer); }
      }
      const raw = await get('/student/for-std/course-table/semester/' + id + '/print-data?semesterId=' + id + '&hasExperiment=true');
      if (!raw || !Array.isArray(raw.studentTableVms) || !raw.studentTableVms[0]) throw Error('未获取到课表，可能需要重新登录学校');
      const detail = await get('/student/ws/semester/get/' + id, true);
      let week1 = '';
      const start = detail && (detail.startDate || detail.startOn || detail.beginDate);
      if (typeof start === 'string' && /^\d{4}-\d{2}-\d{2}/.test(start)) {
        const d = new Date(start.slice(0,10) + 'T00:00:00Z');
        if (!Number.isNaN(d.getTime())) { d.setUTCDate(d.getUTCDate() - (d.getUTCDay() + 6) % 7); week1 = d.toISOString().slice(0,10); }
      }
      const name = selected ? selected.textContent.trim() : String(semester && semester.name || id);
      window.__nufeLocalImport = {state: 'done', schedule: {semester_id: id, semester_name: name, week1_monday: week1, activities: raw.studentTableVms[0].activities || []}};
    } catch (e) { window.__nufeLocalImport = {state: 'error', error: String(e.message || e)}; }
  })();
  return 'started';
})();
