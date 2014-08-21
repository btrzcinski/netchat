"""
Messages that pertain to the NCFileTransfer Module.
"""

from xml.etree.ElementTree import Element, SubElement

from netclient.cmanager import cmanager
from netclient.messages.data import MMessage

TYPE = 'filetransfer'

class NCFTransferIDReq(MMessage):
    """
    Module Message for requesting a File Transfer ID from the server.
    """

    def __init__(self):
        props = {
            'type': 'file_transfer_id_request',
        }
        MMessage.__init__(self, TYPE, properties=props)

class NCFTransferReq(MMessage):
    """
    Module Message for requesting permission to send a file.
    """

    def __init__(self, id, to, frm, finfo, comment):
        fname, (bsize,) = finfo

        cont = Element('content')
        idtag = SubElement(cont, 'id')
        idtag.text = id
        totag = SubElement(cont, 'to')
        totag.text = to
        fromtag = SubElement(cont, 'from')
        fromtag.text = frm

        fattrs = {
            'bsize': bsize,
        }
        ftag = SubElement(cont, 'filename', fattrs)
        ftag.text = fname
        
        com = SubElement(cont, 'comment')
        com.text = comment
        props = {
            'type': 'file_transfer_request',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCFTransferDecision(MMessage):
    """
    Module Message for sending a File Transfer decision.
    """

    def __init__(self, id, frm, accept, fname):
        cont = Element('content')
        idtag = SubElement(cont, 'id')
        idtag.text = id
        fromtag = SubElement(cont, 'from')
        fromtag.text = frm
        accepttag = SubElement(cont, 'accept')
        accepttag.text = accept
        filetag = SubElement(cont, 'filename')
        filetag.text = fname
        props = {
            'type': 'file_transfer_decision',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCFileChunkReq(MMessage):
    """
    Module Message requesting a specific chunk of a transferring file.
    """

    def __init__(self, id, route, chunk):
        ((rfrom, rdest),) = route
        ((cnum,),) = chunk

        cont = Element('content')
        idtag = SubElement(cont, 'id')
        idtag.text=id

        rattrs = {
            'from': rfrom,
            'to': rdest,
        }
        routetag = SubElement(cont, 'route', rattrs)

        cattrs = {
            'number': cnum,
        }
        chunktag = SubElement(cont, 'chunk', cattrs)

        props = {
            'type': 'file_chunk_request',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCFileChunk(MMessage):
    """
    Module Message containing a specific chunk of a transferring file.
    """

    def __init__(self, id, route, chunk):
        ((rfrom, rdest),) = route
        chunk, (cnum, maxc, fname, totalbsize) = chunk

        cont = Element('content')
        idtag = SubElement(cont, 'id')
        idtag.text = id

        rattrs = {
            'from': rfrom,
            'to': rdest,
        }
        routetag = SubElement(cont, 'route', rattrs)

        cattrs = {
            'number': cnum,
            'maxchunk': maxc,
            'filename': fname,
            'totalbsize': totalbsize,
        }
        chunktag = SubElement(cont, 'chunk', cattrs)
        chunktag.text = chunk

        props = {
            'type': 'file_chunk',
        }
        MMessage.__init__(self, TYPE, cont, props)

class NCFTransferComplete(MMessage):
    """
    Module message informing the server that a file transfer has
    completed successfully.
    """

    def __init__(self, id, frm, to, success):
        cont = Element('content')
        idtag = SubElement(cont, 'id')
        idtag.text = id
        fromtag = SubElement(cont, 'from')
        fromtag.text = frm
        totag = SubElement(cont, 'to')
        totag.text = to
        successtag = SubElement(cont, 'success')
        successtag.text = success

        props = {
            'type': 'transfer_complete',
        }
        MMessage.__init__(self, TYPE, cont, props)
