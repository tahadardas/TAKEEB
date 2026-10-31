/**
 * سيرفر المزامنة المركزي لتطبيق «تكعيب الغضب»
 * لغة البرمجة: Node.js (Express + SQLite)
 * متوافق بالكامل مع Ubuntu و لوحة تحكم Plesk
 */

const express = require('express');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const sqlite3 = require('sqlite3').verbose();
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;
const COMPANY_SECRET_KEY = process.env.COMPANY_SECRET_KEY || 'Ghadab_Sync_2026_SecretKey';
const PHOTOS_DIR = path.resolve(process.env.PHOTOS_DIR || './uploads');
const DB_FILE = path.resolve(process.env.DATABASE_FILE || './database.sqlite');

// التأكد من وجود مجلد رفع الصور
if (!fs.existsSync(PHOTOS_DIR)) {
  fs.mkdirSync(PHOTOS_DIR, { recursive: true });
}

// تجهيز قاعدة بيانات SQLite
const db = new sqlite3.Database(DB_FILE, (err) => {
  if (err) {
    console.error('❌ خطأ في فتح قاعدة بيانات SQLite:', err.message);
  } else {
    console.log('✅ تم الاتصال بقاعدة بيانات SQLite المركزية بنجاح:', DB_FILE);
    initDatabase();
  }
});

function initDatabase() {
  db.serialize(() => {
    // جدول السيارات والشاحنات
    db.run(`
      CREATE TABLE IF NOT EXISTS truck_entries (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        truckNumber TEXT UNIQUE NOT NULL,
        driverName TEXT NOT NULL,
        driverPhone TEXT DEFAULT '',
        hasHeadAndTail INTEGER NOT NULL DEFAULT 1,
        headVolume REAL NOT NULL DEFAULT 0.0,
        tailVolume REAL NOT NULL DEFAULT 0.0,
        totalVolume REAL NOT NULL DEFAULT 0.0,
        hasExtensions INTEGER NOT NULL DEFAULT 0,
        extensionDetails TEXT DEFAULT '',
        materialType TEXT DEFAULT '',
        siteLocation TEXT DEFAULT '',
        notes TEXT DEFAULT '',
        photoPaths TEXT DEFAULT '',
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL
      )
    `);

    // جدول سجل تعديلات وترقيات السيارات
    db.run(`
      CREATE TABLE IF NOT EXISTS truck_modifications (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        truckNumber TEXT NOT NULL,
        modificationDate INTEGER NOT NULL,
        modificationType TEXT NOT NULL,
        previousHeadVolume REAL NOT NULL,
        previousTailVolume REAL NOT NULL,
        previousTotalVolume REAL NOT NULL,
        previousHasExtensions INTEGER NOT NULL,
        previousExtensionDetails TEXT DEFAULT '',
        newHeadVolume REAL NOT NULL,
        newTailVolume REAL NOT NULL,
        newTotalVolume REAL NOT NULL,
        newHasExtensions INTEGER NOT NULL,
        newExtensionDetails TEXT DEFAULT '',
        volumeDifference REAL NOT NULL,
        notes TEXT DEFAULT '',
        photoPaths TEXT DEFAULT '',
        syncedAt INTEGER NOT NULL
      )
    `);

    // جدول سجل عمليات المزامنة (Audit Log)
    db.run(`
      CREATE TABLE IF NOT EXISTS sync_audit_log (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        clientIp TEXT,
        action TEXT,
        itemsCount INTEGER,
        timestamp INTEGER NOT NULL
      )
    `);
  });
}

// Middlewares
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// إعداد Multer لتخزين صور السيارات
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, PHOTOS_DIR);
  },
  filename: (req, file, cb) => {
    // الحفاظ على اسم الملف الأصلي لتطابق الروابط مع الهواتف
    const safeName = path.basename(file.originalname).replace(/[^a-zA-Z0-9_.-]/g, '_');
    cb(null, safeName);
  }
});
const upload = multer({
  storage: storage,
  limits: { fileSize: 25 * 1024 * 1024 } // 25 MB max per photo
});

// التحقق من مفتاح أمان الشركة (Secret API Key)
function verifyApiKey(req, res, next) {
  const clientKey = req.headers['x-api-key'] || req.query.api_key;
  if (!clientKey || clientKey !== COMPANY_SECRET_KEY) {
    return res.status(401).json({
      success: false,
      message: 'غير مصرح: مفتاح أمان الشركة غير صحيح أو مفقود.'
    });
  }
  next();
}

