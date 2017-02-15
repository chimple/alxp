"""
Change :allemoji: into utf-8
Standard usage: replace_shortcode_to_utf8.py filename
"""

import sys

emoji = {
	':acorn:': u'\uF000',
	':alligator:': u'\uF001',
	':apron:': u'\uF002',
	':arm:': u'\uF003',
	':axe:': u'\uF004',
	':igloo:': u'\uF005',
	':iguana:': u'\uF006',
	':inch:': u'\uF007',
	':ink:': u'\uF008',
	':insect:': u'\uF009',
	':ivy:': u'\uF00A',
	':jacket:': u'\uF00B',
	':jam:': u'\uF00C',
	':jar:': u'\uF00D',
	':napkin:': u'\uF00E',
	':neck:': u'\uF00F',
	':nest:': u'\uF010',
	':net:': u'\uF011',
	':oar:': u'\uF012',
	':olive:': u'\uF013',
	':omlette:': u'\uF014',
	':ostrich:': u'\uF015',
	':otter:': u'\uF016',
	':oval:': u'\uF017',
	':oven:': u'\uF018',
	':quail:': u'\uF019',
	':quarter:': u'\uF01A',
	':queen:': u'\uF01B',
	':ukelele:': u'\uF01C',
	':unicycle:': u'\uF01D',
	':x-ray:': u'\uF01E',
	':xylophone:': u'\uF01F',
	':yak:': u'\uF020',
	':yatch:': u'\uF021',
	':yogurt:': u'\uF022',
	':yoyo:': u'\uF023',
}

t = open(sys.argv[1], 'r')
tempstr = t.read()
t.close()

for key, val in emoji.items():
	tempstr = tempstr.replace(key, val)

fout = open(sys.argv[1], 'w')
fout.write(tempstr)
fout.close()
