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

@memory = 4096 x 0;

while(<>){
    chop;
    next if(/^\-/);
    next if(/^[ \+]\d+\t\t/);
    ($address, $value) = m/^[ \+](\d+)\s+(\d+)/;
    $memory[oct($address)] = oct($value);
}

open(OUT, ">spacewar.bin");
binmode OUT;
$out = pack("N4096", @memory);
print OUT $out;
close(OUT);
