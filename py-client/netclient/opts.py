from sys import exit

from twisted.python import usage

from netclient.settings import LOG, HOST, PORT, PWORD_HASH

class Options(usage.Options):
    """
    An extension of Twisted Options, used for configuring
    the NC Py-Client's command line opts.
    """

    optFlags = [
        ['debug', 'd', 'Enables the debug command module.'],
        ['verbose', 'v', 'Enables verbose logging.'],
        ['saved-password', 's', 'Uses the previous login information.'],
        ['no-color', 'n', 'Disables curses coloring.'],
        ['no-pingpong', None, 'Disables the ping-pong handshake.'],
        ['no-ssl', None, 'Forces a non SSL connection.'],
    ]

    optParameters = [
        ['host', 'H', HOST, 'Overrides the host.'],
        ['port', 'P', PORT, 'Overrides the port.'],
        ['log', 'l', LOG, 'Overrides the log file.'],
        ['hash', None, PWORD_HASH, 'Overrides the password hashing algorithm.'],
        ['username', 'u', None, 'Sets the username for login.'],
        ['password', 'p', None, 'Sets the password for login.']
    ]

    def postOptions(self):
        if (self['password'] or self['saved-password']) and not self['username']:
            print 'Cannot set password without username.'
            exit()
        if self['password'] and self['saved-password']:
            print 'Cannot set password and simultaneously used saved one.'
            exit()
