# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file serves as the bootstrap for the rest of the server code,
# requiring all needed files and starting the main event loop in
# NCServer (ncserver.rb).

# Check that this file was directly executed.
if $0 == __FILE__
   puts
   puts "
NetChat Server
Copyright(C) 2006-2007 Barnett Trzcinski and the NetChat Project.

This software is licensed under the GNU GPL v2.
Please see the enclosed file COPYING for further details.

"

   require 'rubygems'	

   require 'digest/sha2'
   require 'gserver'
   require 'highline'
   require 'logger'
   require 'mysql'
   require 'openssl'
   require 'optparse'
   require 'rexml/document'
   require 'socket'
   require 'timeout'

   require 'modland/NCMBase'
   require 'modland/NCMLogin'
   require 'modland/NCMChat'
   require 'modland/NCMFileTransfer'

   require 'ncadmin'
   require 'ncerror'
   require 'ncinit'
   require 'ncmodule'
   require 'ncp'
   require 'ncserver'

   # Fire up the main function. This will block.
   NCServer.main
end
