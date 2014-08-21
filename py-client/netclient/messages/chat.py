"""
Messages that pertain to the NCChat Module.
"""

from xml.etree.ElementTree import Element, SubElement

from netclient.cmanager import cmanager
from netclient.messages.data import MMessage

TYPE = 'chat'

class NCChat(MMessage):
    """
    Module Message for sending an instant message.
    """

    def __init__(self, target, message, mprops):
        ((mtype,),) = mprops

        cont = Element('content')

        mattrs = {
            'type': mtype,
        }
        proptag = SubElement(cont, 'properties', mattrs)

        nam = SubElement(cont, ('username' if mattrs['type'] == 'private' else 'room'))
        nam.text = target
        mes = SubElement(cont, 'message')
        mes.text = message
        props = {
            'type': 'message',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCFriendsReq(MMessage):
    """
    Module Message for requesting an NCChat Friends' List.
    """

    def __init__(self):
        props = {
            'type': 'friends_list_request',
        }
        MMessage.__init__(self, TYPE, properties=props)

class NCAddFriend(MMessage):
    """
    Module Message for requesting to add a friend.
    """

    def __init__(self, uname):
        cont = Element('content')
        nam = SubElement(cont, 'username')
        nam.text = uname
        props = {
            'type': 'request_add_friend',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCRemFriend(MMessage):
    """
    Module Message for requesting to remove a friend.
    """

    def __init__(self, uname):
        cont = Element('content')
        nam = SubElement(cont, 'username')
        nam.text = uname
        props = {
            'type': 'remove_friend',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCBacklogReq(MMessage):
    """
    Module Message to request a chat backlog.
    """

    def __init__(self):
        props = {
            'type': 'backlog_request',
        }
        MMessage.__init__(self, TYPE, properties=props)

class NCRoomSubscribe(MMessage):
    """
    Module Message to subscribe to an NCChat Room.
    """

    def __init__(self, room):
        cont = Element('content')
        rm = SubElement(cont, 'room')
        rm.text = room

        props = {
            'type': 'room_subscribe',
        }

        MMessage.__init__(self, TYPE, cont, props)

class NCRoomUnsubscribe(MMessage):
    """
    Module Message to unsubscribe from an NCChat Room.
    """

    def __init__(self, room):
        cont = Element('content')
        rm = SubElement(cont, 'room')
        rm.text = room

        props = {
            'type': 'room_unsubscribe',
        }

        MMessage.__init__(self, TYPE, cont, props)

class NCRoomQuery(MMessage):
    """
    Module Message to query the server about the status of
    an NCChat Room.
    """

    def __init__(self, room):
        cont = Element('content')
        rm = SubElement(cont, 'room')
        rm.text = room

        props = {
            'type': 'room_query',
        }

        MMessage.__init__(self, TYPE, cont, props)

class NCRoomsReq(MMessage):
    """
    Module Message to query the server for a list of existing rooms.
    """

    def __init__(self):
        props = {
            'type': 'room_list_request',
        }
        MMessage.__init__(self, TYPE, properties=props)

class NCBlockListReq(MMessage):
    """
    Module Message to query the server for a list of blocked users.
    """

    def __init__(self):
        props = {
            'type': 'block_list_request',
        }
        MMessage.__init__(self, TYPE, properties=props)

class NCRemoveBlock(MMessage):
    """
    Module Message to ask the server to unblock a user.
    """

    def __init__(self, user):
        cont = Element('content')
        uname = SubElement(cont, 'username')
        uname.text = user
        
        props = {
            'type': 'remove_block',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCRequestBlock(MMessage):
    """
    Module Message to ask the server to block a user.
    """

    def __init__(self, user):
        cont = Element('content')
        uname = SubElement(cont, 'username')
        uname.text = user

        props = {
            'type': 'request_block_user',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCTypingEvent(MMessage):
    """
    Module Message to inform the server that the user has
    started typing.
    """

    def __init__(self, to, frm, istyp):
        cont = Element('content')
        
        totag = SubElement(cont, 'to')
        totag.text = to

        frmtag = SubElement(cont, 'from')
        frmtag.text = frm

        typtag = SubElement(cont, 'is-typing')
        typtag.text = istyp

        props = {
            'type': 'typing_event',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCCSetMessage(MMessage):
    """
    Module Message for informing the server of a chat status change.
    """

    def __init__(self, stat):
        cont = Element('content')

        stattag = SubElement(cont, 'message')
        stattag.text = stat

        props = {
            'type': 'set_message',
        }
        MMessage.__init__(self, TYPE, cont, props)
