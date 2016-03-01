# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the definition for NCMMail within ModLand.
# If you wish to develop a module for the server, see NCMBase.

# This module contains all base module classes (those directly
# loaded by the server).
module ModLand
   
   # This is a mail module for the server, capable of handling
   # per-user folder hierarchies, cc/bcc, and attachments.
   class NCMMail < NCMBase
      # Nothing more than what a module like NCMChat does.
      def initialize
         @name = 'mail'
         @protocol_version = '0.1a'
         @depends = ["login"]
         
         @mysql_host = "localhost"
         @mysql_user = "netchat"
         @mysql_pass = @mysql_user.reverse
         @mysql_db = "netchat"
      end
      # Overridden to connect to MySQL.
      def on_registry_add
         $log.debug "[NCMMail] Connecting to database..."
         @mysql = Mysql2::Client.new :host => @mysql_host, :username => @mysql_user, :password => @mysql_pass, :reconnect => true, :database => @mysql_db
      end
      # Overridden to disconnect from MySQL.
      def on_registry_delete
         $log.debug "[NCMMail] Disconnecting from database..."
         begin
            @mysql.close
         rescue => e
            # do nothing, we don't care
         end
      end
      # Shorthand for calling login to check for authorization of a client.
      def checkauth (client)
         return self.moduleaccessor.access("login").authorized? :client => client
      end
      # Partially overridden to set up mail framework for users who have never used the system before,
      # in order to avoid confused message methods. Also checks for authorization.
      def parse (client, header, content)
         return nil unless checkauth(client) # prevents any nefarious message handling if unauthorized
         checksetup(client) # checks that a client has the appropriate mail folders in place
         # proceed with base functionality
         super(client, header, content)
      end
      # Turns a folder ID into a folder name.
      def id_to_folder(folderid)
         q = @mysql.query("SELECT folder FROM mail_folders WHERE id=#{@mysql.escape(folderid)}")
         return nil unless q.count > 0
      end
      # If a client has no folders set up for the mail system, go for it.
      def checksetup(client)
         username = self.moduleaccessor.access("login").get_username client
         userid = self.moduleaccessor.access("login").id_by_username(username)
      
         q = @mysql.query("SELECT * FROM mail_folders WHERE user_id=#{@mysql.escape(username)}")
         if q.count == 0
            $log.warn "[NCMMail] User #{username} has no mail folders, setting them up"
            @mysql.query("INSERT INTO mail_folders (user_id, parent_id, folder) VALUES (#{@mysql.escape(userid)}, -1, 'Inbox')")
            @mysql.query("INSERT INTO mail_folders (user_id, parent_id, folder) VALUES (#{@mysql.escape(userid)}, -1, 'Outbox')")
            @mysql.query("INSERT INTO mail_folders (user_id, parent_id, folder) VALUES (#{@mysql.escape(userid)}, -1, 'Sent')")
         end
      end

      #--
      ## MESSAGE METHODS
      #++

      # Processes type 'folder_list_request'.
      # This is when a client wants to get a list of mail folders, such as Inbox and Sent Items.
      def msg_folder_list_request (client, header, content)
         q = @mysql.query("SELECT * FROM mail_folders WHERE user_id=#{@mysql.escape(self.moduleaccessor.access("login").id_by_username(self.moduleaccessor.access("login").get_username(client)))}")
         $log.warn "[NCMMail] User #{self.moduleaccessor.access("login").get_username(client)} does not have any mail folders" if q.count == 0
         response = {:header => REXML::Element.new 'modulemessage', :content => REXML::Element.new 'content'}
         response[:header].add REXML::Element.new('properties')
         response[:header].elements['properties'].attributes['type'] = 'folder_list'
         
         folders = []
         parents = {}
         q.each { |h|
            ef = REXML::Element.new 'folder'
            ef.attributes['name'] = h['folder']
            folders[h['folder']] = ef
            parents[h['folder']] = h['parent_id']
         }
         folders.each { |n,f|
            parentid = parents[n]
            unless parentid < 0
               parent = self.id_to_folder parentid
               folders[parent].add f
               folders.delete n
            end
         }
         folders.each_value { |f|
            response[:content].add f
         }
         
         return response
      end
      # Processes type 'get_mail_for_folder_request'.
      # This is when a client wants to retrieve a list of message headers for a folder.
      def msg_get_mail_for_folder_request (client, header, content)
         # TODO
      end
      # Processes type 'retrieve_message_request'.
      # This is when a client wants to get the complete details of a particular message.
      def msg_retrieve_message_request (client, header, content)
         # TODO
      end
      # Processes type 'retrieve_attachment_request'.
      # This is when a client wants to get an attachment previously stipulated as being part of a message.
      def msg_retrieve_attachment_request (client, header, content)
         # TODO
      end
      # Processes type 'send_message'.
      # This is when a client wants to queue a completed message for transmission.
      def msg_send_message (client, header, content)
         # TODO
      end
   end
   
end
