"""Toques e digitação no Simulator, em pixels da tela do aparelho (1320x2868)."""
import subprocess, sys, time
import Quartz

W, H = 1320, 2868

def window():
    out = subprocess.check_output(['osascript', '-e',
        'tell application "System Events" to tell process "Simulator" to get {position, size} of window 1']).decode()
    x, y, w, h = [int(v) for v in out.replace(' ', '').split(',')]
    title = h - w * H / W
    return x, y + title, w, h - title

def point(px, py):
    x, y, w, h = window()
    return x + px * w / W, y + py * h / H

def click(px, py):
    subprocess.run(['osascript', '-e', 'tell application "Simulator" to activate'])
    time.sleep(0.3)
    x, y = point(px, py)
    for kind in (Quartz.kCGEventLeftMouseDown, Quartz.kCGEventLeftMouseUp):
        ev = Quartz.CGEventCreateMouseEvent(None, kind, (x, y), Quartz.kCGMouseButtonLeft)
        Quartz.CGEventPost(Quartz.kCGHIDEventTap, ev)
        time.sleep(0.08)

def drag(px1, py1, px2, py2, steps=20):
    subprocess.run(['osascript', '-e', 'tell application "Simulator" to activate'])
    time.sleep(0.3)
    a = point(px1, py1); b = point(px2, py2)
    Quartz.CGEventPost(Quartz.kCGHIDEventTap, Quartz.CGEventCreateMouseEvent(None, Quartz.kCGEventLeftMouseDown, a, 0))
    for i in range(1, steps + 1):
        p = (a[0] + (b[0]-a[0])*i/steps, a[1] + (b[1]-a[1])*i/steps)
        Quartz.CGEventPost(Quartz.kCGHIDEventTap, Quartz.CGEventCreateMouseEvent(None, Quartz.kCGEventLeftMouseDragged, p, 0))
        time.sleep(0.02)
    Quartz.CGEventPost(Quartz.kCGHIDEventTap, Quartz.CGEventCreateMouseEvent(None, Quartz.kCGEventLeftMouseUp, b, 0))

def type_text(text):
    subprocess.run(['osascript', '-e', 'tell application "Simulator" to activate'])
    time.sleep(0.3)
    esc = text.replace('\\', '\\\\').replace('"', '\\"')
    subprocess.run(['osascript', '-e', f'tell application "System Events" to keystroke "{esc}"'])

if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'click': click(float(sys.argv[2]), float(sys.argv[3]))
    elif cmd == 'drag': drag(*[float(v) for v in sys.argv[2:6]])
    elif cmd == 'type': type_text(sys.argv[2])
