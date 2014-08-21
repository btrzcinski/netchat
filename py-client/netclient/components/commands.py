"""
Component for parsing command line instructions.
"""

componentclass = 'Commander'
depends = ('config',)

import sys

from netclient.extensibles import Component, Plugin
from netclient.settings import P_ARG
from netclient.util import get_commands, dynamicLoad 
from netclient.lib.comparison import bin_search
from netclient.cmanager import cmanager
from netclient.mmanager import mmanager

PLUGIN_BASE = 'command_plugins'

MODULE, INSTANCE, BUILD = 0, 1, 2

class CommandEvent(object):
    """
    Var container for specific dispatched commands.
    """
    
    __slots__ = ('command', 'args')

    def __init__(self, command, args):
        self.command = command
        self.args = args

class CommandContext:
    """
    Var container for all commands.
    """
    
    def __init__(self, commander):
        self.factory = cmanager.get_proxy('factory')
        self.command = commander
        self.MODULE, self.INSTANCE, self.BUILD = MODULE, INSTANCE, BUILD

    def dispatch(self, command, *args):
        """
        Shortcut for dispatching a command to the Commander.
        """

        return self.commander.dispatch(command, str.join(' ', args))

class Commander(Component):
    """
    Command parsing Component.
    """

    def __init__(self):
        Component.__init__(self)
        self.modules = {}
        self.underlying_modules = []
        self.commands = {}
        self.logger = cmanager['logger'].get_context('commander')
        self.context = CommandContext(self)
        self.string_list = []
        self.refreshConfig()

    def getdmodules(self):
        return cmanager['config'].find('commands', 'plugins')

    def refreshConfig(self):
        self.commands.clear()
        self.modules.clear()
        dmodules = self.getdmodules()
        if dmodules:
            map(self.loadModule, dmodules)
        if self.underlying_modules:
            map(self.loadModule, self.underlying_modules)
        
    def rem_module(self, name):
        """
        Removes a command Plugin by name.
        """

        self.logger.msg('Removing command plugin \'%s\'...' % name)
        try:
            dmodules = self.getdmodules()
            if name in dmodules:
                self.logger.msg('Module \'%s\' is a default plugin, cannot remove.' % name)
            self.underlying_modules.remove(name)
            clist = self.modules.get(name, None)
            clist = clist[BUILD] if clist else set([])
            for c in clist:
                if c in self.commands:
                    del self.commands[c]
                    self.logger.log('*Deleting command \'%s\'.' % c)
            self.update_list()
            self.logger.msg('...Successfully removed command plugin.')
            return True
        except ValueError:
            self.logger.msg('Unable to remove command plugin \'%s\'.' % name)
            return False

    def update_list(self):
        """
        Rebuilds the internal list of commands.
        """


        self.string_list = self.commands.keys()
        self.string_list.sort()
        self.num = len(self.string_list)

    def loadModule(self, name):
        """
        Loads a command Plugin by name.
        """
        
        self.logger.msg('Loading command plugin \'%s\'...' % name)

        c = cmanager.get_proxy('config')
        if name in c.find('sets', 'valid_sets'):
            if not name in mmanager:
                self.logger.log('Command plugin \'%s\' requires loaded \'%s\' module.' % (name, name))
                try:
                    self.underlying_modules.remove(name)
                    self.logger.log('Removed from underlying modules.')
                except ValueError:
                    pass
                return False

        nls = {}

        ls = self.modules.get(name, None)
        build = ls[BUILD] if ls else set([])

        module = dynamicLoad(PLUGIN_BASE, name)
        if not module:
            self.logger.msg('Command plugin \'%s\' not found.' % name)
            return False
        reload(module)
       
        nls[MODULE] = module

        mclass = getattr(module, 'p_class', None)
        if not mclass:
            self.logger.msg('Command plugin \'%s\' has no Plugin specified.' % name)
            return False

        mclass = getattr(module, mclass, None)
        if not mclass:
            self.logger.msg('Command plugin \'%s\' has no Plugin class.' % name)
            return False

        if not issubclass(mclass, Plugin):
            self.logger.msg('Plugin class of \'%s\' must extend Plugin.' % name)
            return False

        cinst = mclass(self)
        nls[INSTANCE] = cinst
        nls_b = get_commands(cinst)
        excludes = build.difference(nls_b)
        for e in excludes:
            if e in self.commands:
                self.logger.log('*Removing deprecated command \'%s\'.' % e)
                del self.commands[e]

        dmodules = self.getdmodules()
        if not name in (self.underlying_modules + dmodules):
            self.underlying_modules.append(name)

        try:
            nls[BUILD] = set([])
            self.modules[name] = nls
            self.autoRegister(cinst, nls_b, name)
            self.update_list()
            self.logger.msg('...Successfully loaded command plugin.')
            return True
        except:
            self.logger.msg('Failed to load command plugin \'%s\'.' % name)
            return False

    def autoRegister(self, plugin, ls, name):
        """
        Parses all commands in a given Plugin.
        """

        for attribute in ls:
            obj = getattr(plugin, attribute)
            self.registerCommand(attribute, obj, name)

    def registerCommand(self, name, func, pname):
        """
        Hooks a callback to a command.
        """
        
        self.logger.log('*Registering command \'%s\'.' % name)
        self.commands[name.lower()] = func
        self.modules[pname][BUILD].add(name)

    def _execute(self, command):
        fractured = command.split(' ', 1)
        command = fractured[0]
        if not command:
            return
        args = len(fractured) == 2 and fractured[1] or ''

        fullcmd = self._locatecmd(command)
        if not fullcmd:
            cmanager['screen'].sendLine('Invalid command.', 0)
            return
        return self.dispatch(fullcmd, args)

    def dispatch(self, command, args):
        """
        Tells the Commander to execute a command.
        """
        
        func = self.commands[command]
        rargs = func.args
        if not (rargs is P_ARG):
            msg = func.msg
            f1 = rargs and not args
            f2 = args and not rargs
            if f1 or f2:
                cmanager['screen'].sendLine(msg, 0)
                return
        if not (func.offline or mmanager['login'].online):
            cmanager['screen'].sendLine('This command is not available in offline mode.', 0)
            return
        event = CommandEvent(command, args)
        return func(self.context, event)

    def _locatecmd(self, command):
        origcom = command
        ind = bin_search(self.string_list, command)
        if command == origcom:
            if ind == -1:
                return
            word = self.string_list[ind]
        else:
            word = command
        return word
