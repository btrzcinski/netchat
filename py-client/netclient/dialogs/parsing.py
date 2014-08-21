"""
The main command parser.
"""

from twisted.internet import reactor

from netclient.cmanager import cmanager
from netclient.dialogs.basic import LineDialog
from netclient import settings, util

class CommandParser(LineDialog):
    """
    A crucial Dialog that takes raw data from the client
    and breaks it into lines for parsing.
    """

    def __init__(self, tab):
        LineDialog.__init__(self, tab, settings.CMD_PROMPT, True)
    
    def connectionMade(self):
        LineDialog.connectionMade(self)
        cmanager['screen'].refresh()

    def lineReceived(self, line):
        # XXX _execute? needs to be renamed
        flag = (False if cmanager['screen'].tab_ind else True)

        if line.startswith('/'):
            line = line[1:]
            flag = True

        if cmanager['screen'].tab_ind != 0 and not flag:
            self.tab.handle(line)

        if flag:
            self.tab.layer_dialog('dialog')
            flag = cmanager['commands']._execute(line)
            if flag != 'noparser':
                reactor.callLater(0.1, self.tab.pop_dialog)
