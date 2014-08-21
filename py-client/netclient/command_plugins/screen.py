from functools import partial

from netclient.lib.comparison import levenshtein

from netclient.cmanager import cmanager
from netclient.decorators import command
from netclient.extensibles import Plugin
from netclient.mmanager import mmanager
from netclient.settings import NO_ARG

name = 'Screen Commands'
p_class = 'Screen'
depends = ()

class Screen(Plugin):
    """
    Curses screen-related commands.
    """

    aliases = {
        'rscreen': 'resetscreen',
        'tt': 'tabto',
        't2': 'tabto',
    }

    @command(NO_ARG, offline=True)
    def resetscreen(self, context, event):
        """
        Resets the curses screen.
        """
        cmanager['screen'].__reinit(False)

    @command('Requires a tab name.', offline=True)
    def tabto(self, context, event):
        """
        Atttempts to switch the screen to a specific tab.
        """
        if cmanager['screen'].has_tab(event.args):
            cmanager['screen'].tab_to(event.args)
            return
        a = event.args.lower()
        l = cmanager['screen'].tab_names
        d = dict(zip(l, l))
        for k, t in cmanager['screen'].tabs.iteritems():
            d[t.shortname] = k
        l = d.keys()
        l2 = map(partial(levenshtein, a), l)
        m = min(l2)
        d2 = dict(zip(l2, l))
        cmanager['screen'].tab_to(d[d2[m]])
