"""
Component used to manage the NC Py-Client logger.
"""

componentclass = "Logger"
depends = ()

from sys import stdout

from twisted.python import log
from weakref import WeakKeyDictionary

from netclient.cmanager import cmanager
from netclient.extensibles import Component

class Logger(Component):
    """
    Actual Component, wraps around twisted.python.log.
    """

    def startLogging(self):
        """
        Initializes twin logs - one to a file for later review, and one to the
        curses log window.
        """
        
        fact = cmanager.get_proxy('factory')
        log.startLogging(fact.opts['log'], setStdout=False)
        log.startLogging(cmanager['screen'], setStdout=False)

    def refreshConfig(self):
        pass

    def msg(self, *message, **kw):
        return log.msg(*message, **kw)

    def err(self, _failure=None, **kw):
        return log.err(_failure, **kw)

    def get_context(self, system):
        return LogContext(system)

class LogContext(object):
    """
    What actually does the work. Stores a context based on what uses
    the logger, and displays this with the log itself.
    """

    __slots__ = ("logger", "system", "echo")
    def __init__(self, system):
        self.system = system
        self.logger = cmanager.get_proxy('logger')

    def msg(self, message, **kw):
        if kw:
            message = '%s [%s]' % (message, str.join(' ; ', ('%s=%s' % item for item in kw.iteritems())))
        self.logger.msg(message, system=self.system)

    def log(self, message, **kw):
        if not cmanager['factory'].opts['verbose']:
            return
        self.msg(message, **kw)
        # TODO: Kwargs

    def err(self):
        self.logger.err(system=self.system)
