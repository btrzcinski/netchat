"""
The NCMail Module. Handles anything related to file transfer and a hierarchial
folder system for it.
"""

from netclient.lib.etreeparser import ETreeParser as ETP

from netclient.cmanager import cmanager
from netclient.extensibles import Module

name = 'Mail'
version = '0.1a'
moduleclass = 'Mail'

class Mail(Module):
    def __init__(self):
        Module.__init__(self)
        self.logger = cmanager['logger'].get_context('mail')

    def open(self):
        Module.open(self)
        # Get mail, etc

    def get_tabbed_list(self, command, intermediate, input):
        pass # Implement TODO
