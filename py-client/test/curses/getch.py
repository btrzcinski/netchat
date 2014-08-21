import curses

s = curses.initscr()
curses.noecho()
y =  s.getch()
d = [x for x in dir(curses) if x.startswith('KEY_')]
for i in d:
    if getattr(curses, i) == y:
        print 'Dict: %s' % i
print 'Getch: %d' % y
print curses.KEY_LEFT
curses.endwin()
