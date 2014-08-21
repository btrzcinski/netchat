from xml import etree

class ETreeParser:
    def __init__(self, tree, attrs=()):
        self.tree = tree
        self.name = self.tree.tag

        for a in attrs:
            if not tree.attrib.has_key(a):
                raise AttributeError, 'Root tree (%s) is missing required attribute (%s).' % (self.name, a)

    def require_tag(self, attrs, *path):
        if not path:
            path = [attrs,]
            attrs = tuple()
        else:
            path = list(path)
        if not path:
            raise ValueError, 'Path not valid.'

        if not isinstance(attrs, tuple):
            attrs = (attrs,)

        tree = self.tree

        while path:
            tags = list(iter(tree))
            tagd = dict(zip((t.tag for t in tags), tags))
            p = path.pop(0)
            tree2 = tagd.get(p)
            if tree2 is None:
                raise KeyError, 'Subtree (%s) of tree (%s) is missing required tag (%s).' % (tree.tag, self.name, p)
            else:
                tree = tree2

        for a in attrs:
            if not tree.attrib.has_key(a):
                raise AttributeError, 'Subtree (%s) of tree (%s) is missing required attribute (%s).' % (tree.tag, self.name, a)
  
    def require_tags(self, attrs, *path):
        if not path:
            path = [attrs,]
            attrs = tuple()
        else:
            path = list(path)
        if not path:
            raise ValueError, 'Path not valid.'

        if not isinstance(attrs, tuple):
            attrs = (attrs,)

        tree = self.tree

        while path[1:]:
            tags = list(iter(tree))
            tagd = dict(zip((t.tag for t in tags), tags))
            p = path.pop(0)
            tree2 = tagd.get(p)
            if tree2 is None:
                raise KeyError, 'Subtree (%s) of tree (%s) is missing required tag (%s).' % (tree.tag, self.name, p)
            else:
                tree = tree2

        l = [x for x in tree if x.tag == path[0]]
        if not l:
            raise KeyError, 'Subtree (%s) of tree (%s) is missing required tag (%s).' % (tree.tag, self.name, path[0])
        for t in l:
            for a in attrs:
                if not t.attrib.has_key(a):
                    raise AttributeError, 'Subtree (%s) of tree (%s) is missing required attribute (%s).' % (tree.tag, self.name, a)
        

    def get(self, *path):
        if not path:
            return self.tree
        path = list(path) 
        head = self.tree
        while path:
            tags = list(iter(head))
            tagd = dict(zip((t.tag for t in tags), tags))
            p = path.pop(0)
            head = tagd.get(p)
            if head is None:
                raise ValueError, 'Invalid path.'
        return head

    def gets(self, *path):
        if not path:
            raise ArgumentError, 'Called gets on no path.'
        path = list(path) 
        head = self.tree
        while path[1:]:
            tags = list(iter(head))
            tagd = dict(zip((t.tag for t in tags), tags))
            p = path.pop(0)
            head = tagd.get(p)
            if head is None:
                raise ValueError, 'Invalid path.'
        l = [t for t in head if t.tag == path[0]]
        return l

class XMLError(Exception):
    pass
