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

require "getopts.pl";

do Getopts('id');

%flex = ('a', 061, 'b', 062, 'c', 063, 'd', 064, 'e', 065, 'f', 066,
	 'g', 067, 'h', 070, 'i', 071, 'j', 041, 'k', 042, 'l', 043,
	 'm', 044, 'n', 045, 'o', 046, 'p', 047, 'q', 050, 'r', 051,
	 's', 022, 't', 023, 'u', 024, 'v', 025, 'w', 026, 'x', 027,
	 'y', 030, 'z', 031, '0', 020, '1', 01, '2', 02, '3', 03, '4', 04,
	 '5', 05, '6', 06,   '7', 07, '8', 010, '9', 011, ')', 055, '(', 057,
	 '.', 073, ',', 033, '|', 056, ' ', 000, "\t",036, "\h",075,
	 "\n", 077
	 );

%ops = (01, \&and_op, 02, \&or_op,  03,  \&xor_op, 04, \&xct_op, 
        07,  \&jda_op, 
	010,\&lac_op, 011,\&lio_op, 012, \&dac_op, 013, \&dap_op,
	015,\&dio_op, 016,\&dzm_op,
	020,\&add_op, 021,\&sub_op, 022, \&idx_op, 023, \&isp_op,
	024,\&sad_op, 025,\&sas_op, 026, \&mus_op, 027, \&dis_op,
	030,\&jmp_op, 031,\&jsp_op, 032, \&skip_op, 033, \&shift_op,
	034,\&law_op, 035,\&iot_op, 037, \&opr_op
	);

$file = shift;
makeoutflex();
#$chars = flex("(cond ((eq 3 3) 23)((eq 5 5) 15)) ");
readTape($file);
initSpace() if ($opt_i);
setup();
while(1){
    step();
    while($pc == $break || $run == 0){
	command();
    }
}

sub initSpace {
    setup();
    $run = 1;
    $run = 0 if($opt_d);

# Set up relocation parameters

    $memory[0234] = 04000;
    $memory[025] = 07776;
    $pc = 02406;
    print "Reloc...";
    while($pc != 02440){
	step();
	while($pc == $break || $run == 0){
	    command();
	}
    }
    print "\nMark...";
    while($pc != 02442){
	step();
	while($pc == $break || $run == 0){
	    command();
	}
    }
    print "\nSweep...";
    while($pc != 4){
	step();
	while($pc == $break || $run == 0){
	    command();
	}
    }
    print "\nDone.\n";
    open(OUT, ">$file") || die "Could not open $file:$!";
    $out = pack("N4096", @memory);
    print OUT $out;
    close(OUT);
}

sub step {
    local($op);
    $md = $memory[$pc];
    printf("pc:%06o md:%06o ac:%06o io:%06o ", $pc, $md, $ac, $io) if($trace);
    $pc++;
    $y = $md & 07777;
    $ib = ($md>>12) & 1;
    $op = $ops{$md>>13};
    printf("Undefined op %o at pc %06o\n", $op, $pc) unless ($op);
    &$op if ($op);
    printf("ea:%06o mem:%06o\n", $y, $memory[$y]) if($trace);
}

sub ea {
    while(1) {
	return unless $ib;
	$ib = (($memory[$y]>>12) & 1);
	$y = $memory[$y] & 07777;
    }
}

sub setup {
    $ac = 0;
    $io = 0;
    $ov = 0;
    $pc = 4;
    $run = 0;
    $break = 0;
    $trace = 0;
    @flags = (0, 0, 0, 0, 0, 0, 0, 0);
    @sense = (0, 0, 0, 0, 0, 0, 0, 0);
}

sub readTape {
    local($file) = @_;
    local($in);
    open(FILE, "<$file") || die "Could not open $file: $!";
    binmode FILE;
    $/ = "";
    $in = "";
    while(<FILE>){
	$in = $in . $_;
    }
    close(FILE);
    $/="\n";
    @memory = unpack("N4096", $in);
}

sub mus_op {
    ea();
    
    if ($io & 1) {
	$ac = $ac + $memory[$y];
	$ov = $ac>>18;
	$ac = ($ac + $ov) & 0777777;
    }
    rcr(1);
    $ac &= 0377777;
}

