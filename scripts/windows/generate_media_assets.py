"""Generate OnlyTechTake brand and Play Store media from the app design system.

Requires Pillow and the generated Full/Demo screenshots under reports/demo-assets.
Run with the bundled Codex Python or any Python installation that provides Pillow.
"""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


TEAL_900 = "#062629"
TEAL_800 = "#0D3B3E"
TEAL_700 = "#004F4F"
TEAL = "#006A6A"
CYAN = "#4DDADA"
CYAN_LIGHT = "#6FF6F6"
AMBER = "#FFBA28"
AMBER_DARK = "#7D5800"
SURFACE = "#F4FBFA"
SURFACE_VARIANT = "#DAE5E4"
CHARCOAL = "#191C1D"
MUTED = "#BFC9C8"
WHITE = "#FFFFFF"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    name = "segoeuib.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(str(Path("C:/Windows/Fonts") / name), size=size)


def gradient(size: tuple[int, int], start: str, end: str) -> Image.Image:
    width, height = size
    a = tuple(int(start[i : i + 2], 16) for i in (1, 3, 5))
    b = tuple(int(end[i : i + 2], 16) for i in (1, 3, 5))
    image = Image.new("RGB", size)
    pixels = image.load()
    for y in range(height):
        for x in range(width):
            t = (x / max(width - 1, 1) * 0.65) + (y / max(height - 1, 1) * 0.35)
            pixels[x, y] = tuple(round(a[c] + (b[c] - a[c]) * t) for c in range(3))
    return image


def draw_grid(image: Image.Image, spacing: int = 56, alpha: int = 18) -> None:
    overlay = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    for x in range(0, image.width, spacing):
        draw.line((x, 0, x, image.height), fill=(111, 246, 246, alpha), width=1)
    for y in range(0, image.height, spacing):
        draw.line((0, y, image.width, y), fill=(111, 246, 246, alpha), width=1)
    image.paste(overlay, mask=overlay)


def draw_mark(
    image: Image.Image,
    box: tuple[int, int, int, int],
    *,
    paper: str = SURFACE,
    primary: str = "#1BB8AE",
    text: str = "#102326",
    accent: str = "#FF9F0A",
) -> None:
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    draw = ImageDraw.Draw(image)
    radius = round(w * 0.08)
    document = (
        x0 + round(w * 0.24),
        y0 + round(h * 0.12),
        x0 + round(w * 0.76),
        y0 + round(h * 0.84),
    )
    draw.rounded_rectangle(document, radius=radius, fill=paper, outline="#B8C7C7", width=max(2, round(w * 0.018)))
    draw.rounded_rectangle(
        (x0 + round(w * 0.32), y0 + round(h * 0.30), x0 + round(w * 0.68), y0 + round(h * 0.365)),
        radius=max(2, round(w * 0.012)),
        fill=primary,
    )
    draw.rounded_rectangle(
        (x0 + round(w * 0.33), y0 + round(h * 0.49), x0 + round(w * 0.57), y0 + round(h * 0.545)),
        radius=max(2, round(w * 0.01)),
        fill=text,
    )
    draw.rounded_rectangle(
        (x0 + round(w * 0.33), y0 + round(h * 0.61), x0 + round(w * 0.50), y0 + round(h * 0.665)),
        radius=max(2, round(w * 0.01)),
        fill=text,
    )
    points = [
        (x0 + round(w * 0.61), y0 + round(h * 0.76)),
        (x0 + round(w * 0.72), y0 + round(h * 0.86)),
        (x0 + round(w * 0.96), y0 + round(h * 0.55)),
    ]
    draw.line(points, fill=accent, width=max(5, round(w * 0.09)), joint="curve")


def rounded_screenshot(path: Path, size: tuple[int, int], radius: int = 30) -> Image.Image:
    source = Image.open(path).convert("RGB")
    # Generated story images include a neutral frame. Crop to the real device UI.
    crop = source.crop((128, 40, 953, 1880)) if source.size == (1080, 1920) else source
    crop.thumbnail(size, Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", size, (0, 0, 0, 0))
    x = (size[0] - crop.width) // 2
    y = (size[1] - crop.height) // 2
    mask = Image.new("L", crop.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, crop.width, crop.height), radius=radius, fill=255)
    canvas.paste(crop, (x, y), mask)
    return canvas


def paste_with_shadow(base: Image.Image, item: Image.Image, xy: tuple[int, int], blur: int = 22) -> None:
    shadow = Image.new("RGBA", base.size, (0, 0, 0, 0))
    alpha = item.getchannel("A") if item.mode == "RGBA" else Image.new("L", item.size, 255)
    shadow_shape = Image.new("RGBA", item.size, (0, 0, 0, 150))
    shadow_shape.putalpha(alpha)
    shadow.paste(shadow_shape, (xy[0] + 10, xy[1] + 14), shadow_shape)
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    base.paste(shadow, mask=shadow)
    base.paste(item, xy, item if item.mode == "RGBA" else None)


