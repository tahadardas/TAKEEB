/**
 * سيرفر المزامنة المركزي لتطبيق «تكعيب الغضب»
 * لغة البرمجة: Node.js (Pure JavaScript - بدون اعتمادات C++ أو مشاكل تجميع)
 * متوافق بنسبة 100% مع Ubuntu، وجميع إصدارات Node.js في لوحة تحكم Plesk
 */

const express = require('express');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;
const COMPANY_SECRET_KEY = process.env.COMPANY_SECRET_KEY || 'Ghadab_Sync_2026_SecretKey';
const PHOTOS_DIR = path.resolve(process.env.PHOTOS_DIR || './uploads');
const DB_FILE = path.resolve(process.env.DATABASE_FILE || './database.json');

// التأكد من وجود مجلد رفع الصور
if (!fs.existsSync(PHOTOS_DIR)) {
  fs.mkdirSync(PHOTOS_DIR, { recursive: true });
}

// ==============================================================
// محرك قاعدة البيانات فائق السرعة والأمان (Pure JS JSON Database)
// يخزن البيانات بشكل ذري (Atomic File Writing) لمنع أي تلف
// ==============================================================
class DatabaseEngine {
  constructor(filePath) {
    this.filePath = filePath;
    this.data = {
      truck_entries: {},      // المفتاح: truckNumber
      truck_modifications: [], // مصفوفة التعديلات
      sync_audit_log: []       // سجل عمليات المزامنة
    };
    this.load();
  }

  load() {
    try {
      if (fs.existsSync(this.filePath)) {
        const raw = fs.readFileSync(this.filePath, 'utf8');
        const parsed = JSON.parse(raw);
        this.data.truck_entries = parsed.truck_entries || {};
        this.data.truck_modifications = parsed.truck_modifications || [];
        this.data.sync_audit_log = parsed.sync_audit_log || [];
        console.log(`✅ تم تحميل قاعدة البيانات المركزية بنجاح: ${Object.keys(this.data.truck_entries).length} شاحنة مسجلة.`);
      } else {
        this.save();
        console.log(`✨ تم إنشاء ملف قاعدة بيانات جديد: ${this.filePath}`);
      }
    } catch (err) {
      console.error('⚠️ تحذير أثناء قراءة قاعدة البيانات، جاري التهيئة:', err.message);
      this.save();
    }
  }

  save() {
    try {
      const dir = path.dirname(this.filePath);
      if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
      
      const tmpPath = `${this.filePath}.tmp`;
      const json = JSON.stringify(this.data, null, 2);
      fs.writeFileSync(tmpPath, json, 'utf8');
      fs.renameSync(tmpPath, this.filePath); // كتابة ذرية آمنة 100%
    } catch (err) {
      console.error('❌ خطأ في حفظ قاعدة البيانات على القرص:', err.message);
    }
  }

  getStatistics() {
    const trucks = Object.values(this.data.truck_entries);
    const totalVolume = trucks.reduce((sum, t) => sum + (Number(t.totalVolume) || 0), 0);
    const photoCount = fs.existsSync(PHOTOS_DIR) ? fs.readdirSync(PHOTOS_DIR).length : 0;

    return {
      totalTrucks: trucks.length,
      totalCubicVolume: Number(totalVolume.toFixed(2)),
      totalModifications: this.data.truck_modifications.length,
      totalPhotosOnServer: photoCount
    };
  }

