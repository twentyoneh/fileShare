(function () {
  const API_BASE = (window.APP_CONFIG && window.APP_CONFIG.API_BASE) || "";

  const form = document.getElementById("uploadForm");
  const fileInput = document.getElementById("fileInput");
  const uploadBtn = document.getElementById("uploadBtn");
  const dropzone = document.getElementById("dropzone");

  const progressWrap = document.getElementById("progressWrap");
  const progressBar  = document.getElementById("progressBar");
  const progressText = document.getElementById("progressText");

  const resultBox    = document.getElementById("result");
  const downloadLink = document.getElementById("downloadLink");
  const copyBtn      = document.getElementById("copyBtn");
  const errorBox     = document.getElementById("error");

  // state
  fileInput.addEventListener("change", () => {
    uploadBtn.disabled = !fileInput.files || fileInput.files.length === 0;
    hide(errorBox); hide(resultBox); resetProgress();
  });

  // drag/drop UX
  ["dragenter","dragover"].forEach(evt =>
    dropzone.addEventListener(evt, e => { e.preventDefault(); dropzone.classList.add("dragover"); })
  );
  ["dragleave","drop"].forEach(evt =>
    dropzone.addEventListener(evt, e => { e.preventDefault(); dropzone.classList.remove("dragover"); })
  );
  dropzone.addEventListener("drop", e => {
    const files = e.dataTransfer.files;
    if (files && files[0]) { fileInput.files = files; uploadBtn.disabled = false; }
  });

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    if (!fileInput.files || fileInput.files.length === 0) return;

    const file = fileInput.files[0];
    hide(errorBox); hide(resultBox); resetProgress(); show(progressWrap);

    const formData = new FormData();
    formData.append("file", file, file.name);

    const xhr = new XMLHttpRequest();
    xhr.open("POST", API_BASE + "/api/files", true);
    xhr.responseType = "json";
    xhr.timeout = 5 * 60 * 1000;

    xhr.upload.onprogress = (ev) => {
      if (ev.lengthComputable) {
        const p = Math.round((ev.loaded / ev.total) * 100);
        progressBar.style.width = p + "%";
        progressText.textContent = p + "%";
      } else {
        progressText.textContent = "Отправка…";
      }
    };

    xhr.onload = () => {
      hide(progressWrap);
      if (xhr.status >= 200 && xhr.status < 300) {
        const data = xhr.response || safeParse(xhr.responseText);
        if (!data || !data.downloadUrl) return showError("Неожиданный ответ сервера");
        downloadLink.href = data.downloadUrl;
        downloadLink.textContent = data.downloadUrl;
        show(resultBox);
        resetForm();
      } else {
        const msg = (xhr.response && xhr.response.error) || httpMsg(xhr.status);
        showError(msg); resetForm();
      }
    };

    xhr.onerror = () => { showError("Сетевая ошибка."); hide(progressWrap); resetForm(); };
    xhr.ontimeout = () => { showError("Время ожидания истекло."); hide(progressWrap); resetForm(); };

    xhr.send(formData);
  });

  copyBtn.addEventListener("click", async () => {
    const url = downloadLink.href;
    try { await navigator.clipboard.writeText(url);
      copyBtn.textContent = "Скопировано";
      setTimeout(()=>copyBtn.textContent="Скопировать ссылку", 1500);
    } catch { prompt("Скопируйте ссылку:", url); }
  });

  // helpers
  function show(el){ el.classList.remove("hidden"); }
  function hide(el){ el.classList.add("hidden"); }
  function resetProgress(){ progressBar.style.width = "0%"; progressText.textContent = "0%"; }
  function resetForm(){ form.reset(); uploadBtn.disabled = true; }
  function safeParse(s){ try { return JSON.parse(s); } catch { return null; } }
  function httpMsg(status){
    if (status===413) return "Файл слишком большой (413).";
    if (status===400) return "Некорректный запрос (400).";
    if (status===404) return "Ресурс не найден (404).";
    return `Ошибка сервера (${status}).`;
  }
})();
