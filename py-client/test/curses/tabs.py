import curses
import curses.panel

def main():
    stdscr = curses.initscr()
    curses.cbreak()
    curses.noecho()

    wins = []

    y, x = 10, 10

    wins.append(stdscr.derwin(y, x, 0, 0))
    wins.append(stdscr.derwin(y, x, 0, 1))
    wins.append(stdscr.derwin(y, x, 0, 2))

    for w in wins:
        w.border()

    panels = []

    for w in wins:
        panels.append(curses.panel.new_panel(w))

    curses.panel.update_panels()

    curses.doupdate()

    stdscr.getch()
    curses.endwin()

if __name__ == '__main__':
    main()
