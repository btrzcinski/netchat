"""
The NCFileTransfer Module. Handles anything related to
transferring files between clients.
"""

import math, os, base64

from netclient.lib.etreeparser import ETreeParser as ETP

from netclient.cmanager import cmanager
from netclient.mmanager import mmanager
from netclient.extensibles import Module
from netclient.messages.filetransfer import NCFTransferIDReq, NCFTransferReq, NCFTransferDecision, NCFileChunkReq, NCFTransferComplete, NCFileChunk
from netclient.settings import FILES, CHAT_TAB

name = 'File Transfer'
version = '0.1a'
moduleclass = 'FileXfer'

class FileXfer(Module):
    def __init__(self):
        Module.__init__(self)
        self.logger = cmanager['logger'].get_context('filexfer')
        self.in_progress = {}
        self.queued = {}
        self.queue = []

    def get_tabbed_list(self, command, intermediate, input, type):
        if not intermediate:
            return mmanager['chat'].get_tabbed_list(command, intermediate, input, type)
        l = []
        p = os.path.abspath(os.path.expanduser(input))
        if not input:
            input = './'
        if input.endswith(os.sep):
            if not os.path.exists(p):
                return l
            l = ['%s%s' % (input, f) for f in os.listdir(p)]
        else:
            base, end = os.path.split(input)
            fs = os.listdir((os.path.expanduser(base) if base else os.path.dirname(p)))
            for f in fs:
                if f.startswith(end):
                    path = os.path.join(base, f)
                    mod = (os.sep if os.path.isdir(path) else ' ')
                    l.append('%s%s' % (path, mod))

        return l

    def open(self):
        Module.open(self)

    def send(self, target, fname):
        if self.queued.has_key(fname):
            return
        self.queued[fname] = target
        self.queue.append(fname)
        cmanager['xmlcomm'].sendLine(NCFTransferIDReq())

    def file_transfer_id(self, content):
        etp = ETP(content)
        etp.require_tag('id')

        id = etp.get('id').text

        name = self.queue.pop()
        to = self.queued.pop(name)
        # TODO: Comment
        finfo = (os.path.basename(name), (str(os.path.getsize(name)),))
        self.logger.log('Received ID#%s server, requesting transfer to %s of \'%s\'.' % (id, to, name))
        cmanager['xmlcomm'].sendLine(NCFTransferReq(id, to, mmanager['login'].username, finfo, ''))
        self.in_progress[id] = to, name

    def file_transfer_request(self, content):
        etp = ETP(content)
        etp.require_tag('id')
        etp.require_tag('to')
        etp.require_tag('from')
        etp.require_tag(('bsize',), 'filename')
        etp.require_tag('comment')

        id = etp.get('id').text
        to = etp.get('to').text
        frm = etp.get('from').text
        comment = etp.get('comment').text

        f = etp.get('filename')
        filename = os.path.basename(f.text)
        fsize = f.attrib['bsize']

        n = CHAT_TAB % frm

        temp = int(fsize)
        vals = ['bytes', 'kilobytes', 'megabytes', 'gigabytes', 'terabytes']
        nd = -1

        while temp > 1:
            nd += 1
            if nd == len(vals) - 1:
                break
            temp /= 1024.
        temp = int(fsize)/(1024.**nd)
        fsize2 = '%.1f %s' % (temp, vals[nd])


        if not cmanager['screen'].has_tab(n):
            cmanager['screen'].new_tab(n, frm, 'onetoone', True)
        cmanager['screen'].sendLine('%s wants to send you a file! Info:' % frm, n)
        cmanager['screen'].sendLine('\tFilename: %s\n\r\tSize: %s\n\r\tComment: %s' % (filename, fsize2, comment), n)
        self.logger.log('File transfer request: [%s]-%s->%s (%s)' % (frm, fsize, filename, comment))
        
        def affirm():
            cmanager['xmlcomm'].sendLine(NCFTransferDecision(id, frm, 'true', filename))
            route = (frm, to)
            cmanager['xmlcomm'].sendLine(NCFileChunkReq(id, (route,), (('1',),)))
            self.in_progress[id] = ReceiveFile(id, filename, frm, fsize)
        def deny():
            cmanager['xmlcomm'].sendLine(NCFTransferDecision(id, frm, 'false', filename))

        map = {
            'y': affirm,
            'n': deny
        }
        cmanager['screen'].tabs[cmanager['screen'].top_tab].layer_dialog('prompt', 'Accept file transfer (y/N)?', map, 'n')

    def file_transfer_decision(self, content):
        etp = ETP(content)
        etp.require_tag('id')
        etp.require_tag('from')
        etp.require_tag('accept')
        etp.require_tag('filename')

        id = etp.get('id').text

        accepted = etp.get('accept').text.lower() == 'true'
        f = os.path.basename(etp.get('filename').text)
        to = self.in_progress[id][0]
        fa = self.in_progress[id][1]
        dec = ('accepted' if accepted else 'declined')
        queues = []
        if dec:
            s = self.in_progress[id]
            if isinstance(s, tuple):
                t = s
            elif isintance(s, list):
                t = s[0]
                queues = s[1:]
            to = t[0]
            fa = t[1]
            self.in_progress[id] = SendFile(id, fa, to)
        else:
            del self.in_progress[id]

        n = CHAT_TAB % to
        if not cmanager['screen'].has_tab(n):
            cmanager['screen'].new_tab(n, to, 'onetoone')

        msg = '%s has %s your file (%s).' % (to, dec, f)
        cmanager['screen'].sendLine(msg, n)
        self.logger.log('Transfer of ID#%s (%s) to %s %s.' % (id, f, to, dec))
                
        for q in queues:
            self.in_progress[id].send_chunk(q)

    def file_chunk(self, content):
        etp = ETP(content)
        etp.require_tag('id')
        etp.require_tag(('from', 'to'), 'route')
        etp.require_tag(('number', 'maxchunk', 'filename', 'totalbsize'), 'chunk')

        id = etp.get('id').text
        c = etp.get('chunk')
        attr = c.attrib
        number = attr['number']
        chunk = c.text
        maxchunk = attr['maxchunk']

        self.logger.log('Received chunk from %s, ID#%s, chunk#%s.' % (etp.get('route').attrib['to'], id, number))
        
        self.in_progress[id].recv_chunk(number, chunk, maxchunk)

    def file_chunk_request(self, content):
        etp = ETP(content)
        etp.require_tag('id')
        etp.require_tag(('from', 'to'), 'route')
        etp.require_tag(('number',), 'chunk')

        id = etp.get('id').text
        c = etp.get('chunk')
        attr = c.attrib
        number = attr['number']

        self.logger.log('FT Message received: Chunk request: #%s of #%s to %s.' % (number, id, etp.get('route').attrib['to']))

        s = self.in_progress[id]
        if not isinstance(s, SendFile):
            if isinstance(s, tuple):
                self.in_progress[id] = [s]
            self.in_progress[id].append(number)
        else:
            s.send_chunk(number)

    def transfer_complete(self, content):
        etp = ETP(content)
        etp.require_tag('id')
        etp.require_tag('from')
        etp.require_tag('to')
        etp.require_tag('success')

        id = etp.get('id').text
        stat = etp.get('success').text

        self.in_progress[id].finish(stat=='true')