  pushSync(trucks = [], modifications = [], clientIp = 'unknown') {
    const now = Date.now();
    let updatedTrucks = 0;
    let newTrucks = 0;

    // دمج أو تحديث الشاحنات
    for (const t of trucks) {
      if (!t.truckNumber || !t.truckNumber.trim()) continue;
      const key = t.truckNumber.trim();
      const existing = this.data.truck_entries[key];

      this.data.truck_entries[key] = {
        truckNumber: key,
        driverName: t.driverName || (existing ? existing.driverName : ''),
        driverPhone: t.driverPhone || (existing ? existing.driverPhone : ''),
        hasHeadAndTail: t.hasHeadAndTail !== undefined ? Boolean(t.hasHeadAndTail) : (existing ? existing.hasHeadAndTail : true),
        headVolume: Number(t.headVolume) || 0,
        tailVolume: Number(t.tailVolume) || 0,
        totalVolume: Number(t.totalVolume) || 0,
        hasExtensions: Boolean(t.hasExtensions),
        extensionDetails: t.extensionDetails || '',
        materialType: t.materialType || '',
        siteLocation: t.siteLocation || '',
        notes: t.notes || (existing ? existing.notes : ''),
        photoPaths: t.photoPaths || (existing ? existing.photoPaths : ''),
        createdAt: t.createdAt || (existing ? existing.createdAt : now),
        updatedAt: now
      };

      if (existing) updatedTrucks++; else newTrucks++;
    }

    // إضافة التعديلات (مع منع التكرار بناءً على truckNumber و modificationDate)
    for (const m of modifications) {
      if (!m.truckNumber || !m.truckNumber.trim()) continue;
      const tNum = m.truckNumber.trim();
      const mDate = Number(m.modificationDate) || now;

      const alreadyExists = this.data.truck_modifications.some(
        item => item.truckNumber === tNum && Math.abs(item.modificationDate - mDate) < 1000
      );

      if (!alreadyExists) {
        this.data.truck_modifications.push({
          truckNumber: tNum,
          modificationDate: mDate,
          modificationType: m.modificationType || 'تعديل',
          previousHeadVolume: Number(m.previousHeadVolume) || 0,
          previousTailVolume: Number(m.previousTailVolume) || 0,
          previousTotalVolume: Number(m.previousTotalVolume) || 0,
          previousHasExtensions: Boolean(m.previousHasExtensions),
          previousExtensionDetails: m.previousExtensionDetails || '',
          newHeadVolume: Number(m.newHeadVolume) || 0,
          newTailVolume: Number(m.newTailVolume) || 0,
          newTotalVolume: Number(m.newTotalVolume) || 0,
          newHasExtensions: Boolean(m.newHasExtensions),
          newExtensionDetails: m.newExtensionDetails || '',
          volumeDifference: Number(m.volumeDifference) || 0,
          notes: m.notes || '',
          photoPaths: m.photoPaths || '',
          syncedAt: now
        });
      }
    }

    // تسجيل في الـ Audit Log
    this.data.sync_audit_log.push({
      clientIp,
      action: 'push',
      itemsCount: trucks.length + modifications.length,
      timestamp: now
    });

    // الحفاظ على حجم الـ Audit Log بحد أقصى 500 عملية
    if (this.data.sync_audit_log.length > 500) {
      this.data.sync_audit_log = this.data.sync_audit_log.slice(-500);
    }

    this.save();

    return {
      newTrucks,
      updatedTrucks,
      totalModifications: modifications.length,
      serverTimestamp: now
    };
  }

  pullSync(since = 0) {
    const sinceNum = Number(since) || 0;
    const allTrucks = Object.values(this.data.truck_entries);

    // تصفية الشاحنات التي تم تحديثها بعد الوقت المعطى
    const filteredTrucks = sinceNum === 0 
      ? allTrucks 
      : allTrucks.filter(t => (t.updatedAt || 0) >= sinceNum || (t.createdAt || 0) >= sinceNum);

    // تصفية التعديلات
    const filteredMods = sinceNum === 0
      ? this.data.truck_modifications
      : this.data.truck_modifications.filter(m => (m.syncedAt || 0) >= sinceNum || (m.modificationDate || 0) >= sinceNum);

    return {
      trucks: filteredTrucks,
      modifications: filteredMods
    };
  }
}

const db = new DatabaseEngine(DB_FILE);

// ==========================================
// Middlewares
// ==========================================
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// إعداد Multer لتخزين صور السيارات
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, PHOTOS_DIR);
  },
  filename: (req, file, cb) => {
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
  const stats = db.getStatistics();
  res.json({
    success: true,
    serverName: 'تكعيب الغضب - سيرفر المزامنة المركزي',
    status: 'online',
    serverTime: Date.now(),
    statistics: stats
  });
});

// ==========================================
// 2. استقبال السجلات الجديدة من الهاتف (Push Sync)
// ==========================================
app.post('/api/sync/push', verifyApiKey, (req, res) => {
  try {
    const { trucks = [], modifications = [] } = req.body;
    const result = db.pushSync(trucks, modifications, req.ip || 'unknown');

    res.json({
      success: true,
      message: 'تم حفظ ومزامنة البيانات في السيرفر بنجاح',
      syncedTrucks: trucks.length,
      syncedModifications: modifications.length,
      serverTimestamp: result.serverTimestamp
    });
  } catch (err) {
    console.error('❌ خطأ أثناء حفظ البيانات:', err);
    res.status(500).json({ success: false, message: 'فشل حفظ المزامنة بالسيرفر', error: err.message });
  }
});

// ==========================================
// 3. سحب البيانات المحدثة إلى الهاتف (Pull Sync)
// ==========================================
app.get('/api/sync/pull', verifyApiKey, (req, res) => {
  try {
    const since = Number(req.query.since) || 0;
    const { trucks, modifications } = db.pullSync(since);

    res.json({
      success: true,
      serverTimestamp: Date.now(),
      trucksCount: trucks.length,
      modificationsCount: modifications.length,
      trucks,
      modifications
    });
  } catch (err) {
    console.error('❌ خطأ أثناء سحب البيانات:', err);
    res.status(500).json({ success: false, error: err.message });
  }
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
    res.download(DB_FILE, 'TakeebGhadab_VPS_Master.json');
  } else {
    res.status(404).send('قاعدة البيانات فارغة أو غير موجودة بعد');
  }
});

