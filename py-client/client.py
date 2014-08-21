#! /usr/local/bin/python2.5

"""
The Py-Client serves as a lightweight altenative to the graphical
NetChat J-Client. Using curses to manage input and output, it is 
entirely terminal-based, and can be configured to run to taste via
command line arguments.
"""

import os.path
import signal
import sys

from twisted.internet import reactor
from twisted.internet.protocol import Protocol, ClientFactory
from twisted.python.usage import UsageError

from netclient.connection import XMLComm, Terminal
from netclient.cmanager import cmanager
from netclient.io import CursesStdIO
from netclient.mmanager import mmanager
from netclient.opts import Options
from netclient.settings import COMPONENTS, MODULES, HOST, PORT, LOG

TERMINATED = 'Unknown.'
"""
Determines the string to be displayed when a connection is closed.
Can be replaced by Twisted if a specific reason is given.
"""

class Client(ClientFactory):
    """
    The twisted ClientFactory for Py-Client.
    """

    protocol = XMLComm

    def __init__(self, opts):
        signal.signal(signal.SIGINT, self._sigint)
        self.opts = opts
        cmanager.add_component('factory', self)
        cmanager.load_component('config')
        self.config = cmanager.get_proxy('config')
        cmanager.load_component('logger')
        cmanager.logger = cmanager['logger'].get_context('cmanager')
        mmanager.logger = cmanager['logger'].get_context('mmanager')
        self.config.logger = cmanager['logger'].get_context('config')
        self.logger = cmanager['logger'].get_context('factory')
        self.exiting = False

    def _sigint(self, sig, frame):
        self.exit('Received SIGINT.')

    def startedConnecting(self, connector):
        h, p = connector.host, connector.port
        self.logger.log('Attemping to connect to port %d at host \'%s\'.' % (p, h))

    def buildProtocol(self, addr):
        self.logger.msg('Establishing connection...')
        p = self.protocol(addr)
        p.factory = self
        return p

    def clientConnectionLost(self, connector, reason):
        self.logger.msg('Connection lost. Shutting down.')
        reactor.callLater(0.01, self.exit, 'Lost connection.')

    def clientConnectionFailed(self, connector, reason):
        self.logger.log('Failed connection.')
        self.exit('Connection failed. Check your network connection or try again later.')

    def exit(self, reason=TERMINATED):
        """
        To be called whenever a shutdown is required.
        """

        global TERMINATED

        if self.exiting:
            return
        self.exiting = True

        if reason:
            TERMINATED = reason

        if 'xmlcomm' in cmanager:
            cmanager['xmlcomm'].drop()
        self.logger.log('Received stop event. %s' % reason)
        reactor.callLater(0.01, reactor.stop)

def main():
    """
    Main driver function for Py-Client. Handles opt-parsing and
    reactor initialization.
    """

    global TERMINATED

    options = Options()
    try:
        options.parseOptions()
    except UsageError, text:
        print '%s: %s' % (sys.argv[0], text)
        print '%s: Try --help for usage details.' % sys.argv[0]
        sys.exit(1)

    options['pingpong'] = not options['no-pingpong']
    options['ssl'] = not options['no-ssl']
    options['color'] = not options['no-color']

    # TODO: Append/Multiple Logs
    if isinstance(options['log'], str):
        options['log'] = file(options['log'], 'w')
    try:
        options['port'] = int(options['port'])
    except ValueError:
        print '%s: Non-int value specified for port.' % sys.argv[0]
        sys.exit(1)

    factory = Client(options)
    sysin = Terminal()
    screen = CursesStdIO(sysin)
    reactor.addReader(screen)
    cmanager['logger'].startLogging()

    s = factory.config.find('sets')

    for set in s['default_sets']:
        if not set in s['valid_sets']:
            raise KeyError, 'All default module-command pairs must be in the valid set list.'

    cmanager.load_components(factory.config.find('components', 'default_components'))

    if options['ssl']:
        from twisted.internet.ssl import DefaultOpenSSLContextFactory
        from netclient.settings import PRIVKEY, CERTIFICATE

        ssl = DefaultOpenSSLContextFactory(PRIVKEY, CERTIFICATE)
        reactor.connectSSL(options['host'], options['port'], factory, ssl)
    else:
        reactor.connectTCP(options['host'], options['port'], factory)
    
    if options['debug']:
        factory.logger.msg('Loading test command suite.')
        cmanager['commands'].loadModule('test')

    reactor.run()

    cmanager.close()
    screen.end()
    print 'Connection lost.'
    print '\tReason: %s' % TERMINATED
    print 'Thank you for using Py-Client. :)'

if __name__ == '__main__':
    main()
