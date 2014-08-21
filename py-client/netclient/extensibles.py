"""
Base classes used as templates for important
aspects of the NC Py-Client.
"""

class Plugin(object):
    """
    A command Plugin serves as a categorical grouping of scripted
    commands.
    """

    aliases = {}

    def __init__(self, commander):
        self.commander = commander
        self.__init_aliases()

    def __init_aliases(self):
        for k, v in self.aliases.iteritems():
            func = getattr(self, v, None)
            if not func:
                raise ValueError
            setattr(self, k, func)

class Component(object):
    """
    A Component used by the netclient.cmanager.ComponentManager.
    """
    
    def __init__(self):
        self.opened = False

    def open(self):
        self.opened = True

    def close(self):
        self.opened = False

class Module(Component):
    """
    A Module used by the netclient.mmanager.ModuleManager.
    """

    def parse_content(self, cont, typ):
        """
        Given a Module command and the content of an NCP message,
        the Module will react in a specified manner.
        """
        
        return getattr(self, typ)(cont)

    def get_tabbed_list(self, command, intermediate, input, type):
        """
        Called by the curses screen when 'tab' is hit, such that each
        module can implement it differently.

        The arguments can be looked at for context. For instance, if
        get_tabbed_list is called when the current string is:
        
        sendfileto user netclient.ja
        
        sendfile to is the command, user is the intermediate input, and
        netclient.ja is the input to be tabbed. This would let the
        Module know to search for a file, for instance.

        The final argument, "type", is a means of further classifying
        commands within a given Module.
        """

        return []
