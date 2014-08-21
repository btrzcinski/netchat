from twisted.internet import reactor, task
from twisted.protocols.basic import LineReceiver, LineOnlyReceiver

from netclient.cmanager import cmanager
from netclient.mmanager import mmanager
from netclient.dialogs.parsing import CommandParser
from netclient.dialogs.basic import Dialog
from netclient.dialogs.login import Login
from netclient.dialogs.prompts import MultiPrompt
from netclient.exceptions import NCPError
from netclient import parser, settings
from netclient.messages.data import SMessage

class XMLComm(LineOnlyReceiver):
    """
    Twisted LineOnlyReceiver used to communicate via the NetChat Protocol
    with a remote NetChat server.
    """

    delimiter = '\n'

    def __init__(self, addr):
        cmanager.add_component('xmlcomm', self)
        self.address = addr
        self.logger = cmanager['logger'].get_context('xmlcomm')

    def lineLengthExceeded(self, line):
        LineOnlyReceiver.lineLengthExceeded(self, line)

    def lineReceived(self, line):
        try:
            parser.parse(line, cmanager['terminal'], self)
        except NCPError, xml:
            cmanager['factory'].exit('\n\r\tNCPError: %s' % xml)

    def sendLine(self, line):
        LineOnlyReceiver.sendLine(self, str(line))

    def connectionMade(self):
        sa = self.factory.opts['pingpong']
        self.logger.msg('Connection successful.')
        
        mmanager.queue_module('login')

        if sa:
            self.pptask = task.LoopingCall(self.pingpong)
            reactor.callLater(.5, self.pptask.start, 30)

    def reconnect(self):
        mmanager['login'].disconnect()
        self.logger.msg('Reconnecting...')
        mmanager.clear()
        mmanager.queue_modules(cmanager['config'].find('modules', 'default_modules') + cmanager['config'].find('sets', 'default_sets'))
        cmanager['terminal'].set_dialog('dialog')
        mmanager.load_module('login')
        mmanager['login'].connect(None, None)

    def pingpong(self):
        """
        Callback for sending a stay-alive pingpong message.
        """

        self.sendLine(SMessage('ping'))

    def drop(self):
        if 'login' in mmanager:
            mmanager['login'].disconnect()
        self.transport.loseConnection()

# TODO: Even more basic? Make it a Protocol?
class Terminal(LineReceiver):
    """
    Twisted LineReceiver used to receive text-based terminal input from the user.
    """

    def __init__(self):
        self.setRawMode()
        cmanager.add_component('terminal', self)
        self.logger = cmanager.get_proxy('logger').get_context('terminal')
        
        self.DIALOGS = {
            'dialog': Dialog,
            'parser': CommandParser,
            'login': Login,
            'prompt': MultiPrompt,
        }

    def dataReceived(self, data):
        return cmanager['screen'].tabs[cmanager['screen'].top_tab].dialog.dataReceived(data)

    def sendLine(self, line):
        cmanager['screen'].sendLine(line, 0)

    def connectionMade(self):
        self.logger.log('Connection established to input stream.')
        cmanager['screen'].tabs[cmanager['screen'].top_tab].set_dialog('dialog')

    def drop(self):
        self.transport.loseConnection()
