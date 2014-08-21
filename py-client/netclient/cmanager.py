from netclient.exceptions import ComponentError
from netclient.extensibles import Component
from netclient.settings import COMPONENTS
from netclient.util import dynamicLoad

class ComponentManager(object):
    """
    A class for handling and managing important aspects
    (components) of the NC Py-Client. When instantiated,
    the ComponentManager serves as a handy bundle of
    references that several areas of the program
    might need access to.

    It keeps a dictionary of proxies that serve as dynamic
    references, should a Component be updated.
    """

    def __init__(self):
        self.path = COMPONENTS
        self.components = {}
        self.proxies = {}

    def __contains__(self, name):
        return name in self.components

    def __getitem__(self, name):
        if name in self:
            return self.components[name]
        else:
            raise ComponentError, 'Component \'%s\' not found.' % name

    def get_proxy(self, name):
        if name in self.proxies:
            return self.proxies[name]
        self.proxies[name] = ComponentProxy(self[name], name)
        return self.proxies[name]

    def add_component(self, name, component):
        self.components[name] = component
        self.get_proxy(name)._replace_component(component)
        if isinstance(component, Component):
            self.components[name].open()

    def load_component(self, name):
        if name in self:
            return True
        component = dynamicLoad(self.path, name)
        if component is not None:
            reload(component)
            cclass = getattr(component, component.componentclass, None)
            if not issubclass(cclass, Component):
                raise ComponentError, 'Component \'%s\' does not extend Component.' % name
            for dependency in getattr(component, 'depends', ()):
                loaded = self.load_component(dependency)
                if not loaded:
                    raise ComponentError, 'Dependency of \'%s\' (\'%s\') could not be loaded.' % (name, dependency)
            cinstance = cclass()
            self.add_component(name, cinstance)
            return True
        else:
            return False

    def load_components(self, names):
        flag = True
        for n in names:
            flag = flag and self.load_component(n)
        return flag

    def refresh_component(self, name):
        self.log('Refreshing Component \'%s\'.' % name)
        self[name].refreshConfig()

    def close(self):
        for component in self.components.iterkeys():
            if isinstance(component, Component):
                component.close()
        del self.components
        del self.proxies

    def msg(self, msg, **kawrgs):
        if hasattr(self, 'logger'):
            self.logger.msg(msg, **kwargs)

    def log(self, msg, **kwargs):
        if hasattr(self, 'logger'):
            self.logger.log(msg, **kwargs)

    def err(self):
        if hasattr(self, 'logger'):
            self.logger.err()

class ComponentProxy(object):
    """
    Serves as a dynamic reference to a Component.
    Should the Component be updated during runtime,
    the proxy will not need to be changed to reflect
    this.
    """
    
    __slots__ = ('_component', '_componentname')
    def __init__(self, component, name):
        self._component = component
        self._componentname = name

    def _replace_component(self, c):
        self._component = c

    def __getattr__(self, attr):
        return getattr(self._component, attr)

    def __setattr__(self, attr, val):
        if attr == '_component':
            return object.__setattr__(self, attr, val)
        else:
            return setattr(self._component, attr, val)

cmanager = ComponentManager()
