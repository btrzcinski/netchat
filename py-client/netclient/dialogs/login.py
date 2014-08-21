from netclient.dialogs.basic import LineDialog
from netclient.cmanager import cmanager
from netclient.mmanager import mmanager

class Login(LineDialog):
    """
    Handles requesting a username/password from the user.
    """
  
    PROMPTS = [
        'Enter username: ',
        'Enter password: ',
    ]

    def __init__(self, tab, uname=None):
        LineDialog.__init__(self, tab, echo=True)
        self.uname = uname

    def connectionMade(self):
        LineDialog.connectionMade(self)
        ind = (1 if self.uname else 0)
        self.tab.prompt = self.PROMPTS[ind]
        self.tab.show_input = not ind
        cmanager['screen'].refresh()

    def lineReceived(self, line):
        if self.uname is None:
            self.uname = line
            cmanager['screen'].reset(cmanager['screen'].top_tab)
            self.tab.prompt = self.PROMPTS[1]
            self.tab.show_input = False
            cmanager['screen'].refresh()
        else:
            mmanager['login'].connect(self.uname, line)
            self.tab.set_dialog('dialog')

    def close(self):
        cmanager['screen'].clear_history()
