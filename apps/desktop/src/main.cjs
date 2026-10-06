const { app, BrowserWindow, shell } = require("electron");

const DEFAULT_WEB_URL = "https://korczaktech.github.io/kz-nexa/";
const WEB_URL = process.env.NEXA_WEB_URL || DEFAULT_WEB_URL;

function createWindow() {
  const win = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 960,
    minHeight: 640,
    backgroundColor: "#06100b",
    autoHideMenuBar: true,
    webPreferences: { contextIsolation: true, sandbox: true },
  });

  const showErrorPage = () => {
    const html = "<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\"><title>Korczak Nexa</title>" +
      "<style>body{margin:0;background:#06100b;color:#fff;font-family:system-ui,-apple-system,sans-serif;display:grid;place-items:center;min-height:100vh}main{max-width:560px;padding:40px;text-align:center}h1{margin:0 0 12px;font-size:32px}p{color:#aebbb4;line-height:1.6}button{border:0;border-radius:10px;padding:12px 20px;background:#19c37d;color:#03110a;font-weight:700;cursor:pointer}</style></head>" +
      "<body><main><h1>Korczak Nexa</h1><p>Não foi possível carregar o Nexa. Verifique sua conexão e tente novamente.</p><button id=\"retry\">Tentar novamente</button></main>" +
      "<script>document.getElementById(\"retry\").onclick=function(){location.href=" + JSON.stringify(WEB_URL) + "};</script></body></html>";
    void win.loadURL("data:text/html;charset=utf-8," + encodeURIComponent(html));
  };

  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/i.test(url)) void shell.openExternal(url);
    return { action: "deny" };
  });

  win.webContents.on("did-fail-load", (_event, errorCode, _errorDescription, _validatedURL, isMainFrame) => {
    if (isMainFrame && errorCode !== -3) showErrorPage();
  });

  void win.loadURL(WEB_URL);
}

app.whenReady().then(() => {
  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});