// ==========================================
// 7. واجهة ويب مريحة للمشرف (Dashboard)
// ==========================================
app.get('/', (req, res) => {
  const stats = db.getStatistics();
  const allTrucks = Object.values(db.data.truck_entries).slice(0, 15);

  const trucksRowsHtml = allTrucks.map(t => `
    <tr>
      <td class="fw-bold">${t.truckNumber}</td>
      <td>${t.driverName || '—'}</td>
      <td><span class="badge bg-primary fs-6">${(Number(t.totalVolume) || 0).toFixed(2)} م³</span></td>
      <td>${t.hasHeadAndTail ? 'رأس + ذنب' : 'رأس فقط'}</td>
      <td>${t.hasExtensions ? `<span class="badge bg-warning text-dark">نعم (${t.extensionDetails || 'وصلة'})</span>` : 'لا'}</td>
      <td>${t.materialType || '—'}</td>
      <td class="small text-secondary">${new Date(t.updatedAt || t.createdAt).toLocaleDateString('ar-EG')}</td>
    </tr>
  `).join('');

  res.send(`
    <!DOCTYPE html>
    <html lang="ar" dir="rtl">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>سيرفر مزامنة تكعيب الغضب</title>
      <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.rtl.min.css">
      <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0b111e; color: #f0f4fc; }
        .card-stat { background: #151f32; border: 1px solid rgba(255,255,255,0.08); border-radius: 16px; }
        .badge-live { background-color: #10b981; color: white; padding: 6px 14px; border-radius: 20px; font-weight: bold; }
        .table-dark { background-color: #151f32; }
      </style>
    </head>
    <body class="p-3 p-md-4">
      <div class="container py-3" style="max-width: 900px;">
        <div class="d-flex justify-content-between align-items-center mb-4 pb-3 border-bottom border-secondary">
          <div>
            <h2 class="fw-bold mb-1">🚚 تكعيب الغضب - سيرفر المزامنة المركزي</h2>
            <p class="text-secondary mb-0">لوحة تحكم السيرفر المباشر (Ubuntu / Plesk) • بدون اعتمادات C++</p>
          </div>
          <span class="badge-live">🟢 السيرفر يعمل بنجاح</span>
        </div>

        <div class="row g-3 mb-4">
          <div class="col-md-3 col-6">
            <div class="card-stat p-3 text-center">
              <div class="text-secondary small">السيارات المسجلة</div>
              <div class="h2 fw-bold text-warning mb-0">${stats.totalTrucks}</div>
            </div>
          </div>
          <div class="col-md-3 col-6">
            <div class="card-stat p-3 text-center">
              <div class="text-secondary small">إجمالي التكعيب م³</div>
              <div class="h2 fw-bold text-info mb-0">${stats.totalCubicVolume}</div>
            </div>
          </div>
          <div class="col-md-3 col-6">
            <div class="card-stat p-3 text-center">
              <div class="text-secondary small">سجلات التعديل</div>
              <div class="h2 fw-bold text-success mb-0">${stats.totalModifications}</div>
            </div>
          </div>
          <div class="col-md-3 col-6">
            <div class="card-stat p-3 text-center">
              <div class="text-secondary small">الصور بالسيرفر</div>
              <div class="h2 fw-bold text-primary mb-0">${stats.totalPhotosOnServer}</div>
            </div>
          </div>
        </div>

        ${allTrucks.length > 0 ? `
          <div class="card-stat p-3 mb-4">
            <h5 class="fw-bold text-light mb-3">📋 أحدث السيارات المسجلة بالسيرفر:</h5>
            <div class="table-responsive">
              <table class="table table-dark table-hover mb-0 align-middle">
                <thead>
                  <tr class="text-secondary small">
                    <th>رقم السيارة</th>
                    <th>السائق</th>
                    <th>الحجم الإجمالي</th>
                    <th>النوع</th>
                    <th>وصلات</th>
                    <th>المادة</th>
                    <th>آخر تحديث</th>
                  </tr>
                </thead>
                <tbody>
                  ${trucksRowsHtml}
                </tbody>
              </table>
            </div>
          </div>
        ` : ''}

        <div class="card-stat p-4 mb-4">
          <h5 class="fw-bold text-warning mb-3">📡 كيفية ربط تطبيق الأندرويد بهذا السيرفر:</h5>
          <ol class="text-light pe-3">
            <li class="mb-2">افتح تطبيق <strong>«تكعيب الغضب»</strong> على أي هاتف في الشركة.</li>
            <li class="mb-2">انقر على أيقونة السيرفر أو من شاشة <strong>«تصدير ونسخ احتياطي»</strong> ⬅️ <strong>«مزامنة سيرفر الشركة VPS»</strong>.</li>
            <li class="mb-2">أدخل رابط هذا السيرفر: <code>http://[عنوان_السيرفر]:${PORT}</code> أو النطاق مثل <code>https://takeeb.io</code></li>
            <li class="mb-2">أدخل رمز أمان الشركة (Secret API Key) المحدد في ملف <code>.env</code>.</li>
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

app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 سيرفر المزامنة يعمل بنجاح على المنفذ: ${PORT}`);
  console.log(`🔐 رمز أمان الشركة المعين: ${COMPANY_SECRET_KEY}`);
  console.log(`📁 مسار تخزين الصور: ${PHOTOS_DIR}`);
  console.log(`💾 مسار قاعدة البيانات: ${DB_FILE}`);
});
