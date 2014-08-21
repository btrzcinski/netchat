#!/usr/bin/ruby

# Author: Barnett Trzcinski
# This code is released into the public domain.

require 'socket'

Port = 45287

server = TCPServer.new(Port)
puts "Server started (port #{Port})"
while true
	client = server.accept
	puts "Connection established"
	while (s = client.gets) do
		client.puts s
	end
	puts "Connection terminated"
end

