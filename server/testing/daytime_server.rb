#!/usr/bin/ruby

# Author: Barnett Trzcinski
# This code is released into the public domain.

require 'webrick'

s = WEBrick::GenericServer.new( :Port => 2000 )
trap("INT") { s.shutdown }
s.start{ |sock|
	sock.print(Time.now.to_s + "\r\n")
}

