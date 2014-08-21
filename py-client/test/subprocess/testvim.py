import subprocess

FILE = 'test.txt'

subprocess.call(['vim', FILE])
f = file(FILE, 'r')
print f.read()
subprocess.call(['rm', FILE])
