#!/usr/bin/perl
#
# Convert a serialised proof-of-liability tree (JSON) to GraphViz DOT format. 
#
# Copyright 2014 Isaac Wilcox.
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
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

    my $name = $uid;
    $uid++;

    my $short_hash = "";
    if (exists($data->{hash})) {
        ($short_hash = $data->{hash}) =~ s/^(....).*(....)$/h: $1...$2/;
    }

    my $sum = "";
    if (exists($data->{sum})) {
        $sum = "Σ: $data->{sum}";
    }

    # Give leaves a different shape and extra info.
    my $shape = "";
    my $user = "";
    my $nonce = "";
    if (!($left || $right)) {
        $shape = ",shape=box";
        if (exists($data->{user})) {
            $user = "u: $data->{user}";
        }
        if (exists($data->{nonce})) {
            ($nonce = $data->{nonce}) =~ s/^(....).*(....)$/n: $1...$2/;
        }
    }

    my $label = join("\\n", grep { length($_) } ($user, $nonce, $sum, $short_hash));
    print "    $name [label=\"$label\"$shape]\n";

    # Uplink (for all but the root).
    if (defined($parent)) {
        print "    $name -> $parent\n";
    }

    if ($left) {
        walk($name, $left->{data}, $left->{left}, $left->{right});
    }
    if ($right) {
        walk($name, $right->{data}, $right->{left}, $right->{right});
    }
}
