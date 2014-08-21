"""
Contains any Exceptions unique to the NC Py-Client.
"""

class ComponentError(Exception):
    pass

class ModuleError(ComponentError):
    pass

class DialogError(Exception):
    pass

class NCPError(Exception):
    pass
