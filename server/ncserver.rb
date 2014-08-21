# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the NCServer module.

include NCError

# This is the primary server module, containing the main loop and other initialization procedures.
# It is called in start.rb to bring up the server.
module NCServer
   # The main method to bring up and run the server.
   # * Reads options.
   # * Starts the logger.
   # * Opens relevant sockets.
   # * Begins encryption.
   # * Accepts and passes off clients.
   def NCServer.main
      $options = {
         :loglevel => Logger::INFO,
         :logtarget => STDOUT,
         :oldlogs => 10,
         :maxlogsize => 1024000, #bytes
         :port => 45287,
         :adminport => 45187,
         :ssl => true,
         :xml => true,
         :win32 => false,
         :modautoload => Array.new
      }

      NCInit::Configuration.read_options
      NCInit::Configuration.parse_options

      $log = NCInit::Logging.start_log $options

      if $log.level <= Logger::DEBUG
         $log.debug "Options:"
         $options.each do |k, v|
            $log.debug "#{k} => [#{v.join(",")}]" if v.is_a? Array
            $log.debug "#{k} => #{v}" unless v.is_a? Array
         end
      end

      #fixes <defunct> processes on UNIX
      begin
         trap("CLD", "SIG_IGN") unless $options[:win32];
      rescue ArgumentError => ae
         $log.warn "Could not register handler for SIGCLD"
         $log.warn "Is the platform Win32? (use -w)"
      end


      begin
         # Added explicit bind address of "0.0.0.0" because
         # on (at least) FreeBSD, there is some unexpected behavior
         # causing the server to bind only to IPv6 addresses or something
         # like that.
         #
         # Right now we're preferring IPv4.

         $server = TCPServer.new("0.0.0.0", $options[:port])
         $log.info "Server started (port #{$options[:port]})"
      rescue => e
         $log.fatal "Fatal error occurred in initialization: #{e}"
         raise
      end

      if $options[:ssl]
         begin
            sslserver = NCInit::SSL.create_server $server
            $log.info "SSL initialized"
         rescue => e
            $log.fatal "Fatal SSL error: #{e}"
            raise
         end
      end

      # start administration server
      begin
         $adminserver = NCAdmin::TelnetAdminServer.new($options[:adminport])
      rescue => e
         $log.fatal "Exception occurred starting admin server: #{e}"
         raise
      end
      $adminserver.start

      # create global module registry
      $moduleregistry = NCModule::ModuleRegistry.new
      $log.info "Module registry initialized"

      #autoloads some modules before we start
      $log.info "Autoloading #{$options[:modautoload].length} modules (#{$options[:modautoload].join(",")})"
      $options[:modautoload].each do |modname|
         begin
            $log.debug "Instantiating ncm with eval()"
            ncm = eval("ModLand::#{modname}").new
            $log.debug "Registering with ModuleRegistry"
            $moduleregistry.register ncm
            $log.debug "Module #{modname} autoloaded"
         rescue => e
            $log.error "Error #{e} occurred loading #{modname}"
            $log.debug e.backtrace.join("\n")
         end
      end

      $startuptime = Time.now
      $log.info "Listening for clients"

      loop do
         begin
            client = if $options[:ssl]
               sslserver.accept
            else
               $server.accept
            end
            client.sync = true
         rescue OpenSSL::SSL::SSLError => ssle
            $log.error "There was a problem connecting a client:"
            $log.error "SSL communications invalid or unknown. Moving on."
            next
            client.close
         rescue Interrupt => i
            $log.fatal "Accept cycle interrupted"
            break
         rescue IOError => ioe
            $log.fatal "Accept cycle interrupted forcibly (admin?)"
            break
         rescue => e
            $log.fatal "Unknown error occurred in accept cycle: #{e}"
            raise
         end
         Thread.new do
            domain, port, name, addr = client.peeraddr
            $log.info "Connection established from #{name} (#{addr})"
            sw = NCP::SocketWorker.new client
            sw.message_loop
            $log.info "Connection terminated for #{name} (#{addr})"
         end
      end

      NCServer.shutdown unless $server.closed?
   end
   # Shuts down the server by unregistering all modules, closing sockets,
   # and stopping the logger.
   def NCServer.shutdown
      $log.info "Unregistering #{$moduleregistry.to_a.length} modules"
      $moduleregistry.to_a.each do |modname|
         $log.debug "Unregistering #{modname}"
         $moduleregistry.unregister modname
      end
      $server.close
      $adminserver.shutdown
      $log.info "Server shut down."
      $log.close
   end
end
