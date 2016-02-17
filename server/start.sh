#!/bin/bash

### start.sh
# Author: Barnett Trzcinski
# Copyright(C) 2006-2007 Barnett Trzcinski and the NetChat Project.

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

if [ -f /usr/local/bin/ruby ]; then
	echo "*** Using /usr/local/bin/ruby (`/usr/local/bin/ruby --version`)"
	echo
	/usr/local/bin/ruby start.rb $@
elif [ -f /usr/bin/ruby ]; then
	echo "*** Using /usr/bin/ruby (`/usr/bin/ruby --version`)"
	echo
	/usr/bin/ruby -I. start.rb $@
else
	echo "Ruby not found in /usr/local/bin or /usr/bin"
fi

