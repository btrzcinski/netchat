import copy, os
import curses
import signal

from twisted.internet import reactor

from netclient.cmanager import cmanager
from netclient.mmanager import mmanager
from netclient.constants import KEY_NEWLN, KEY_DEL, KEY_TAB
from netclient.settings import KEY_TABLEFT, KEY_TABRIGHT, KEY_TABPREV, KEY_CLOSETAB
from netclient.settings import APP_NAME, APP_VERSION
from netclient.settings import WINDOWS, SIZE_MODIFIERS
from netclient.settings import TAB_LEN
from netclient.settings import COLOR_EXP, TRUE_COLORS, VALID_COLORS, COLOR_MARKUP, COLOR_PAIRS
from netclient.settings import OUT_DOWN, OUT_UP, LOG_DOWN, LOG_UP
from netclient.util import wrap, reconstruct, truncate

class CursesStdIO(object):
    def __init__(self, proto):
        self.mac = os.name == 'mac'
        
        self.timer = 0
        self.disconnecting = False
        self.logger = cmanager['logger'].get_context('curses')
        self.clear_history()

        self.log_history = []
        s, l = WINDOWS['out']
        self.tabs = {
            l: Tab(l, s, 'main')
        }
        self.tab_names = [l]
        self.top_tab = self.tab_names[0]
        self.tab_span = 0, 0
        self.tab_ind = 0
        self.prev_tab = l

        self.stdscr = curses.initscr()
        self.stdscr.nodelay(1)
        curses.cbreak()
        self.stdscr.keypad(1)
        if not self.mac:
            curses.curs_set(0)

        curses.start_color()
        if curses.has_colors():
            self.logger.log('Initializing color pairs...')
            for c, i in COLOR_PAIRS.iteritems():
                curses.init_pair(i, getattr(curses, 'COLOR_%s' % c), curses.COLOR_BLACK)
        else:
            self.logger.log('Colors not supported by this terminal.')

        self.protocol = proto
        self.protocol.transport = self

        self.init()

        self.write = self.logout

        cmanager.add_component('screen', self)

        signal.signal(signal.SIGWINCH, self.__reinit)

    def __reinit(self, sig, stack, resized=True):
        if resized:
            self.logger.msg('Caught SIGWINCH. Resizing...')
        self.end()

        self.stdscr = curses.initscr()
        self.stdscr.nodelay(1)
        curses.cbreak()
        self.stdscr.keypad(1)
        if not self.mac:
            curses.curs_set(0)

        curses.start_color()

        self.init(False)

        diff = len(self.log_history) - self.logpad.getmaxyx()[0]
        if diff > 0:
            self.log_history = self.log_history[diff:]
        
        #re_split = COLOR_EXP.split(' '.join(self.log_history))
        #logs = wrap(re_split, 4, x)
        #self.paint_win(self.logpad, logs, (0,0))

        for ln in self.log_history:
            self.logout(ln, False)
        self.refresh(self.logwin)

        for n in self.tab_names:
            diff = len(self.tabs[n].history) - self.tabs[n].getmaxyx()[0]
            if diff > 0:
                self.tabs[n].history = self.tabs[n].history[diff:]
            for ln in self.tabs[n].history:
                self.sendLine(ln, n, False)
        self.refresh(self.outwin)

        if resized:
            self.logger.msg('...Resize complete.')

    def init(self, clear=True):
        t_offset = int(len(self.tab_names) > 1)

        max_y, max_x = self.stdscr.getmaxyx()
        y = (max_y*WINDOWS['%out'])/100

        self.outborder = self.stdscr.derwin(y - t_offset, max_x, t_offset, 0)
        
        y1, x1 = self.outborder.getmaxyx()

        self.outwin = self.outborder.derwin(y1 - 2, x1 - 2, 1, 1)
        
        mwidth = WINDOWS['outbuffer']
        if isinstance(mwidth, str):
            try:
                if mwidth.isdigit():
                    mwidth = int(mwidth)
                    m = SIZE_MODIFIERS[' ']
                else:
                    mwidth, modifier = int(mwidth[:-1]), mwidth[-1]
                    m = SIZE_MODIFIERS.get(modifier)
                    if not m:
                        raise ValueError
            except (TypeError, ValueError):
                raise ValueError, '\'outbuffer\' of the WINDOWS settings has an invalid value.'
        else:
            m = SIZE_MODIFIERS[' ']
        
        y12, x12 = self.outwin.getmaxyx()
        
        for v in self.tabs.itervalues():
            v.newpad(self)
        
        self.logborder = self.stdscr.derwin(max_y - y1 - 2 - t_offset, max_x, y1 + t_offset, 0)
        self.logtitle = WINDOWS['log']

        y2, x2 = self.logborder.getmaxyx()
        self.logwin = self.logborder.derwin(y2 - 2, x2 - 2, 1, 1)
        
        mwidth = WINDOWS['logbuffer']
        if isinstance(mwidth, str):
            try:
                if mwidth.isdigit():
                    mwidth = int(mwidth)
                    m = SIZE_MODIFIERS[' ']
                else:
                    mwidth, modifier = int(mwidth[:-1]), mwidth[-1]
                    m = SIZE_MODIFIERS.get(modifier)
                    if not m:
                        raise ValueError
            except (TypeError, ValueError):
                raise ValueError, '\'logbuffer\' of the WINDOWS settings has an invalid value.'
        else:
            m = SIZE_MODIFIERS[' ']

        self.logpad = curses.newpad(m(y2, mwidth), x2 - 2)
        y22, x22 = self.logwin.getmaxyx()
        self.logpad.scrollok(True)
        self.logln = 0
        self.logoffset = 0

        self.inwin = self.stdscr.derwin(1, max_x, max_y-1, 0)
        
        mwidth = WINDOWS['inbuffer']
        if isinstance(mwidth, str):
            try:
                mwidth, modifier = int(mwidth[:-1]), mwidth[-1]
                m = SIZE_MODIFIERS.get(modifier)
                if not m:
                    raise ValueError
            except (TypeError, ValueError):
                raise ValueError, '\'inbuffer\' of the WINDOWS settings has an invalid value.'
        else:
            m = SIZE_MODIFIERS['+']
        
        self.inpad = curses.newpad(1, m(max_x, mwidth))
        self.inoffset = 0

        self.status = self.stdscr.derwin(1, max_x, max_y-2, 0)
        self.attron(self.status, curses.A_REVERSE)

        if t_offset:
            self.tabwin = self.stdscr.derwin(1, max_x, 0, 0)
            self.attron(self.tabwin, curses.A_BOLD)
            self.tabwin.erase()
            m = self.__get_max_tabs()
            if m >= len(self.tab_names):
                self.tab_span = (0, len(self.tab_names) - 1)

        else:
            self.tabwin = None

        if clear:
            self.wipe_line()
        else:
            self.outwin.erase()
            self.logwin.erase()
            self.inwin.erase()
            self.status.erase()

        self.refresh()

    def paint_win(self, win, markup, coord):
        y, x = coord
        ym = win.getmaxyx()[0]
        xoff = 0
        yoff = 0

        markup = filter(None, markup)

        for j in range(len(markup)):
            l = markup[j]
            next = None
            prev = None
            for i in range(len(l)):
                k = l[i]
                if i > 0:
                    prev = l[i-1]
                if i < len(l)-1:
                    next = l[i+1]
                if k in VALID_COLORS and prev in COLOR_MARKUP:
                    if COLOR_PAIRS.has_key(k):
                        k = curses.color_pair(COLOR_PAIRS[k])
                    else:
                        k = getattr(curses, 'A_%s' % k)
                    if prev == COLOR_MARKUP[0]:
                        self.attron(win, k)
                    else:
                        self.attroff(win, k)
                    continue
                elif k in COLOR_MARKUP and next in VALID_COLORS:
                    continue
                else:
                    yval = min(ym-1, y + yoff)
                    xval = x + xoff
                    win.addstr(yval, xval, k)
                    xoff += len(k)

            xval = x + xoff
            yval = min(ym-1, y + yoff)
            if j < len(markup) - 2:
                try:
                    win.addstr(yval, xval, '\n\r')
                except:
                    self.logger.log('Warning: Window is too small.')
            xoff = 0
            yoff += 1

    def has_tab(self, name):
        return self.tabs.has_key(name)

    def new_tab(self, name, shortname=None, type='notepad', focus=False):
        if not shortname:
            shortname = name
        if self.has_tab(name):
            raise ValueError, 'Tabs require unique names. %s' % name
        self.tabs[name] = Tab(name, shortname, type)
        self.tabs[name].set_dialog('parser')
        self.tab_names.append(name)
        if len(self.tab_names) == 2:
            self.__reinit(None, None, False)
        else:
            self.tabs[name].newpad(self)
            self.tab_over()
            self.update_tabwin()
        if focus:
            self.tab_to(name)

    def kill_tab(self, name):
        if name == self.tab_names[0]:
            if len(self.tab_names) > 1:
                self.sendLine('Cannot close main tab while other tabs are active.')
            else:
                cmanager['factory'].exit('Main tab closed.')
            return

        if not self.has_tab(name):
            raise ValueError, 'Tab does not exist. %s' % name
        inc = (1 if self.tab_ind != len(self.tab_names)-1 else -1)
        if self.tabs[name].typing_event:
            self.tabs[name].stopped_typing()
        if name == self.top_tab:
            self.tab_over(inc, False)
        if self.tabs[name].type == 'group':
            mmanager['chat'].unsubscribe(self.tabs[name].shortname)
            mmanager['chat'].leave_room(self.tabs[name].shortname)
        del self.tabs[name]
        self.tab_names.remove(name)
        if len(self.tab_names) == 1:
            self.__reinit(None, None, False)
        else:
            self.tab_over(-1*inc)

    def tab_over(self, inc=0, update=True):
        self.tab_ind += inc
        self.tab_ind = min(max(self.tab_ind, 0), len(self.tab_names)-1)
        temp = self.top_tab
        self.top_tab = self.tab_names[self.tab_ind]

        if self.tab_ind != self.tab_ind - inc:
            self.prev_tab = temp

        self.tabs[self.top_tab].updated = False

        if update:
            m = self.__get_max_tabs()
            if m >= len(self.tab_names):
                self.tab_span = (0, len(self.tab_names) - 1)
            else:
                mn, mx = self.tab_span
                if mx - mn + 1 < m:
                    mx += m - (mx - mn + 1)
                if self.tab_ind < mn:
                    mn -= 1
                    mx -= 1
                elif self.tab_ind > mx:
                    mn += 1
                    mx += 1
                mn = max(0, mn)
                mx = min(len(self.tab_names) - 1, mx)
                self.tab_span = mn, mx

            if inc:
                self.update_tabwin()
                self.outborder.border()
                self.outborder.addstr(0, 2, self.top_tab)
                self.outborder.nooutrefresh()
                
                self.refresh(self.outwin)
                #self.refresh(self.inwin)

    def tab_to(self, name):
        if name is 0:
            name = self.tab_names[0]
        if not name:
            return
        if not self.has_tab(name):
            raise ValueError, 'Cannot focus a tab that does not exist.'
        if name == self.top_tab:
            return
        self.prev_tab = self.top_tab
        self.top_tab = name
        self.tab_ind = self.tab_names.index(name)

        self.tabs[self.top_tab].updated = False

        m = self.__get_max_tabs()
        if m >= len(self.tab_names):
            self.tab_span = (0, len(self.tab_names) - 1)
        else:
            mn, mx = self.tab_span
            if mx - mn + 1 < m:
                mx += m - (mx - mn + 1)
            if self.tab_ind < mn:
                mn -= 1
                mx -= 1
            elif self.tab_ind > mx:
                mn += 1
                mx += 1
            mn = max(0, mn)
            mx = min(len(self.tab_names) - 1, mx)
            self.tab_span = mn, mx

        self.update_tabwin()
        self.outborder.border()
        self.outborder.addstr(0, 2, self.top_tab)
        self.outborder.nooutrefresh()
        
        self.refresh(self.outwin)
        self.refresh(self.inwin)

    def attron(self, win, attr):
        if cmanager['factory'].opts['color']:
            win.attron(attr)

    def attroff(self, win, attr):
        if cmanager['factory'].opts['color']:
            win.attroff(attr)

    def scroll_log(self, inc):
        self.logoffset += inc
        self.logoffset = min(max(self.logoffset, 0), self.logln - (self.logwin.getmaxyx()[0] - 1))
        
        self.refresh(self.logwin)

    def scroll_pad(self, inc):
        t = self.tabs[self.top_tab]
        
        self.tabs[self.top_tab].offset += inc
        self.tabs[self.top_tab].offset = min(max(self.tabs[self.top_tab].offset, 0), self.tabs[self.top_tab].lines - (self.outwin.getmaxyx()[0] - 1))
        

        self.refresh(self.outwin)

    def reset(self, tab):
        if tab == self.top_tab:
            self.wipe_line()
        self.tabs[tab].show_input = False
        self.tabs[tab].prompt = ''

    def wipe_line(self):
        self.tabs[self.top_tab].input = ''
        self.tabs[self.top_tab].cursor = 0
        self.inpad.move(0, self.tabs[self.top_tab].cursor)
        self.inpad.clrtoeol()

    def line_received(self):
        if not self.tabs[self.top_tab].input:
            return
        self.cmd_history.append(self.tabs[self.top_tab].input)
        if len(self.cmd_history) > WINDOWS['cmdbuffer']:
            self.cmd_history = self.cmd_history[1:]
        self.history_buffer = []
        self.history_pointer = 0

    def update_inwin(self):
        self.inwin.addstr(0, 0, self.tabs[self.top_tab].prompt)
        if self.tabs[self.top_tab].show_input:
            self.inpad.addstr(0, 0, self.tabs[self.top_tab].input)
            self.inpad.clrtoeol()
            self.inpad.move(0, self.tabs[self.top_tab].cursor)

        iy, ix = self.inwin.getbegyx()
        y, x = self.inwin.getmaxyx()
        self.inoffset = max(0, (len(self.tabs[self.top_tab].prompt) + self.tabs[self.top_tab].cursor) - (x-1))
        xy, xx = iy + y, ix + x
        y, x = 0, 0 + self.inoffset

        if self.tabs[self.top_tab].show_input:
            self.inwin.move(0, min(self.inwin.getmaxyx()[1]-1, len(self.tabs[self.top_tab].prompt) + self.tabs[self.top_tab].cursor))

        self.inpad.refresh(y, x, iy, ix + len(self.tabs[self.top_tab].prompt), xy, xx-2)

        self.inwin.nooutrefresh()

    def update_status(self):
        mx = self.status.getmaxyx()[1]
        
        len_title = mx/2
        stat_title = ('%%-%ds' % len_title) % truncate('%s %s' % (APP_NAME, APP_VERSION), len_title)

        len_host = mx/2 - 1
        username = (('%s@' % mmanager['login'].username) if ('login' in mmanager and mmanager['login'].online) else '')
        hostname = (cmanager['xmlcomm'].address.host if 'xmlcomm' in cmanager else 'host')
        stat_host = ('%%+%ds' % len_host) % truncate('%s%s' % (username, hostname), len_host)
        
        fields = (len_title, len_host)
        diff = mx - 1 - sum(fields)
        stat_host = '%s%s' % (' '*diff, stat_host)

        status_bar = '%s%s' % (stat_title, stat_host)
        self.status.addstr(0, 0, status_bar)
        self.status.nooutrefresh()

    def __get_max_tabs(self):
        # '(#) '
        prefix = 4 + len(str(len(self.tab_names))) + len(str(self.tab_ind+1))
        mx = self.tabwin.getmaxyx()[1] - 1
        num = mx - prefix - 1
        den = TAB_LEN + 1
        return num/den

    def update_tabwin(self):
        if not self.tabwin:
            return
        self.tabwin.erase()
        
        mx = self.status.getmaxyx()[1]

        tabcount = '(%d/%d) ' % (self.tab_ind+1, len(self.tab_names))

        names = [truncate(self.tabs[x].decoration % self.tabs[x].shortname, TAB_LEN-2).center(TAB_LEN) for x in self.tab_names]
        num_tabs = self.__get_max_tabs()
        if num_tabs <= 1:
            return

        mn, mx = self.tab_span

        self.tabwin.addstr(0, 0, tabcount)
        cursor = len(tabcount)

        for n in range(mn, mx+1):
            self.tabwin.addstr(0, cursor, '|')
            cursor += 1
            if n == self.tab_ind:
                self.attron(self.tabwin, curses.A_REVERSE)
            elif self.tabs[self.tab_names[n]].updated:
                self.attron(self.tabwin, curses.A_BLINK)
            self.tabwin.addstr(0, cursor, names[n])
            cursor += len(names[n])
            if n == self.tab_ind:
                self.attroff(self.tabwin, curses.A_REVERSE)
            elif self.tabs[self.tab_names[n]].updated:
                self.attroff(self.tabwin, curses.A_BLINK)
        self.tabwin.addstr(0, cursor, '|')

        self.tabwin.nooutrefresh()

    def update_outpane(self):
        iy, ix = self.outwin.getbegyx()
        y = self.outborder.getmaxyx()[0]
        x = self.outwin.getmaxyx()[1]
        xy, xx = iy + y - 4, ix + x
        y, x = 0 + self.tabs[self.top_tab].offset, 0

        self.tabs[self.top_tab].refresh(y, x, iy, ix, xy, xx)
        
        self.outborder.nooutrefresh()

    def update_logpane(self):
        iy, ix = self.logwin.getbegyx()
        y = self.logborder.getmaxyx()[0]
        x = self.logwin.getmaxyx()[1]
        xy, xx = iy + y - 4, ix + x
        y, x = 0 + self.logoffset, 0

        self.logpad.refresh(y, x, iy, ix, xy, xx)

        self.logborder.nooutrefresh()

    def update_panes(self):
        self.reborder()
        self.update_status()
        self.update_outpane()
        self.update_tabwin()
        self.update_logpane()

    def refresh(self, spec=None):
        if not self.mac:
            curses.curs_set(0)
        
        self.stdscr.nooutrefresh()
        if not spec:
            self.update_inwin()
            self.update_panes()
        elif spec == self.inwin:
            self.update_inwin()
        elif spec == self.outwin:
            self.update_outpane()
        elif spec == self.logwin:
            self.update_logpane()
        elif spec == self.tabwin:
            self.update_tabwin()

        y = self.inwin.getbegyx()[0]
        x = (self.tabs[self.top_tab].cursor + len(self.tabs[self.top_tab].prompt) if self.tabs[self.top_tab].show_input else len(self.tabs[self.top_tab].prompt))
        curses.setsyx(y, x)

        if not self.mac:
            curses.curs_set(0)
        curses.doupdate()
        if self.tabs[self.top_tab].prompt and not self.mac:
            curses.curs_set(1)

    def reborder(self):
        self.outborder.border()
        self.outborder.addstr(0, 2, self.top_tab)

        self.logborder.border()
        self.logborder.addstr(0, 2, self.logtitle)

        self.outborder.nooutrefresh()
        self.logborder.nooutrefresh()

    def logout(self, msg, buffer=True):
        y, x = self.logpad.getmaxyx()
        
        re_split = COLOR_EXP.split(msg)

        lines = wrap(re_split, 4, x)
        if len(lines) is 1 and not lines[0]:
            return
        if buffer:
            lns = []
            for l in lines:
                lns.append(reconstruct(l))
            self.log_history += lns
            diff = len(self.log_history) - self.logpad.getmaxyx()[0]
            if diff > 0:
                self.log_history = self.log_history[diff:]

        lines[-1].append('\n\r')

        self.paint_win(self.logpad, lines, (self.logln, 0))

        self.logln = min(y - 1, self.logln + len(lines))
        self.logoffset = max(0, self.logln - (self.logwin.getmaxyx()[0] - 1))
        if buffer:
            self.refresh(self.logwin)

    def sendLine(self, msg, tab=None, buffer=True):
        if tab is 0:
            tab = self.tab_names[0]
        if not tab:
            tab = self.top_tab

        msg = msg.split('\n\r')
        if len(msg) > 1:
            for l in msg:
                self.sendLine(l, tab, buffer)
            return
        msg = msg[0]
        self.out('%s\n\r' % msg, tab, buffer)

    def out(self, msg, tab=None, buffer=True):
        if tab is 0:
            tab = self.tab_names[0]
        if not tab:
            tab = self.top_tab

        msg = msg.split('\n\r')
        newln = not msg[-1]
        
        if newln:
            msg = msg[:-1]

        if len(msg) > 1:
            for ln in msg:
                self.sendLine(ln, tab, buffer)
        msg = msg[-1]

        re_split = COLOR_EXP.split(msg)

        y, x = self.tabs[tab].getmaxyx()
        lines = wrap(re_split, 4, x)

        if buffer:
            lns = []
            for l in lines:
                lns.append(reconstruct(l))

            self.tabs[tab].history += lns
            diff = len(self.tabs[tab].history) - self.tabs[tab].getmaxyx()[0]
            if diff > 0:
                self.tabs[tab].history = self.tabs[tab].history[diff:]

        if newln:
            lines[-1].append('\n\r')

        self.paint_win(self.tabs[tab], lines, (self.tabs[tab].lines, 0))

        self.tabs[tab].lines = min(y - 1, self.tabs[tab].lines + len(lines))
        self.tabs[tab].offset = max(0, self.tabs[tab].lines - (self.outwin.getmaxyx()[0] - 1))
        if tab != self.top_tab:
            self.tabs[tab].updated = True
            self.update_tabwin()
        if buffer:
            self.refresh(self.outwin)

    def line_handle(self, data):
        #self.sendLine(str(data))

        flag = False
        if (data >= 32 and data <= 126) or (data >= 128 and data < 255):
            self.handle_key_ascii(data)
        else:
            if data in (curses.KEY_LEFT, curses.KEY_RIGHT):
                self.handle_key_direction(data)
        
            elif data == curses.KEY_UP:
                self.handle_key_cmd_up(data)
            elif data == curses.KEY_DOWN:
                self.handle_key_cmd_down(data)
            
            elif data in (OUT_DOWN, OUT_UP):
                self.scroll_pad((1 if data == OUT_DOWN else -1))
            elif data in (LOG_DOWN, LOG_UP):
                self.scroll_log((1 if data == LOG_DOWN else -1))
            
            elif (data in KEY_TABRIGHT or data in KEY_TABLEFT) and len(self.tab_names) > 1:
                self.tab_over((1 if data in KEY_TABRIGHT else -1))
            elif data == KEY_TABPREV:
                if self.tabs.has_key(self.prev_tab):
                    self.tab_to(self.prev_tab)
            elif data == KEY_CLOSETAB:
                self.kill_tab(self.top_tab)

            elif data == curses.KEY_DC and self.tabs[self.top_tab].cursor < len(self.tabs[self.top_tab].input):
                self.tabs[self.top_tab].input = '%s%s' % (self.tabs[self.top_tab].input[:self.tabs[self.top_tab].cursor], self.tabs[self.top_tab].input[self.tabs[self.top_tab].cursor+1:])
            elif data in (KEY_DEL, curses.KEY_BACKSPACE)and self.tabs[self.top_tab].cursor > 0:
                self.handle_key_del(data)
            
            elif data == KEY_NEWLN:
                flag = True
            
            elif data == KEY_TAB:
                self.handle_key_tab(data)

        if flag:
            self.line_received()

        self.refresh(self.inwin)
        
        return flag

    def handle_key_ascii(self, data):
        if len(self.tabs[self.top_tab].input) + len(self.tabs[self.top_tab].prompt) + 1 >= self.inpad.getmaxyx()[1]:
            self.logger.log('Input character overflow (%s).' % chr(data))
            return
        if self.tabs[self.top_tab].cursor == len(self.tabs[self.top_tab].input):
            self.tabs[self.top_tab].input = '%s%s' % (self.tabs[self.top_tab].input, chr(data))
        else:
            self.tabs[self.top_tab].input = '%s%s%s' % (self.tabs[self.top_tab].input[:self.tabs[self.top_tab].cursor], chr(data), self.tabs[self.top_tab].input[self.tabs[self.top_tab].cursor:])
        t = self.tabs[self.top_tab]
        t.cursor += 1
        t = self.tabs[self.top_tab]
        if t.type == 'onetoone' and t.input[0] != '/':
            if not t.typing_event:
                mmanager['chat'].started_typing(t.shortname)
                t.typing_event = reactor.callLater(.5, t.stopped_typing)
            else:
                t.typing_event.reset(.5)

    def handle_key_direction(self, data):
        self.tabs[self.top_tab].cursor += (1 if data == curses.KEY_RIGHT else -1)
        self.tabs[self.top_tab].cursor = min(max(0, self.tabs[self.top_tab].cursor), len(self.tabs[self.top_tab].input))

    def handle_key_cmd_up(self, data):
        if not self.cmd_history:
            return
        
        if self.history_buffer:
            if self.history_pointer > 0:
                self.history_buffer[self.history_pointer] = self.tabs[self.top_tab].input
                self.history_pointer -= 1
                self.tabs[self.top_tab].input = self.history_buffer[self.history_pointer]
                self.tabs[self.top_tab].cursor = len(self.tabs[self.top_tab].input)
        else:
            self.history_buffer = copy.copy(self.cmd_history)
            temp = self.tabs[self.top_tab].input
            self.history_pointer = len(self.history_buffer)-1
            self.tabs[self.top_tab].input = self.history_buffer[self.history_pointer]
            self.history_buffer.append(temp)
            self.tabs[self.top_tab].cursor = len(self.tabs[self.top_tab].input)


    def handle_key_cmd_down(self, data):
        if not (self.history_buffer and self.history_pointer < len(self.history_buffer) - 1):
            return
        
        self.history_buffer[self.history_pointer] = self.tabs[self.top_tab].input
        self.history_pointer += 1
        self.tabs[self.top_tab].input = self.history_buffer[self.history_pointer]
        self.tabs[self.top_tab].cursor = len(self.tabs[self.top_tab].input)

    def handle_key_del(self, data):
        self.tabs[self.top_tab].input = '%s%s' % (self.tabs[self.top_tab].input[:self.tabs[self.top_tab].cursor-1], self.tabs[self.top_tab].input[self.tabs[self.top_tab].cursor:])
        self.tabs[self.top_tab].cursor -= 1

    def handle_key_tab(self, data):
        if self.tabs[self.top_tab].dialog.__class__ == cmanager['terminal'].DIALOGS['parser']:
            lns = self.tabs[self.top_tab].input.split(' ', 1)
            if len(lns) is 2:
                cmd, inp = lns
                flag = False
                if cmd.startswith('/'):
                    cmd = cmd[1:]
                    flag = True
                if self.tab_ind and not flag:
                    return
                inp = inp.rsplit(' ', 1)
                inter, inp = (inp[0] if len(inp) is 2 else ''), inp[-1]
                fullcmd = cmanager['commands']._locatecmd(cmd)
                if fullcmd:
                    cmd = cmanager['commands'].commands[fullcmd]
                    mod = cmd.im_self.__module__.split('.')[-1]
                    l = (mmanager[mod] if mod in mmanager else None)
                    if l:
                        l = l.get_tabbed_list(fullcmd, inter, inp, cmd.type)
                        self.tab_completion_callback(l, inp, self.top_tab)
                    else: 
                        self.tab_to(self.tab_names[0])
                        self.sendLine('Tab functionality not supported for this command.')
        else:
            self.tab_to(self.tab_names[0])
            self.sendLine('Tabbing is only supported during command parsing.')

    def tab_completion_callback(self, ls, inp, tab):
        ls = [n for n in ls if n.startswith(inp)]
        if not ls:
            return
        if len(ls) is 1:
            ls = ls[0][len(inp):]
            for ch in ls: # TODO: Rework?
                self.line_handle(ord(ch))
        else: # TODO: Handling for large numbers
            # TODO: Tabbing seems wacky
            self.sendLine('%d possibilities for TAB:' % len(ls))
            x = self.tabs[self.top_tab].getmaxyx()[1]-5
            msg = '\t%s' % '\t'.join(ls)
            self.sendLine(msg)

    def clear_history(self):
        self.logger.log('Clearing command history...')
        self.cmd_history = []
        self.history_buffer = []
        self.history_pointer = 0

    def fileno(self):
        """
        Madhax to make Twisted treat this as stdin.
        """
        return 0

    def flush(self):
        self.refresh(self.logwin)

    def loseConnection(self):
        self.connectionLost(None)

    def connectionLost(self, reason):
        self.close()

    def doRead(self):
        """
        Called when the transport is selected for IO.
        """
        curses.noecho()
        self.timer = self.timer + 1
        c = self.stdscr.getch()
        self.protocol.dataReceived(c)

    def close(self):
        pass

    def end(self):
        self.disconnecting = True
        curses.nocbreak()
        self.stdscr.keypad(0)
        curses.echo()
        curses.endwin()

    def logPrefix(self):
        return 'CursesIO'

