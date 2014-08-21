# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the NCAdmin module.

# This module contains TelnetAdminServer which is a GServer accessible
# by telnet for inspecting the server.
module NCAdmin
   # This class is an extension of GServer. See its documentation for specifics
   # on implementing a GServer.
   #
   # All of the fun stuff is in serve.
   class TelnetAdminServer < GServer
      # The port argument is specified when NCInit reads configuration options.
      def initialize(port=45187, *args)
         super(port, *args)
         self.audit = true
      end
      # Transfers log duty onto the serverwide logger.
      def log(msg)
         $log.info msg
      end
      # Puts errors onto the serverwide logger and logs a debug backtrace.
      def error(detail)
         $log.error "Admin server experienced a #{detail} error"
         $log.debug detail.backtrace.join("\n")
      end
      # The basic method overridden to provide the actual functionality.
      def serve(io)

         # The real admin prompt stuff in here!
         hl = HighLine.new(io, io)

         loop do
            response = hl.choose do |menu|
               menu.header = "Choose an action"
               menu.choice("Display server uptime", "Shows how long the server has been running.")
               menu.choice("List logged in users", "Lists users that have logged in with NCMLogin.")
               menu.choice("Invalidate a user", "Invalidates a user in NCMLogin that is currently logged in. Useful for zombie sockets.")
               menu.choice("List loaded modules", "Lists currently loaded modules in the registry.")
               menu.choice("Load a module", "Loads a module into the module registry.")
               menu.choice("Unload a module", "Unloads a module from the module registry.")
               menu.choice("ADVANCED: (Re)load code file", "Dynamically injects a Ruby code file into the running server.")
               menu.choice("Shutdown the server","Terminates the NetChat Server gracefully.")
               menu.choice("Disconnect","Disconnect from the admin server if you are done.")
            end
            case response
            when "Display server uptime"
               utms = Time.now - $startuptime
               uts = utms.to_i
               utms -= uts
               utm,uts = uts / 60, uts % 60
               uth,utm = utm / 60, utm % 60
               utd,uth = uth / 24, uth % 24
               days = if utd > 0
                         "#{utd} days, "
                      else
                         ""
                      end
               hours = if uth > 0
                          "#{uth} hours, "
                       else
                          ""
                       end
               minutes = if utm > 0
                            "#{utm} minutes, "
                         else
                            ""
                         end
               seconds = if uts > 0
                            "#{uts} seconds."
                         else
                            ""
                         end
               hl.say "Server has been up for: #{days+hours+minutes+seconds}"
            when "List logged in users"
               unless $moduleregistry.registered? "login"
                  hl.say "The login module is not registered"
               else
                  hl.say "Users:"
                  u = $moduleregistry['login'].list_users
                  if u.empty?
                     hl.say("None")
                  else
                     hl.say(u.join(", "))
                  end
               end
            when "Invalidate a user"
               unless $moduleregistry.registered? "login"
                  hl.say "The login module is not registered"
               else
                  username = hl.ask "Username:"
                  loginm = $moduleregistry['login']
                  client = loginm.get_client username
                  if client.nil?
                     hl.say "That user is not logged in or was just invalidated automatically."
                  else
                     hl.say "User is logged in and not automatically invalidated."
                     hl.say "The client will now be forcefully logged out."
                     loginm.disconnect_client client
                     hl.say "#{username} has been forcefully logged out."
                  end
               end
            when "List loaded modules"
               hl.say "No module registry initialized" if $moduleregistry.nil?
               hl.say $moduleregistry.list_modules
            when "Load a module"
               modname = hl.ask "Module name (i.e. NCMBase):"
               if $moduleregistry.registered? modname
                  hl.say "Module already registered"
               else
                  begin
                     $log.debug "Instantiating ncm with eval()"
                     ncm = eval("ModLand::#{modname}").new
                     $log.debug "Registering with ModuleRegistry"
                     $moduleregistry.register ncm
                     hl.say "Module #{modname} registered"
                  rescue => e
                     hl.say "Exception occurred trying to load (check modland and friends)"
                     hl.say e
                     $log.error "Exception #{e} occurred trying to load (check modland and friends)"
                     $log.debug e.backtrace.join("\n")
                  end
               end
            when "Unload a module"
               modname = hl.ask "Module name (i.e. NCMBase):"
               unless $moduleregistry.registered? modname
                  hl.say "Module not loaded"
               else
                  $moduleregistry.unregister modname
                  hl.say "Module #{modname} unregistered."
               end
            when "ADVANCED: (Re)load code file"
               codefile = hl.ask "Code filename:"
               load codefile
               hl.say "#{codefile} loaded"
            when "Shutdown the server"
               NCServer.shutdown
            when "Disconnect"
               break
            end
         end

      end
      # Send a log message about starting up.
      def starting
         $log.info "Telnet admin server (#{@host}:#{@port}) starting"
      end
      # Send a log message about stopping.
      def stopping
         $log.info "Telnet admin server (#{@host}:#{@port}) stopping"
      end
      # Send a log message about a connecting client.
      def connecting(client)
         $log.info "Administrator connecting to telnet admin server"
      end
      # Send a log message about a disconnecting client.
      def disconnecting(clientPort)
         $log.info "Administrator disconnecting from telnet admin server"
      end
   end
end
