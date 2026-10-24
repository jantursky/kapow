"""Draws the Kapow mark: a comic burst with a block K cut out. Writes the SVG files in design/brand."""
import math
import pathlib

OUT = pathlib.Path(__file__).parent


def rounded(points, radius, transform=lambda p: p):
    points = [transform(p) for p in points]
    path = ""
    for i, corner in enumerate(points):
        before, after = points[i - 1], points[(i + 1) % len(points)]

        def toward(target):
            dx, dy = target[0] - corner[0], target[1] - corner[1]
            length = math.hypot(dx, dy)
            r = min(radius, length / 2.2)
            return corner[0] + dx / length * r, corner[1] + dy / length * r

        a, b = toward(before), toward(after)
        path += ("M" if i == 0 else "L") + f"{a[0]:.2f},{a[1]:.2f} Q{corner[0]:.2f},{corner[1]:.2f} {b[0]:.2f},{b[1]:.2f} "
    return path + "Z"


def burst(outer, inner, jitter, rotation):
    count = len(outer)
    points = []
    for i in range(2 * count):
        k = i // 2
        r = outer[k] if i % 2 == 0 else inner[k % len(inner)]
        angle = math.radians(rotation + i * 180 / count + (jitter[k] if i % 2 == 0 else 0))
        points.append((32 + r * math.cos(angle), 32 + r * math.sin(angle)))
    return points


def k_transform(p):
    # The K is drawn around (33.5, 32), then tilted by 8 degrees and enlarged to sit in the burst.
    x, y = (p[0] - 33.5) * 1.12, (p[1] - 32) * 1.12
    a = math.radians(-8)
    return 32 + x * math.cos(a) - y * math.sin(a), 32 + x * math.sin(a) + y * math.cos(a)


BURST = rounded(
    burst(
        outer=[31, 25, 30, 24, 31, 26, 29, 24, 31, 25, 30, 24],
        inner=[18, 17, 19, 17, 18, 19],
        jitter=[0, 5, -4, 4, -3, 5, -5, 3, -4, 4, -3, 4],
        rotation=-96,
    ),
    1.2,
)
K = (
    rounded([(22, 20), (29, 20), (29, 44), (22, 44)], 2.5, k_transform)
    + rounded([(31, 31), (39, 20), (45, 20), (45, 25), (36, 33)], 2, k_transform)
    + rounded([(31, 34), (37, 34), (45, 44), (36, 44)], 2, k_transform)
)
LIGHT = ("#7f52ff", "#e44857")
DARK = ("#9b7bff", "#ff6b7a")
GRADIENT = '<linearGradient id="kapow" x1="4" y1="4" x2="60" y2="60" gradientUnits="userSpaceOnUse">{stops}</linearGradient>'
PATH = f'<path fill="url(#kapow)" fill-rule="evenodd" d="{BURST} {K}"/>'


def svg(stops, style=""):
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">'
        f"{style}<defs>{GRADIENT.format(stops=stops)}</defs>{PATH}</svg>\n"
    )


def fixed(colors):
    return svg(f'<stop offset="0" stop-color="{colors[0]}"/><stop offset="1" stop-color="{colors[1]}"/>')


adaptive = svg(
    '<stop offset="0" class="a"/><stop offset="1" class="b"/>',
    f"<style>.a{{stop-color:{LIGHT[0]}}}.b{{stop-color:{LIGHT[1]}}}"
    f"@media (prefers-color-scheme:dark){{.a{{stop-color:{DARK[0]}}}.b{{stop-color:{DARK[1]}}}}}</style>",
)

(OUT / "mark.svg").write_text(adaptive)
(OUT / "icon.svg").write_text(fixed(LIGHT))
(OUT / "icon-dark.svg").write_text(fixed(DARK))
