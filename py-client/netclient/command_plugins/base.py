from netclient.cmanager import cmanager
from netclient.decorators import command
from netclient.extensibles import Plugin
from netclient.mmanager import mmanager
from netclient.settings import NO_ARG

name = 'Base Commands'
p_class = 'Base'
depends = ()

class Base(Plugin):
    """
    Basic commands.
    """

    aliases = {
        'reconnect': 'connect',
        'dc': 'disconnect',
        'quit': 'exit',
        'shutdown': 'exit',
    }

    @command(NO_ARG, offline=True)
    def connect(self, context, event):
        """
        Connects the client to the server is in offline mode, or reconnects if online.
        """
        # TODO: Hostname? Port? Username?
        mmanager['login'].reconnect()
        return 'noparser'

    @command(NO_ARG)
    def disconnect(self, context, event):
        mmanager['login'].disconnect()

    @command(offline=True)
    def exit(self, context, event):
        """
        Shuts down the client with a message as needed.
        """
        cmanager['factory'].exit(event.args if event.args else 'Client quit.')
