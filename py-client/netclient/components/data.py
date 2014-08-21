"""
Component for generating and reading the NC Py-Client data file.
"""

componentclass = 'Data'
depends = ()

import os

from netclient.extensibles import Component
from netclient.settings import NETCLIENT_DATA

class Data(Component):
    def __init__(self):
        Component.__init__(self)
        self.path = NETCLIENT_DATA
        self.config = {}
        if os.path.exists(self.path):
            self.read_config()
        self.out = file(self.path, 'w')

    def __getitem__(self, item):
        return self.config.get(item)

    def __setitem__(self, item, val):
        self.config[item] = val
        self.out.write('%s: %s\n' % (item, val))

    def read_config(self):
        input = file(self.path, 'r')
        for line in input:
            key, val = line.strip().split(': ', 1)
            self.config[key] = val

    def close(self):
        self.out.flush()
        self.out.close()
        Component.close(self)

    def refreshConfig(self):
        pass
