# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the definition for NCMChat.

# This module contains all base module classes (those directly
# loaded by the server).
module ModLand

   # A one-to-one instant messaging module that depends on NCMLogin for log
   # in/out information. It stores friends and backlogged messages in MySQL.
   class NCMChat < NCMBase
      # The constructor defines a few more MySQL configuration variables. Feel
      # free to change these in the code directly until a configuration file
      # system is in place.
      #
      # mysql_host::         MySQL server name/IP address
      # mysql_user::         Username
      # mysql_pass::         Password
      # mysql_db::           Database name
      def initialize
         @name = 'chat'
         @protocol_version = '0.1a'
         @depends = ["login"]

         @mysql_host = "localhost"
         @mysql_user = "netchat"
         @mysql_pass = @mysql_user.reverse
         @mysql_db = "netchat"

         @loadedclients = Array.new

         @groupchatrooms = Hash.new
         
         @usermessages = Hash.new
      end
      # NCMChat keeps track of all clients which have loaded it (in order to prevent
      # premature messages, since chat can involve the server initiating message sending)
      # and uses this callback to add a client to its list of loaded clients.
      def on_client_load(c)
         domain, port, name, addr = nil, nil, nil, nil
         begin
            domain, port, name, addr = c.peeraddr
         rescue => se
            $log.debug "[NCMChat] Error getting name/address for client"
         end
         name = "unknown" if name.nil?
         addr = "unknown" if addr.nil?
         $log.debug "[NCMChat] Client #{name} (#{addr}) loaded this module"
         @loadedclients << c
      end
      # NCMChat keeps track of all clients which have loaded it (in order to prevent
      # premature messages, since chat can involve the server initiating message sending)
      # and uses this callback to remove a client from its list of loaded clients.
      def on_client_unload(c)
         domain, port, name, addr = nil, nil, nil, nil
         begin
            domain, port, name, addr = c.peeraddr
         rescue => se
            $log.debug "[NCMChat] Error getting name/address for client"
         end
         name = "unknown" if name.nil?
         addr = "unknown" if addr.nil?
         $log.debug "[NCMChat] Client #{name} (#{addr}) unloaded this module"
         @loadedclients.delete c
      end
      # Called by NCMLogin's callback system when a user logs in or out. This
      # method will send out "signed in" and "signed out" messages to clients
      # based on the friends list.
      def statuschange(u, c)
         response_header = REXML::Element.new 'modulemessage'
         response_properties = REXML::Element.new 'properties'
         response_header.add response_properties
         response_properties.attributes['type'] = 'friend_status_update'
         response_content = REXML::Element.new 'content'
         response_content.add(REXML::Element.new('username'))
         response_content.elements['username'].text = u
         response_content.elements['username'].attributes['online'] = self.moduleaccessor.access("login").user_online?(u).to_s


         q = @mysql.query("SELECT username FROM chat_friends WHERE buddy='#{@mysql.escape(u)}'")
         q.each do |row|
            dest_client = self.moduleaccessor.access("login").get_client row['username']
            unless dest_client.nil? or dest_client.closed?
               begin
                  domain, port, name, addr = dest_client.peeraddr
                  if not @loadedclients.include? dest_client
                     $log.debug "[NCMChat] Skipping client #{name} (#{addr}) because its client chat module is not loaded"
                  else
                     $log.debug "[NCMChat] Sending notification to client #{name} (#{addr}) about #{u}"
                     begin
                        self.communicator.send_message dest_client, response_header, response_content
                     rescue => e
                        $log.warn "[NCMChat] Error sending status change notification to client for #{row['username']}"
                        $log.debug "[NCMChat] Specific error: #{e}"
                        $log.debug "[NCMChat] Backtrace: #{e.backtrace.join("\n")}"
                     end
                  end
               rescue Errno::ENOTCONN => enotconn
                  $log.warn "[NCMChat] Encountered ENOTCONN processing client, disconnecting now"
                  self.moduleaccessor.access("login").disconnect_client dest_client
               end
            end
         end

         unless self.moduleaccessor.access("login").user_online?(u)
            @groupchatrooms.each do |name, chatroom|
               if chatroom.usernames.include? u
                  $log.warn "[NCMChat] User #{u} disconnected without unsubscribing from chat room #{name}"
                  $log.warn "[NCMChat] User #{u} has been forcibly unsubscribed from chat room #{name}"
                  chatroom.usernames.delete u
                  # event
                  self.room_unsubscribe_event(u, name)
               end
            end

            # clear message
            @usermessages.delete u
         else
            @usermessages[u] = "" if @usermessages[u].nil?
         end

         self.cleanup_chatrooms
      end

      # Notifies clients about message changes.
      def messagechange(u, c)
         response_header = REXML::Element.new 'modulemessage'
         response_properties = REXML::Element.new 'properties'
         response_header.add response_properties
         response_properties.attributes['type'] = 'message_update'
         response_content = REXML::Element.new 'content'
         response_content.add(REXML::Element.new('username'))
         response_content.elements['username'].text = u
         response_content.add(REXML::Element.new('message'))
         response_content.elements['message'].text = @usermessages[u]

         q = @mysql.query("SELECT username FROM chat_friends WHERE buddy='#{@mysql.escape(u)}'")
         q.each do |row|
            dest_client = self.moduleaccessor.access("login").get_client row['username']
            unless dest_client.nil? or dest_client.closed?
               domain, port, name, addr = dest_client.peeraddr
               if not @loadedclients.include? dest_client
                  $log.debug "[NCMChat] Skipping client #{name} (#{addr}) because its client chat module is not loaded"
               else
                  $log.debug "[NCMChat] Sending notification to client #{name} (#{addr}) about #{u}"
                  begin
                     self.communicator.send_message dest_client, response_header, response_content
                  rescue => e
                     $log.warn "[NCMChat] Error sending status change notification to client for #{row['username']}"
                     $log.debug "[NCMChat] Specific error: #{e}"
                     $log.debug "[NCMChat] Backtrace: #{e.backtrace.join("\n")}"
                  end
               end
            end
         end

         @usermessages.delete(u) unless self.moduleaccessor.access('login').user_online?(u)
      end
      # Overridden to:
      # * Connect and establish a MySQL database hook
      # * Register callbacks with NCMLogin
      def on_registry_add
         $log.debug "[NCMChat] Connecting to database..."
         @mysql = Mysql2::Client.new :host => @mysql_host, :username => @mysql_user, :password => @mysql_pass, :reconnect => true, :database => @mysql_db
         $log.debug "[NCMChat] Registering NCMLogin hooks..."
         self.moduleaccessor.access("login").register_signin_hook(self.method(:statuschange))
         self.moduleaccessor.access("login").register_signout_hook(self.method(:statuschange))
      end
      # Overriden to:
      # * Disconnect from the MySQL database
      # * Unregister callbacks with NCMLogin
      def on_registry_delete
         $log.debug "[NCMChat] Disconnecting from database..."
         begin
            @mysql.close
         rescue => e
            # do nothing, we don't care
         end
         $log.debug "[NCMChat] Unregistering NCMLogin hooks..."
         self.moduleaccessor.access("login").unregister_signin_hook(self.method(:statuschange))
         self.moduleaccessor.access("login").unregister_signout_hook(self.method(:statuschange))
      end
      # Convenience method to check if a client has logged in before processing any message from them.
      # If they are not, the method sends an "unauthorized" message via sendauthfail before returning false.
      def checkauth (client)
         auth = self.moduleaccessor.access("login").authorized? :client => client
         return true if auth

         sendauthfail client
         return false
      end
      # Sends a "You are not authorized" error message to the client.
      def sendauthfail (client)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']

         response_properties.attributes["type"] = "error_message"
         error = REXML::Element.new 'message'
         error.text = 'Your client is not authorized. Log in with the \'login\' module first.'
         response_content.add error
         self.communicator.send_message client, response_header, response_content
      end
      # Convenience method to create a skeleton module message for use in constructing a response.
      def make_skeleton_message
         response_header = REXML::Element.new 'modulemessage'
         response_properties = REXML::Element.new 'properties'
         response_header.add response_properties
         response_content = REXML::Element.new 'content'
         return {:header => response_header, :content => response_content}
      end

      #--
      ## MESSAGE HANDLERS
      #++

      # Handles the type 'message'.
      # * If the client the message is destined for is online at the moment, it is routed to them
      #   and sent accordingly.
      # * If the client the message is destined for is offline, it is added to the backlog, and a
      #   warning message about their offline status is sent back to the sending client.
      def msg_message (client, header, content)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']

         src_name = self.moduleaccessor.access("login").get_username client

         # split off, is it a private msg or a room msg?
         msg_properties = content.elements['properties']
         if msg_properties.nil?
            # uh oh, old client with bad NCP
            response_properties.attributes["type"] = 'error_message'
            response_content.add(REXML::Element.new('message'))
            response_content.elements['message'].text = 'Your message could not be routed because an NCP properties block for the message was not defined. Please update your client to the newest revision.'
            $log.warn "[NCMChat] Client for username #{src_name} attempted to use old chat NCP for message routing"
            return {:header => response_header, :content => response_content}
         end
         msg_type = msg_properties.attributes['type']
         if msg_type == "private"
            # route as normal.
            dest_name = content.elements["username"].text
            # check for blocks
            q = @mysql.query("SELECT * FROM chat_blocks WHERE username='#{@mysql.escape(src_name)}' AND block='#{@mysql.escape(dest_name)}'")
            if q.count > 0
               # blocked!
               $log.debug "[NCMChat] User #{src_name} tried to send message to #{dest_name} who has blocked #{src_name}"
               response_properties.attributes["type"] = 'error_message'
               response_content.add(REXML::Element.new('message'))
               response_content.elements['message'].text = "The user #{dest_name} blocked you."
               return {:header => response_header, :content => response_content}
            else
               dest_message = content.elements["message"].text
               dest_client = self.moduleaccessor.access("login").get_client dest_name
               unless dest_client.nil? or dest_client.closed?
                  response_properties.attributes["type"] = 'message'
                  response_content.add(REXML::Element.new('properties'))
                  response_content.add(REXML::Element.new('username'))
                  response_content.add(REXML::Element.new('message'))
                  response_content.elements['username'].text = src_name
                  response_content.elements['message'].text = dest_message
                  response_content.elements['properties'].attributes['type'] = 'private'
                  self.communicator.send_message dest_client, response_header, response_content
               else
                  if self.moduleaccessor.access("login").user_exists? dest_name
                     @mysql.query("INSERT INTO chat_backlog (sent, source, destination, message) VALUES (NOW(), '#{@mysql.escape(src_name)}', '#{@mysql.escape(dest_name)}', '#{@mysql.escape(dest_message)}')")
                     response_properties.attributes["type"] = 'warning_message'
                     response_content.add(REXML::Element.new('message'))
                     response_content.elements['message'].text = 'Your buddy is not online, but they will receive your messages when they sign on.'
                     response_content.add(REXML::Element.new('username'))
                     response_content.elements['username'].text = dest_name
                     return {:header => response_header, :content => response_content}
                  else
                     $log.debug "[NCMChat] User #{src_name} tried to send message to #{dest_name} who does not exist"
                     response_properties.attributes["type"] = 'error_message'
                     response_content.add(REXML::Element.new('message'))
                     response_content.elements['message'].text = "The user #{dest_name} does not exist."
                     return {:header => response_header, :content => response_content}
                  end
               end
            end
         elsif msg_type == "room"
            # route to room.
            dest_room = content.elements["room"].text
            dest_message = content.elements["message"].text
            room_obj = @groupchatrooms[dest_room]
            if room_obj.nil?
               # error: room does not exist. subscribe to it to create it.
               response_properties.attributes["type"] = 'error_message'
               response_content.add(REXML::Element.new('message'))
               response_content.elements['message'].text = "The room '#{dest_room}' does not exist. Subscribe to it to create it."
               $log.debug "[NCMChat] Client with username #{src_name} attempted to send a message to nonexistant room #{dest_room}"
               return {:header => response_header, :content => response_content}
            elsif not room_obj.usernames.include? src_name
               # error: you need to subscribe first
               response_properties.attributes["type"] = 'error_message'
               response_content.add(REXML::Element.new('message'))
               response_content.elements['message'].text = "You must subscribe to room #{dest_room} first before you can send messages to it."
               $log.warn "[NCMChat] Client with username #{src_name} attempted to send message to room that they were not subscribed to."
               return {:header => response_header, :content => response_content}
            else
               # send to the room, skipping the sender of the message.
               room_obj.usernames.each do |member|
                  next if member == src_name
                  dest_name = member
                  dest_message = content.elements["message"].text
                  dest_client = self.moduleaccessor.access("login").get_client dest_name
                  unless dest_client.nil? or dest_client.closed?
                     response_properties.attributes["type"] = 'message'
                     response_content.add(REXML::Element.new('properties')) if response_content.elements['properties'].nil?
                     response_content.add(REXML::Element.new('username')) if response_content.elements['username'].nil?
                     response_content.add(REXML::Element.new('room')) if response_content.elements['room'].nil?
                     response_content.add(REXML::Element.new('message')) if response_content.elements['message'].nil?
                     response_content.elements['username'].text = src_name
                     response_content.elements['message'].text = dest_message
                     response_content.elements['room'].text = dest_room
                     response_content.elements['properties'].attributes['type'] = 'room'
                     self.communicator.send_message dest_client, response_header, response_content
                  else
                     # uhhh... that's bad
                     # they shouldn't be subscribed to begin with
                     # let it go since statuschange will probably pick up on it
                     $log.debug "[NCMChat] Message to #{member} in chat room #{dest_room} cannot be delivered because #{member} is offline"
                  end
               end # username loop
            end # room handler conditions
         else
            # do nothing and put in a log warning
            $log.warn "[NCMChat] Client for username #{src_name} attempted to route message of type #{msg_type} which is not recognized"
            return
         end

         nil
      end

      # Handles the type 'friends_list_request'.
      # * Looks up the list of friends for the sending client's username from MySQL and returns
      #   a list with their statuses as attributes.
      def msg_friends_list_request (client, header, content)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']

         username = self.moduleaccessor.access("login").get_username client
         q = @mysql.query("SELECT * FROM chat_friends WHERE username='#{@mysql.escape(username)}'")
         $log.debug "[NCMChat] Querying MySQL with: 'SELECT * FROM chat_friends WHERE username='#{@mysql.escape(username)}'"
         response_properties.attributes["type"] = 'friends_list'
         q.each do |row|
            f = REXML::Element.new 'username'
            f.text = row['buddy']
            f.attributes['online'] = self.moduleaccessor.access("login").user_online?(row['buddy']).to_s
            @usermessages[row['buddy']] = "" if @usermessages[row['buddy']].nil?
            f.attributes['message'] = @usermessages[row['buddy']]
            response_content.add f
         end

         return {:header => response_header, :content => response_content}
      end

      # Handles the type 'request_add_friend'.
      # * If the proposed friend exists and is not already on the user's list, it is added and an
      #   'authorize_friend' response is sent back.
      # * If the proposed friend does not exist or is already on the user's list, nothing happens and
      #   a 'deny_friend' response is sent back with the reason for the denial.
      def msg_request_add_friend (client, header, content)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']

         username = self.moduleaccessor.access("login").get_username client
         f = content.elements["username"].text
         ex = self.moduleaccessor.access("login").user_exists? f
         if ex
            q = @mysql.query("SELECT * FROM chat_friends WHERE username='#{@mysql.escape(username)}' AND buddy='#{@mysql.escape(f)}'")
            if q.count > 0
               response_properties.attributes["type"] = 'deny_friend'
               response_content.add(REXML::Element.new('username'))
               response_content.elements['username'].text = f
               response_content.add(REXML::Element.new("reason"))
               response_content.elements['reason'].text = 'You already have that user on your list.'
            else
               @mysql.query("INSERT INTO chat_friends (username, buddy) VALUES ('#{@mysql.escape(username)}', '#{@mysql.escape(f)}')")
               response_properties.attributes["type"] = 'authorize_friend'
               response_content.add content.elements['username']
               response_content.elements['username'].attributes['online'] = self.moduleaccessor.access("login").user_online?(f).to_s
               response_content.add(REXML::Element.new('message'))
               response_content.elements['message'].text = @usermessages[f]
            end
         else
            response_properties.attributes["type"] = 'deny_friend'
            response_content.add content.elements["username"]
            response_content.add(REXML::Element.new("reason"))
            response_content.elements["reason"].text = 'That user does not exist.'
         end
         return {:header => response_header, :content => response_content}
      end

      # Handles the type 'remove_friend'.
      # * Removes the friend from the friends list, regardless of its existence. Sends no response.
      def msg_remove_friend (client, header, content)
         username = self.moduleaccessor.access("login").get_username client
         buddy = content.elements["username"].text
         @mysql.query("DELETE FROM chat_friends WHERE username='#{@mysql.escape(username)}' AND buddy='#{@mysql.escape(buddy)}'")

         return nil
      end

      # Handles the type 'backlog_request'.
      # * Every backlog message is sent back to the client, sorted in the order they were received,
      #   and cleared from the database.
      # * An empty <content/> section is sent back if there are no backlogged messages.
      def msg_backlog_request (client, header, content)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']

         response_properties.attributes['type'] = 'backlog'
         username = self.moduleaccessor.access('login').get_username client
         q = @mysql.query("SELECT * FROM chat_backlog WHERE destination='#{@mysql.escape(username)}' ORDER BY sent ASC")
         q.each do |row|
            m = REXML::Element.new 'message'
            m.attributes['src'] = row['source']
            m.attributes['sent'] = row['sent']
            m.text = row['message']
            response_content.add m
         end
         @mysql.query("DELETE FROM chat_backlog WHERE destination='#{@mysql.escape(username)}'")

         return {:header => response_header, :content => response_content}
      end

      # Handles a block request. This is only denied if they are already in the block list or if they are
      # not a user (basically the same requirements as adding a friend).
      def msg_request_block_user (client, header, content)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']

         username = self.moduleaccessor.access("login").get_username client
         f = content.elements["username"].text
         ex = self.moduleaccessor.access("login").user_exists? f
         if ex
            q = @mysql.query("SELECT * FROM chat_blocks WHERE username='#{@mysql.escape(username)}' AND block='#{@mysql.escape(f)}'")
            if q.count > 0
               response_properties.attributes["type"] = 'deny_block_user'
               response_content.add(REXML::Element.new('username'))
               response_content.elements['username'].text = f
               response_content.add(REXML::Element.new("reason"))
               response_content.elements['reason'].text = 'You already have that user on your block list.'
            else
               @mysql.query("INSERT INTO chat_blocks (username, block) VALUES ('#{@mysql.escape(username)}', '#{@mysql.escape(f)}')")
               response_properties.attributes["type"] = 'authorize_block_user'
               response_content.add content.elements['username']
            end
         else
            response_properties.attributes["type"] = 'deny_block_user'
            response_content.add content.elements["username"]
            response_content.add(REXML::Element.new("reason"))
            response_content.elements["reason"].text = 'That user does not exist.'
         end
         return {:header => response_header, :content => response_content}
      end

      # Handles a block list request. Returns the list of users that they have currently blocked.
      def msg_block_list_request (client, header, content)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']

         username = self.moduleaccessor.access("login").get_username client
         q = @mysql.query("SELECT * FROM chat_blocks WHERE username='#{@mysql.escape(username)}'")
         $log.debug "[NCMChat] Querying MySQL with: 'SELECT * FROM chat_blocks WHERE username='#{@mysql.escape(username)}'"
         response_properties.attributes["type"] = 'block_list'
         q.each do |row|
            f = REXML::Element.new 'username'
            f.text = row['block']
            response_content.add f
         end

         return {:header => response_header, :content => response_content}
      end

      # Handles a block remove request. Ignores if that user isn't in the block list.
      def msg_remove_block (client, header, content)
         username = self.moduleaccessor.access("login").get_username client
         block = content.elements["username"].text
         @mysql.query("DELETE FROM chat_blocks WHERE username='#{@mysql.escape(username)}' AND block='#{@mysql.escape(block)}'")

         return nil
      end

      # Handles typing event notifications.
      def msg_typing_event (client, header, content)
         username = self.moduleaccessor.access("login").get_username client
         from = content.elements['from'].text
         to = content.elements['to'].text
         if username != from
            $log.warn "[NCMChat] User #{username} tried to tell #{to} that #{from} was typing"
         else
            toc = self.moduleaccessor.access("login").get_client to
            unless toc.nil?
               $log.debug "[NCMChat] Sending typing notification from #{from} to #{to}"
               self.communicator.send_message toc, header, content
            else
               $log.warn "[NCMChat] User #{from} tried to tell #{to} that they were typing but #{to} is unavailable"
            end
         end
      end

      # Sets messages and fires status updates.
      def msg_set_message (client, header, content)
         username = self.moduleaccessor.access('login').get_username client
         message = content.elements['message'].text
         oldmessage = @usermessages[username]
         @usermessages[username] = message
         
         self.messagechange(username, client) unless message == oldmessage
      end

      # Class to simply encapsulate data about chat rooms.
      class GroupChatRoom
         # The name of the chat room.
         attr_accessor :name
         # The usernames in the chat room.
         attr_accessor :usernames
         def initialize(name=nil, usernames=nil)
            @name = if name.nil?
                       ""
                    else
                       name
                    end
            @usernames = if usernames.nil?
                            Array.new
                         else
                            usernames
                         end
         end
      end

      # Utility method to clean out old chat rooms with no members.
      def cleanup_chatrooms
         @groupchatrooms.each do |name, chatroom|
            if chatroom.usernames.empty?
               $log.debug "[NCMChat] Empty room #{name} detected in cleanup, destroying"
               @groupchatrooms.delete name
            end
         end
      end

      # Responds to a room list request by providing a list of rooms. Usernames are omitted right now because 
      # of the potential of lots of data.
      def msg_room_list_request(client, header, content)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']

         response_properties.attributes['type'] = 'room_list'
         @groupchatrooms.each do |name, chatroom|
            roomxml = REXML::Element.new 'room'
            roomxml.attributes['name'] = name
            response_content.add roomxml
         end

         return {:header => response_header, :content => response_content}
      end

      # Subscribes the user to a room, ignoring if they are already subscribed. Starts a new chat room if the user
      # is the first member.
      def msg_room_subscribe(client, header, content)
         username = self.moduleaccessor.access("login").get_username client
         room = content.elements['room'].text
         room_obj = @groupchatrooms[room]
         if room_obj.nil?
            $log.debug "[NCMChat] New group chat room #{room} started"
            @groupchatrooms[room] = GroupChatRoom.new(room)
            room_obj = @groupchatrooms[room]
         end
         unless room_obj.usernames.include? username
            room_obj.usernames << username
            $log.debug "[NCMChat] Username #{username} subscribed to chat room #{room}"
            # event handler
            self.room_subscribe_event(username, room)
         end
         nil
      end

      # Unsubscribes the user from a room, deleting the room if they are the last to leave.
      def msg_room_unsubscribe(client, header, content)
         username = self.moduleaccessor.access("login").get_username client
         room = content.elements['room'].text
         room_obj = @groupchatrooms[room]
         unless room_obj.nil? or not room_obj.usernames.include? username
            room_obj.usernames.delete username
            $log.debug "[NCMChat] Username #{username} unsubscribed from chat room #{room}"
            if room_obj.usernames.empty?
               $log.debug "[NCMChat] Last person left room #{room}, destroying it"
               @groupchatrooms.delete room
            else
               # event handler
               self.room_unsubscribe_event(username, room)
            end
         end

         nil
      end

      # Returns information about the members of a room.
      def msg_room_query(client, header, content)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']

         response_properties.attributes['type'] = 'room_info'
         room = content.elements['room'].text
         response_content.add(REXML::Element.new('room'))
         response_content.elements['room'].attributes['name'] = room

         room_obj = @groupchatrooms[room]
         # will just return an empty list if the room doesn't exist, since the first person who joins
         # will make the room.
         unless room_obj.nil?
            room_obj.usernames.each do |username|
               userxml = REXML::Element.new 'username'
               userxml.text = username
               response_content.elements['room'].add userxml
            end
         end

         return {:header => response_header, :content => response_content}
      end

      # Utility method to handle sent messages in case of a subscribe/unsubscribe event (to notify
      # other members of the room).
      def room_change_event(username, room, event)
         m = make_skeleton_message
         response_header,response_content = m[:header], m[:content]
         response_properties = response_header.elements['properties']
         response_properties.attributes['type'] = 'room_event'
         response_content.add(REXML::Element.new('event'))
         response_content.elements['event'].attributes['type'] = event
         response_content.add(REXML::Element.new('room'))
         response_content.elements['room'].text = room
         response_content.add(REXML::Element.new('username'))
         response_content.elements['username'].text = username

         room_obj = @groupchatrooms[room]
         room_obj.usernames.each do |user|
            unless user == username
               cli = self.moduleaccessor.access("login").get_client user
               self.communicator.send_message cli, response_header, response_content
            end
         end
      end

      # Helper method. See room_change_event.
      def room_subscribe_event(username, room)
         self.room_change_event(username, room, 'subscribe')
      end

      # Helper method. See room_change_event.
      def room_unsubscribe_event(username, room)
         self.room_change_event(username, room, 'unsubscribe')
      end

      # Partially overriden to force an authentication check before processing _any_ message.
      # The original functionality is kept assuming that check succeeds.
      def parse (client, header, content)
         return nil unless checkauth(client)   # prevents any nefarious message handling if unauthorized
         # proceed with base functionality
         super(client, header, content)
      end
   end

end
