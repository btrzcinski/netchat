import time

from netclient.cmanager import cmanager
from netclient.mmanager import mmanager
from netclient.decorators import command
from netclient.extensibles import Plugin
from netclient.messages.data import SMessage
from netclient.settings import ARG, NO_ARG

name = 'Test Commands'
p_class = 'Test'
depends = ()

class Test(Plugin):
    """
    Test commands for debug purposes.
    """

    @command('Requires a message to send.')
    def txt(self, context, event):
        """
        Sends a plaintext message to the server.
        """
        cmanager['xmlcomm'].sendLine(event.args)

    @command('Requires a message to send.')
    def xml(self, context, event):
        # TODO
        """
        Sends an NCP echo signal to the server.
        """
        data = SMessage('echo', event.args)
        cmanager['xmlcomm'].sendLine(data)

    @command(NO_ARG)
    def ctest(self, context, event):
        """
        Dummy command for testing registration.
        """
        cmanager['xmlcomm'].lineReceived(f.read().strip())

    @command(NO_ARG)
    def testprompt(self, context, event):
        """
        Layers a Prompt Dialog on top of the current one.
        """
        def yes():
            cmanager['screen'].sendLine('Message!', 0)
        def no():
            cmanager['screen'].sendLine('Fine.', 0)
        map = {
            'y': yes,
            'n': no,
        }
        cmanager['screen'].tabs[cmanager['screen'].top_tab].layer_dialog('prompt', 'Print a message (Y/n)?', map, 'y')
        return 'noparser'

    @command(NO_ARG)
    def gentab(self, context, event):
        """
        Creates a new, useless tab.
        """
        ID = str(int(time.time() * 100000))
        name = 'Test Tab %s' % ID
        cmanager['screen'].new_tab(name, 'Test Tab')
        cmanager['screen'].sendLine('This tab has some random text for you!', name)

    @command(NO_ARG)
    def bigim(self, context, event):
        """
        Sends a large instant message to self.
        """
        big = 'ttttttttttttttttttttttttttthhhhhhhhhhhhhhhhhhhhhhiiiiiiiiiiiiiiiiiiiiisssssssssssssssssssssss isssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa teeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeesssssssssssssssssssssssssssssssssssssttttttttttttttttttttttttttttttttt.'
        mmanager['chat'].write(mmanager['login'].username, big)
