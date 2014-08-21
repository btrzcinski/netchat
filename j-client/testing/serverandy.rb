#!/usr/bin/ruby

# Author: Barnett Trzcinski
# Copyright(C) 2006 Barnett Trzcinski and the NetChat Project.

# This file is part of NetChat Server.
#
# NetChat Server is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# NetChat Server is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with NetChat Server; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

require 'socket'
require 'openssl'
require 'optparse'
require 'rexml/document'
require 'logger'

trap("CLD", "SIG_IGN"); # fixes <defunct> processes

SSLKey = "ssl/key.pem"
SSLCert = "ssl/cert.pem"

$options = {
	:log => Logger::INFO,
	:logtarget => STDOUT,
	:oldlogs => 10,
	:maxlogsize => 1024000, #bytes
	:port => 45287,
	:ssl => true,
	:xml => true
}

OptionParser.new do |opts|
	opts.banner = "Usage: #{__FILE__} [options]"

	opts.on("-l","--log LEVEL", [:DEBUG, :INFO, :WARN, :ERROR, :FATAL],  "Logging level (default: INFO)") do |v|
		case v
			when :DEBUG
				$options[:log] = Logger::DEBUG
			when :INFO
				$options[:log] = Logger::INFO
			when :WARN
				$options[:log] = Logger::WARN
			when :ERROR
				$options[:log] = Logger::ERROR
			when :FATAL
				$options[:log] = Logger::FATAL
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
	
	opts.on("-s","--[no-]ssl","Use OpenSSL (default: yes)") do |v|
		$options[:ssl] = v
	end
	
	opts.on("-X","--[no-]xml","Use XML for data (default: yes)") do |v|
		$options[:xml] = v
	end
end.parse!

$log = if $options[:logtarget].is_a? IO
	Logger.new($options[:logtarget])
      else
        Logger.new($options[:logtarget],$options[:oldlogs],$options[:maxlogsize])
      end
$log.level = $options[:log]
$log.progname = __FILE__
$log.datetime_format = "%Y-%m-%d %H:%M:%S PID"

if $options[:log] <= Logger::DEBUG
	$log.debug "Options:"
	$options.each { |k, v| $log.debug "#{k} => #{v}" }
end

begin
	# Added explicit bind address of "0.0.0.0" because 
	# on (at least) FreeBSD, there is some unexpected behavior
	# causing the server to bind only to IPv6 addresses or something
	# like that.
	#
	# Right now we're preferring IPv4.
	
	$server = TCPServer.new("0.0.0.0", $options[:port])
	$log.info "Server started (port #{$options[:port]})"
rescue => e
	$log.fatal "Fatal error occurred in initialization: #{e}"
	raise
end

if $options[:ssl]
	begin
		ctx = OpenSSL::SSL::SSLContext.new
		ctx.key = OpenSSL::PKey::RSA.new(File::read(SSLKey))
		ctx.cert = OpenSSL::X509::Certificate.new(File::read(SSLCert))
		ctx.verify_mode = OpenSSL::SSL::VERIFY_NONE
	
		sslserver = OpenSSL::SSL::SSLServer.new($server, ctx)
		$log.info "SSL initialized"
	rescue => e
		$log.fatal "Fatal SSL error: #{e}"
		raise
	end
end

class NCPException < RuntimeError
end

def send_message(data)
	client = data[:client]
	if client.nil?
		raise NCPException, "NCP send_message needs a client to send message to"
	end
	type = data[:globaltype]
	if type.nil?
		raise NCPException, "NCP send_message needs a type to construct message"
	end

	# construction of some of the XML document to start
	document = REXML::Document.new
	message = REXML::Element.new "message"
	document.add_element message
	header = REXML::Element.new "header"
	message.add_element header
	content = REXML::Element.new "content"
	message.add_element content
	global = REXML::Element.new "global"
	header.add_element global
	globalproperties = REXML::Element.new "properties"
	global.add_element globalproperties

	case type
		when :server
			messagetype = data[:messagetype]
			if messagetype.nil?
				raise NCPException, "NCP send_message needs to know what server message to send"
			end
			globalproperties.add_attribute "type", "servermessage"
			servermessageheader = REXML::Element.new "message"
			header.add_element servermessageheader
			servermessageproperties = REXML::Element.new "properties"
			servermessageheader.add_element servermessageproperties
			case messagetype
				when :ack
					servermessageproperties.add_attribute "type", "ack"
				when :syn
					servermessageproperties.add_attribute "type", "syn"
				when :data
					servermessageproperties.add_attribute "type", "data"
					content.text = data[:content] unless data[:content].nil?
				else
					raise NCPException, "NCP send_message doesn't know how to send server message specified"
			end
		when :module
			raise NCPException, "NCP send_message doesn't know how to deal with modules yet"
		else
			raise NCPException, "NCP send_message doesn't know how to send anything but server or module messages"
	end

	string_out = ""
	document.write string_out

	$log.debug "-- Message sent to client: #{string_out}"

	client.puts string_out
