import curses
import curses.panel

def main():
    stdscr = curses.initscr()
    curses.cbreak()
    curses.noecho()

    y, x = 10, 10
    w = stdscr.derwin(y, x, 0, 0)
    w.border()
    pad = curses.newpad(y, x)
    p = curses.panel.new_panel(w)
    p2 = curses.panel.new_panel(pad)

    curses.panel.update_panels()

    curses.doupdate()

    stdscr.getch()
    curses.endwin()

if __name__ == '__main__':
    main()
