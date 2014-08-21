import curses

def main(stdscr):
    #curses.curs_set(0)
    max_y, max_x = stdscr.getmaxyx()
    y = (max_y*7)/10
    output = curses.newwin(y, max_x, 0, 0)
    output.border()
    output.scrollok(True)
    log = curses.newwin(max_y-y-1, max_x, y, 0)
    log.border()
    log.scrollok(True)
    input = curses.newwin(1, max_x, max_y-1, 0)
    input.addstr('Input cmd: ')
    output.addstr('Output Window')
    log.addstr('Log Window')
    line = ''
    cursor = 0
    queue = []
    lcurs = 0
    c = 0
    while True:
        stdscr.refresh()
        output.refresh()
        log.refresh()
        input.refresh()
        char = stdscr.getch()
        if char == 27:
            break
        if char > 255 or char < 32:
            if char == curses.KEY_LEFT:
                cursor = max(0, cursor - 1)
            if char == curses.KEY_RIGHT:
                cursor = min(len(line), cursor + 1)
            if char in [ord('\r'), ord('\n')]:
                queue = queue[:lcurs]
                queue.append(line)
                line = ''
                cursor = 0
                lcurs = len(queue)
            if char == curses.KEY_UP:
                lcurs = max(0, lcurs - 1)
                line = queue[lcurs]
                cursor = len(line)
            if char == curses.KEY_DOWN:
                lcurs = min(lcurs + 1, len(queue))
                line = (line if lcurs == len(queue) else queue[lcurs])
                cursor = len(line)
            if char == curses.KEY_DC:
                if cursor > 0:
                    line = '%s%s' % (line[:cursor-1], line[cursor:])
                    cursor -= 1
        else:
            if cursor == len(line):
                line = '%s%s' % (line, chr(char))
            else:
                line = '%s%s%s' % (line[:cursor], chr(char), line[cursor:])
            cursor += 1
        input.addstr(0, 11, line)
        input.clrtoeol()
        input.move(0, 11 + cursor)
        c += 1
        log.addstr('Log #%d' % c)

if __name__ == '__main__':
    curses.wrapper(main)
