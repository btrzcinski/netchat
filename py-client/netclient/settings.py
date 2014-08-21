"""
Configurable var data for the NC Py-Client.
"""

import curses, os, re

# -- Status --

APP_NAME = 'NetChat Py-Client'
APP_VERSION = '2.1b'

# -- Connection Settings --

HOST = 'netchat.tjhsst.edu'
PORT = 45287

PWORD_HASH = 'sha512'

# -- Paths --

LOG = 'nc.log'

FILES = 'files'
DATA = 'data'
NETCLIENT_CONF = os.path.join(DATA, 'netclient.conf')
NETCLIENT_DATA = os.path.join(DATA, 'netclient.dat')

SSL = 'ssl'
CERTIFICATE = os.path.join(SSL, 'cacert.pem')
PRIVKEY = os.path.join(SSL, 'privkey.pem')

COMPONENTS = 'components'
MODULES = 'modules'

# -- NCP Settings --

MMESSAGE = 'modulemessage'
SMESSAGE = 'servermessage'
MKEYPAIRS = {
    MMESSAGE: 'modulemessage',
    SMESSAGE: 'servermessage',
}
SKEYPAIRS = {
    MMESSAGE: 'name',
    SMESSAGE: 'type',
}

# -- Other Settings --

CMD_PROMPT = '> '

CMD_ATT = 'is_command'

ARG_ERRORS = [
    'Syntax error: %s',
    'Arguments required.',
    'Command takes no arguments.',
]

ARG, NO_ARG, P_ARG = (True, False, None)

V_WRAP = '- %s'

# - Chat formatting -

# Month/day/year/hour/min/sec
COLOR = lambda s: '|c%s|%s|x%s|' % (s.upper(), '%s', s.upper())
#CHAT_TIMESTAMP = '%s%s%s%s%s %s%s%s%s%s' % ('%02d', __bold % '-', '%02d', __bold % '-', '%d', '%02d', __bold % ':', '%02d', __bold % ':', '%02d')
CHAT_TIMESTAMP = '%02d-%02d-%d %02d:%02d:%02d'
# Timestamp/from/message
CHAT_FORMAT = '%s\n\r\t%s %s' % ('%s', COLOR('bold'), '%s')
CHAT_COLORS = map(COLOR, ['RED', 'GREEN', 'YELLOW', 'MAGENTA', 'CYAN'])

# -- Curses Configuration --

WINDOWS = {
    'out': ('Main', 'Main Status Window'),
    'log': 'Log Window',
    'outbuffer': '25x',
    'logbuffer': '25x',
    'inbuffer': '20x',
    'cmdbuffer': '50',
    '%out': 65,
}

SIZE_MODIFIERS = {
    '%': lambda val, mod: int(val*(mod/100.)),
    'x': lambda val, mod: val*mod,
    '+': lambda val, mod: val+mod,
    '-': lambda val, mod: val-mod,
    ' ': lambda val, mod: mod,
}

TRUE_COLORS = ['RED', 'BLUE', 'CYAN', 'YELLOW', 'MAGENTA', 'WHITE', 'GREEN']
COLOR_PAIRS = dict(zip(TRUE_COLORS, range(1, len(TRUE_COLORS)+1)))
VALID_COLORS = TRUE_COLORS + ['BLINK', 'BOLD', 'UNDERLINE', 'DIM', 'STANDOUT']
COLOR_MARKUP = 'cx'
COLOR_EXP = re.compile('\|([%s])(%s)\|'% (COLOR_MARKUP, '|'.join(VALID_COLORS)))

OUT_UP = curses.KEY_PPAGE
OUT_DOWN = curses.KEY_NPAGE

LOG_UP = curses.KEY_HOME
LOG_DOWN = curses.KEY_END

KEY_TABLEFT = 515, 5 # Ctrl + <-, Ctrl + e
KEY_TABRIGHT = 517, 18 # Ctrl + ->, Ctrl + r
KEY_TABPREV = 20 # Ctrl + t
KEY_CLOSETAB = 24 # Ctrl + x
TAB_LEN = 16

# - Tab Names -

CHAT_TAB = 'One to one: %s' # %s -> Username
GCHAT_TAB = 'Multiuser Room: %s' # %s -> Room name