class ReceiveFile:
    def __init__(self, id, name, frm, size):
        self.id = id
        self.name = name
        self.frm = frm
        self.to = mmanager['login'].username
        self.size = size
        self.maxchunk = 0
        self.init_file()
        self.logger = mmanager['filetransfer'].logger

    def init_file(self):
        subpath = os.path.join(FILES, self.frm)
        self.path = os.path.join(subpath, self.name)
        if not os.path.exists(subpath):
            os.mkdir(subpath, 0755)
        self.descriptor = file(self.path, 'w')

    def recv_chunk(self, chunknum, chunk, maxchunk):
        chunknum = int(chunknum)
        maxchunk = int(maxchunk)
        self.descriptor.write(chunk)
        route = (self.frm, mmanager['login'].username)
        if chunknum < maxchunk:
            self.logger.log('Requesting chunk#%d of ID#%s from %s (file: %s).' % (chunknum+1, self.id, self.frm, self.name))
            cmanager['xmlcomm'].sendLine(NCFileChunkReq(self.id, (route,), ((str(chunknum+1),),)))
        else:
            self.finish()

    def finish(self, stat=True):
        n = CHAT_TAB % self.frm

        if not cmanager['screen'].has_tab(n):
            cmanager['screen'].new_tab(n, self.frm, 'onetoone')
        if not stat:
            cmanager['screen'].sendLine('File transfer of %s aborted.' % self.name, n)
            self.logger.log('File transfer ID#%s (%s-->[%s]) aborted.' % (self.id, self.name, self.frm))
            self.descriptor.close()
        else:
            cmanager['screen'].sendLine('File transfer of %s completed. Decoding...' % self.name, n)
            self.descriptor.flush()
            self.descriptor.close()

            try:
                self.__decode()
                flag = 'true'
            except TypeError:
                flag = 'false'

            cmanager['xmlcomm'].sendLine(NCFTransferComplete(self.id, self.frm, self.to, flag))
            fillers = (('succeeded', 'valid') if flag=='true' else ('failed', 'invalid'))
            cmanager['screen'].sendLine('Decoding %s, file is %s.' % fillers, n)
            self.logger.log('Transfer of file %s from %s %s.' % (self.name, self.frm, fillers[0]))
        del mmanager['filetransfer'].in_progress[self.id]

    def __decode(self):
        f = file(self.path, 'r')
        decoded = base64.b64decode(f.read())
        f.close()
        f2 = file(self.path, 'w')
        f2.write(decoded)
        f2.flush()
        f2.close()

