from PIL import Image, ImageDraw, ImageFont
import os

# Colors
BG_COLOR = (26, 35, 126)       # Deep navy #1A237E
GOLD_COLOR = (255, 215, 0)     # Gold
LIGHT_GOLD = (255, 236, 100)
PAGE_COLOR = (240, 235, 210)

def make_icon(size):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    pad = int(size * 0.06)
    r = int(size * 0.22)

    # Rounded rectangle background
    draw.rounded_rectangle([pad, pad, size - pad, size - pad], radius=r, fill=BG_COLOR)

    # Book dimensions
    bx = int(size * 0.18)
    by = int(size * 0.20)
    bw = int(size * 0.64)
    bh = int(size * 0.46)
    spine = int(size * 0.07)

    # Back cover (gold)
    draw.rectangle([bx + spine, by, bx + bw, by + bh], fill=GOLD_COLOR)
    # Pages
    draw.rectangle([bx + spine + int(size*0.03), by + int(size*0.03),
                    bx + bw - int(size*0.02), by + bh - int(size*0.02)], fill=PAGE_COLOR)
    # Page lines
    for i in range(3):
        ly = by + int(size * 0.11) + i * int(size * 0.09)
        draw.rectangle([bx + spine + int(size*0.08), ly,
                        bx + bw - int(size*0.06), ly + max(1, int(size*0.015))],
                       fill=(180, 170, 140))
    # Spine
    draw.rectangle([bx, by, bx + spine, by + bh], fill=(180, 140, 0))

    # Star decoration on cover
    star_cx = bx + spine + int((bw - spine) * 0.5)
    star_cy = by + int(bh * 0.35)
    star_r = int(size * 0.07)
    draw.ellipse([star_cx - star_r, star_cy - star_r,
                  star_cx + star_r, star_cy + star_r], fill=(255, 245, 150, 120))

    # Text: two lines of Arabic
    font_path = r'C:\Windows\Fonts\arabtype.ttf'
    line1 = 'مكتبة'
    line2 = 'المتميز'

    fs1 = max(8, int(size * 0.155))
    fs2 = max(8, int(size * 0.135))

    try:
        f1 = ImageFont.truetype(font_path, fs1)
        f2 = ImageFont.truetype(font_path, fs2)
    except Exception:
        f1 = f2 = ImageFont.load_default()

    text_y = by + bh + int(size * 0.04)

    bb1 = draw.textbbox((0, 0), line1, font=f1)
    tw1 = bb1[2] - bb1[0]
    draw.text(((size - tw1) / 2, text_y), line1, font=f1, fill=GOLD_COLOR)

    bb2 = draw.textbbox((0, 0), line2, font=f2)
    tw2 = bb2[2] - bb2[0]
    draw.text(((size - tw2) / 2, text_y + fs1 + int(size * 0.005)), line2, font=f2, fill=LIGHT_GOLD)

    return img


# Android mipmap sizes
sizes = {
    'mipmap-mdpi':    48,
    'mipmap-hdpi':    72,
    'mipmap-xhdpi':   96,
    'mipmap-xxhdpi':  144,
    'mipmap-xxxhdpi': 192,
}

base = r'C:\Users\Ahmed Samir\Projects\ElMotamyezGallery\composeApp\src\androidMain\res'

for folder, size in sizes.items():
    d = os.path.join(base, folder)
    os.makedirs(d, exist_ok=True)
    icon = make_icon(size)
    icon.save(os.path.join(d, 'ic_launcher.png'))
    print(f'  Created {folder}/ic_launcher.png ({size}x{size})')

# 1024x1024 preview
preview = make_icon(1024)
preview_path = r'C:\Users\Ahmed Samir\Projects\ElMotamyezGallery\icon_preview.png'
preview.save(preview_path)
print(f'  Saved icon_preview.png (1024x1024)')
print('Done!')
