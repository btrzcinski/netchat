from netclient.cmanager import cmanager
from netclient.mmanager import mmanager
from netclient.decorators import command
from netclient.extensibles import Plugin
from netclient.settings import COLOR, P_ARG, ARG, NO_ARG, ARG_ERRORS
from netclient.util import wrap

name = 'Module Related Commands'
p_class = 'Modularity'
depends = ()

class Modularity(Plugin):
    """
    Commands relating to the modular aspect of the NC Py-Client.
    """

    aliases = {
        'rplugin': 'remplugin',
        'lplugin': 'loadplugin',
        'lset': 'loadset',
        'lmod': 'loadmodule',
        'rmod': 'remmodule',
        'help': 'pluginstatus',
        'pstat': 'pluginstatus',
    }

    @command('Must specify component to reload.', offline=True)
    def reload(self, context, event):
        """
        Reloads a component of the program.
        """
        args = event.args.lower()
        if args in cmanager:
            cmanager['screen'].sendLine('Affirmative.', 0)
            cmanager.refresh_component(args)
        else:
            cmanager['screen'].sendLine('Negative, component fails to exist.', 0)

    @command('Must specify plugin to load.', offline=True)
    def loadplugin(self, context, event):
        """
        Loads and activates a command plugin.
        """
        args = event.args.lower()
        c = context.command
        a = c.loadModule(args)
        line = 'Affirmative.' if a else 'Unable to load plugin.'
        cmanager['screen'].sendLine(line, 0)

    @command('Must specify plugin to remove.', offline=True)
    def remplugin(self, context, event):
        """
        Removes a command plugin from the command list.
        """
        args = event.args.lower()
        c = context.command
        a = c.rem_module(args)
        line = 'Affirmative.' if a else 'Module not loaded...'
        cmanager['screen'].sendLine(line, 0)

    @command('Must specify module set to load.', offline=True)
    def loadset(self, context, event):
        """
        Loads a module-command pair into the system.
        """
        args = event.args.lower()
        c = cmanager.get_proxy('config')
        if not args in c.find('sets', 'valid_sets'):
            cmanager['screen'].sendLine('Set not valid.', 0)
            return
        if args in mmanager:
            cmanager['screen'].sendLine('Set already loaded, reloading command module instead.', 0)
            context.command.load_module(args)
            return
        mmanager.queue_module(args)

    @command('Must specify module to load.', offline=True)
    def loadmodule(self, context, event):
        """
        Loads a module into the system.
        """
        args = event.args.lower()
        mmanager.queue_module(args)

    @command('Must specify module to remove.', offline=True)
    def remmodule(self, context, event):
        """
        Removes a module from the system.
        """
        args = event.args.lower()
        mmanager.remove_module(args)

    @command(offline=True)
    def pluginstatus(self, context, event):
        """
        Displays information about loaded command plugins.
        """
        c = self.commander

        if not event.args:
            names = c.modules.keys()
            names.sort()
            cmanager['screen'].sendLine(('The following modules are active:' if names else 'No plugins are active.'), 0)
            for n in names:
                mod = c.modules[n]
                num = len(mod[context.BUILD])
                cmanager['screen'].sendLine('   - %s [%s commands/aliases]' % (COLOR('BOLD') % '%s' % n, COLOR('GREEN') % str(num)), 0)
        else:
            name = event.args.lower()
            pack = c.modules.get(name, None)
            if not pack:
                cmanager['screen'].sendLine('Module not active.', 0)
                return
            coms = pack[context.BUILD]
            cmanager['screen'].sendLine(('The following commands/aliases are active in %s:' % (COLOR('BOLD') % name) if coms else 'No commands active in %s.' % COLOR('BOLD')), 0)
            for c in coms:
                i = pack[context.INSTANCE]
                f = getattr(i, c)
                a = i.aliases.get(c, False)
                cn = '  - %s' % (('%s (alias of %s)' % (COLOR('CYAN') % c, a)) if a else COLOR('GREEN') % c)
                cmanager['screen'].sendLine(cn)

                if a:
                    continue

                d = (f.__doc__.strip() if f.__doc__ else None)
                rargs = f.args
                if rargs is P_ARG:
                    argval = ''
                else:
                    v1, v2 = ARG_ERRORS[1:]
                    argval = v1 if rargs else v2
                if not (d or argval):
                    cmanager['screen'].sendLine('    * %s' % (COLOR('RED') % '(Undocumented)'), 0)
                elif (d and argval):
                    cmanager['screen'].sendLine('    * %s' % d, 0)
                    cmanager['screen'].sendLine('    * %s' % (COLOR('BOLD') % argval), 0)
                else:
                    cmanager['screen'].sendLine('    * %s' % (COLOR('BOLD') % argval), 0)
