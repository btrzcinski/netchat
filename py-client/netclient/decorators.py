"""
Decorators used by the NC Py-Client.
"""

from netclient.settings import CMD_ATT, ARG_ERRORS, ARG, NO_ARG, P_ARG

def command(args=P_ARG, arg_msg=None, offline=False, type=None):
    """
    Used to decorate certain functions in Plugins to denote them as commands.
    args specifies whether or not the command explicitly requires arguments,
    and arg_msg is an error to print if this requirement is violated.
    """

    if isinstance(args, str):
        args, arg_msg = True, args
    def _cmd_dec(func):
        setattr(func, CMD_ATT, True)
        err, arg, noarg = ARG_ERRORS
        func.args = args
        if args is P_ARG:
            msg = None
        else:
            msg = arg_msg if arg_msg else \
                err % (arg if args is ARG else noarg)
        func.msg = msg
        func.offline = offline
        func.type = type
        return func
    return _cmd_dec
