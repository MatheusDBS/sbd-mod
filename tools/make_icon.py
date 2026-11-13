# Draws the mod icon as 32x32 pixel art and scales it up without smoothing (Minecraft look).
# Usage: python make_icon.py <out.png>
import sys
from PIL import Image, ImageDraw

S = 32
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
px = lambda x, y, c: d.point((x, y), fill=c)
rect = lambda x0, y0, x1, y1, c: d.rectangle((x0, y0, x1, y1), fill=c)

# background: rounded dark-blue tile with a lighter rim
BG, BG2, RIM = (27, 36, 64), (35, 48, 84), (58, 78, 130)
rect(1, 0, 30, 31, RIM); rect(0, 1, 31, 30, RIM)
rect(2, 1, 29, 30, BG); rect(1, 2, 30, 29, BG)
for y in range(2, 16):  # subtle top highlight
    rect(2, y, 29, y, BG2)

# clipboard board
WOOD, WOOD_D, WOOD_L = (139, 90, 43), (98, 60, 26), (170, 115, 60)
rect(6, 5, 24, 28, WOOD_D)
rect(7, 6, 23, 27, WOOD)
rect(7, 6, 23, 6, WOOD_L)

# paper
PAPER, PAPER_S = (242, 236, 222), (205, 196, 176)
rect(9, 8, 21, 26, PAPER)
rect(9, 26, 21, 26, PAPER_S); rect(21, 8, 21, 26, PAPER_S)

# metal clip
CLIP, CLIP_D = (170, 176, 184), (110, 116, 124)
rect(12, 3, 18, 7, CLIP_D); rect(13, 3, 17, 6, CLIP); rect(14, 4, 16, 4, CLIP_D)

# three rows: checkbox + text line
BOX, GREEN, GREEN_D, LINE = (90, 90, 90), (76, 196, 70), (40, 130, 40), (150, 150, 150)
for i, checked in enumerate((True, True, False)):
    y = 10 + i * 5
    rect(10, y, 13, y + 3, BOX); rect(11, y + 1, 12, y + 2, PAPER)
    rect(15, y + 1, 20, y + 1, LINE); rect(15, y + 3, 18, y + 3, LINE)
    if checked:  # green tick spilling out of the box
        for x, yy in ((10, y + 1), (11, y + 2), (12, y + 1), (13, y), (14, y - 1)):
            px(x, yy, GREEN)
        for x, yy in ((10, y + 2), (11, y + 3), (12, y + 2), (13, y + 1), (14, y)):
            px(x, yy, GREEN_D)

# gold coin bottom-right
GOLD, GOLD_D, GOLD_L, GOLD_DD = (240, 190, 40), (196, 140, 20), (255, 232, 120), (130, 86, 10)
cx, cy = 24, 23
for y in range(cy - 4, cy + 5):
    for x in range(cx - 4, cx + 5):
        r2 = (x - cx) ** 2 + (y - cy) ** 2
        if r2 <= 20:
            px(x, y, GOLD_DD if r2 > 14 else GOLD)
rect(cx - 1, cy - 2, cx + 1, cy + 2, GOLD_D)  # coin mark
px(cx, cy - 1, GOLD_L); px(cx, cy, GOLD_L); px(cx, cy + 1, GOLD_L)
px(cx - 2, cy - 2, GOLD_L); px(cx - 1, cy - 3, GOLD_L)  # shine

img.resize((256, 256), Image.NEAREST).save(sys.argv[1])
