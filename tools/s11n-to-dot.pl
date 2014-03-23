#!/usr/bin/perl
#
# Convert a serialised proof-of-liability tree (JSON) to GraphViz DOT format. 
#
# Copyright 2014 Isaac Wilcox.
# Distributed under the Boost Software License, Version 1.0.  See accompanying
# file LICENCE.txt or copy at <http://www.boost.org/LICENSE_1_0.txt>.
#
use strict;
use warnings;

use Data::Dumper;
use JSON;

my $json = join("", <>);
my $tree = decode_json($json);
my $uid = 0;

print "digraph tree {\n";
print "    rankdir=BT\n";
print "    ordering=out\n";
walk(undef, $tree->{data}, $tree->{left}, $tree->{right});
print "}\n";

sub walk {
    my ($parent, $data, $left, $right) = @_;

    if (!defined($data->{hash})) {
        print Dumper(\$data);
        die "^^^ node with no hash?";
    }

    (my $short_hash = $data->{hash}) =~ s/^(...).*(...)$/h: $1...$2/;
    my $sum = "Σ: $data->{sum}";

    # Give leaves a different shape and extra info.
    my $shape = "";
    my $user = "";
    my $nonce = "";
    if (!($left || $right)) {
        $shape = ",shape=box";
        $user = "u: $data->{user}";
        $nonce .= "n: $data->{nonce}";
    }

    my $name = "\"$short_hash #$uid\"";
    my $label = join("\\n", grep { length($_) } ($user, $nonce, $sum, $short_hash));
    print "    $name [label=\"$label\"$shape]\n";

    # Uplink (for all but the root).
    if (defined($parent)) {
        print "    $name -> $parent\n";
    }

    $uid++;

    if ($left) {
        walk($name, $left->{data}, $left->{left}, $left->{right});
    }
    if ($right) {
        walk($name, $right->{data}, $right->{left}, $right->{right});
    }
}
