#!/bin/sh
name="zippo-3.3.0-`date '+%y%m%d'`.tar.gz"
echo name=$name
git archive --format=tar.gz --prefix=zippo-3.3.0/ HEAD > ~/Documents/BC/$name

cp ~/Documents/BC/$name /Volumes/t5/.co/
