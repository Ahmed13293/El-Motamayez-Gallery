-- ═══════════════════════════════════════════════════════════════════════════
--  مكتبة المتميز — Full Setup (Run in order)
--  STEP A first, then STEP B
-- ═══════════════════════════════════════════════════════════════════════════


-- ════════════════════════════════════════════════════════════════════════════
--  STEP A — CLEANUP (wipes ALL existing data including duplicates)
-- ════════════════════════════════════════════════════════════════════════════

-- Truncate with CASCADE removes all rows from all three tables at once,
-- ignoring foreign-key order — this is the safest way to clear duplicates.
TRUNCATE TABLE products, brands, categories RESTART IDENTITY CASCADE;

-- Drop and recreate constraints so IDs can be simple TEXT strings
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_pkey CASCADE;
ALTER TABLE brands     DROP CONSTRAINT IF EXISTS brands_pkey CASCADE;
ALTER TABLE brands     DROP CONSTRAINT IF EXISTS brands_category_id_fkey CASCADE;
ALTER TABLE brands     DROP COLUMN    IF EXISTS parent_id;

-- Convert id columns from UUID → TEXT
ALTER TABLE categories ALTER COLUMN id          TYPE TEXT;
ALTER TABLE brands     ALTER COLUMN id          TYPE TEXT;
ALTER TABLE brands     ALTER COLUMN category_id TYPE TEXT;

-- Re-add primary keys
ALTER TABLE categories ADD PRIMARY KEY (id);
ALTER TABLE brands     ADD PRIMARY KEY (id);

-- Re-add parent_id and FK
ALTER TABLE brands ADD COLUMN IF NOT EXISTS parent_id TEXT;
ALTER TABLE brands ADD CONSTRAINT brands_category_id_fkey
    FOREIGN KEY (category_id) REFERENCES categories(id);


-- ════════════════════════════════════════════════════════════════════════════
--  STEP B — INSERT DATA (tree structure, easy to read and maintain)
-- ════════════════════════════════════════════════════════════════════════════

INSERT INTO categories (id, name) VALUES
    ('cat_1', 'أدوات مكتبية'),
    ('cat_2', 'ملفات وحوافظ'),
    ('cat_3', 'شنط'),
    ('cat_4', 'ألعاب'),
    ('cat_5', 'مستلزمات أعياد ميلاد'),
    ('cat_6', 'أدوات تجميل'),
    ('cat_7', 'اكسسوارات ستانليس ستيل'),
    ('cat_8', 'مميات وأكواب زجاج');

-- ─────────────────────────────────────────────────────────────────────────────
--  cat_1 ── أدوات مكتبية
--      br_1_1 ── أدوات مكتب
--      br_1_2 ── أدوات هندسية
--      br_1_3 ── براية
--      br_1_4 ── أباتيك وحبايه
--      br_1_5 ── أقلام
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO brands (id, name, category_id, parent_id) VALUES
    ('br_1_1', 'أدوات مكتب',    'cat_1', NULL),
    ('br_1_2', 'أدوات هندسية',  'cat_1', NULL),
    ('br_1_3', 'براية',          'cat_1', NULL),
    ('br_1_4', 'أباتيك وحبايه', 'cat_1', NULL),
    ('br_1_5', 'أقلام',          'cat_1', NULL);

-- ─────────────────────────────────────────────────────────────────────────────
--  cat_2 ── ملفات وحوافظ
--      br_2_1 ── ملفات شفاف
--      br_2_2 ── ملفات ديوي الوان
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO brands (id, name, category_id, parent_id) VALUES
    ('br_2_1', 'ملفات شفاف',       'cat_2', NULL),
    ('br_2_2', 'ملفات ديوي الوان', 'cat_2', NULL);

-- ─────────────────────────────────────────────────────────────────────────────
--  cat_3 ── شنط
--      br_3_1 ── شنط لانجيري
--      br_3_2 ── هدايا وبوكسه
--      br_3_3 ── شنط قماش
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO brands (id, name, category_id, parent_id) VALUES
    ('br_3_1', 'شنط لانجيري', 'cat_3', NULL),
    ('br_3_2', 'هدايا وبوكسه','cat_3', NULL),
    ('br_3_3', 'شنط قماش',    'cat_3', NULL);