end

def handle_message(xmldoc, client)
	string_in = ""
	xmldoc.write string_in
	$log.debug "-- Message received from client: #{string_in}"
	
	message = xmldoc.elements["message"]
	header = message.elements["header"]
	content = message.elements["content"]
	if header.nil? or content.nil? or message.nil?
		raise NCPException, "<message/>, <header/>, or <content/> are missing"
	end
	global = header.elements["global"]
	if global.nil?
		raise NCPException, "Global section required in header"
	end
	globalprops = global.elements["properties"]
	if globalprops.nil?
		raise NCPException, "Global properties missing"
	end
	globaltype = globalprops.attributes["type"]
	if globaltype.nil?
		raise NCPException, "Type attribute in global properties missing"
	end
	case globaltype
		when "modulemessage"
			raise NCPException, "This is a module message: modules are not implemented"
		when "servermessage"
			messageheader = header.elements["message"]
			if messageheader.nil?
				raise NCPException, "Invalid server message: <message/> must be in header"
			end
			messageprops = messageheader.elements["properties"]
			if messageprops.nil?
				raise NCPException, "Invalid server message: <properties/> element must be in server message header"
			end
			messagetype = messageprops.attributes["type"]
			if messagetype.nil?
				raise NCPException, "Invalid server message: type not specified"
			end
			case messagetype
				when "syn"
					begin
						send_message(:client => client, :globaltype => :server, :messagetype => :ack)
					rescue NCPException => e
						raise
					end
				when "ack"
					begin
						send_message(:client => client, :globaltype => :server, :messagetype => :syn)
					rescue NCPException => e
						raise
					end
				when "echo"
					begin
						send_message(:client => client, :globaltype => :server, :messagetype => :data, :content => content.text)
					rescue NCPException => e
						raise
					end
				else
					raise NCPException, "Unrecognized server message: #{messagetype.to_s}"
			end
		else
			raise NCPException, "Unrecognized global type"
	end
end

$log.info "Listening for clients"

loop do
	if $options[:ssl]
		begin
			client = sslserver.accept
		rescue OpenSSL::SSL::SSLError => ssle
			$log.error "There was a problem connecting a client:"
			$log.error "SSL communications invalid or unknown. Moving on."
			next
			client.close
		rescue Interrupt => i
			$log.fatal "Accept cycle interrupted"
			break
		rescue => e
			$log.fatal "Unknown error occurred in accept cycle: #{e}"
			raise
		end
	else
		begin
			client = $server.accept
		rescue Interrupt => i
			$log.fatal "Accept cycle interrupted"
			break
		rescue => e
			$log.fatal "Unknown error occurred in accept cycle: #{e}"
			raise
		end
	end
	fork do
		domain, port, name, addr = client.peeraddr
		$log.info "Connection established from #{name} (#{addr})"
		begin 
			while s = client.gets
				unless $options[:xml]
					$log.debug "Received message."
					client.write s
				else
					$log.debug "Received a message, firing handler"
					begin
						xmldoc = REXML::Document.new s
						handle_message(xmldoc, client)
					rescue REXML::ParseException => pe
						$log.warn "Parse exception constructing XML tree from client data"
						next
					rescue NCPException => ne
						$log.warn "NCPException handling message: #{ne}"
					rescue => e
						$log.fatal "Unknown error occurred in handling message: #{e}"
						raise
					end
				end
			end
			client.close
		rescue Interrupt => i
			$log.error "Client interrupted"
		rescue => e
			$log.fatal "Unknown error occurred in client: #{e}"
			raise
		end
		$log.info "Connection terminated for #{name} (#{addr})"
		exit
	end
end

$server.close
$log.info "Server shut down."
$log.close

