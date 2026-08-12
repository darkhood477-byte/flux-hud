import re

path = "/app/applet/app/src/main/java/com/example/overlay/RadialMenuOverlay.kt"
with open(path, "r") as f:
    content = f.read()

# Replace outer rendering
pattern = re.compile(
    r"(val subBgColor = if \(isOuterHovered\) primary else surfaceVariant\.copy\(alpha = ringOpacity\).*?)" +
    r"(\s*drawArc\(.*?\)\s*)" +
    r"(val oMidAngleRad = Math\.toRadians.*?)" +
    r"(val subTextResult = textMeasurer\.measure\()",
    re.DOTALL
)

def repl(match):
    prefix = match.group(1).replace("surfaceVariant", "glassColor")
    draw_arc = match.group(2)
    draw_arc = draw_arc.replace("startAngle = oStart + 1f,", "startAngle = oStart + 2f,")
    draw_arc = draw_arc.replace("sweepAngle = subSliceSweep - 2f,", "sweepAngle = subSliceSweep - 4f,")
    math_vars = match.group(3)
    suffix = match.group(4)
    
    # We want: math_vars + scale { prefix + draw_arc + suffix
    return f"""{prefix}
{math_vars}
                                val outerScale = outerScales.getOrElse(j) {{ 1f }}
                                scale(scale = outerScale, pivot = Offset(subTextX, subTextY)) {{{draw_arc}
{suffix}"""

content = pattern.sub(repl, content)

# Now fix the end brace
pattern2 = re.compile(
    r"(drawText\(\s*textLayoutResult = subTextResult,.*?topLeft = Offset\(.*?\)[\s\n]*\))([\s\n]*\}[ \t]*\n[ \t]*\}[ \t]*\n[ \t]*\})",
    re.DOTALL
)

def repl2(match):
    return match.group(1) + "\n                                } // End of outer scale" + match.group(2)

content = pattern2.sub(repl2, content)

with open(path, "w") as f:
    f.write(content)
