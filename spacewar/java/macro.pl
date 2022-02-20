#!/usr/local/bin/perl
#
#Copyright (c) 1996 Barry Silverman, Brian Silverman, Vadim Gerasimov.
#
#Permission is hereby granted, free of charge, to any person obtaining a copy
#of this software and associated documentation files (the "Software"), to deal
#in the Software without restriction, including without limitation the rights
#to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#copies of the Software, and to permit persons to whom the Software is
#furnished to do so, subject to the following conditions:
#
#The above copyright notice and this permission notice shall be included in
#all copies or substantial portions of the Software.
#
#THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
#THE SOFTWARE.

%macros = ();
$gensym = 1;

while ($line = <>) {
    chop $line;
    next if ($line =~ /^\s*$/);
    if ($line =~ /^\s*\//){
	print "-$line\n";
	next;
    }
    if ($line =~ /^\s+define/){
	define($line);
	next;
    }
    $line = expand($line);
    $line =~ s/^\+//;
    print $line;
}

sub define {
    local($line) = @_;
    local($macro, $args, @args, $definition, $string, $temp, $count, $value);
    ($macro) = $line =~ m/^\s+define\s+(\w+)\s*/;
    ($args) = $line =~ m/\s+define\s+\w+\s+([\.\(\~\w\s,]+)$/;
    $args =~ s/,\s+/,/g;
    @args = split(/,/, $args);
    print "-$line\n";
    while($line = <>){
	chop $line;
	print "-$line\n";
	last if ($line =~ /term/);
	$line =~ s+\s\/.*$++;
	$line =~ s/\s+$//;
	$line =~ s/$args[0]/ZZ1\$gensym/g if $args[0];
	$line =~ s/$args[1]/ZZ2\$gensym/g if $args[1];
	$line =~ s/$args[2]/ZZ3\$gensym/g if $args[2];
	$line =~ s/^(\w+),/\1\$gensym,/;
	$line =~ s/\+r/\$gensym/g;
	$definition = $definition . "\n" . $line;
    }

    $definition =~ s/^\n//;
    $macros{$macro}++;
    $string = <<"EOB";
sub $macro {
    local(\$_1_,\$_2_,\$_3_) = \@_;
    local(\$temp);
    \$temp = <<"EOF";
*    ZZ1\$gensym=\$_1_
*    ZZ2\$gensym=\$_2_
*    ZZ3\$gensym=\$_3_	
    $definition
EOF
    return \$temp;
}
EOB
    eval($string);
}

sub expand {
    local ($line) = @_;
    local ($line2, @arg, @lines, $macro, $arg, $foo, $expansion, $expline, $count, $value, $temp);
    $expansion = "";
    $line2 = $line;
    $line2 =~ s/\s+\/.*$//;
    ($macro) = $line2 =~ m/^\s+(\w+)\s*/;
    ($arg) = $line2 =~ m/^\s+\w+\s+([(\.\~\(\+\-\s\w,]+)$/;
    if ($line2 =~ /^\s+repeat/) {
	($count, $value) = $line2 =~ m/\s+repeat\s+(\d+)\s*,\s*([\s\(~\_\+\$=\w]+)/;
	$value = "\t$value";
	$count = oct($count);
	while ($count--){
	    $temp = &expand($value);
	    $expansion = $expansion . $temp;
	}
	return $expansion;
    } else {
	$temp = '+';
	$temp = '' if ($line =~ /^\*/);
	return "$temp$line\n" unless ($macro && $macros{$macro});
    }
    $expansion = "-$line\n";
    $arg =~ s/,\s+/,/g;				    
    @arg = split(/,/, $arg);
    $foo = "&$macro(\'$arg[0]\',\'$arg[1]\',\'$arg[2]\')";
    @lines = split(/\n/, eval($foo));
    $gensym++;				    
    foreach $expline (@lines) {
	next if ($expline =~ /^\s+$/);
	$expansion = $expansion . &expand($expline);
    }
    return $expansion;
}

