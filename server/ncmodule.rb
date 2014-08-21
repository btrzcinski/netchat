# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the NCModule module.

include NCError

# This module contains stuff related to organizing the module system.
module NCModule
   # This class serves as a collection point for active server modules.
   class ModuleRegistry
      # Makes a new module registry.
      def initialize
         @ncmodules = Hash.new
      end
      # Returns a comma separated string list of module names loaded.
      def list_modules
         return "None" if @ncmodules.length == 0
         names = @ncmodules.collect { |k, v| v.to_s }
         return names.join(", ")
      end
      # Returns an array of module name strings loaded.
      def to_a
         @ncmodules.collect { |k, v| v.to_s }
      end
      # Registers an instance (ncmodule) in the registry.
      def register (ncmodule)
         $log.debug "Attempting to load module '#{ncmodule.to_s}' of type '#{ncmodule.class.to_s}'"
         raise NCModuleRegistryException, "Cannot add any ncmodule not deriving from ModLand::NCMBase" unless ncmodule.is_a? ModLand::NCMBase
         raise NCModuleRegistryException, "A module named #{ncmodule} is already registered" if @ncmodules.has_value? ncmodule or @ncmodules.has_key? ncmodule.to_s
         ncmodule.depends.each do |dependency|
            raise NCModuleRegistryException, "Cannot load this ncmodule (#{ncmodule}) because of a missing dependency" unless self.registered? dependency
         end
         ncmodule.moduleaccessor = ModuleAccessor.new self
         ncmodule.communicator = NCP::ModuleCommunicator.new ncmodule
         @ncmodules.store ncmodule.to_s, ncmodule
         ncmodule.on_registry_add
      end
      # Unregisters an instance corresponding to the name ncmodule from the registry.
      def unregister (ncmodule)
         name = if ncmodule.is_a? ModLand::NCMBase
            ncmodule.to_s
         elsif ncmodule.is_a? String
            ncmodule
         else
            nil
         end
         raise NCModuleRegistryException, "Cannot look up ncmodules by anything other than their instance or their name" if name.nil?
         @ncmodules[name].on_registry_delete
         @ncmodules.delete name
      end
      # Returns true if ncmodule is registered (either instance or string name); otherwise, returns false.
      # Incorrect types throw an NCModuleRegistryException.
      def registered? (ncmodule)
         if ncmodule.is_a? String
            @ncmodules.has_key? ncmodule
         elsif ncmodule.is_a? ModLand::NCMBase
            @ncmodules.has_value? ncmodule
         else
            raise NCModuleRegistryException, "Cannot check the registration status of something other than the name or the ncmodule itself"
         end
      end
      # Returns initialized instance corresponding to string name ncmodule.
      def access (ncmodule)
         raise NCModuleRegistryException, "Cannot access ncmodules by anything other than their String name" unless ncmodule.is_a? String
         return @ncmodules[ncmodule]
      end
      # See access.
      def [] (ncmodule)
         self.access ncmodule
      end
   end

   # This class is used by modules to communicate with other loaded modules.
   class ModuleAccessor
      # Basic initialization.
      def initialize (modregistry)
         @registry = modregistry
      end
      # See access.
      def [] (modname)
         @registry[modname]
      end
      # Access a particular module by name.
      def access (modname)
         @registry.access modname
      end
   end

end