class SendFile:
    def __init__(self, id, name, to, csize=342):
        self.id = id
        self.path = name
        self.size = os.path.getsize(name)
        self.name = os.path.basename(name)
        self.to = to
        self.chunksize = csize

        self.init_file()
        self.logger = mmanager['filetransfer'].logger
        self.logger.log('Beginning transfer ID#%s, %s-->[%s].' % (id, self.name, to))
    
    def init_file(self):
        f = file(self.path, 'r')
        text = f.read()
        f.close()
        self.encoded = base64.b64encode(text)
        l = float(len(self.encoded))

        self.maxchunks = int(math.ceil(l/self.chunksize))

        self.remaining = self.encoded

    def send_chunk(self, chunknum):
        chunknum = int(chunknum)

        if len(self.remaining) < self.chunksize:
            chunk = self.remaining
            self.remaining = ''
        else:
            chunk = self.remaining[:self.chunksize]
            self.remaining = self.remaining[self.chunksize:]
        
        rt = ((mmanager['login'].username, self.to),)
        c = chunk, (str(chunknum), str(self.maxchunks), self.name, str(self.size))
        self.logger.log('Sending chunk #%d of ID#%s to %s.' % (chunknum, self.id, self.to))
        msg = NCFileChunk(self.id, rt, c)
        cmanager['xmlcomm'].sendLine(msg)

    def finish(self, stat):
        n = CHAT_TAB % self.to

        if not cmanager['screen'].has_tab(n):
            cmanager['screen'].new_tab(n, self.to, 'onetoone')

        m = ('completed successfully' if stat else 'failed')
        cmanager['screen'].sendLine('Transfer of %s to %s %s.' % (self.name, self.to, m), n)
        self.logger.log('File transfer ID#%s (%s-->[%s]) %s.' % (self.id, self.name, self.to, m))
        del mmanager['filetransfer'].in_progress[self.id]
