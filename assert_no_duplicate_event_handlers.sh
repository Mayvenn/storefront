#!/bin/bash


event_dups_for_file () {
    grep "defmethod \(\S\+\/\\)\?$1" -r src* \
        | perl -pe "s/.*(\S+\/)?$1 (\S+).*/\2/" \
        | sort | uniq -c | egrep '  1 ' -v
}

transition_dups () { event_dups_for_file transition-state; }
effect_dups ()     { event_dups_for_file perform-effects; }
tracking_dups ()   { event_dups_for_file perform-track; }

test "0" = `transition_dups | wc -l` &&
  test "0" = `effect_dups | wc -l` &&
  test "0" = `tracking_dups | wc -l`

SOME_DUPS=$?

if [[ ! $SOME_DUPS = 0 ]]; then
  (
    echo "Duplicate handlers:"
    echo "transitions:"
    echo "  " `transition_dups`
    echo "effects:"
    echo "  " `effect_dups`
    echo "trackings:"
    echo "  " `tracking_dups`
  ) 1>&2
else
  echo "No duplicate handlers found"
fi

exit $SOME_DUPS