sub dis_op {
    ea();
    rcl(1);
    $io ^= 1;
    if ($io & 1) {
	$ac = $ac + ($memory[$y] ^ 0777777);
	$ov = $ac>>18;
	$ac = ($ac + $ov) & 0777777;
    } else {
	$ac = $ac + $memory[$y] + 1;
	$ov = $ac>>18;
	$ac = ($ac + $ov) & 0777777;
    }

    $ac = 0 if ($ac == 0777777) ;
}


sub and_op {
    ea();
    $ac = $ac & $memory[$y];
}

sub or_op {
    ea();
    $ac = $ac | $memory[$y];
}

sub xor_op {
    ea();
    $ac = $ac ^ $memory[$y];
}

sub xct_op {
    ea();
    $md = $memory[$y];
    printf("\nxct:%06o md:%06o ac:%06o io:%06o ", $y, $md, $ac, $io) if($trace);
    $y = $md & 07777;
    $ib = ($md>>12) & 1;
    $op = $ops{$md>>13};
    printf("Undefined op %o at pc %06o\n", $op, $pc) unless ($op);
    &$op if ($op);
    printf("ea:%06o mem:%06o\n", $y, $memory[$y]) if($trace);
}

sub jda_op {
    local($target);
    if ($ib) {
	$target = $y;
    } else {
	$target = 0100;
    }
	    
    $memory[$target] = $ac;
    $ac =  ($ov<<17) + $pc;
    $pc = $target + 1;
}

sub lac_op {
    ea();
    $ac = $memory[$y];
}

sub lio_op {
    ea();
    $io = $memory[$y];
}

sub dac_op {
    ea();
    $memory[$y] = $ac;
}

sub dio_op {
    ea();
    $memory[$y] = $io;
}

sub dap_op {
    ea();
    $memory[$y] = ($memory[$y]&0770000) +($ac & 07777);
}

sub idx_op {
    ea();
    $ac = $memory[$y] + 1;
    $ac = 0 if ($ac == 0777777);
    $memory[$y] = $ac;
}

sub isp_op {
    idx_op();
    $pc++ unless ($ac & 0400000);
}

sub add_op {
    ea();
    $ac = $ac + $memory[$y];
    $ov = $ac>>18;
    $ac = ($ac + $ov) & 0777777;
    $ac = 0 if ($ac == 0777777) ;
}

sub sub_op {
    ea();
    $ac = $ac + ($memory[$y] ^ 0777777);
    $ov = $ac>>18;
    $ac = ($ac + $ov) & 0777777;
    $ac = 0 if ($ac == 0777777) ;
}

sub sas_op {
    ea();
    $pc++ if ($ac == $memory[$y]);
}

sub sad_op {
    ea();
    $pc++ if ($ac != $memory[$y]);
}

sub dzm_op {
    ea();
    $memory[$y] = 0;
}

sub jmp_op {
    ea();
    $pc = $y;
}

sub jsp_op {
    ea();
    $ac =  ($ov<<17) + $pc;
    $pc = $y;
}

sub skip_op {
    local($skip,$cond);

    $cond =  ((($y&0100) == 0100) && ($ac==0))     ||
	     ((($y&0200) == 0200) && ($ac>>17==0)) ||
	     ((($y&0400) == 0400) && ($ac>>17==1)) ||
	     ((($y&02000)== 02000)&& ($io>>17==0)) ||
	     ((($y&7)    != 0)    && !$flags[$y&7])||
	     ((($y&070)  != 0)    && !$sense[($y&070)>>3])||
	      (($y&070)  == 010);
    if ($ib == 0) 
    {
	$pc++ if ($cond);
    } else {
	$pc++ unless ($cond);
    }
}

sub law_op {
    $ac = ($y ^ 0777777) if ($ib);
    $ac = $y unless ($ib);
}

