from netclient.cmanager import cmanager
from netclient.mmanager import mmanager
from netclient.decorators import command
from netclient.extensibles import Plugin
from netclient.settings import NO_ARG, GCHAT_TAB, CHAT_TAB

name = 'Chat Commands'
p_class = 'Chat'
depends = ()

class Chat(Plugin):
    """
    Chat/Instant messaging-related commands.
    """

    aliases = {
        'im': 'instantmessage',
        'msg': 'instantmessage',
        'groupchat': 'subscribe',
        'gc': 'subscribe',
        'fl': 'getfriends',
        'rl': 'getrooms',
        'bl': 'getblocked',
        'qr': 'queryroom',
        'friend': 'addfriend',
        'unfriend': 'remfriend',
    }

    @command('Must specify an addressee and a message (optional).')
    def instantmessage(self, context, event):
        """
        Sends an NCChat message to the addressee.
        """
        args = event.args.strip().split(' ', 1)
        if len(args) is 2:
            mmanager['chat'].write(args[0], args[1])
        else:
            n = CHAT_TAB % args[0]

            if cmanager['screen'].has_tab(n):
                cmanager['screen'].tab_to(n)
            else:
                cmanager['screen'].new_tab(n, args[0], 'onetoone', True)

    @command('Must specify a room to subscribe to.', type='group')
    def subscribe(self, context, event):
        n = GCHAT_TAB % event.args

        if cmanager['screen'].has_tab(n):
            cmanager['screen'].tab_to(n)
        else:
            cmanager['screen'].new_tab(n, event.args, 'group', True)
            mmanager['chat'].subscribe(event.args)

    @command(NO_ARG)
    def getfriends(self, context, event):
        """
        Sends a request to retrieve your friends list from the server.
        """
        mmanager['chat'].request_friends(True)

    @command(NO_ARG)
    def getblocked(self, context, event):
        """
        Sends a request to retrieve your blocked list from the server.
        """
        mmanager['chat'].request_blocked()

    @command(NO_ARG)
    def getrooms(self, context, event):
        """
        Sends a request to retrieve the rooms list from the server.
        """
        mmanager['chat'].request_rooms()

    @command(type='group')
    def queryroom(self, context, event):
        """
        Sends a query to the server for information regarding a room.
        """
        if event.args:
            n = event.args
        else:
            t = cmanager['screen'].tabs[cmanager['screen'].top_tab]
            if t.type == 'group':
                n = t.shortname
            else:
                cmanager['screen'].sendLine('You must specify a room to query.')
                return
        mmanager['chat'].query_room(n)

    @command('Must specify a username to add.')
    def addfriend(self, context, event):
        mmanager['chat'].add_friend(event.args.strip())

    @command('Must specify a friend to remove.')
    def remfriend(self, context, event):
        mmanager['chat'].rem_friend(event.args.strip())

    @command('Must specify a user to block.')
    def block(self, context, event):
        mmanager['chat'].block(event.args.strip())

    @command('Must specify a user to unblock.')
    def unblock(self, context, event):
        mmanager['chat'].unblock(event.args.strip())

    @command()
    def setstatus(self, context, event):
        mmanager['chat'].set_status(event.args.strip())
