# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the NCInit module.

# This module contains various routines used during server startup.
module NCInit
   # This module contains some methods used to parse configuration options.
   module Configuration
      # This generic utility method parses booleans, returning default if text is not true or false.
      def Configuration.parse_boolean(text,default)
         text.downcase!
         if text == "true"
            return true
         elsif text == "false"
            return false
         else
            return default
         end
      end
      # This method reads options from etc/server.xml and sets $options.
      def Configuration.read_options
         begin
            File.open('etc/server.xml') do |f|
               conf = REXML::Document.new(f).elements['configuration']

               general = conf.elements['general']
               unless general.nil?
                  $options[:xml] = parse_boolean(general.elements['use-xml'].text, true)
                  $options[:ssl] = parse_boolean(general.elements['use-ssl'].text, true)
                  $options[:loglevel] = eval("Logger::#{general.elements['loglevel'].text}") unless general.elements['loglevel'].nil?
                  logtarget = general.elements['logtarget']
                  unless logtarget.nil?
                     case logtarget.attributes['type']
                     when "stream"
                        $options[:logtarget] = eval(logtarget.text)
                     when "file"
                        $options[:logtarget] = logtarget.text
                     else
                        raise Exception, "Unknown log target type"
                     end
                  end
                  $options[:maxlogsize] = eval(general.elements['maxlogsize'].text) unless general.elements['maxlogsize'].nil?
                  $options[:oldlogs] = eval(general.elements['oldlogs'].text) unless general.elements['oldlogs'].nil?
                  $options[:port] = eval(general.elements['service-port'].text) unless general.elements['service-port'].nil?
                  $options[:adminport] = eval(general.elements['admin-port'].text) unless general.elements['admin-port'].nil?
                  $options[:win32] = eval(general.elements['win32'].text) unless general.elements['win32'].nil?
               end

               initialization = conf.elements['initialization']
               unless initialization.nil?
                  autoloadsection = initialization.elements['module-autoload']
                  unless autoloadsection.nil?
                     autoloadsection.each_element_with_text do |mod|
                        $options[:modautoload] << mod.text
                     end
                  else
                     puts "No autoload section"
                  end
               end

            end
         rescue => e
            puts "INIT ERROR: Problem (#{e}) reading server configuration file"
            puts "Backtrace: #{e.backtrace.join("\n")}"
         end
      end
      # This method parses options from the command line and sets $options accordingly.
      def Configuration.parse_options
         OptionParser.new do |opts|
            opts.banner = "Usage: start.rb [options]"

            opts.on("-l","--log LEVEL", [:DEBUG, :INFO, :WARN, :ERROR, :FATAL],  "Logging level (default: INFO)") do |v|
               $options[:loglevel] =	if v == :DEBUG
                  Logger::DEBUG
               elsif v == :INFO
                  Logger::INFO
               elsif v == :WARN
                  Logger::WARN
               elsif v == :ERROR
                  Logger::ERROR
               elsif v == :FATAL
                  Logger::FATAL
               end
            end

            opts.on("-L","--log-to-file FILE", "Log to a specified file (default: log to STDOUT)") do |v|
               $options[:logtarget] = v
            end

            opts.on("--old-logs NUMBER", Integer, "Keep NUMBER old logs before discarding (default: 10)") do |v|
               $options[:oldlogs] = v
            end

            opts.on("--max-log-size SIZE", Integer, "Grow a log until it reaches SIZE bytes (default: 1024000)") do |v|
               $options[:maxlogsize] = v
            end

            opts.on("-p","--port PORT", Integer, "Service port (default: 45287)") do |v|
               $options[:port] = v
            end

            opts.on("-a","--admin-port PORT", Integer, "Admin port (default: 45187)") do |v|
               $options[:adminport] = v
            end

            opts.on("-s","--[no-]ssl","Use OpenSSL (default: yes)") do |v|
               $options[:ssl] = v
            end

            opts.on("-x","--[no-]xml","Use XML for data (default: yes)") do |v|
               $options[:xml] = v
            end

            opts.on("-w","--[no-]win32","Turn off child signal trapping for Win32 (default: no)") do |v|
               $options[:win32] = v
            end
         end.parse!
      end

   end
   # This module has utility methods related to the logger.
   module Logging
      # This utility method performs the setup procedure for the logger.
      def Logging.start_log (options)
         log = if options[:logtarget].is_a? IO
            Logger.new(options[:logtarget])
         else
            Logger.new(options[:logtarget],options[:oldlogs],options[:maxlogsize])
         end
         log.level = options[:loglevel]
         log.progname = "Server"
         log.datetime_format = "%Y-%m-%d %H:%M:%S PID"

         log
      end
   end
   # This module has utility methods related to SSL.
   module SSL
      # Specifies the location of the private key file
      SSLKey = "ssl/key.pem"
      # Specifies the location of the certificate file
      SSLCert = "ssl/cert.pem"
      # This method initializes and returns a new OpenSSL context.
      def SSL.create_context
         ctx = OpenSSL::SSL::SSLContext.new
         ctx.key = OpenSSL::PKey::RSA.new(File::read(SSLKey))
         ctx.cert = OpenSSL::X509::Certificate.new(File::read(SSLCert))
         ctx.verify_mode = OpenSSL::SSL::VERIFY_NONE

         ctx
      end
      # This method wraps a TCPServer in OpenSSL.
      def SSL.create_server (server)
         ctx = create_context

         OpenSSL::SSL::SSLServer.new(server, ctx)
      end
   end
end