class Tab:
    """
    Acts as a container for data pertaining to tabs in the curses setup.
    """

    DECORATIONS = {
        'notepad': '[%s]',
        'group': '(g) %s',
    }

    CHAT_DECORATIONS = {
        'typing': '(t) %s',
        'offline': '(x) %s',
    }

    def __init__(self, name, shortname, type):
        self.name = name
        self.shortname = shortname
        self.type = type
        self.history = []
        self.lines = 0
        self.offset = 0
        self.cursor = 0
        self.input = ''
        self.prompt = ''
        self.dialog_layers = []
        self.dialog = None
        self.show_input = True
        self.pad = None
        self.updated = False
        self.typing_event = False
        self.decoration = self.DECORATIONS.get(type, '%s')

    def __getattr__(self, attr):
        return getattr(self.pad, attr)

    def set_dialog(self, dialog, *args):
        """
        Called to cleanly initiate a dialog transition.
        The previous dialog will be allowed to gracefull exit,
        and the new one will initialize.

        *args will be passed verbatim to the constructor of the new
        dialog.
        """
        if not callable(dialog):
            dialog = cmanager['terminal'].DIALOGS[dialog]
        cmanager['screen'].reset(self.name)
        if self.dialog:
            self.dialog.close()
        self.dialog = dialog(self, *args)
        self.dialog_args = args
        self.dialog.makeConnection(cmanager['terminal'].transport)

    def layer_dialog(self, dialog, *args):
        self.dialog_layers.append((self.dialog, self.dialog_args))
        self.set_dialog(dialog, *args)

    def pop_dialog(self):
        if not cmanager['screen'].has_tab(self.name):
            return
        d, a = self.dialog_layers.pop()
        while d.__class__ == cmanager['terminal'].DIALOGS['dialog']:
            d, a = self.dialog_layers.pop()
        self.set_dialog(d.__class__, *a)

    def newpad(self, screen):
        self.lines = 0
        self.offset = 0
        
        mwidth = WINDOWS['outbuffer']
        if isinstance(mwidth, str):
            try:
                if mwidth.isdigit():
                    mwidth = int(mwidth)
                    m = SIZE_MODIFIERS[' ']
                else:
                    mwidth, modifier = int(mwidth[:-1]), mwidth[-1]
                    m = SIZE_MODIFIERS.get(modifier)
                    if not m:
                        raise ValueError
            except (TypeError, ValueError):
                raise ValueError, '\'outbuffer\' of the WINDOWS settings has an invalid value.'
        else:
            m = SIZE_MODIFIERS[' ']
        y1, x1 = screen.outborder.getmaxyx()

        self.pad = curses.newpad(m(y1, mwidth), x1- 2)
        self.scrollok(True)

    def sendLine(self, line):
        cmanager['screen'].sendLine(line, self.name)

    def out(self, data):
        cmanager['screen'].out(data, self.name)

    def handle(self, line):
        if not line:
            return
        getattr(self, 'handle_%s' % self.type)(line)
        cmanager['screen'].wipe_line()
        cmanager['screen'].refresh(cmanager['screen'].inwin)

    def stopped_typing(self):
        if self.type == 'onetoone':
            mmanager['chat'].stopped_typing(self.shortname)
        self.typing_event = False

    def handle_main(self, line):
        pass

    def handle_notepad(self, line):
       self.sendLine(line) 

    def handle_onetoone(self, line):
        mmanager['chat'].write(self.shortname, line)

    def handle_group(self, line):
        mmanager['chat'].write(self.shortname, line, True)
