from twisted.internet import reactor

from netclient.dialogs.basic import Dialog
from netclient.cmanager import cmanager
from netclient.mmanager import mmanager
from netclient.constants import KEY_NEWLN

class MultiPrompt(Dialog):
    """
    Requests specific input from a user.
    """
  
    def __init__(self, tab, prompt, mapping={}, default=None):
        Dialog.__init__(self, tab, prompt)
        c = chr(KEY_NEWLN)
        if not default:
            default = c
        if not mapping:
            mapping[c] = self.close
        mapping[c] = mapping[default]
        self.mapping = mapping
        self.default = default
        self.blocking = True
        reactor.callLater(3, self.unblock)

    def unblock(self):
        self.blocking = False

    def connectionMade(self):
        Dialog.connectionMade(self)
        cmanager['screen'].refresh()

    def dataReceived(self, data):
        if self.blocking:
            return
        if (data < 32 or data >= 255) and data != KEY_NEWLN:
            return
        data = chr(data).lower()
        func = self.mapping.get(data)
        if not func:
            return
        assert callable(func)
        func()
        self.tab.pop_dialog()
