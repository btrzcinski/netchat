# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the definition for NCMFileTransfer within ModLand.
# If you wish to develop a module for the server, see NCMBase.

# This module contains all base module classes (those directly
# loaded by the server).
module ModLand

   # NCMFileTransfer is a basic file transfer module which uses Base64 to send
   # files between clients in chunks. The server relays the chunks between
   # clients which means that the clients do not need to directly connect.
   #
   # TODO: Introduce bandwidth throttling to make sure that the server relay
   # of files does not bog down more important communication.
   class NCMFileTransfer < NCMBase
      # Defines the basic variables. No database is necessary.
      # Depends on NCMLogin.
      def initialize
         @name = 'filetransfer'
         @protocol_version = '0.1a'
         @depends = ["login"]

         @counter = 1;
      end
      def get_id
         return (@counter += 1)
      end
      # Consistency with other modules in the representation of the loaded/unloaded messages.
      def on_client_load(c)
         domain, port, name, addr = nil, nil, nil, nil
         begin
            domain, port, name, addr = c.peeraddr
         rescue => se
            $log.debug "[NCMFileTransfer] Error getting name/address for client"
         end
         name = "unknown" if name.nil?
         addr = "unknown" if addr.nil?
         $log.debug "[NCMFileTransfer] Client #{name} (#{addr}) loaded this module"
      end
      # Consistency with other modules in the representation of the loaded/unloaded messages.
      def on_client_unload(c)
         domain, port, name, addr = nil, nil, nil, nil
         begin
            domain, port, name, addr = c.peeraddr
         rescue => se
            $log.debug "[NCMFileTransfer] Error getting name/address for client"
         end
         name = "unknown" if name.nil?
         addr = "unknown" if addr.nil?
         $log.debug "[NCMFileTransfer] Client #{name} (#{addr}) unloaded this module"
      end
      # This message is used by the sending client to initiate a request. The server module
      # just relays it to the destination.
      def msg_file_transfer_request(client, header, content)
         to = content.elements['to'].text
         toc = self.moduleaccessor.access("login").get_client(to)
         if toc.nil?
            $log.warn "[NCMFileTransfer] file_transfer_request had a to user '#{to}' who is not available"
         else
            $log.debug "[NCMFileTransfer] Relaying request to transfer file '#{content.elements['filename'].text}' from #{content.elements['from'].text} to #{to}"
            self.communicator.send_message toc, header, content
         end
      end
      # This message is used by the receiving client to accept/decline a request. The
      # server module just relays it to the destination.
      def msg_file_transfer_decision(client, header, content)
         from = content.elements['from'].text
         fromc = self.moduleaccessor.access("login").get_client(from)
         if fromc.nil?
            $log.warn "[NCMFileTransfer] file_transfer_decision had a from user '#{from}' who is not available"
         else
            $log.debug "[NCMFileTransfer] Relaying decision to transfer file from #{content.elements['from'].text} to #{self.moduleaccessor.access("login").get_username(client)}"
            self.communicator.send_message fromc, header, content
         end
      end
      # Request for a file chunk from the receiving client.
      def msg_file_chunk_request(client, header, content)
         from = content.elements['route'].attributes['from']
         to = content.elements['route'].attributes['to'] # this sent the request
         fromc = self.moduleaccessor.access("login").get_client(from) 
         if fromc.nil?
            $log.warn "[NCMFileTransfer] file_chunk_request had a from user '#{from}' who is not available"
         else
            $log.debug "[NCMFileTransfer] Relaying chunk request for #{content.elements['chunk'].attributes['number']} from #{from} to #{to}"
            self.communicator.send_message fromc, header, content
         end
      end
      # This message is a chunk of the file being transferred. Again, it is relayed to the 
      # destination.
      def msg_file_chunk(client, header, content)
         from = content.elements['route'].attributes['from']
         to = content.elements['route'].attributes['to']
         toc = self.moduleaccessor.access("login").get_client(to)
         if toc.nil?
            $log.warn "[NCMFileTransfer] file_chunk had a to user '#{to}' who is not available"
         else
            $log.debug "[NCMFileTransfer] Relaying chunk #{content.elements['chunk'].attributes['number']}/#{content.elements['chunk'].attributes['maxchunk']} from #{from} to #{to}"
            self.communicator.send_message toc, header, content
         end
      end
      # Generates a unique ID and sends it back for use by the clients.
      def msg_file_transfer_id_request(client, header, content)
         id = self.get_id
         header.elements['properties'].attributes['type'] = 'file_transfer_id'
         
         ide = REXML::Element.new 'id'
         ide.text = id
         content.add ide
         
         return {:header => header, :content => content}
      end
      # Relays success from receiving client to sending client.
      def msg_transfer_complete(client, header, content)
         id = content.elements['id'].text
         success = if content.elements['success'].text == 'true'
                      true
                   elsif content.elements['success'].text == 'false'
                      false
                   else
                      nil
                   end
         $log.warn "[NCMFileTransfer] Value for success not recognized in transfer_complete message from #{self.moduleaccessor.access('login').get_username(client)} for file transfer ID #{id}" if success.nil?
         if content.elements['from'].nil?
            $log.warn "[NCMFileTransfer] Discarded incomplete transfer_complete message from #{self.moduleaccessor.access('login').get_username(client)}, no 'from' element"
            return
         end
         if content.elements['to'].nil?
            $log.warn "[NCMFileTransfer] Discarded incomplete transfer_complete message from #{self.moduleaccessor.access('login').get_username(client)}, no 'to' element"
            return
         end
         from = content.elements['from'].text
         to = content.elements['to'].text
         fromc = self.moduleaccessor.access('login').get_client from
         if fromc.nil?
            $log.warn "[NCMFileTransfer] transfer_complete had a from user '#{from}' who is not available"
         else
            $log.debug "[NCMFileTransfer] Relaying success of transfer #{id} from #{from} to #{to}"
            self.communicator.send_message fromc, header, content
         end
      end
   end
end
