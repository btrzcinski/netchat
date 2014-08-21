"""
Module for housing basic dialogs.
"""

from twisted.internet.protocol import Protocol
from twisted.protocols.basic import LineOnlyReceiver

from netclient.cmanager import cmanager

class BaseDialog:
    """
    Base class, to be extended.
    """

    def __init__(self, tab, prompt='', echo=False):
        self.prompt = prompt
        self.echo = echo
        self.tab = tab
        self.config = cmanager.get_proxy('config')
        self.factory = cmanager.get_proxy('factory')

    def connectionMade(self):
        self.tab.prompt = self.prompt
        self.tab.show_input = self.echo

    def dataReceived(self, data):
        raise NotImplementedError

    def close(self):
        """
        Called to cleanly exit a dialog.
        """

        pass

class Dialog(BaseDialog, Protocol):
    """
    A dummy dialog that simply disregards received data.
    """

    def dataReceived(self, data):
        pass

class LineDialog(BaseDialog, LineOnlyReceiver):
    """
    Dialog for when line input is required.
    """
    
    delimiter = '\n'

    def dataReceived(self, data):
        if cmanager['screen'].line_handle(data):
            self.lineReceived(cmanager['screen'].tabs[cmanager['screen'].top_tab].input)
