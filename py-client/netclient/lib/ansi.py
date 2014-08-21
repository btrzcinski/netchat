from copy import deepcopy
from functools import partial

ESCAPE = chr(27) + '['
FG = 0
BG = 1
MISC = 2

COLORS = {
    FG: {
        'red': '31m',
        'green': '32m',
        'yellow': '33m',
        'blue': '34m',
        'magenta': '35m',
        'cyan': '36m',
        'white': '37m',
        'black': '30m',
    },
    BG: {
        'black': '40m',
    },
    MISC: {
        'bold': '1m',
        'blinking': '5m',
        'underlined': '4m',
        'clear': '0m',
    }
}

def __color(txt, color=None):
    assert color is not None
    if isinstance(txt, str):
        txt = text(txt)
    return txt._color(color)

for k, v in COLORS[FG].iteritems():
    globals()[k] = partial(__color, color=v)
for k, v in COLORS[MISC].iteritems():
    globals()[k] = partial(__color, color=v)
for k, v in COLORS[BG].iteritems():
    globals()['%s_%s' % ('bg', k)] = partial(__color, color=v)

del k
del v

class text(object):
    def __init__(self, string):
        self.txt = string
        self.format = ''
        self.texts = []

    def __len__(self):
        return len(self.txt) + sum(len(t) for t in self.texts)

    def __add__(self, txt):
        t = deepcopy(self)
        if isinstance(txt, str):
            txt = text(txt)
        t.texts.append(txt)
        return t

    def __str__(self):
        clr = '%s%s' % (ESCAPE, COLORS[MISC]['clear'])
        mine = '%s%s%s' % (self.format, self.txt, clr)
        return '%s%s' % (mine, clr.join(str(t) for t in self.texts))

    def raw(self):
        return '%s%s' % (self.txt, ''.join(t.raw() for t in self.texts))

    def _color(self, color):
        self.format = '%s%s%s' % (self.format, ESCAPE, color)
        return self
