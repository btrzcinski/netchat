"""
Messages that pertain to the NCLogin Module.
"""

from xml.etree.ElementTree import Element, SubElement

from netclient.cmanager import cmanager
from netclient.messages.data import MMessage

TYPE = 'login'

class NCLogin(MMessage):
    """
    Module Message for requesting a login authorization.
    """

    def __init__(self, name, password, hash):
        cont = Element('content')
        nam = SubElement(cont, 'username')
        nam.text = name
        pas = SubElement(cont, 'password', {'hash': hash.upper()})
        pas.text = password
        props = {
            'type': 'login_request',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCLogout(MMessage):
    """
    Module Message for informing the server of a terminated connection.
    """

    def __init__(self):
        props = {
            'type': 'logout',
        }
        MMessage.__init__(self, TYPE, properties=props)
