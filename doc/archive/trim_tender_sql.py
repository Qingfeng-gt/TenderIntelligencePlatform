# -*- coding: utf-8 -*-
# 从 tender.sql 中删除已裁剪模块的 system_menu INSERT 以及 system_role_menu 关联行
import re

SQL_PATH = r'D:\MyProjects\TenderIntelligencePlatform\backend\sql\mysql\tender.sql'
DELETED_PATH = r'C:\Users\Administrator\AppData\Local\Temp\deleted_ids.txt'

with open(DELETED_PATH, encoding='utf-8') as f:
    deleted = set(int(x) for x in f.read().split() if x.strip())

with open(SQL_PATH, encoding='utf-8') as f:
    lines = f.readlines()

menu_pat = re.compile(r"^INSERT INTO `system_menu`.*VALUES \(([0-9]+),")
rm_pat = re.compile(r"^INSERT INTO `system_role_menu`.*VALUES \(([0-9]+), ([0-9]+), ([0-9]+),")

kept = []
removed_menu = 0
removed_rm = 0
for line in lines:
    m = menu_pat.match(line)
    if m and int(m.group(1)) in deleted:
        removed_menu += 1
        continue
    m = rm_pat.match(line)
    if m:
        rid, mid = int(m.group(2)), int(m.group(3))
        if mid in deleted or rid in deleted:
            removed_rm += 1
            continue
    kept.append(line)

with open(SQL_PATH, 'w', encoding='utf-8') as f:
    f.writelines(kept)

print(f'removed system_menu lines: {removed_menu}')
print(f'removed system_role_menu lines: {removed_rm}')