def draw_pill(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], label: str, fill: str, color: str = WHITE) -> None:
    draw.rounded_rectangle(xy, radius=(xy[3] - xy[1]) // 2, fill=fill)
    label_font = font(round((xy[3] - xy[1]) * 0.44), bold=True)
    bounds = draw.textbbox((0, 0), label, font=label_font)
    draw.text(
        ((xy[0] + xy[2] - (bounds[2] - bounds[0])) / 2, (xy[1] + xy[3] - (bounds[3] - bounds[1])) / 2 - bounds[1]),
        label,
        font=label_font,
        fill=color,
    )


def brand_svg_files(identity: Path, icons: Path, social: Path) -> None:
    mark = """<svg width="256" height="256" viewBox="0 0 256 256" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="18" y="18" width="220" height="220" rx="54" fill="#062629"/>
  <rect x="67" y="49" width="122" height="166" rx="19" fill="#F4FBFA" stroke="#B8C7C7" stroke-width="5"/>
  <rect x="87" y="90" width="82" height="15" rx="4" fill="#1BB8AE"/>
  <rect x="89" y="131" width="55" height="13" rx="3" fill="#102326"/>
  <rect x="89" y="159" width="39" height="13" rx="3" fill="#102326"/>
  <path d="M151 180L176 202L230 132" stroke="#FF9F0A" stroke-width="21" stroke-linecap="square" stroke-linejoin="round"/>
</svg>
"""
    logo = """<svg width="720" height="180" viewBox="0 0 720 180" fill="none" xmlns="http://www.w3.org/2000/svg">
  <g transform="translate(8 8) scale(.64)">__MARK__</g>
  <text x="190" y="87" font-family="Segoe UI, Arial, sans-serif" font-size="54" font-weight="700" fill="#191C1D">OnlyTechTake</text>
  <text x="192" y="126" font-family="Segoe UI, Arial, sans-serif" font-size="22" font-weight="600" fill="#006A6A">REPAIR APP</text>
  <text x="192" y="155" font-family="Segoe UI, Arial, sans-serif" font-size="20" fill="#4A6363">Un'app per ogni problema.</text>
</svg>
""".replace("__MARK__", mark.split(">", 1)[1].rsplit("</svg>", 1)[0])
    favicon = mark.replace('width="256" height="256"', 'width="128" height="128"')
    cover = """<svg width="1280" height="640" viewBox="0 0 1280 640" fill="none" xmlns="http://www.w3.org/2000/svg">
  <defs><linearGradient id="bg" x1="0" y1="0" x2="1280" y2="640" gradientUnits="userSpaceOnUse"><stop stop-color="#062629"/><stop offset="1" stop-color="#0D4A4D"/></linearGradient><pattern id="grid" width="48" height="48" patternUnits="userSpaceOnUse"><path d="M48 0H0V48" stroke="#6FF6F6" stroke-opacity=".08"/></pattern></defs>
  <rect width="1280" height="640" rx="36" fill="url(#bg)"/><rect width="1280" height="640" rx="36" fill="url(#grid)"/>
  <g transform="translate(70 66) scale(.34)">__MARK__</g>
  <text x="176" y="126" font-family="Segoe UI, Arial, sans-serif" font-size="30" font-weight="700" fill="#F4FBFA">OnlyTechTake</text>
  <text x="70" y="252" font-family="Segoe UI, Arial, sans-serif" font-size="68" font-weight="700" fill="#FFFFFF">Riparazioni,</text>
  <text x="70" y="326" font-family="Segoe UI, Arial, sans-serif" font-size="68" font-weight="700" fill="#6FF6F6">senza caos.</text>
  <text x="70" y="386" font-family="Segoe UI, Arial, sans-serif" font-size="28" fill="#DAE5E4">Commesse, clienti e documenti.</text>
  <text x="70" y="426" font-family="Segoe UI, Arial, sans-serif" font-size="28" fill="#DAE5E4">Tutto locale. Tutto sotto controllo.</text>
  <rect x="70" y="486" width="236" height="52" rx="26" fill="#006A6A"/><text x="99" y="521" font-family="Segoe UI, Arial, sans-serif" font-size="20" font-weight="700" fill="white">PRIVACY LOCALE</text>
  <rect x="322" y="486" width="256" height="52" rx="26" fill="#7D5800"/><text x="350" y="521" font-family="Segoe UI, Arial, sans-serif" font-size="20" font-weight="700" fill="white">FLUSSO COMPLETO</text>
  <g transform="translate(790 104)"><rect width="388" height="460" rx="34" fill="#191C1D" stroke="#4DDADA" stroke-width="3"/><text x="36" y="62" font-family="Segoe UI, Arial, sans-serif" font-size="28" font-weight="700" fill="#F4FBFA">Banco riparazioni</text><g font-family="Segoe UI, Arial, sans-serif" text-anchor="middle"><rect x="35" y="94" width="92" height="92" rx="20" fill="#1E3536"/><text x="81" y="143" font-size="30" font-weight="700" fill="#4DDADA">2</text><text x="81" y="170" font-size="14" fill="#DAE5E4">Utenti</text><rect x="148" y="94" width="92" height="92" rx="20" fill="#1E3536"/><text x="194" y="143" font-size="30" font-weight="700" fill="#4DDADA">12</text><text x="194" y="170" font-size="14" fill="#DAE5E4">Clienti</text><rect x="261" y="94" width="92" height="92" rx="20" fill="#1E3536"/><text x="307" y="143" font-size="30" font-weight="700" fill="#4DDADA">8</text><text x="307" y="170" font-size="14" fill="#DAE5E4">Attive</text></g><rect x="35" y="215" width="318" height="66" rx="22" fill="#4DDADA"/><text x="112" y="257" font-family="Segoe UI, Arial, sans-serif" font-size="22" font-weight="700" fill="#062629">+  Nuova commessa</text><rect x="35" y="312" width="318" height="105" rx="22" fill="#202A2B"/><text x="60" y="350" font-family="Segoe UI, Arial, sans-serif" font-size="20" font-weight="700" fill="#F4FBFA">In lavorazione</text><text x="60" y="384" font-family="Segoe UI, Arial, sans-serif" font-size="17" fill="#BFC9C8">4 commesse in coda</text><circle cx="326" cy="365" r="11" fill="#FFBA28"/></g>
</svg>
""".replace("__MARK__", mark.split(">", 1)[1].rsplit("</svg>", 1)[0])
    (identity / "onlytechtake-mark.svg").write_text(mark, encoding="utf-8")
    (identity / "onlytechtake-logo.svg").write_text(logo, encoding="utf-8")
    (icons / "favicon.svg").write_text(favicon, encoding="utf-8")
    (social / "github-cover.svg").write_text(cover, encoding="utf-8")


def make_icon(path: Path, variant: str) -> None:
    colors = {
        "pro": (TEAL_900, CYAN, AMBER),
        "demo": ("#173238", AMBER, CYAN),
        "showcase": ("#102A2D", CYAN_LIGHT, AMBER),
    }
    bg, halo, accent = colors[variant]
    image = gradient((512, 512), bg, TEAL_700)
    draw = ImageDraw.Draw(image, "RGBA")
    draw.ellipse((62, 58, 450, 446), fill=halo + "20", outline=halo + "66", width=3)
    draw.ellipse((112, 108, 400, 396), fill="#FFFFFF0A")
    draw_mark(image, (92, 76, 420, 420), primary=halo, accent=accent)
    image.save(path, optimize=True)


def make_feature(path: Path, variant: str, screenshot: Path) -> None:
    image = gradient((1024, 500), TEAL_900 if variant != "demo" else "#132F34", TEAL_700)
    draw_grid(image, spacing=52, alpha=14)
    draw = ImageDraw.Draw(image)
    draw_mark(image, (56, 40, 152, 140), primary=CYAN if variant != "demo" else AMBER)
    title = {"pro": "OnlyTechTake Pro", "demo": "OnlyTechTake Demo", "showcase": "OnlyTechTake"}[variant]
    subtitle = {
        "pro": "Il banco riparazioni, sotto controllo.",
        "demo": "Prova il flusso completo in locale.",
        "showcase": "Dal banco alla consegna, senza caos.",
    }[variant]
    draw.text((172, 52), title, font=font(40, bold=True), fill=WHITE)
    draw.text((58, 166), subtitle, font=font(29, bold=True), fill=CYAN_LIGHT)
    if variant == "demo":
        draw_pill(draw, (58, 232, 225, 282), "1 COMMESSA", AMBER_DARK)
        draw_pill(draw, (238, 232, 388, 282), "ADMIN", TEAL)
        draw.text((60, 314), "Dati fittizi. Nessun cloud.", font=font(21), fill=SURFACE_VARIANT)
    else:
        bullets = ["Commesse e clienti", "Documenti e firme", "Dati solo sul dispositivo"]
        for index, label in enumerate(bullets):
            y = 232 + index * 58
            draw.ellipse((60, y + 6, 78, y + 24), fill=AMBER if index == 2 else CYAN)
            draw.text((94, y), label, font=font(22, bold=index == 2), fill=SURFACE)
    screen = rounded_screenshot(screenshot, (360, 610), radius=28)
    paste_with_shadow(image, screen, (632, -48), blur=18)
    image.save(path, optimize=True)


def make_cover(path: Path, screenshot: Path, size: tuple[int, int]) -> None:
    width, height = size
    image = gradient(size, TEAL_900, "#164B4E")
    draw_grid(image, spacing=max(36, width // 24), alpha=16)
    draw = ImageDraw.Draw(image)
    scale = width / 1280
    draw_mark(image, (round(64 * scale), round(58 * scale), round(156 * scale), round(154 * scale)), primary=CYAN)
    draw.text((round(174 * scale), round(70 * scale)), "OnlyTechTake", font=font(round(34 * scale), bold=True), fill=WHITE)
    draw.text((round(64 * scale), round(214 * scale)), "Riparazioni,", font=font(round(68 * scale), bold=True), fill=WHITE)
    draw.text((round(64 * scale), round(290 * scale)), "senza caos.", font=font(round(68 * scale), bold=True), fill=CYAN_LIGHT)
    draw.text((round(66 * scale), round(384 * scale)), "Commesse, clienti e documenti.", font=font(round(27 * scale)), fill=SURFACE_VARIANT)
    draw.text((round(66 * scale), round(425 * scale)), "Tutto locale. Tutto sotto controllo.", font=font(round(27 * scale)), fill=SURFACE_VARIANT)
    draw_pill(draw, (round(66 * scale), round(492 * scale), round(294 * scale), round(544 * scale)), "PRIVACY LOCALE", TEAL)
    draw_pill(draw, (round(310 * scale), round(492 * scale), round(558 * scale), round(544 * scale)), "FLUSSO COMPLETO", AMBER_DARK)
    screen_size = (round(404 * scale), round(716 * scale))
    screen = rounded_screenshot(screenshot, screen_size, radius=round(34 * scale))
    paste_with_shadow(image, screen, (round(790 * scale), round(-40 * scale)), blur=max(12, round(22 * scale)))
    image.save(path, optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()
    root = args.root.resolve()
    brand = root / "media" / "brand"
    identity = brand / "identity"
    icons = brand / "icons"
    social = brand / "social"
    play = root / "media" / "play-store"
    landing = root / "landing" / "assets"
    landing_brand = landing / "brand"
    landing_icons = landing / "icons"
    landing_social = landing / "social"
    for directory in (identity, icons, social, landing_brand, landing_icons, landing_social):
        directory.mkdir(parents=True, exist_ok=True)
    for variant in ("pro", "demo", "showcase"):
        (play / variant).mkdir(parents=True, exist_ok=True)

    full_home = root / "reports/demo-assets/Full/screenshots/01-operatore-banco/02-home.png"
    full_intake = root / "reports/demo-assets/Full/screenshots/01-operatore-banco/06-order-created.png"
    demo_home = root / "reports/demo-assets/Demo/screenshots/01-operatore-banco/02-home.png"
    for required in (full_home, full_intake, demo_home):
        if not required.is_file():
            raise FileNotFoundError(f"Missing generated screenshot: {required}")

    brand_svg_files(identity, icons, social)
    make_icon(icons / "app-icon-preview.png", "showcase")
    make_icon(icons / "favicon-512.png", "showcase")
    favicon = Image.open(icons / "favicon-512.png").convert("RGBA")
    favicon.resize((192, 192), Image.Resampling.LANCZOS).save(icons / "favicon-192.png", optimize=True)
    favicon.save(icons / "favicon.ico", sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])

    make_cover(social / "github-cover.png", full_home, (1280, 640))
    make_cover(social / "github-social-preview.png", full_home, (1280, 640))

    make_icon(play / "pro/icon-512.png", "pro")
    make_icon(play / "demo/icon-512.png", "demo")
    make_icon(play / "showcase/icon-512.png", "showcase")
    make_feature(play / "pro/feature-graphic-1024x500.png", "pro", full_home)
    make_feature(play / "demo/feature-graphic-1024x500.png", "demo", demo_home)
    make_feature(play / "showcase/feature-graphic-1024x500.png", "showcase", full_intake)

    landing_copies = {
        identity / "onlytechtake-logo.svg": landing_brand / "onlytechtake-logo.svg",
        identity / "onlytechtake-mark.svg": landing_brand / "onlytechtake-mark.svg",
        icons / "app-icon-preview.png": landing_icons / "app-icon-preview.png",
        icons / "favicon.svg": landing_icons / "favicon.svg",
        icons / "favicon.ico": landing_icons / "favicon.ico",
        icons / "favicon-192.png": landing_icons / "favicon-192.png",
        icons / "favicon-512.png": landing_icons / "favicon-512.png",
        social / "github-cover.png": landing_social / "github-cover.png",
        social / "github-social-preview.png": landing_social / "github-social-preview.png",
    }
    for source, target in landing_copies.items():
        shutil.copy2(source, target)


if __name__ == "__main__":
    main()