sub opr_op {
    local($flag);

    $ac = 0 if ($y&0200);
    $io = 0 if ($y&04000);
    $ac = ($ac ^ 0777777) if ($y&01000);
    if ($y&0400){
	printf("\nHalt at %06o\n", $pc);
	@inbuffer = ();
	$run = 0;
    }

    $flag = $y & 7;
    if($flag > 0){
	if(($y&010) == 0){
	    @flags = (0, 1, 0, 0, 0, 0, 0) if($flag == 7);
	    return if($flag == 1);
	    $flags[$flag] = 0 if ($flag != 7);
	} else {
	    @flags = (0, 1, 1, 1, 1, 1, 1) if($flag == 7);
	    $flags[$flag] = 1 if ($flag != 7);
	}
    }
}

sub iot_op {
    tyi() if ($y == 4);
    tyo() if ($y == 3);
    box() if ($y == 11);
    dpy() if (($y&7) == 7);
}

sub dpy {
    printf("dpy - pc:%06o md:%06o ac:%06o io:%06o\n", $pc, $md, $ac, $io);
}

sub box {
    printf("box - pc:%06o md:%06o ac:%06o io:%06o\n", $pc, $md, $ac, $io);
}

sub shift_op {
    local($type);
    $type = ($md>>9) & 0xf;
    if($type == 3){
	rcl(($y&0777));
    } elsif ($type == 1) {
	$ac = rol($ac, ($y&0777));
    } elsif ($type == 2){
	$io = rol($io, ($y&0777));
    } elsif ($type == 5){
	$ac = sl($ac, ($y&0777));
    } elsif ($type == 6){
	$io = sl($io, ($y&0777));
    } elsif ($type == 7){
	scl(($y&0777));
    } elsif ($type == 9){
	$ac = ror($ac, ($y&0777));
    } elsif ($type == 10){
	$io = ror($io, ($y&0777));
    } elsif ($type == 11){
	rcr(($y&0777));
    } elsif ($type == 13){
	$ac = sr($ac, ($y&0777));
    } elsif ($type == 14){
	$io = sr($io, ($y&0777));
    } elsif ($type == 15){
	scr(($y&0777));
    } 
}

sub ror {
    local($a, $n) = @_;
    local($i);
    $i = 12;
    while($i--){
	return $a if($n == 0);
	$a = (($a&1)*0400000) + ($a>>1);
	$n = $n>>1;
    }
}

sub rol {
    local($a, $n) = @_;
    local($i);
    $i = 12;
    while($i--){
	return $a if($n == 0);
	$a = (($a<<1)&0777777) + (($a>>17)&1);
	$n = $n>>1;
    }
}

sub sr {
    local($a, $n) = @_;
    local($i);
    $i = 12;
    while($i--){
	return $a if($n == 0);
	$a = ($a&0400000) + ($a>>1);
	$n = $n>>1;
    }
}

sub sl {
    local($a, $n) = @_;
    local($i);
    $i = 12;
    while($i--){
	return $a if($n == 0);
#	$a = ($a&1) + (($a<<1)&0777777);
	$a = (($a&0400000) + (($a<<1)|($a>>17))&0377777);
	$n = $n>>1;
    }
}

sub rcr {
    local($n) = @_;
    local($i,$c);
    $i = 12;
    while($i--){
	return if($n == 0);
	$c = $ac&1;
	$ac = ($ac>>1) + (0400000*($io&1));
	$io = ($io>>1) + (0400000*$c);
	$n = $n>>1;
    }
}

sub scr {
    local($n) = @_;
    local($i,$temp);
    $i = 12;
    while($i--){
	return if($n == 0);
	$temp = ($ac&1)<<17;
	$ac = ($ac>>1) | ($ac&0400000);
	$io = ($io>>1) | $temp;
	$n = $n>>1;
    }
}

sub rcl {
    local($n) = @_;
    local($i,$c);
    $i = 12;
    while($i--){
	return if($n == 0);
	$c = ($ac>>17)&1;
	$ac = (($ac<<1)&0777777) + (($io>>17)&1);
	$io = (($io<<1)&0777777) + $c;
	$n = $n>>1;
    }
}

sub scl {
    local($n) = @_;
    local($i,$both, $temp, $temp2);
    $i = 12;
    while($i--){
	return if($n == 0);
	$temp = ($io&0400000)>>17;
	$temp2 = ($ac&0400000)>>17;
	$ac = ((($ac<<1) | $temp)&0377777) + ($ac&0400000);
	$io = ($temp2 + ($io<<1))&0777777;
	$n = $n>>1;
    }
}

