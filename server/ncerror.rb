# Author::     Barnett Trzcinski  (mailto:btrzcins@tjhsst.edu)
# Copyright::  Copyright(C) 2006-2007 The NetChat Project.
# License::    Distributed under the GNU GPLv2
#
# This file contains the NCError module.

# This module contains all exception and error classes specific to NetChat.
module NCError
   # This exception is raised when a protocol error is detected.
   class NCPException < RuntimeError
   end
   # This exception is raised when invalid use of the module registry is detected.
   class NCModuleRegistryException < RuntimeError
   end
end
