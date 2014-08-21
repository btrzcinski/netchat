"""
The NCLogin Module. Handles username/password authorization,
registration of new accounts, and anything pertaining to the
initial login sequence.
"""

import hashlib

from twisted.internet import reactor

from netclient.lib.etreeparser import ETreeParser

from netclient.mmanager import mmanager
from netclient.cmanager import cmanager
from netclient.extensibles import Module
from netclient.messages.login import NCLogin, NCLogout

name = 'Login'
version = '0.1a'
moduleclass = 'Login'

class Login(Module):
    def __init__(self):
        Module.__init__(self)
        self.online = False
        self.username = ''
        self.logger = cmanager['logger'].get_context('login')

    def open(self):
        Module.open(self)
        cmanager['screen'].tabs[cmanager['screen'].top_tab].set_dialog('dialog')
        u = cmanager['factory'].opts['username']
        p = cmanager['factory'].opts['password']
        s = cmanager['factory'].opts['saved-password']
        if p:
            self.connect(u, p)
        elif s:
            p = cmanager['data']['password']
            cmanager['factory'].opts['hash'] = cmanager['data']['hash']
            if not (p and cmanager['data']['hash']):
                cmanager['factory'].exit('Invalid data file.')
            if u != cmanager['data']['username']:
                cmanager['factory'].exit('Invalid credentials.')
                return
            self.connect(u, p, False)
        else:
            cmanager['screen'].tabs[cmanager['screen'].top_tab].set_dialog('login', u)

    def connect(self, name, password, usehash=True):
        hash = cmanager['factory'].opts['hash']
        if usehash:
            if hash != 'none':
                try:
                    if hash.startswith('_') or hash == 'new':
                        raise ValueError
                    func = getattr(hashlib, hash)
                except (AttributeError, ValueError):
                    cmanager['factory'].exit('Invalid hash module (%s).' % hash)
                    return
                try:
                    password = func(password).hexdigest()
                except AttributeError:
                    cmanager['factory'].exit('Module has no valid hash functions (%s)' % hash)

        connection = NCLogin(name, password, hash)
        self.logger.log('Attempting login.')
        cmanager['xmlcomm'].sendLine(connection)
        cmanager['data']['username'] = name
        cmanager['data']['hash'] = hash
        cmanager['data']['password'] = password

    def reconnect(self):
        if self.online:
            self.disconnect()
            self.logger.log('Reconnecting...')
        mmanager.clear()
        cmanager['factory'].opts['username'] = None
        cmanager['factory'].opts['password'] = None
        cmanager['factory'].opts['saved-password'] = None
        mmanager.queue_module('login')

    def disconnect(self):
        self.logger.log('Disconnecting...')
        cmanager['xmlcomm'].sendLine(NCLogout())
        self.online = False
        for t in cmanager['screen'].tab_names[1:]:
            cmanager['screen'].kill_tab(t)

    def accept_login(self, content):
        self.logger.log('Login accepted.')
        self.online = True
        
        etp = ETreeParser(content)
        etp.require_tag('username')
        self.username = etp.get('username').text
        
        mmanager.queue_modules(cmanager['config'].find('modules', 'default_modules') + cmanager['config'].find('sets', 'default_sets'))
        cmanager['screen'].tabs[cmanager['screen'].top_tab].set_dialog('parser')

    def reject_login(self, content):
        cmanager['screen'].sendLine('Login refused.%s' % ('\n\r\tReason: %s' % content.text if content.text else ''), 0)
        self.reconnect()
