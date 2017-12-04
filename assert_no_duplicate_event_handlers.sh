#!/bin/bash

event_dups () {
  grep "defmethod $1" src-cljs/storefront/$2 | perl -pe "s/.*defmethod $1 (\S+).*/\1/" | sort | uniq -c | egrep '  1 ' -v
}

effect_dups ()     { event_dups perform-effects frontend_effects.cljs; }
transition_dups () { event_dups transition-state frontend_transitions.cljs; }
tracking_dups ()   { event_dups perform-track trackings.cljs; }

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
