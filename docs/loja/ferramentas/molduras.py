"""Imagens de loja: fundo da marca, título em fonte pixel e a captura num iPhone."""
import sys, os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

RAW, OUT, PIXEL_FONT, BODY_FONT = sys.argv[1:5]
W, H = int(sys.argv[5]), int(sys.argv[6])

BLUE = (1, 93, 250)
NAVY = (11, 16, 32)
MAGENTA = (250, 0, 251)
WHITE = (255, 255, 255)

SLIDES = [
    ('01-home', BLUE, 'A GELADEIRA DA\nEMPRESA NO\nSEU BOLSO', 'Veja o que tem, quanto custa\ne quantos pixels você tem.'),
    ('02-loja', NAVY, 'ESCOLHA PELO\nCELULAR', 'Bebidas, lanches e snacks com\no estoque da geladeira na hora.'),
    ('06-pedido', BLUE, 'PAGOU,\nA PORTA ABRE', 'Retire na hora, sem fila\ne sem caixa.'),
    ('05-pagar', NAVY, 'PAGUE COM\nPIXELS', 'O benefício da sua empresa\nvira desconto na compra.'),
    ('08-progresso', BLUE, 'GANHE XP A\nCADA COMPRA', 'Missões e XP a cada compra,\ncom prêmio em pixels.'),
    ('09-perfil', NAVY, 'VIRE UM\nPERSONAGEM\n8-BIT', 'Sua foto vira avatar\nem pixel art.'),
    ('03-produto', BLUE, 'TUDO SOBRE\nCADA ITEM', 'Preço em reais e em pixels,\ne quanto ainda tem.'),
    ('07-pixels', NAVY, 'SEUS PIXELS\nEM DIA', 'Cada ganho e cada desconto,\nno extrato.'),
]

def background(color):
    bg = Image.new('RGB', (W, H), color)
    d = ImageDraw.Draw(bg)
    # Grade de pixels discreta, como a tela do app.
    step = max(24, W // 44)
    grid = tuple(min(255, c + 14) for c in color)
    for x in range(0, W, step):
        d.line([(x, 0), (x, H)], fill=grid, width=1)
    for y in range(0, H, step):
        d.line([(0, y), (W, y)], fill=grid, width=1)
    # Faixa quadriculada de pit stop no rodapé.
    cell = step
    rows = 2
    for r in range(rows):
        for i in range(0, W // cell + 1):
            if (i + r) % 2 == 0:
                y0 = H - (rows - r) * cell
                d.rectangle([i * cell, y0, (i + 1) * cell - 1, y0 + cell - 1], fill=WHITE if color == BLUE else BLUE)
    return bg

def rounded(im, radius):
    mask = Image.new('L', im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, im.size[0] - 1, im.size[1] - 1], radius, fill=255)
    out = Image.new('RGBA', im.size, (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out

def phone(shot, width):
    scale = width / shot.width
    screen = shot.resize((width, int(shot.height * scale)), Image.LANCZOS).convert('RGB')
    bezel = int(width * 0.035)
    radius = int(width * 0.14)
    pw, ph = screen.width + 2 * bezel, screen.height + 2 * bezel
    body = Image.new('RGBA', (pw, ph), (0, 0, 0, 0))
    bd = ImageDraw.Draw(body)
    bd.rounded_rectangle([0, 0, pw - 1, ph - 1], radius + bezel, fill=(18, 20, 28))
    bd.rounded_rectangle([2, 2, pw - 3, ph - 3], radius + bezel - 2, outline=(70, 76, 96), width=3)
    body.alpha_composite(rounded(screen, radius), (bezel, bezel))
    # Dynamic Island.
    iw, ih = int(screen.width * 0.30), int(screen.width * 0.085)
    ix = (pw - iw) // 2
    iy = bezel + int(screen.width * 0.028)
    bd.rounded_rectangle([ix, iy, ix + iw, iy + ih], ih // 2, fill=(0, 0, 0))
    return body

def draw_lines(d, text, font, y, fill, spacing):
    for line in text.split('\n'):
        box = d.textbbox((0, 0), line, font=font)
        d.text(((W - (box[2] - box[0])) // 2, y), line, font=font, fill=fill)
        y += (box[3] - box[1]) + spacing
    return y

os.makedirs(OUT, exist_ok=True)
title_size = int(W * 0.055)
body_size = int(W * 0.036)
title_font = ImageFont.truetype(PIXEL_FONT, title_size)
body_font = ImageFont.truetype(BODY_FONT, body_size)

for index, (name, color, title, subtitle) in enumerate(SLIDES, start=1):
    canvas = background(color).convert('RGBA')
    d = ImageDraw.Draw(canvas)
    y = int(H * 0.06)
    y = draw_lines(d, title, title_font, y, WHITE, int(title_size * 0.55))
    y += int(title_size * 0.5)
    sub_color = (205, 222, 255) if color == BLUE else (190, 200, 230)
    y = draw_lines(d, subtitle, body_font, y, sub_color, int(body_size * 0.45))
    shot = Image.open(os.path.join(RAW, name + '.png'))
    device = phone(shot, int(W * 0.74))
    top = y + int(H * 0.035)
    # Sombra suave embaixo do aparelho.
    shadow = Image.new('RGBA', canvas.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    x = (W - device.width) // 2
    sd.rounded_rectangle([x + 20, top + 40, x + device.width - 20, top + device.height + 40], 120, fill=(0, 0, 0, 110))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(40)))
    canvas.alpha_composite(device, (x, top))
    canvas.convert('RGB').save(os.path.join(OUT, f'{index:02d}-{name[3:]}.png'), optimize=True)
print('ok', W, H)