-- ─────────────────────────────────────────────────────────────────────────────
--  cat_4 ── ألعاب
--      br_4_1 ── ألعاب أطفال
--      br_4_2 ── ألعاب ورقية جامبو
--      br_4_3 ── بالون ومعاصر
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO brands (id, name, category_id, parent_id) VALUES
    ('br_4_1', 'ألعاب أطفال',       'cat_4', NULL),
    ('br_4_2', 'ألعاب ورقية جامبو', 'cat_4', NULL),
    ('br_4_3', 'بالون ومعاصر',      'cat_4', NULL);

-- ─────────────────────────────────────────────────────────────────────────────
--  cat_5 ── مستلزمات أعياد ميلاد
--      br_5_1 ── أدوات وحفلات
--      br_5_2 ── شمع
--      br_5_3 ── أرقام
--      br_5_4 ── كروت معايده
--      br_5_5 ── طوارق ميعاد
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO brands (id, name, category_id, parent_id) VALUES
    ('br_5_1', 'أدوات وحفلات', 'cat_5', NULL),
    ('br_5_2', 'شمع',           'cat_5', NULL),
    ('br_5_3', 'أرقام',         'cat_5', NULL),
    ('br_5_4', 'كروت معايده',   'cat_5', NULL),
    ('br_5_5', 'طوارق ميعاد',   'cat_5', NULL);

-- ─────────────────────────────────────────────────────────────────────────────
--  cat_6 ── أدوات تجميل
--      br_6_1 ── عطور
--      br_6_2 ── أظافر
--      br_6_3 ── هيكل
--      br_6_4 ── أدوات مكياج
--      br_6_5 ── تانو
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO brands (id, name, category_id, parent_id) VALUES
    ('br_6_1', 'عطور',        'cat_6', NULL),
    ('br_6_2', 'أظافر',       'cat_6', NULL),
    ('br_6_3', 'هيكل',        'cat_6', NULL),
    ('br_6_4', 'أدوات مكياج', 'cat_6', NULL),
    ('br_6_5', 'تانو',        'cat_6', NULL);

-- ─────────────────────────────────────────────────────────────────────────────
--  cat_7 ── اكسسوارات ستانليس ستيل
--      br_7_1 ── أثواب
--          br_7_1_1 ── بنات    (sub-sub)
--          br_7_1_2 ── أولاد   (sub-sub)
--      br_7_2 ── ساعات
--      br_7_3 ── خيال
--      br_7_4 ── أسوار
--      br_7_5 ── حلي
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO brands (id, name, category_id, parent_id) VALUES
    ('br_7_1', 'أثواب', 'cat_7', NULL),
    ('br_7_2', 'ساعات', 'cat_7', NULL),
    ('br_7_3', 'خيال',  'cat_7', NULL),
    ('br_7_4', 'أسوار', 'cat_7', NULL),
    ('br_7_5', 'حلي',   'cat_7', NULL);

-- Level 3 — children of أثواب (parent_id = 'br_7_1')
INSERT INTO brands (id, name, category_id, parent_id) VALUES
    ('br_7_1_1', 'بنات',  'cat_7', 'br_7_1'),
    ('br_7_1_2', 'أولاد', 'cat_7', 'br_7_1');

-- ─────────────────────────────────────────────────────────────────────────────
--  cat_8 ── مميات وأكواب زجاج
--      br_8_1 ── ورد
--      br_8_2 ── ساعات حائط
--      br_8_3 ── العاب
--      br_8_4 ── دباسة
--      br_8_5 ── خاتم شمع
--      br_8_6 ── سنتيمتر وأطباق
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO brands (id, name, category_id, parent_id) VALUES
    ('br_8_1', 'ورد',            'cat_8', NULL),
    ('br_8_2', 'ساعات حائط',     'cat_8', NULL),
    ('br_8_3', 'العاب',          'cat_8', NULL),
    ('br_8_4', 'دباسة',          'cat_8', NULL),
    ('br_8_5', 'خاتم شمع',       'cat_8', NULL),
    ('br_8_6', 'سنتيمتر وأطباق', 'cat_8', NULL);
