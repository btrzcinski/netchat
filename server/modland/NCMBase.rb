# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the definition for NCMBase within ModLand.
# If you wish to develop a module for the server, see NCMBase.

# This module contains all base module classes (those directly
# loaded by the server).
module ModLand

   # This is the base module from which all others derive. It serves no
   # purpose when loaded, responding to every message with an "unknown
   # message received" debug log.
   #
   # If you wish to develop a module for the server, read on to learn about
   # what you need to implement in your own class. It should go in the module
   # ModLand, inherit from NCMBase (this class), and follow the guidelines below.
   class NCMBase
      # The constructor takes no arguments, but should set three class variables:
      # name::                The name of the module, such as 'news'
      # protocol_version::    The version of the protocol the module uses. This is not
      #                       the internal module revision, it should be the global
      #                       protocol version for your service. This enables modules
      #                       written by different authors to communicate if they use
      #                       the same protocol. Your module may not be allowed to load
      #                       client-side if the protocol versions do not match.
      # depends::             An array containing names of modules that need to be loaded
      #                       before this one.
      def initialize
         @name = 'NCMBase'
         @protocol_version = '1'
         @depends = Array.new
      end
      # A NCP::ModuleCommunicator. Use it to send messages back to your clients.
      attr_accessor :communicator
      # A NCModule::ModuleAccessor. Use it to find other modules and call their exposed methods.
      attr_accessor :moduleaccessor
      # Name of your module (set in the constructor).
      attr_reader :name
      # Version of your module's protocol (set in the constructor).
      attr_reader :protocol_version
      # List of dependencies (set in the constructor).
      attr_reader :depends
      # The string representation of a module class is its name.
      def to_s
         @name
      end
      # This method provides the basic reflection capability that enables derived modules
      # to receive incoming messages. Based on the 'type' property set in the modulemessage
      # <properties/> section of the message, a method named msg_<type> is called with the
      # socket client, message header, and message content as arguments. If such a method is
      # not found, a debug log message is generated.
      #
      # parse should never be fully overridden. Most people will never need to
      # override this at all. If you do need to override it (see NCMLogin for an example),
      # call super(client, header, content) at some point to ensure that the basic functionality
      # does not go away.
      #
      # Arguments sent to your msg_<type> method (passed from parse):
      # client::        Socket client. Don't use directly, but pass to your NCP::ModuleCommunicator.
      # header::        The <modulemessage/> section of the header.
      # content::       The <content/> section of the message.
      def parse (client, header, content)
         safeheader = header.deep_clone
         safecontent = content.deep_clone

         $log.debug "#{@name} instance has received data" unless $log.nil?

         # default behavior, route to a reflected method based on message type
         type = header.elements['properties'].attributes['type']
         response = self.__send__("msg_#{type}".to_sym, client, safeheader, safecontent)
         unless response.nil? or not response.is_a? Hash
            responseheader = response[:header].deep_clone
            responsecontent = response[:content].deep_clone
            self.communicator.send_message client, responseheader, responsecontent
         end
      end
      # Modules are expected to override this method. It is called when the module is registered
      # with the server, to perform startup and initialization routines that depend on it being
      # loaded.
      def on_registry_add
         $log.debug "#{@name} instance has been added to the registry" unless $log.nil?
      end
      # Modules are expected to override this method. It is called when the module is unregistered
      # with the server, to perform shutdown and destruction routines that need to be called before
      # the module is finally removed.
      def on_registry_delete
         $log.debug "#{@name} instance has been deleted from the registry" unless $log.nil?
      end
      # Modules are expected to override this method. It is called when a particular client has had
      # this module loaded. 
      def on_client_load (client)
         domain, port, name, addr = nil, nil, nil, nil
         begin
            domain, port, name, addr = client.peeraddr
         rescue => se
            $log.debug "[NCMChat] Error getting name/address for client"
         end
         name = "unknown" if name.nil?
         addr = "unknown" if addr.nil?
         $log.debug "#{@name} instance has been loaded by the client #{name} (#{addr})"
      end
      # Modules are expected to override this method. It is called when a particular client has had
      # this module unloaded. Note that on an unclean disconnect the server acts as if the
      # module has been simply unloaded.
      def on_client_unload (client)
         domain, port, name, addr = nil, nil, nil, nil
         begin
            domain, port, name, addr = client.peeraddr
         rescue => se
            $log.debug "[NCMChat] Error getting name/address for client"
         end
         name = "unknown" if name.nil?
         addr = "unknown" if addr.nil?
         $log.debug "#{@name} instance has been unloaded by the client #{name} (#{addr})"
      end
      # This method should never be overridden. Do so at your own risk; its purpose is to log debug
      # messages when an unrecognized message is attempted to be called by parse. You probably will
      # want to call super(sym, args) at some point to ensure that the base functionality does not
      # disappear.
      def method_missing (sym, *args)
         unless sym.to_s =~ /^msg_/
            $log.debug "#{@name} instance got unknown method call: #{sym.to_s} with args [#{args.join(", ")}]"
         else
            header = args[1]
            content = args[2]
            type = header.elements['properties'].attributes['type']
            $log.debug "#{@name} instance received unknown message of type: #{type}"
         end
      end
   end

end
