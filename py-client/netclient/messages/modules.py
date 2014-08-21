"""
Messages that pertain to the modular aspect of NetChat.
"""

from xml.etree.ElementTree import Element, SubElement

from netclient.messages.data import SMessage

class NCMRequest(SMessage):
    """
    Server Message for requesting a Module loading authorization.
    """

    def __init__(self, name, version):
        cont = Element('content')
        nam = SubElement(cont, 'name')
        nam.text = name
        ver = SubElement(cont, 'protocol-version')
        ver.text = version
        SMessage.__init__(self, 'module_request', cont)

class NCMRemove(SMessage):
    """
    Server Message to inform the server that a Module has been unloaded.
    """

    def __init__(self, name, version):
        cont = Element('content')
        nam = SubElement(cont, 'name')
        nam.text = name
        ver = SubElement(cont, 'protocol-version')
        ver.text = version
        SMessage.__init__(self, 'module_unload', cont)
