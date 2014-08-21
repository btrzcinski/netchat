# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the definition for NCMLogin, an extensible database-backed
# login module system.

# This module contains all base module classes (those directly
# loaded by the server).
module ModLand

   # A login module which stores usernames/passwords in MySQL (enforcing use of
   # SHA-512 checksums) and keeps a live "authentication database" of who is
   # logged in. It uses callbacks extensively to make sure that the live DB is
   # fresh.
   class NCMLogin < NCMBase
      # More variables are defined here than in the base. Their functions are listed
      # below. If the MySQL options are unsuitable, change them directly in the code
      # as there is no configuration file at the moment.
      #
      # mysql_host::          MySQL server name/address
      # mysql_user::          MySQL username
      # mysql_pass::          MySQL password
      # mysql_db::            MySQL database name (*NOTE*: even though the database
      #                       can be different you *MUST* follow the schema outlined
      #                       at {our Trac site}[http://netchat.tjhsst.edu/trac/netchat/wiki/SettingUpTheServerDatabase].
      #
      # signin_hooks::        Initializing an array containing methods which the class
      #                       can call upon a user's logging in.
      # signout_hooks::       Same purpose as signin_hooks, but obviously for log out
      #                       instead.
      #
      # disconnect_hook is also registered with the global set of disconnect call backs
      # so that unclean disconnects can be cleanly handled and logged out of the system.
      def initialize
         @name = 'login'
         @protocol_version = '0.1a'
         @depends = Array.new

         @mysql_host = "localhost"
         @mysql_user = "netchat"
         @mysql_pass = @mysql_user.reverse
         @mysql_db = "netchat"

         @signin_hooks = Array.new
         @signout_hooks = Array.new
      end
      # NCMLogin does not need to do anything with this callback.
      def on_client_load(c)
         domain, port, name, addr = nil, nil, nil, nil
         begin
            domain, port, name, addr = c.peeraddr
         rescue => se
            $log.debug "[NCMLogin] Error getting name/address for client"
         end
         name = "unknown" if name.nil?
         addr = "unknown" if addr.nil?
         $log.debug "[NCMLogin] Client #{name} (#{addr}) loaded this module"
         # nothing necessary
      end
      # NCMLogin does not need to do anything with this callback.
      def on_client_unload(c)
         domain, port, name, addr = nil, nil, nil, nil
         begin
            domain, port, name, addr = c.peeraddr
         rescue => se
            $log.debug "[NCMLogin] Error getting name/address for client"
         end
         name = "unknown" if name.nil?
         addr = "unknown" if addr.nil?
         $log.debug "[NCMLogin] Client #{name} (#{addr}) unloaded this module"
         # clear out the client
         self.disconnect_client c
      end
      # Returns a list of usernames currently logged in as strings.
      def list_users
         @userhash.keys
      end
      # Method used to cleanly sign out a client, particularly on external
      # calling.
      def disconnect_client(c)
         k = @clienthash[c]
         unless k.nil?
            @userhash.delete(k)
            @clienthash.delete(c)

            $log.debug "[NCMLogin] Calling signout hooks"
            @signout_hooks.each do |h|
               $log.debug "[NCMLogin] -- Calling #{h.to_s}"
               h.call(k, c)
            end
            $log.debug "[NCMLogin] Done calling signout hooks"
         end
      end
      # Method other modules can use to register signin hook methods.
      def register_signin_hook(h)
         @signin_hooks << h if h.is_a? Method
      end
      # Method other modules can use to unregister signin hook methods.
      def unregister_signin_hook(h)
         @signin_hooks.delete h if h.is_a? Method
      end
      # Method other modules can use to register signout hook methods.
      def register_signout_hook(h)
         @signout_hooks << h if h.is_a? Method
      end
      # Method user modules can use to unregister signout hook methods.
      def unregister_signout_hook(h)
         @signout_hooks.delete h if h.is_a? Method
      end
      # Returns true if a user is online and authorized in NCMLogin's live authentication
      # database. Otherwise, returns false.
      def authorized?(data)
         if @userhash.has_key?(data[:username])
            unless @userhash[data[:username]].closed?
               return true
            else
               @clienthash.delete[@userhash[data[:username]]]
               @userhash.delete(data[:username])
               return false
            end
         end

         unless data[:client].nil?
            return true if @userhash[@clienthash[data[:client]]] == data[:client]
         end
         return false
      end
      # Using the live authentication database, returns the client socket for a particular
      # username, if any. Returns nil if there's a problem or that username is not online.
      def get_client(username)
         begin
            $log.debug "[NCMLogin] Retrieving client for username #{username}"
            return nil unless username.is_a? String
            return nil if @userhash[username].nil?
            unless @userhash[username].closed?
               # try to get peeraddr, typically crashes bad sockets
               begin
                  domain, name, port, addr = @userhash[username].peeraddr
               rescue Errno::ENOTCONN => enot
                  $log.debug "[NCMLogin] Caught bad socket for #{username}, forcibly disconnecting now"
                  self.disconnect_client @userhash[username]
                  return nil
               end
               return @userhash[username]
            else
               client = @userhash[username]
               $log.debug "[NCMLogin] Invalidating login for username #{username}"

               self.disconnect_client client

               return nil
            end
         rescue Errno::ENOTCONN => notconne
            $log.debug "[NCMLogin] Experienced Errno:ENOTCONN for client for username #{username}, invalidating"
            
            client = @userhash[username]
            self.disconnect_client client

            return nil
         end
      end
      # Using the live authentication database, returns the username for a particular
      # client socket, if any. Returns nil if there's a problem or that socket isn't associated
      # with a username.
      def get_username(client)
         return @clienthash[client]
      end
      # Checks that a user exists at all in MySQL. Returns true/false.
      def user_exists?(username)
         q = @mysql.query("SELECT * FROM login_users WHERE username='#{Mysql.quote(username)}'")
         return q.num_rows > 0
      end
      # Checks that a user is in the authentication database and is online. Returns true/false.
      def user_online?(username)
         return false unless @userhash.key? username
         return false if @userhash[username].closed?
         return true
      end
      # Overridden to connect to MySQL and initialize a new authentication database.
      def on_registry_add
         $log.debug "[NCMLogin] Connecting to database..."
         @mysql = Mysql.new @mysql_host, @mysql_user, @mysql_pass
         @mysql.reconnect = true
         @mysql.select_db @mysql_db
         @userhash = Hash.new
         @clienthash = Hash.new
      end
      # Overridden to disconnect from MySQL.
      def on_registry_delete
         $log.debug "[NCMLogin] Disconnecting from database..."
         begin
            @mysql.close
         rescue => e
            # do nothing, we don't care
         end
      end
      # Retrieves a user by database ID.
      # Returns a hash with:
      # username::         username of the user
      # password::         hashed password
      def user_by_id(id)
         q = @mysql.query("SELECT username,password FROM login_users WHERE id=#{Mysql.quote(id)}")
         return nil unless q.num_rows > 0
         row = q.fetch_hash
         return {:username => row['username'], :password => row['password']}
      end
      # Retrieves a database ID by username.
      def id_by_username(username)
         q = @mysql.query("SELECT id FROM login_users WHERE username='#{Mysql.quote(username)}'")
         return nil unless q.num_rows > 0
         row = q.fetch_hash
         return row['id']
      end

      #--
      ## MESSAGE METHODS
      #++

      # Processes type 'login_request'.
      # This is where it all starts.
      # * If authentication succeeds, the client/username/passhash is added to the authentication
      #   database and an 'accept_login' message is sent back. Signin hooks are called that have been
      #   registered with NCMLogin.
      # * If authentication fails for any reason, a 'reject_login' message is sent back with the reason.
      def msg_login_request (client, header, content)
            successful = false
            username = content.elements["username"]
            password = content.elements["password"]
            hash = password.attributes["hash"]
            raise NCError::NCPException, "#{hash} hash unsupported" if hash != "SHA512" and hash != "SHA-512"
            $log.debug "[NCMLogin] Processing login for '#{username.text}' with password hash (#{password.attributes["hash"]}) '#{password.text}'"

            response_header = REXML::Element.new 'modulemessage'
            response_properties = REXML::Element.new 'properties'
            response_header.add response_properties
            response_content = REXML::Element.new 'content'

            # check u/p validity

            if password.text.length != 128
               response_properties.attributes["type"] = "reject_login"
               response_content.text "Your client did not send a valid password hash."
            elsif username.text !~ /^[A-Za-z0-9]+$/
               response_properties.attributes["type"] = "reject_login"
               response_content.text = "Usernames can only contain letters and numbers."
            else

               # check database
               q = @mysql.query("SELECT * FROM login_users WHERE username='#{Mysql.quote(username.text)}'")
               if q.num_rows > 0
                  row = q.fetch_hash
                  if row['password'] == password.text
                     if self.user_online? username.text
                        $log.debug "[NCMLogin] Authentication passed for #{username.text} but user already signed on and active"
                        response_properties.attributes['type'] = "reject_login"
                        response_content.text = "You are already logged in elsewhere. Sign off there first."
                     else
                        $log.debug "[NCMLogin] Authentication passed for #{username.text}, adding to @userhash"
                        unless @clienthash[client].nil?
                           k = @clienthash[client]
                           $log.debug "[NCMLogin] Invalidating old login with username #{k} (client #{client})"
                           self.disconnect_client client
                        end

                        @userhash[username.text] = client
                        @clienthash[client] = username.text

                        successful = true

                        response_properties.attributes["type"] = "accept_login"
                        response_content.add username
                     end
                  else
                     response_properties.attributes["type"] = "reject_login"
                     response_content.text = "Incorrect password."
                  end
               else
                  response_properties.attributes["type"] = "reject_login"
                  response_content.text = "Username not found. You must register first."
               end

            end

            self.communicator.send_message client, response_header, response_content

            if successful
               $log.debug "[NCMLogin] Calling signin hooks"
               @signin_hooks.each do |h|
                  $log.debug "[NCMLogin] -- Calling #{h.to_s}"
                  h.call(username.text, client)
               end
               $log.debug "[NCMLogin] Done calling signin hooks"
            end

            return nil
      end
      
      # Processes type 'logout'.
      # * If the username is in the live authentication database they are taken out, effectively
      #   logging them out. Signout hooks are called as registered with NCMLogin.
      # * If the client that sent the message never actually signed in, there's no harm done.
      #   Signout hooks are not called erroneously.
      def msg_logout (client, header, content)
            username = get_username(client)

            $log.debug "[NCMLogin] Client signed out (username at the time: #{(username.nil? ? 'None' : username)})."
            
            self.disconnect_client client

            return nil
      end
   end

end
