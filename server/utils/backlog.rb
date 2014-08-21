#!/usr/bin/env ruby

require 'mysql'

if ARGV.length < 1
   puts "Usage: backlog.rb [-c] username"
   exit
end

if ARGV[0] == "-c" and ARGV.length < 2
   puts "ERROR: Username not specified."
   exit
end

MysqlHost = 'localhost'
MysqlUser = 'netchat'
MysqlPass = 'tahcten'
MysqlDb = 'netchat'

mysql = Mysql.new MysqlHost, MysqlUser, MysqlPass
mysql.reconnect = true
mysql.select_db MysqlDb

unless ARGV[0] == "-c"
   q = mysql.query("SELECT * FROM chat_backlog WHERE destination='#{ARGV[0]}' ORDER BY sent ASC")
   q.each_hash do |h|
      puts "Sent #{h['sent']} from #{h['source']}, Message: #{h['message']}"
   end
   q.free
else
   mysql.query("DELETE FROM chat_backlog WHERE destination='#{ARGV[1]}'")
	 puts "Cleared #{ARGV[1]}'s backlog"
end

mysql.close


