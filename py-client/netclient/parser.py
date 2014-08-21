"""
Contains all of the server/modulemessage hooks
needed by the NCP parser, as well as the parser
itself.
"""

from xml.etree.ElementTree import XML, tostring
from xml.parsers.expat import ExpatError

from netclient.lib.etreeparser import ETreeParser as ETP

from netclient.cmanager import cmanager
from netclient.exceptions import DialogError, ModuleError, NCPError
from netclient.messages.data import Data, SMessage
from netclient.mmanager import mmanager
from netclient.settings import MMESSAGE, SMESSAGE, MKEYPAIRS, SKEYPAIRS

VMESSAGETYPES = MKEYPAIRS.keys()
VMESSAGES = MKEYPAIRS.values()

def parse(packet, term, xmlc):
    """
    Takes a complete NCP message and parses it, using
    the contained information to either relay the content
    to a hook or Module.
    """
    
    global logger

    # TODO: Implement into ETP
    opts = {
        MMESSAGE: {
            'data': print_data
        },
        SMESSAGE: {
            'data': print_data,
            'ping': s_ping,
            'pong': s_pong,
            'authorize_module': s_auth_mod,
            'deny_module': s_deny_mod,
        },
    }

    try:
        logger
    except NameError:
        logger = cmanager['logger'].get_context('parser')

    try:
        tree = XML(packet)
    except ExpatError:
        term.sendLine('Warning: Malformed XML. Interpreting as data.')
        tree = XML(Data(packet).to_xml())

    # -- Required Tag Elements -- (TODO: Abstract away magical strings?)

    # TODO: Stop NCPError'ing and cleanly exit
    if tree.tag != 'message':
        raise NCPError, 'Received non-message XML packet.'

    treetags = list(iter(tree))

    try:
        head = treetags.pop(0)
        cont = treetags.pop(0)
        if treetags:
            raise IndexError
    except IndexError:
        raise NCPError, 'Received message with wrong # of subelements.'

    if head.tag != 'header' or cont.tag != 'content':
        raise NCPError, 'Received out-of-order or malformed message.'

    # -- Header Tag Elements --

    headtags = list(iter(head))

    try:
        glob = headtags.pop(0)
        if len(headtags) != 1:
            raise IndexError
    except IndexError:
        raise NCPError, 'Received message header with wrong # of subelements.'

    if glob.tag != 'global' or not headtags[0].tag in VMESSAGES:
        raise NCPError, 'Received out-of-order or malformed message header.'

    # -- Break for Properties --

    globtags = list(iter(glob))

    try:
        gprop = globtags.pop(0)
        if globtags:
            raise IndexError
    except IndexError:
        raise NCPError, 'Received message global data with wrong # of subelements.'

    if gprop.tag != 'properties':
        raise NCPError, 'Received malformed message global data.'

    gat = gprop.attrib
    t = gat.pop('type', None)
    if not t:
        raise NCPError, 'Received message with no type.'
    if gat:
        raise NCPError, 'Received message with extraneous global header properties.'
    if not t in VMESSAGETYPES:
        raise NCPError, 'Received message with invalid type.'

    # -- Back to Header --

    gs = headtags.pop(0)

    if gs.tag != MKEYPAIRS[t]:
        raise NCPError, 'Received message with a message type mismatch.'

    # -- Specifics --

    stags = list(iter(gs))
    try:
        sprop = stags.pop(0)
        if stags:
            raise IndexError
    except IndexError:
        raise NCPError, 'Received header subelement with wrong # of subelements.'

    if sprop.tag != 'properties':
        raise NCPError, 'Received malformed header subelement.'
    
    spat = sprop.attrib

    spec = spat.pop(SKEYPAIRS[t], None)
    if not spec:
        raise NCPError, 'Received message with no specific type or name.'
    if t == MMESSAGE:
        mmtyp = spat.pop('type', None)
    if spat:
        raise NCPError, 'Received header subelement with extraneous properties.'
   
    func = opts[t].get(spec, None)
    if not func:
        if t == MMESSAGE:
            # Pass whole tree?
            try:
                mmanager[spec].parse_content(cont, mmtyp)
            except ModuleError:
                mmanager.queue_content(spec, cont, mmtyp)
        else:
            raise NCPError, 'Received server message with no hook.'
    else:
        func(cont, term, xmlc)

def print_data(tree, term, xmlc):
    term.sendLine('Incoming server data: %s' % tree.text)

def s_ping(tree, term, xmlc):
    logger.log('Received ping request.')
    xmlc.sendLine(SMessage('pong'))

def s_pong(tree, term, xmlc):
    pass

def s_auth_mod(tree, term, xmlc):
    nam = 'name'
    ver = 'protocol-version'
    etp = ETP(tree)
    etp.require_tag(nam)
    etp.require_tag(ver)
    nam = etp.get(nam)
    ver = etp.get(ver)

    # TODO: Version
    mmanager.approve_module(nam.text)

def s_deny_mod(tree, term, xmlc):
    nam = 'name'
    ver = 'protocol-version'
    etp = ETP(tree)
    etp.require_tag(nam)
    etp.require_tag(ver)
    nam = etp.get(nam)
    ver = etp.get(ver)

    # TODO: Version
    mmanager.deny_module(nam.text)
