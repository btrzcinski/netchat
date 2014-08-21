"""
The NCChat Module. Handles pretty much anything related to instant
messaging.
"""

import copy, time

from twisted.internet import reactor

from netclient.lib.etreeparser import ETreeParser as ETP

from netclient.cmanager import cmanager
from netclient.mmanager import mmanager
from netclient.extensibles import Module
from netclient.io import Tab
from netclient.settings import CHAT_TAB, GCHAT_TAB, CHAT_TIMESTAMP, CHAT_FORMAT, CHAT_COLORS, COLOR
from netclient.messages.chat import *

name = 'Chat'
version = '0.1a'
moduleclass = 'Chat'

class Chat(Module):
    def __init__(self):
        Module.__init__(self)
        self.logger = cmanager['logger'].get_context('chat')
        self.friends = []
        self.disp_friends = False
        self.room_tab_callback = False
        self.color_list = copy.copy(CHAT_COLORS)
        self.room_colors = {}

    def open(self):
        Module.open(self)
        self.request_friends(False)
        self.request_backlog()

    def get_tabbed_list(self, command, intermediate, input, type):
        if type == 'group':
            self.room_tab_callback = (input, cmanager['screen'].top_tab)
            self.request_rooms()
            return []
        if not intermediate:
            return ['%s ' % x for x in self.friends]
        return []

    def write(self, target, message, group=False):
        p = ((('room' if group else 'private'),),)
        cmanager['xmlcomm'].sendLine(NCChat(target, message, p))
        
        year, month, day, hours, minutes, seconds, weekday, yearday, dst = time.localtime()
        
        c = (GCHAT_TAB if group else CHAT_TAB)

        if not cmanager['screen'].has_tab(c % target):
            cmanager['screen'].new_tab(c % target, target, ('group' if group else 'onetoone'), True)
            if group:
                self.subscribe(target)
        stamp = CHAT_TIMESTAMP % (month, day, year, hours, minutes, seconds)
        msg = CHAT_FORMAT % (stamp, COLOR('blue') % ('%s:' % mmanager['login'].username), message)
        cmanager['screen'].sendLine(msg, c % target)

    def subscribe(self, room):
        cmanager['xmlcomm'].sendLine(NCRoomSubscribe(room))
        self.room_colors[room] = {}

    def unsubscribe(self, room):
        cmanager['xmlcomm'].sendLine(NCRoomUnsubscribe(room))

    def request_friends(self, disp_friends=False):
        self.disp_friends = disp_friends
        cmanager['xmlcomm'].sendLine(NCFriendsReq())

    def request_rooms(self):
        cmanager['xmlcomm'].sendLine(NCRoomsReq())

    def request_blocked(self):
        cmanager['xmlcomm'].sendLine(NCBlockListReq())

    def query_room(self, name):
        cmanager['xmlcomm'].sendLine(NCRoomQuery(name))

    def add_friend(self, friend):
        cmanager['xmlcomm'].sendLine(NCAddFriend(friend))

    def rem_friend(self, friend):
        cmanager['xmlcomm'].sendLine(NCRemFriend(friend))

    def block(self, user):
        cmanager['xmlcomm'].sendLine(NCRequestBlock(user))

    def unblock(self, user):
        cmanager['xmlcomm'].sendLine(NCRemoveBlock(user))
        cmanager['screen'].sendLine('Unblocking %s.' % user)

    def request_backlog(self):
        cmanager['xmlcomm'].sendLine(NCBacklogReq())

    def started_typing(self, to):
        cmanager['xmlcomm'].sendLine(NCTypingEvent(to, mmanager['login'].username, 'true'))

    def stopped_typing(self, to):
        cmanager['xmlcomm'].sendLine(NCTypingEvent(to, mmanager['login'].username, 'false'))

    def set_status(self, stat):
        cmanager['xmlcomm'].sendLine(NCCSetMessage(stat))
        cmanager['screen'].sendLine('Status changed to: %s' % stat if stat else 'Status cleared.')

    def leave_room(self, room):
        del self.room_colors[room]

    #-- Callbacks --

    def message(self, content):
        etp = ETP(content)
        etp.require_tag(('type',), 'properties')
        p = content.find('properties')
        assert p is not None and p.attrib.has_key('type')
        t = p.attrib['type']
        assert t == 'private' or t == 'room'
        flag = t == 'private'
        
        etp.require_tag('username')
        if not flag:
            etp.require_tag('room')
        etp.require_tag('message')

        uname = etp.get('username').text
        rm = (etp.get('room').text if not flag else '')
        c = (CHAT_TAB if flag else GCHAT_TAB)
        m = (uname if flag else rm)
        c %= m

        mesg = etp.get('message').text

        year, month, day, hours, minutes, seconds, weekday, yearday, dst = time.localtime()
      
        if not cmanager['screen'].has_tab(c):
            if not flag:
                raise TypeError
            else:
                cmanager['screen'].new_tab(c, uname, 'onetoone', True)
        stamp = CHAT_TIMESTAMP % (month, day, year, hours, minutes, seconds)
        if flag:
            clr = COLOR('RED')
        else:
            if not self.color_list:
                self.color_list = copy.copy(CHAT_COLORS)
            if self.room_colors[m].has_key(uname):
                clr = self.room_colors[m][uname]
            else:
                n = hash(uname) % len(self.color_list)
                clr = self.color_list[n]
                self.room_colors[m][uname] = clr
                self.color_list.pop(n)
        msg = CHAT_FORMAT % (stamp, clr % ('%s:' % uname), mesg)
        try:
            cmanager['screen'].sendLine(msg, c)
        except UnicodeEncodeError:
            cmanager['screen'].sendLine('\tReceived invalid character in chat message.')

    def friends_list(self, content):
        etp = ETP(content)
        if etp.get():
            etp.require_tags(('online',), 'username')
        
        fs = etp.gets('username')
        friends = {}
        online = []
        offline = []
        for f in fs:
            k, v = f.text, f.attrib['online'] == 'true'
            (online if v else offline).append(k)
        
        self.friends = online + offline
        if self.disp_friends:
            online.sort()
            offline.sort()
            if len(online) + len(offline) == 0:
                msg = 'You have no friends. Sorry! :('
            else:
                msg = 'Friends:%s'
                if online:
                    msg %= (' (*)%s%%s' % ', (*)'.join(online))
                    msg %= (', %s' % ', '.join(offline) if offline else '')
                else:
                    msg %= (' %s' % ', '.join(offline))
            cmanager['screen'].sendLine(msg)
            self.disp_friends = False

    def friend_status_update(self, content):
        etp = ETP(content)
        etp.require_tag(('online',), 'username')
        uname = etp.get('username')

        o = uname.attrib['online'] == 'true'
        msg = '%s has signed %s' % (COLOR('BOLD') % uname.text, ('on!' if o else 'off.'))
        t = CHAT_TAB % uname.text
        t = (t if cmanager['screen'].has_tab(t) else 0)
        cmanager['screen'].sendLine(msg, t)

    def authorize_friend(self, content):
        etp = ETP(content)
        etp.require_tag(('online',), 'username')
        uname = etp.get('username')

        cmanager['screen'].sendLine('\'%s\' added to friends list!' % uname.text)

    def deny_friend(self, content):
        etp = ETP(content)
        etp.require_tag('username')
        etp.require_tag('reason')
        uname = etp.get('username')
        reas = etp.get('reason')

        cmanager['screen'].sendLine('\'%s\' could not be added to friends list.\n\r\tReason: %s' % (uname.text, reas.text))

    def warning_message(self, content):
        etp = ETP(content)
        etp.require_tag('message')
        etp.require_tag('username')
        mesg = etp.get('message')
        unam = etp.get('username')

        cmanager['screen'].sendLine('NCChat Warning: %s' % mesg.text)

    def error_message(self, content):
        etp = ETP(content)
        etp.require_tag('message')
        mesg = etp.get('message')

        cmanager['screen'].sendLine('NCChat ERROR: %s' % mesg.text)

    def block_list(self, content):
        etp = ETP(content)
        if etp.get():
            etp.require_tags('username')
        
        fs = etp.gets('username')
        
        msg = ('Blocked: %s' % ', '.join([x.text for x in fs]) if fs else 'You\'re not blocking anyone.')
        cmanager['screen'].sendLine(msg)

    def deny_block_user(self, content):
        etp = ETP(content)
        etp.require_tag('username')
        etp.require_tag('reason')

        user = etp.get('username').text
        reas = etp.get('reason').text

        cmanager['screen'].sendLine('Unable to block %s. Reason: %s' % (user, reas))

    def authorize_block_user(self, content):
        etp = ETP(content)
        etp.require_tag('username')

        user = etp.get('username').text

        cmanager['screen'].sendLine('Successfully blocked %s.' % user)

    def room_event(self, content):
        etp = ETP(content)
        etp.require_tag(('type',), 'event')
        etp.require_tag('room')
        etp.require_tag('username')

        c = GCHAT_TAB % etp.get('room').text

        uname = etp.get('username').text
        stat = etp.get('event').attrib['type']
        stat = ('joined' if stat == 'subscribe' else 'left')

        msg = '%s has %s the room.' % (COLOR('BOLD') % uname, stat)
        if cmanager['screen'].has_tab(c):
            cmanager['screen'].sendLine(msg, c)

    def room_info(self, content):
        etp = ETP(content) 
        etp.require_tag(('name',), 'room')
        
        r = etp.get('room').attrib['name']
        sc = cmanager.get_proxy('screen')
        
        if etp.get('room'):
            etp.require_tags((), 'room', 'username')
        else:
            sc.sendLine('Room \'%s\' does not exist!' % r, 0, True)
            sc.tab_to(0)
            return

        us = etp.gets('room', 'username')
        u = ', '.join([s.text for s in us])
        msg = 'Users: %s' % u

        t = sc.tabs[sc.top_tab]
        if t.shortname == r and t.type == 'group':
            sc.sendLine(msg)
        else:
            msg = 'Room name: %s\n\r%s' % (r, msg)
            sc.sendLine(msg, 0, True)
            sc.tab_to(0)

    def room_list(self, content):
        etp = ETP(content)

        rooms = [r.attrib['name'] for r in etp.gets('room')]

        if self.room_tab_callback:
            inp, tab = self.room_tab_callback
            self.room_tab_callback = False
            cmanager['screen'].tab_completion_callback(rooms, inp, tab)
        else:
            msg = ('Rooms: %s' % ', '.join(rooms) if rooms else 'No active rooms exist.')
            cmanager['screen'].sendLine(msg)

    def backlog(self, content):
        etp = ETP(content)
        if etp.get():
            etp.require_tags(('src', 'sent'), 'message')

        blog = etp.gets('message')
        blog = [(b.attrib['sent'], b.attrib['src'], b.text) for b in blog]
        
        if not blog:
            cmanager['screen'].sendLine('No chat backlog.', 0)
        for stamp, frm, txt in blog:
            t = CHAT_TAB % frm
            if not cmanager['screen'].has_tab(t):
                cmanager['screen'].new_tab(t, frm, 'onetoone')
                cmanager['screen'].sendLine('Backlog:', t)
            cmanager['screen'].sendLine('   [%s] [%s]: %s' % (stamp, COLOR('BOLD') % COLOR('RED') %  frm, txt), t)

    def typing_event(self, content):
        etp = ETP(content)
        etp.require_tag('to')
        etp.require_tag('from')
        etp.require_tag('is-typing')

        frm = etp.get('from').text
        c = CHAT_TAB % frm
        if not cmanager['screen'].has_tab(c):
            return
        dflt = Tab.DECORATIONS.get(cmanager['screen'].tabs[c].type, '%s')
        dec = (Tab.CHAT_DECORATIONS['typing'] % dflt if etp.get('is-typing').text.lower() == 'true' else dflt)
        cmanager['screen'].tabs[c].decoration = dec
        cmanager['screen'].update_tabwin()
        cmanager['screen'].refresh(cmanager['screen'].outwin)

    def message_update(self, content):
        etp = ETP(content)
        etp.require_tag('username')
        etp.require_tag('message')
        uname = etp.get('username').text
        msg = etp.get('message').text
        c = CHAT_TAB % uname

        cmanager['screen'].sendLine('%s has changed their status to: %s' % (uname, msg), (0 if not cmanager['screen'].has_tab(c) else c))
