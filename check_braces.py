import re

with open(r'd:\MYAPPS\colorby\app\src\main\java\com\example\colorby\ui\CameraScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

count = 0
last_zero_line = None
for i, line in enumerate(lines, 1):
    s = line
    s = re.sub(r'//.*', '', s)
    s = re.sub(r'"([^"\\]|\\.)*"', '', s)
    s = re.sub(r"'([^'\\]|\\.)*'", '', s)
    opens = s.count('{')
    closes = s.count('}')
    count += opens - closes
    if count == 0:
        last_zero_line = i
print('final count:', count)
print('last zero line:', last_zero_line)
print('context after last zero:')
for i in range(last_zero_line, min(last_zero_line+15, len(lines))):
    print(i+1, lines[i].rstrip())
