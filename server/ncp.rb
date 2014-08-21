# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file is responsible for the NCP module, which has all base-level
# communications relating to XML and NCP handling.

include NCError

# This module includes the ModuleCommunicator class that modules use
# to send messages to clients as well as the SocketWorker class that
# is spawned for every client to handle all messages before they reach
# server message/module message parsing status.
module NCP
   # This class handles indirect communication between module and client.
   # Passing all messages through send_message ensures that the proper values
   # are set in the properties for the message to at least go through to the
   # module on the other end.
   class ModuleCommunicator
      # Instantiated with the module that is doing the communicating.
      def initialize(m)
         @module = m
      end
      # Modules use this to send their messages.
      # * client is a OpenSSL socket that should have been passed to the module.
      # * modheader is a section of the <header/> section that should be encompassed
      #   in <modulemessage/>.
      # * content is the content section of the entire message, encompassed in
      #   <content/>.
      def send_message(client, modheader, content)
         document = REXML::Document.new

         message = REXML::Element.new 'message'
         document.add message

         props = modheader.elements['properties']
         props.attributes['name'] = @module.name

         header = REXML::Element.new 'header'

         global = REXML::Element.new 'global'
         globalprops = REXML::Element.new 'properties'
         globalprops.attributes["type"] = 'modulemessage'
         global.add globalprops

         header.add global
         header.add modheader

         message.add header
         message.add content

         string_out = ""
         document.write string_out

         $log.debug "-- Module '#{@module.name}' sent message: #{string_out}"

         client.puts string_out
      end
   end
   # This class is used by NCServer to handle one client when it is accepted.
   class SocketWorker
      # Instantiated with client and lists of callbacks for when the client
      # connects/disconnects.
      def initialize(c)
         @client = c
         @loadedmods = Array.new
         puts "Warning: logger is nil" if $log.nil?
      end
      # An overall method that is used to really send server messages, but could
      # in theory take module messages. If it gets a module message, however,
      # it just chucks it out.
      #
      # data is a hash containing:
      # globaltype::    the type contained in global properties, i.e. 'servermessage'
      # messagetype::   the type of the actual message, i.e. 'ping' or 'pong'
      # content::       the main <content/> section
      def send_message(data)
         type = data[:globaltype]
         raise NCPException, "NCP send_message needs a type to construct message" if type.nil?

         # construction of some of the XML document to start
         document = REXML::Document.new
         message = REXML::Element.new "message"
         document.add_element message
         header = REXML::Element.new "header"
         message.add_element header
         content = if data[:content].nil?
            REXML::Element.new 'content'
         else
            data[:content]
         end
         message.add_element content
         global = REXML::Element.new "global"
         header.add_element global
         globalproperties = REXML::Element.new "properties"
         global.add_element globalproperties

         case type
         when :server
            messagetype = data[:messagetype]
            raise NCPException, "NCP send_message needs to know what server message to send" if messagetype.nil?
            globalproperties.add_attribute "type", "servermessage"
            servermessageheader = REXML::Element.new "servermessage"
            header.add_element servermessageheader
            servermessageproperties = REXML::Element.new "properties"
            servermessageheader.add_element servermessageproperties
            case messagetype
            when :pong
               servermessageproperties.add_attribute "type", "pong"
            when :ping
               servermessageproperties.add_attribute "type", "ping"
            when :data
               servermessageproperties.add_attribute "type", "data"
            when :authorize_module
               servermessageproperties.add_attribute "type", "authorize_module"
            when :deny_module
               servermessageproperties.add_attribute "type", "deny_module"
            else
               raise NCPException, "NCP send_message doesn't know how to send server message specified"
            end
         when :module
            raise NCPException, "NCP send_message should never be called for modules: modules should use their ModuleCommunicator"
         else
            raise NCPException, "NCP send_message doesn't know how to send anything but server messages"
         end

         string_out = ""
         document.write string_out

         $log.debug "-- Message sent to client: '#{string_out}'"

         @client.puts string_out
      end
      # This function is called with the complete XML document received every time
      # this client sends something.
      #
      # xmldoc is a complete REXML::Document. An NCPException is raised if it is not.
      def handle_message(xmldoc)
         raise NCPException, "Invalid XML received from client, ignoring message" unless xmldoc.is_a? REXML::Document

         string_in = ""
         xmldoc.write string_in
         $log.debug "-- Message received from client: '#{string_in}'"
         return if string_in == ""

         message = xmldoc.elements["message"]
         header = message.elements["header"]
         content = message.elements["content"]
         raise NCPException, "<message/>, <header/>, or <content/> are missing" if header.nil? or content.nil? or message.nil?
         global = header.elements["global"]
         raise NCPException, "Global section required in header" if global.nil?
         globalprops = global.elements["properties"]
         raise NCPException, "Global properties missing" if globalprops.nil?
         globaltype = globalprops.attributes["type"]
         raise NCPException, "Type attribute in global properties missing" if globaltype.nil?
         case globaltype
         when "modulemessage"
            messageheader = header.elements["modulemessage"]
            raise NCPException, "Invalid module message: <modulemessage/> must be in header" if messageheader.nil?
            messageprops = messageheader.elements["properties"]
            raise NCPException, "Invalid module message: <properties/> element must be in module message header" if messageprops.nil?
            modname = messageprops.attributes["name"]
            raise NCPException, "Invalid module message: name of module needs to be in properties" if modname.nil?
            raise NCPException, "Cannot process module message since module registry is not initialized" if $moduleregistry.nil?
            mod = $moduleregistry.access modname
            raise NCPException, "Module #{modname} required to process this message is not loaded" if mod.nil?
            begin
               mod.parse @client, messageheader, content
            rescue => e
               $log.error "Module #{modname} experienced an error #{e} while parsing a received message."
               raise
            end
         when "servermessage"
            messageheader = header.elements["servermessage"]
            raise NCPException, "Invalid server message: <servermessage/> must be in header" if messageheader.nil?
            messageprops = messageheader.elements["properties"]
            raise NCPException, "Invalid server message: <properties/> element must be in server message header" if messageprops.nil?
            messagetype = messageprops.attributes["type"]
            raise NCPException, "Invalid server message: type not specified" if messagetype.nil?
            case messagetype
            when "ping"
               begin
                  self.send_message(:globaltype => :server, :messagetype => :pong)
               rescue NCPException => e
                  raise
               end
            when "pong"
               begin
                  self.send_message(:globaltype => :server, :messagetype => :ping)
               rescue NCPException => e
                  raise
               end
            when "module_request"
               begin
                  $log.debug "Module request with content #{content}"
                  modname = content.elements["name"].text
                  modver = content.elements["protocol-version"].text
                  $log.debug "Module name is undefined!" if modname.nil?
                  $log.debug "Module protocol version is undefined!" if modver.nil?
                  if $moduleregistry.nil? or not $moduleregistry.registered? modname
                     self.send_message(:globaltype => :server, :messagetype => :deny_module, :content => content)
                  else
                     loadedmod = $moduleregistry.access modname
                     if loadedmod.protocol_version == modver
                        self.send_message(:globaltype => :server, :messagetype => :authorize_module, :content => content)
                     else
                        self.send_message(:globaltype => :server, :messagetype => :deny_module, :content => content)
                     end
                     mod = $moduleregistry.access modname
                     raise NCPException, "Module #{modname} required to process this message is not loaded" if mod.nil?
                     begin
                        @loadedmods << modname
                        mod.on_client_load @client
                     rescue => e
                        $log.error "Module #{modname} experienced an error #{e} while executing callback for client load."
                        raise
                     end
                  end
               rescue NCPException => e
                  raise
               end
            when "module_unload"
               begin
                  $log.debug "Module unload with content #{content}"
                  modname = content.elements["name"].text
                  modver = content.elements["protocol-version"].text
                  $log.debug "Module name is undefined!" if modname.nil?
                  $log.debug "Module protocol version is undefined!" if modver.nil?
                  unless $moduleregistry.nil? or not $moduleregistry.registered? modname
                     mod = $moduleregistry.access modname
                     raise NCPException, "Module #{modname} required to process this message is not loaded" if mod.nil?
                     begin
                        @loadedmods.delete modname
                        mod.on_client_unload @client
                     rescue => e
                        $log.error "Module #{modname} experienced an error #{e} while executing callback for client load."
                        raise
                     end
                  end
               rescue NCPException => e
                  raise
               end
            when "echo"
               begin
                  self.send_message(:globaltype => :server, :messagetype => :data, :content => content)
               rescue NCPException => e
                  raise
               end
            else
               raise NCPException, "Unrecognized server message: #{messagetype.to_s}"
            end
         else
            raise NCPException, "Unrecognized global type"
         end
      end
      # This function is called by the main server loop in a separate thread to
      # process the client until disconnect.
      def message_loop
         domain, port, name, addr = @client.peeraddr
         begin
            loop do
               s = nil
               status = Timeout::timeout(60) { s = @client.readline }
               unless $options[:xml]
                  $log.debug "Received a non-XML message, echoing back"
                  @client.write s
               else
                  $log.debug "Received a message, firing handler"
                  begin
                     xmldoc = REXML::Document.new s
                     self.handle_message xmldoc
                  rescue REXML::ParseException => pe
                     $log.warn "Parse exception constructing XML tree from client data (#{name}:#{addr}), message: '#{s}'"
                     next
                  rescue NCPException => ne
                     $log.warn "NCPException handling message: #{ne}"
                  rescue => e
                     raise
                  end
               end
            end
         rescue Timeout::Error => te
            # The positioning of Timeout::Error before Interrupt is important because as of 1.8.5
            # the Timeout::Error object inherits from Interrupt.
            $log.debug "Client timed out"
         rescue Interrupt => i
            $log.error "Client interrupted"
         rescue EOFError => e
            $log.debug "Client disconnected by EOF"
         rescue Errno::ENOTCONN => enocon
            $log.debug "Client socket endpoint not connected"
         rescue Errno::ECONNRESET => ereset
            $log.debug "Client connection was reset by peer"
         rescue => e
            ## This used to be fatal. It is NOT fatal as it only terminates the client.
            ## The server is unaffected and can continue to connect and process other
            ## clients.
            domain, port, name, addr = nil, nil, nil, nil
            begin
               domain, port, name, addr = @client.peeraddr
            rescue => se
               $log.debug "Unable to resolve socket information for crashed client"
            end
            name = "unknown" if name.nil?
            addr = "unknown" if addr.nil?
            $log.error "Unknown error occurred in client (#{name}:#{addr}): #{e} (#{e.class})"
            $log.debug "Backtrace: #{e.backtrace.join("\n")}"
         ensure
            $log.debug "Disconnect sequence for client"
            domain, port, name, addr = nil, nil, nil, nil
            begin
               domain, port, name, addr = @client.peeraddr
            rescue => se
               $log.debug "Unable to resolve socket information for client"
            end
            name = "unknown" if name.nil?
            addr = "unknown" if addr.nil?
            # "unload" any still-loaded client modules
            $log.debug "Trying to unload modules #{@loadedmods.join(", ")}"
            @loadedmods.each do |modname|
               mod = $moduleregistry.access modname
               $log.warn "Cannot unload module #{modname} for client #{name} (#{addr}) since server module is not loaded" if mod.nil?
               unless mod.nil?
                  $log.debug "Unloading #{modname} for client (#{name}:#{addr})"
                  mod.on_client_unload @client
               end
            end
            $log.debug "Closing socket for client (#{name}:#{addr})"
            @client.close
         end
      end
   end #SocketWorker
end #module
