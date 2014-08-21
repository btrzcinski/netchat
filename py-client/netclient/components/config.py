"""
Component for parsing the NC Py-Client configuration file.
"""

componentclass = 'Configuration'
depends = ()

from netclient.extensibles import Component
from netclient.lib.luaparser import parseLua, generateLua
from netclient.settings import NETCLIENT_CONF

class Configuration(Component):
    def __init__(self):
        Component.__init__(self)
        self.configs = {}
        self.cfile = ''
        self.loadConfig(NETCLIENT_CONF)

    def __getitem__(self, k):
        return self.configs.__getitem__(k)

    def get(self, *args):
        return self.configs.get(*args)

    def find(self, *args):
        base = self.configs
        for p in args:
            if base is None:
                break
            base = base.get(p, None)
        return base

    def add(self, *args):
        if len(args) < 3:
            raise ValueError, 'Cannot add element without location, key, and value.'
        tree, target, value = args[:-2], args[-2], args[-1]
        tree = self.find(*tree)
        if tree:
            if not target in tree:
                raise KeyError, 'Location not found in tree.'
            tree[target] += [value]
        else:
            raise KeyError, 'Tree not found.'
        self.logger.log('\'%s\' added to config tree at %s.' % (value, target))

    def loadConfig(self, filename):
        self.cfile = filename
        fp = open(filename, 'r')
        code = ''
        for line in fp:
            if not line.lstrip().startswith('#'):
                code += line
        fp.close()
        lua = parseLua(code)
        if len(lua) != 3:
            raise Exception, '?' #FIXME
        if lua[0] != 'config':
            #FIXME
            raise Exception, 'mal-formed config file: file must only have one element named config'
        self.configs.update(lua[2])

    def refreshConfig(self):
        pass
