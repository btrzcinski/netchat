#!/usr/bin/env ruby

require 'rubygems'
require 'mysql2'
require 'digest/sha2'
require 'password'

if ARGV.length < 1
   puts "Usage: users.rb [add|change-password|remove|list] [username]"
   exit
end

MysqlHost = 'localhost'
MysqlUser = 'netchat'
MysqlPass = 'tahcten'
MysqlDb = 'netchat'

mysql = Mysql2::Client.new :host => MysqlHost, :username => MysqlUser, :password => MysqlPass, :reconnect => true, :database => MysqlDb

case ARGV[0]
when "add"
   if ARGV.length < 2
      puts "ERROR: Username not specified."
      exit
   end
   pass = Password.get("Password:")
   conf = Password.get("Confirm password:")
   while (conf != pass)
      puts "Did not match"
      pass = Password.get("Password:")
      conf = Password.get("Confirm password:")
   end

   hash = Digest::SHA512.hexdigest(pass)
   puts "Adding #{ARGV[1]} with password hashed to #{hash}"
   mysql.query("INSERT INTO login_users (username, password) VALUES ('#{mysql.escape(ARGV[1])}', '#{mysql.escape(hash)}')")
   puts "Done"
when "remove"
   if ARGV.length < 2
      puts "Give me a username"
      exit
   end
   puts "Deleting #{ARGV[1]}"
   mysql.query("DELETE FROM login_users WHERE username='#{mysql.escape(ARGV[1])}'")
   puts "Done"
when "change-password"
   if ARGV.length < 2
      puts "Give me a username"
      exit
   end
   puts "Changing password for #{ARGV[1]}"
   pass = Password.get("Password:")
   conf = Password.get("Confirm password:")
   while (conf != pass)
      puts "Did not match"
      pass = Password.get("Password:")
      conf = Password.get("Confirm password:")
   end

   hash = Digest::SHA512.hexdigest(pass)
   mysql.query("UPDATE login_users SET password='#{mysql.escape(hash)}' WHERE username='#{mysql.escape(ARGV[1])}'")
   puts "Done"
when "list"
   users = []
   q = mysql.query("SELECT * FROM login_users")
   q.each { |h| users << h['username'] }
   puts "Users: #{users.sort.join(", ")}"
else
   puts "I only take add, remove, or list as an action"
end

mysql.close
