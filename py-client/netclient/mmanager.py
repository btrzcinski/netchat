from netclient.cmanager import cmanager, ComponentProxy
from netclient.exceptions import ModuleError
from netclient.extensibles import Module
from netclient.settings import MODULES
from netclient.messages.modules import NCMRequest, NCMRemove
from netclient.util import dynamicLoad

MODULE, VERSION = 0, 1

class ModuleManager:
    """
    A class based on the netclient.cmanager.ComponentManager,
    used for managing and queuing NCModules.
    """

    def __init__(self):
        self.queue = {}
        self.modules = {}
        self.proxies = {}
        self.module_queues = {}
        self.path = MODULES

    def __len__(self):
        return len(self.modules)

    def __contains__(self, name):
        return name in self.modules

    def __getitem__(self, name):
        if name in self:
            return self.modules[name][MODULE]
        else:
            raise ModuleError, 'Module \'%s\' not found.' % name

    def get_proxy(self, name):
        """
        Returns a self-updating L{ComponentProxy} object that can be used
        as a reference to any Module. As opposed to setting
        mmanager['name'] to a variable, an appropriate Proxy should
        be assigned instead.

        @type   name:   str
        @param  name:   The name of a Module.
        @rtype:         L{ComponentProxy}
        @return:        A Proxy to the cooresponding Module.
        """
        
        if name in self.proxies:
            return self.proxies[name]
        self.proxies[name] = ComponentProxy(self[name], name)
        return self.proxies[name]

    def add_module(self, name, module, version='1.0', q=False):
        d = self.queue if q else self.modules
        d[name] = {MODULE: module, VERSION: version}

    def get_module(self, name):
        assert name in self, 'Module %s not found' % name
        return self.modules[name]

    def load_module(self, name, q=False):
        d = self.queue if q else self.modules
        if name in d:
            return True
        module = dynamicLoad(self.path, name)
        if module is not None:
            reload(module)
            mclass = getattr(module, module.moduleclass, None)
            if not issubclass(mclass, Module):
                raise ModuleError, 'Modules must extend Module.'
            minstance = mclass()
            version = getattr(module, 'version', None)
            if not isinstance(version, str):
                raise ModuleError, 'Module %s does not have a valid version.' % name
            self.add_module(name, minstance, version, q)
            return True
        else:
            return False

    def load_modules(self, names, d=None):
        flag = True
        for n in names:
            flag = flag and self.load_module(n, d)
        return flag

    def queue_module(self, name):
        if name in self:
            cmanager['screen'].sendLine('Module \'%s\' already loaded.' % name)
            return

        flag = self.load_module(name, True)
        if flag:
            mod = self.queue[name]
            ver = mod[VERSION]
            req = NCMRequest(name, ver)
            self.logger.log('Requesting Module \'%s\', version %s.' % (name, ver))
            cmanager['xmlcomm'].sendLine(req)
        return flag

    def queue_modules(self, names):
        flag = True
        for n in names:
            flag = flag and self.queue_module(n)
        return flag

    def approve_module(self, name):
        # TODO: Version checking
        assert self.queue.has_key(name), 'Module %s not queued.' % name
        self.logger.log('Module \'%s\' approved.' % name)
        self.modules[name] = self.queue.pop(name)
        self[name].open()
        if name in cmanager['config'].find('sets', 'valid_sets'):
            cmanager['commands'].loadModule(name)
        for cont, typ in self.module_queues.get(name, []):
            self[name].parse_content(cont, typ)

    def deny_module(self, name):
        assert self.queue.has_key(name), 'Module %s not queued.' % name
        self.logger.log('Module \'%s\' denied.' % name)
        del self.queue[name]

    def remove_module(self, name):
        self.logger.msg('Removing module \'%s\'...' % name)
        if not name in self.modules:
            self.logger.msg('Module not loaded.')
            return
        
        mod = self[name]
        ver = self.modules[name][VERSION]
        rem = NCMRemove(name, ver)
        cmanager['xmlcomm'].sendLine(rem)
        del self.modules[name]
        self.logger.msg('...Successfully removed module.')
        if name in cmanager['config'].find('sets', 'default_sets'):
            self.logger.log('Module has an associated command plugin.')
            cmanager['commands'].rem_module(name)

    def queue_content(self, name, content, typ):
        self.module_queues.setdefault(name, []).append((content, typ))

    def clear(self):
        self.logger.msg('Wiping ModuleManager.')
        self.modules.clear()
        self.proxies.clear()
        self.queue.clear()

mmanager = ModuleManager()
