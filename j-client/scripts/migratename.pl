#!/usr/bin/perl -w

open(LISTCMD, "find . | grep -v \~ | grep -v class | grep -v svn |");

$oldname = "NC";
$oldfilename = "NC";
$newname = "";

while(<LISTCMD>) {
 # $_ = one line from the file
 $currline = $_;
 if($currline =~ /^(\.\/)($oldfilename)([a-zA-Z]+\.java)$/)
 {
	print "Processing $2$3... ";
	$sedres = `sed -i \'s\/$oldname\/$newname\/g\' $2$3`;
	$mvres = `svn mv $2$3 $newname$3`;
	print "done.\n";
	print "$mvres\n";
	print "$sedres\n";
 }
}

close(LISTCMD);
