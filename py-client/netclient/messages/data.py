"""
Base Message classes for use in NCP.
"""

import time

from xml.etree.ElementTree import _ElementInterface, Element, SubElement, tostring

from netclient.exceptions import NCPError
from netclient.settings import MMESSAGE, SMESSAGE, MKEYPAIRS, SKEYPAIRS

class Message:
    """
    A simple Message. Has no functionality, meant to be extended.
    """

    def __init__(self, typ, spec, cont, properties={}):
        if not typ in MKEYPAIRS:
            raise NCPError, 'Invalid type passed to Message constructor.'
        self.type = typ
        self.spec = spec
        self.content = cont
        self.cflag = isinstance(cont, _ElementInterface)
        self.properties = properties

    def __str__(self):
        return self.to_xml()

    def build(self):
        """
        Compiles class data into an ElementTree.
        """

        tree = Element('message', {'id': str(time.time())})
        head = SubElement(tree, 'header')

        glob = SubElement(head, 'global')
        gprop = SubElement(glob, 'properties', type=self.type)

        spec = SubElement(head, MKEYPAIRS[self.type])
        props = {SKEYPAIRS[self.type]: self.spec}
        props.update(self.properties)
        spec2 = SubElement(spec, 'properties', props)

        if not self.cflag:
            cont = SubElement(tree, 'content')
            cont.text = self.content
        else:
            tree.insert(2, self.content)

        return tree

    def to_xml(self):
        return tostring(self.build())

class SMessage(Message):
    """
    A basic Server Message for extension purposes.
    """

    def __init__(self, spec, cont=''):
        Message.__init__(self, SMESSAGE, spec, cont)

class MMessage(Message):
    """
    A basic Module Message for extension purposes.
    """
    
    def __init__(self, spec, cont='', properties={}):
        Message.__init__(self, MMESSAGE, spec, cont, properties)

class Data(Message):
    """
    A simple Message of ambiguous type for transmitting raw data.
    """
    
    def __init__(self, contents, spec='data'):
        Message.__init__(self, 'servermessage', spec, contents)
