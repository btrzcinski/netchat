"""
Utility functions used by the NC Py-Client.
"""

from netclient import constants, settings

def dynamicLoad(basepath, name):
    basemod = __import__(basepath, globals(), globals(), [name])
    module = getattr(basemod, name, None)
    return module

# Determines whether or not a given attribute is a command
def is_command(attr, sclass):
    att = getattr(sclass, attr, None)
    if not att:
        return False
    return callable(att) and getattr(att, settings.CMD_ATT, False)

def get_commands(sclass):
    st = set([])
    for s in dir(sclass):
        if is_command(s, sclass):
            st.add(s)

    return st

def wrap(regex, tab=8, width=None):
    if isinstance(regex, str):
        regex = [regex]
    assert isinstance(regex, list)
   
    w = (width if width else constants.WRAP_WIDTH) - 2
    lines = [[]]
    linesize = 0
    ln = ''
    tabch = ' ' * tab
    tc = ' $TAB ' 
    tc2 = tc.strip()
    tcx = '$T%sAB' % chr(27)
    
    prev = None
    next = None
    for i in range(len(regex)):
        if i > 0:
            prev = regex[i-1]
        if i < len(regex) - 1:
            next = regex[i+1]
        tok = regex[i]
        if tok in settings.VALID_COLORS and prev in settings.COLOR_MARKUP:
            lines[-1].append(tok)
            continue
        elif tok in settings.COLOR_MARKUP and next in settings.VALID_COLORS:
            lines[-1].append(tok)
            continue
        else:
            # Plain text case
            tok = tok.replace(tc2, tcx).replace(tabch, tc).replace('\t', tc)
            if tok != tc:
                leading_space = len(tok) - len(tok.lstrip())
                ending_space = len(tok) - len(tok.rstrip())
            else:
                leading_space, ending_space = 0, 0
            toks = tok.split()
            for t in toks:
                if t == tc2:
                    t = '\t'.expandtabs(tab)
                elif t == tcx:
                    t = tc2
                if len(t) > w:
                    if len(ln) >= 2 and ln[-1] == ' ' and ln[-2] != ' ':
                        ln = ln.rstrip()
                    if ln:
                        lines[-1].append('%s%s%s' % (' '*leading_space, ln, ' '*ending_space))
                        ln = ''

                    while len(t) > w:
                        lines.append([t[:w]])
                        t = t[w:]
                    lines.append(['%s%s' % (t, ' ')])
                    linesize = len(t) + 1
                    continue
                newsize = len(t) + 1
                if linesize + newsize > w:
                    if len(ln) >= 2 and ln[-1] == ' ' and ln[-2] != ' ':
                        ln = ln.rstrip()
                    if ln:
                        lines[-1].append('%s%s%s' % (' '*leading_space, ln, ' '*ending_space))
                        ln = ''
                    lines.append(['%s%s' % (t, ' ')])
                    linesize = newsize
                else:
                    ln = '%s%s%s' % (ln, t, ' ')
                    linesize += newsize
    
        if len(ln) >= 2 and ln[-1] == ' ' and ln[-2] != ' ':
            ln = ln.rstrip()
        if ln:
            lines[-1].append('%s%s%s' % (' '*leading_space, ln, ' '*ending_space))
            ln = ''

    return lines

def truncate(text, length):
    if len(text) <= length:
        return text
    if length >= 4:
        var = text[:length]
        return '%s...' % var[:-3]
    return text[:length]

def reconstruct(chopped):
    ln = ''
    prev = None
    next = None
    for i in range(len(chopped)):
        k = chopped[i]
        if i > 0:
            prev = chopped[i-1]
        if i < len(chopped)-1:
            next = chopped[i+1]
        if k in settings.VALID_COLORS and prev in settings.COLOR_MARKUP:
            ln = '%s|%s%s|' % (ln, prev, k)
        elif k in settings.COLOR_MARKUP and next in settings.VALID_COLORS:
            continue
        else:
            ln = '%s%s' % (ln, k)

    return ln

def do_nothing(obj=None):
    pass