// ==========================================
// 1. فحص حالة السيرفر والاتصال (Health Check)
// ==========================================
app.get('/api/status', verifyApiKey, (req, res) => {
  db.get(`SELECT COUNT(*) AS truckCount, SUM(totalVolume) AS totalVolume FROM truck_entries`, (err, truckRow) => {
    if (err) return res.status(500).json({ success: false, error: err.message });

    db.get(`SELECT COUNT(*) AS modCount FROM truck_modifications`, (err2, modRow) => {
      const photoFiles = fs.existsSync(PHOTOS_DIR) ? fs.readdirSync(PHOTOS_DIR).length : 0;

      res.json({
        success: true,
        serverName: 'تكعيب الغضب - سيرفر المزامنة المركزي',
        status: 'online',
        serverTime: Date.now(),
        statistics: {
          totalTrucks: truckRow ? truckRow.truckCount : 0,
          totalCubicVolume: truckRow && truckRow.totalVolume ? Number(truckRow.totalVolume.toFixed(2)) : 0,
          totalModifications: modRow ? modRow.modCount : 0,
          totalPhotosOnServer: photoFiles
        }
      });
    });
  });
});

// ==========================================
// 2. استقبال السجلات الجديدة من الهاتف (Push Sync)
// ==========================================
app.post('/api/sync/push', verifyApiKey, (req, res) => {
  const { trucks = [], modifications = [] } = req.body;
  const now = Date.now();
  let updatedCount = 0;
  let addedCount = 0;

  db.serialize(() => {
    db.run('BEGIN TRANSACTION');

    const truckStmt = db.prepare(`
      INSERT INTO truck_entries (
        truckNumber, driverName, driverPhone, hasHeadAndTail, headVolume, tailVolume,
        totalVolume, hasExtensions, extensionDetails, materialType, siteLocation,
        notes, photoPaths, createdAt, updatedAt
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(truckNumber) DO UPDATE SET
        driverName = excluded.driverName,
        driverPhone = excluded.driverPhone,
        hasHeadAndTail = excluded.hasHeadAndTail,
        headVolume = excluded.headVolume,
        tailVolume = excluded.tailVolume,
        totalVolume = excluded.totalVolume,
        hasExtensions = excluded.hasExtensions,
        extensionDetails = excluded.extensionDetails,
        materialType = excluded.materialType,
        siteLocation = excluded.siteLocation,
        notes = excluded.notes,
        photoPaths = excluded.photoPaths,
        updatedAt = excluded.updatedAt
    `);

    for (const t of trucks) {
      if (!t.truckNumber || !t.truckNumber.trim()) continue;

      truckStmt.run(
        t.truckNumber.trim(),
        t.driverName || '',
        t.driverPhone || '',
        t.hasHeadAndTail ? 1 : 0,
        Number(t.headVolume) || 0,
        Number(t.tailVolume) || 0,
        Number(t.totalVolume) || 0,
        t.hasExtensions ? 1 : 0,
        t.extensionDetails || '',
        t.materialType || '',
        t.siteLocation || '',
        t.notes || '',
        t.photoPaths || '',
        t.createdAt || now,
        now
      );
    }
    truckStmt.finalize();

    // إدراج تعديلات الشاحنات
    const modStmt = db.prepare(`
      INSERT INTO truck_modifications (
        truckNumber, modificationDate, modificationType, previousHeadVolume, previousTailVolume,
        previousTotalVolume, previousHasExtensions, previousExtensionDetails, newHeadVolume,
        newTailVolume, newTotalVolume, newHasExtensions, newExtensionDetails, volumeDifference,
        notes, photoPaths, syncedAt
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);

    for (const m of modifications) {
      if (!m.truckNumber || !m.truckNumber.trim()) continue;
      modStmt.run(
        m.truckNumber.trim(),
        m.modificationDate || now,
        m.modificationType || 'تعديل',
        Number(m.previousHeadVolume) || 0,
        Number(m.previousTailVolume) || 0,
        Number(m.previousTotalVolume) || 0,
        m.previousHasExtensions ? 1 : 0,
        m.previousExtensionDetails || '',
        Number(m.newHeadVolume) || 0,
        Number(m.newTailVolume) || 0,
        Number(m.newTotalVolume) || 0,
        m.newHasExtensions ? 1 : 0,
        m.newExtensionDetails || '',
        Number(m.volumeDifference) || 0,
        m.notes || '',
        m.photoPaths || '',
        now
      );
    }
    modStmt.finalize();

    // تسجيل العملية في Audit Log
    db.run(
      `INSERT INTO sync_audit_log (clientIp, action, itemsCount, timestamp) VALUES (?, 'push', ?, ?)`,
      [req.ip || 'unknown', trucks.length + modifications.length, now]
    );

    db.run('COMMIT', (err) => {
      if (err) {
        return res.status(500).json({ success: false, message: 'فشل حفظ المزامنة بالسيرفر', error: err.message });
      }
      res.json({
        success: true,
        message: 'تم حفظ ومزامنة البيانات في السيرفر بنجاح',
        syncedTrucks: trucks.length,
        syncedModifications: modifications.length,
        serverTimestamp: now
      });
    });
  });
});

// ==========================================
// 3. سحب البيانات المحدثة إلى الهاتف (Pull Sync)
// ==========================================
app.get('/api/sync/pull', verifyApiKey, (req, res) => {
  const since = Number(req.query.since) || 0;

  // جلب السيارات المحدثة أو المضافة بعد وقت محدد (أو كل السيارات إذا كان since=0)
  db.all(
    `SELECT * FROM truck_entries WHERE updatedAt >= ? OR createdAt >= ? ORDER BY updatedAt ASC`,
    [since, since],
    (err, trucks) => {
      if (err) return res.status(500).json({ success: false, error: err.message });

      db.all(
        `SELECT * FROM truck_modifications WHERE syncedAt >= ? OR modificationDate >= ? ORDER BY modificationDate ASC`,
        [since, since],
        (err2, modifications) => {
          if (err2) return res.status(500).json({ success: false, error: err2.message });

          // تحويل الأنواع لتطابق كيانات الأندرويد
          const mappedTrucks = (trucks || []).map(t => ({
            truckNumber: t.truckNumber,
            driverName: t.driverName,
            driverPhone: t.driverPhone,
            hasHeadAndTail: Boolean(t.hasHeadAndTail),
            headVolume: t.headVolume,
            tailVolume: t.tailVolume,
            totalVolume: t.totalVolume,
            hasExtensions: Boolean(t.hasExtensions),
            extensionDetails: t.extensionDetails,
            materialType: t.materialType,
            siteLocation: t.siteLocation,
            notes: t.notes,
            photoPaths: t.photoPaths,
            createdAt: t.createdAt
          }));

          const mappedMods = (modifications || []).map(m => ({
            truckNumber: m.truckNumber,
            modificationDate: m.modificationDate,
            modificationType: m.modificationType,
            previousHeadVolume: m.previousHeadVolume,
            previousTailVolume: m.previousTailVolume,
            previousTotalVolume: m.previousTotalVolume,
            previousHasExtensions: Boolean(m.previousHasExtensions),
            previousExtensionDetails: m.previousExtensionDetails,
            newHeadVolume: m.newHeadVolume,
            newTailVolume: m.newTailVolume,
            newTotalVolume: m.newTotalVolume,
            newHasExtensions: Boolean(m.newHasExtensions),
            newExtensionDetails: m.newExtensionDetails,
            volumeDifference: m.volumeDifference,
            notes: m.notes,
            photoPaths: m.photoPaths
          }));

          res.json({
            success: true,
            serverTimestamp: Date.now(),
            trucksCount: mappedTrucks.length,
            modificationsCount: mappedMods.length,
            trucks: mappedTrucks,
            modifications: mappedMods
          });
        }
      );
    }
  );
});

// ==========================================
// 4. رفع صورة إلى السيرفر (Photo Upload)
// ==========================================
app.post('/api/photos/upload', verifyApiKey, upload.single('photo'), (req, res) => {
  if (!req.file) {
    return res.status(400).json({ success: false, message: 'لم يتم إرسال أي صورة' });
  }

  res.json({
    success: true,
    message: 'تم رفع الصورة وحفظها في السيرفر بنجاح',
    fileName: req.file.filename,
    size: req.file.size
  });
});

// ==========================================
// 5. تحميل ومعاينة الصورة من السيرفر (Photo Download)
// ==========================================
app.get('/api/photos/:fileName', (req, res) => {
  const fileName = path.basename(req.params.fileName);
  const filePath = path.join(PHOTOS_DIR, fileName);

  if (fs.existsSync(filePath)) {
    res.sendFile(filePath);
  } else {
    res.status(404).json({ success: false, message: 'الصورة غير موجودة على السيرفر' });
  }
});

// ==========================================
// 6. تحميل نسخة كاملة من قاعدة بيانات السيرفر
// ==========================================
app.get('/api/download/database', verifyApiKey, (req, res) => {
  if (fs.existsSync(DB_FILE)) {
    res.download(DB_FILE, 'TakeebGhadab_VPS_Master.sqlite');
  } else {
    res.status(404).send('قاعدة البيانات فارغة أو غير موجودة بعد');
  }
});

// ==========================================
// 7. واجهة ويب مريحة للمشرف (Dashboard)
// ==========================================
app.get('/', (req, res) => {
  db.get(`SELECT COUNT(*) AS truckCount, SUM(totalVolume) AS totalVolume FROM truck_entries`, (err, truckRow) => {
    db.get(`SELECT COUNT(*) AS modCount FROM truck_modifications`, (err2, modRow) => {
      const photoFiles = fs.existsSync(PHOTOS_DIR) ? fs.readdirSync(PHOTOS_DIR).length : 0;
      const count = truckRow ? truckRow.truckCount : 0;
      const volume = truckRow && truckRow.totalVolume ? truckRow.totalVolume.toFixed(2) : '0.00';
      const mods = modRow ? modRow.modCount : 0;

      res.send(`
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>سيرفر مزامنة تكعيب الغضب</title>
          <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.rtl.min.css">
          <style>
            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0d131f; color: #f0f4fc; }
            .card-stat { background: #182234; border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; }
            .badge-live { background-color: #10b981; color: white; padding: 6px 12px; border-radius: 20px; font-weight: bold; }
          </style>
        </head>
        <body class="p-4">
          <div class="container py-4" style="max-width: 820px;">
            <div class="d-flex justify-content-between align-items-center mb-4 pb-3 border-bottom border-secondary">
              <div>
                <h2 class="fw-bold mb-1">🚚 تكعيب الغضب - سيرفر المزامنة المركزي</h2>
                <p class="text-secondary mb-0">لوحة تحكم السيرفر المباشر (Ubuntu / Plesk)</p>
              </div>
              <span class="badge-live">🟢 السيرفر يعمل بنجاح</span>
            </div>

            <div class="row g-3 mb-4">
              <div class="col-md-3 col-6">
                <div class="card-stat p-3 text-center">
                  <div class="text-secondary small">السيارات المسجلة</div>
                  <div class="h2 fw-bold text-warning mb-0">${count}</div>
                </div>
              </div>
              <div class="col-md-3 col-6">
                <div class="card-stat p-3 text-center">
                  <div class="text-secondary small">إجمالي التكعيب م³</div>
                  <div class="h2 fw-bold text-info mb-0">${volume}</div>
                </div>
              </div>
              <div class="col-md-3 col-6">
                <div class="card-stat p-3 text-center">
                  <div class="text-secondary small">سجلات التعديل</div>
                  <div class="h2 fw-bold text-success mb-0">${mods}</div>
                </div>
              </div>
              <div class="col-md-3 col-6">
                <div class="card-stat p-3 text-center">
                  <div class="text-secondary small">الصور بالسيرفر</div>
                  <div class="h2 fw-bold text-primary mb-0">${photoFiles}</div>
                </div>
              </div>
            </div>

            <div class="card-stat p-4 mb-4">
              <h5 class="fw-bold text-warning mb-3">📡 كيفية ربط تطبيق الأندرويد بهذا السيرفر:</h5>
              <ol class="text-light pe-3">
                <li class="mb-2">افتح تطبيق <strong>«تكعيب الغضب»</strong> على أي هاتف في الشركة.</li>
                <li class="mb-2">انتقل إلى <strong>«مزامنة السيرفر السحابي (VPS)»</strong> من القائمة العلوية أو شاشة النسخ الاحتياطي.</li>
                <li class="mb-2">أدخل رابط هذا السيرفر: <code>http://[عنوان_السيرفر]:${PORT}</code> أو النطاق المشفر <code>https://sync.yourdomain.com</code></li>
                <li class="mb-2">أدخل رمز أمان الشركة (Secret API Key) المحدد في إعدادات السيرفر.</li>
                <li>اضغط <strong>«مزامنة الآن»</strong>، وسيتم تبادل السيارات والصور بين جميع الهواتف فوراً!</li>
              </ol>
            </div>

            <div class="text-center text-secondary small">
              تطبيق تكعيب الغضب © 2026 | خادم المزامنة المحلي والسحابي الآمن للشركات
            </div>
          </div>
        </body>
        </html>
      `);
    });
  });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 سيرفر المزامنة يعمل بنجاح على المنفذ: ${PORT}`);
  console.log(`🔐 رمز أمان الشركة المعين: ${COMPANY_SECRET_KEY}`);
  console.log(`📁 مسار تخزين الصور: ${PHOTOS_DIR}`);
});
