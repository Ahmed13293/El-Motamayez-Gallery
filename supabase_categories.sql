-- ═══════════════════════════════════════════════════════════════════════════
--  مكتبة المتميز — Categories, Sub-categories, Sub-sub-categories
--  Run this in Supabase SQL Editor
-- ═══════════════════════════════════════════════════════════════════════════

-- ── Step 1: Add parent_id column (UUID, no FK constraint) ────────────────────
ALTER TABLE brands ADD COLUMN IF NOT EXISTS parent_id UUID;

-- ── Step 2: CATEGORIES ───────────────────────────────────────────────────────
INSERT INTO categories (name) VALUES
    ('أدوات مكتبية'),
    ('ملفات وحوافظ'),
    ('شنط'),
    ('ألعاب'),
    ('مستلزمات أعياد ميلاد'),
    ('أدوات تجميل'),
    ('اكسسوارات ستانليس ستيل'),
    ('مميات وأكواب زجاج')
ON CONFLICT DO NOTHING;

-- ── Step 3: SUB-CATEGORIES (Level 2) — reference category by name ────────────

-- أدوات مكتبية
INSERT INTO brands (name, category_id, parent_id)
SELECT b.name, c.id, NULL FROM (VALUES
    ('أدوات مكتب'),
    ('أدوات هندسية'),
    ('براية'),
    ('أباتيك وحبايه'),
    ('أقلام')
) AS b(name)
CROSS JOIN (SELECT id FROM categories WHERE name = 'أدوات مكتبية') c
ON CONFLICT DO NOTHING;

-- ملفات وحوافظ
INSERT INTO brands (name, category_id, parent_id)
SELECT b.name, c.id, NULL FROM (VALUES
    ('ملفات شفاف'),
    ('ملفات ديوي الوان')
) AS b(name)
CROSS JOIN (SELECT id FROM categories WHERE name = 'ملفات وحوافظ') c
ON CONFLICT DO NOTHING;

-- شنط
INSERT INTO brands (name, category_id, parent_id)
SELECT b.name, c.id, NULL FROM (VALUES
    ('شنط لانجيري'),
    ('هدايا وبوكسه'),
    ('شنط قماش')
) AS b(name)
CROSS JOIN (SELECT id FROM categories WHERE name = 'شنط') c
ON CONFLICT DO NOTHING;

-- ألعاب
INSERT INTO brands (name, category_id, parent_id)
SELECT b.name, c.id, NULL FROM (VALUES
    ('ألعاب أطفال'),
    ('ألعاب ورقية جامبو'),
    ('بالون ومعاصر')
) AS b(name)
CROSS JOIN (SELECT id FROM categories WHERE name = 'ألعاب') c
ON CONFLICT DO NOTHING;

-- مستلزمات أعياد ميلاد
INSERT INTO brands (name, category_id, parent_id)
SELECT b.name, c.id, NULL FROM (VALUES
    ('أدوات وحفلات'),
    ('شمع'),
    ('أرقام'),
    ('كروت معايده'),
    ('طوارق ميعاد')
) AS b(name)
CROSS JOIN (SELECT id FROM categories WHERE name = 'مستلزمات أعياد ميلاد') c
ON CONFLICT DO NOTHING;

-- أدوات تجميل
INSERT INTO brands (name, category_id, parent_id)
SELECT b.name, c.id, NULL FROM (VALUES
    ('عطور'),
    ('أظافر'),
    ('هيكل'),
    ('أدوات مكياج'),
    ('تانو')
) AS b(name)
CROSS JOIN (SELECT id FROM categories WHERE name = 'أدوات تجميل') c
ON CONFLICT DO NOTHING;

-- اكسسوارات ستانليس ستيل (Level 2)
INSERT INTO brands (name, category_id, parent_id)
SELECT b.name, c.id, NULL FROM (VALUES
    ('أثواب'),
    ('ساعات'),
    ('خيال'),
    ('أسوار'),
    ('حلي')
) AS b(name)
CROSS JOIN (SELECT id FROM categories WHERE name = 'اكسسوارات ستانليس ستيل') c
ON CONFLICT DO NOTHING;

-- مميات وأكواب زجاج
INSERT INTO brands (name, category_id, parent_id)
SELECT b.name, c.id, NULL FROM (VALUES
    ('ورد'),
    ('ساعات حائط'),
    ('العاب'),
    ('دباسة'),
    ('خاتم شمع'),
    ('سنتيمتر وأطباق')
) AS b(name)
CROSS JOIN (SELECT id FROM categories WHERE name = 'مميات وأكواب زجاج') c
ON CONFLICT DO NOTHING;

-- ── Step 4: SUB-SUB-CATEGORIES (Level 3) — أثواب → بنات / أولاد ─────────────
INSERT INTO brands (name, category_id, parent_id)
SELECT b.name, cat.id, parent.id
FROM (VALUES ('بنات'), ('أولاد')) AS b(name)
CROSS JOIN (SELECT id FROM categories WHERE name = 'اكسسوارات ستانليس ستيل') cat
CROSS JOIN (SELECT id FROM brands WHERE name = 'أثواب') parent
ON CONFLICT DO NOTHING;
