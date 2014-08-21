import re

class LuaTokenizer:
    def gobble(self):
        pass
    def peek(self):
        pass

class LuaStringTokenizer(LuaTokenizer):
    """Return tokens given a string of lua source"""
    def __init__(self, source):
        self.source = source
        self.sourcepos = 0
        self.sourcemax = len(source)

    def gobble(self):
        if self.sourcepos >= self.sourcemax: return None
        v = self.source[self.sourcepos]
        self.sourcepos += 1
        return v

    def peek(self):
        if self.sourcepos >= self.sourcemax: return None
        return self.source[self.sourcepos]

    def iterpeek(self):
        return iter(self.peek, None)

    def itergobble(self):
        return iter(self.gobble, None)

    def __iter__(self):
        return self.iterpeek()

def parseLua(code):
    tokenizer = LuaStringTokenizer(code)
    return [m for m in iter(lambda: lua_parse(tokenizer), None)]

def lua_parse(lt):
    for tok in lt.iterpeek():
        if tok == "{":
            lt.gobble()
            return lua_array(lt)
        elif iswhitespace(tok) or tok == ",":
            lt.gobble()
            continue
        elif tok == "\"":
            lt.gobble()
            return lua_string(lt)
        elif tok == "[" or tok == "]":
            lt.gobble()
            continue
        elif tok == "(":
            lt.gobble()
            return lua_list(lt)
        elif tok == '=':
            return lt.gobble()
        elif tok == '}':
            return
        else:
            return lua_value(lt)
    return None

INT_PATTERN = re.compile('^-?[1-9][0-9]*$') 
FLOAT_PATTERN = re.compile('^-?[0-9]+\.[0-9]+$')

def lua_value(lt):
    myval = ''
    for tok in lt.iterpeek():
        if iswhitespace(tok) or tok in ",][}{":
            break
        else:
            myval += lt.gobble()
    if myval in ("True", "true"):
        return True
    elif myval in ("False", "false"):
        return False
    if INT_PATTERN.match(myval):
        return int(myval)
    elif FLOAT_PATTERN.match(myval):
        return float(myval)
    else:
        return myval

def lua_list(lt):
    meme = []
    for tok in lt.iterpeek():
        if iswhitespace(tok) or tok in ('[', ']', ','):
            lt.gobble()
        elif tok == ")":
            lt.gobble()
            break
        else:
            meme.append(lua_parse(lt))
    return meme

def lua_array(lt):
    meme = {}
    for tok in lt.iterpeek():
        if tok == "[" or tok == "]":
            lt.gobble()
            continue
        elif tok == "}":
            lt.gobble()
            break
        elif tok == ",":
            lt.gobble()
            continue
        elif iswhitespace(tok):
            lt.gobble()
        else:
            key = lua_parse(lt)
            eq = lua_parse(lt)
            val = lua_parse(lt)
            meme[key] = val
    return meme

def lua_string(lt):
    val = ''
    for tok in lt.itergobble():
        if tok == "\"":
            return val
        else:
            val += tok
    raise Exception # XXX bogus!

def iswhitespace(tok):
    return (tok in " \t\n\r") 

def generateLua(lstruct, level = 1):
    tehcode = "" 
    indent = " " * (4 * level)
    if type(lstruct) is dict:        
        tehcode += "{\n"         
        for i,k in lstruct.iteritems():
            tehcode += indent + "%s = %s\n" % (i, generateLua(k, level + 1))
        tehcode += " " * (4 * (level-1)) + "}"
        return tehcode
    elif type(lstruct) in (int, float, bool):
        return repr(lstruct)
    elif type(lstruct) is long:
        return str(lstruct)
    else:
        return  "\"%s\"" % lstruct.replace("\"", "\\\"")


