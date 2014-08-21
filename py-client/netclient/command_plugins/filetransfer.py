import os

from netclient.cmanager import cmanager
from netclient.mmanager import mmanager
from netclient.decorators import command
from netclient.extensibles import Plugin
from netclient.settings import NO_ARG

name = 'File Transfer Commands'
p_class = 'FileTransfer'
depends = ()

class FileTransfer(Plugin):
    """
    File transfer-related commands.
    """

    aliases = {
        'sf': 'sendfile',
        'kt': 'killtransfer',
    }

    @command('Must specify an addressee and a file.')
    def sendfile(self, content, event):
        """
        Attempts to initiate a file transfer with the addressee.
        """
        args = event.args.strip().split(' ', 1)
        if len(args) is not 2:
            cmanager['screen'].sendLine('Must have two args -- addressee and filename.'), 0
            return
        p = os.path.abspath(os.path.expanduser(args[1]))
        if not os.path.exists(p):
            cmanager['screen'].sendLine('Specified file path does not exist.', 0)
            return
        if not os.path.isfile(p):
            cmanager['screen'].sendLine('The specified path does not point to a file.', 0)
            return
        mmanager['filetransfer'].send(args[0], p)

    @command('Must specify an ID# to kill.')
    def killtransfer(self, context, event):
        """
        Attempts to prematurely end a file transfer with a given ID number.
        """
        t = mmanager['filetransfer'].in_progress.get(event.args.strip())
        if not t:
            cmanager['screen'].sendLine('Specified file transfer does not exist.', 0)
            return
        t.finish(False)

