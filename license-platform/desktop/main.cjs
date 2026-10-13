'use strict';

const { app, BrowserWindow, dialog } = require('electron');
const fs = require('node:fs');
const path = require('node:path');
const { createLicenseServer } = require('../server.js');

let mainWindow;
let licenseServer;
const port = Number.parseInt(process.env.PORT || '8787', 10);

async function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 940,
    minWidth: 960,
    minHeight: 680,
    title: 'Tu Tien License Control',
    backgroundColor: '#080b13',
    autoHideMenuBar: true,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true,
    },
  });
  await mainWindow.loadURL(`http://127.0.0.1:${port}`);
  mainWindow.on('closed', () => { mainWindow = null; });
}

app.whenReady().then(async () => {
  const dataDir = path.join(app.getPath('userData'), 'data');
  const tokenFile = path.join(dataDir, 'admin-token.txt');
  const envToken = String(process.env.TUTIEN_ADMIN_TOKEN || '').trim();
  const storedToken = fs.existsSync(tokenFile) ? fs.readFileSync(tokenFile, 'utf8').trim() : '';
  const hadToken = envToken.length >= 24 || storedToken.length >= 24;
  licenseServer = createLicenseServer({ dataDir, adminToken: envToken });
  try {
    await new Promise((resolve, reject) => {
      licenseServer.server.once('error', reject);
      licenseServer.server.listen(port, '127.0.0.1', resolve);
    });
  } catch (error) {
    await dialog.showMessageBox({ type:'error', title:'License Control', message:'Không thể mở local license API.', detail:`Cổng ${port} có thể đang được sử dụng.\n${error.message}` });
    app.quit();
    return;
  }
  if (!hadToken) {
    await dialog.showMessageBox({ type:'info', title:'Lưu admin token', message:'Đây là admin token được tạo cho lần chạy đầu.', detail:`${licenseServer.adminToken}\n\nHãy đăng nhập trong cửa sổ app và lưu token an toàn. Có thể xem lại trong thư mục dữ liệu ứng dụng.` });
  }
  await createWindow();
  app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });
});

app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
app.on('before-quit', () => { if (licenseServer?.server?.listening) licenseServer.server.close(); });