sub dumpmem {
    local($addr, $len) = @_;
    local($x);

    while($len > 0){
	printf("%06o: ", $addr);
	$x = 6;
	while($x--){
	    printf("%06o ", $memory[$addr]);
	    $addr++;
	    $len--;
	}
	print "\n";
    }
}

sub command {
    local($line, $savepc, $addr, $len, $temp);
    printf("pc:%06o md:%06o ac:%06o io:%06o\n", $pc, $md, $ac, $io);
    print "pdp1> ";
    while($line = <>){
	chop $line;
	if($line =~ /^[sS]/){
	    $trace = 1;
	    step();
	    $trace = 0;
	}

	if($line =~ /^[rR]/){
	    printf("pc:%06o md:%06o ac:%06o io:%06o\n", $pc, $md, $ac, $io);
	}

	if($line =~ /^[aA]/){
	    ($temp) = $line =~ /[aA]\s*(\d+)/;
	    next unless $temp;
	    $ac = oct($temp);
	    printf("pc:%06o md:%06o ac:%06o io:%06o\n", $pc, $md, $ac, $io);
	}

	if($line =~ /^[iI]/){
	    ($temp) = $line =~ /[iI]\s*(\d+)/;
	    next unless $temp;
	    $io = oct($temp);
	    printf("pc:%06o md:%06o ac:%06o io:%06o\n", $pc, $md, $ac, $io);
	}

	if($line =~ /^[pP]/){
	    ($temp) = $line =~ /[pP]\s*(\d+)/;
	    next unless $temp;
	    $pc = oct($temp);
	    printf("pc:%06o md:%06o ac:%06o io:%06o\n", $pc, $md, $ac, $io);
	}

	if($line =~ /^[nN]/){
	    $savepc = $pc+1;
	    $run = 1;
	    while(($run == 1) && ($pc != $savepc)){
		step();
	    }
	    printf("pc:%06o md:%06o ac:%06o io:%06o\n", $pc, $md, $ac, $io);
	    $run = 0;
	}
	if ($line =~ /^[bB]/) {
	    ($temp) = $line =~ /[bB]\s*(\d+)/;
	    next unless $temp;
	    $break = oct($temp);
	    printf("Breakpoint at %06o\n", $break);
	}
	if ($line =~ /^[cCgG]/) {
	    ($temp) = $line =~ /[gG]\s*(\d+)/;
	    if($temp) {
		$break = oct($temp);
		printf("Breakpoint at %06o\n", $break);
	    } else {
		$break = 0;
	    }
	    $run = 1;
	    step();
	    return;
	}
	if ($line =~ /^[Zz]/) {
	    $DB::single = 1;
	}

	if ($line =~ /^[dD]/) {
	    ($addr) = $line =~ /[dD]\s*(\d+)/;
	    ($len) = $line =~ /[dD]\s*\d+[\s,]+(\d+)/;
	    $len = 1 unless ($len);
	    $addr = $lastaddr unless ($addr);
	    $lastaddr = $addr+$len;
	    dumpmem(oct($addr), $len);
	}
	if ($line =~ /^[xX]/) {
	    exit(1);
	}
	print "pdp1> ";
    }
}

sub tyi {
    local($temp);
    while(1) {
	if($#inbuffer >= 0) {
	    $io = $flex{shift(@inbuffer)};
	    return;
	}
	if($cr) {
	    $cr = 0;
	} else {
	    print "\n";
	}
	print "Lisp> ";
	$inbuffer = <>;
	$cr = 1;
	chop $inbuffer;
	@inbuffer = split(//,$inbuffer);
    }
}

sub makeoutflex {
    local($temp);
    foreach $temp (keys(%flex)){
	$outflex{$flex{$temp}} = $temp;
    }
}

sub tyo {
    local($temp);
    $temp = $io & 077;
    if ($cr && $temp == 077){
	$cr = 0;
	return;
    }
    print $outflex{$temp};
}
