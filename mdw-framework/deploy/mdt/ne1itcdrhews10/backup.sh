#!/bin/bash
 #Purpose = Backup of Important Data
 #Created on 04-30-2015
 #Author = Manoj Agrawal
 #Version 1.0
 #START

TIME=`date +%b-%d-%y`            # This Command will add date in Backup File Name.
 FILENAME=mdwews-dev1-$TIME.tar.gz    # Here i define Backup file name format.
 SRCDIR=/foss/foss-ews/instances/mdwews-dev1/1.0.0.0                    # Location of Important Data Directory (Source of backup).
 DESDIR=/home/mdwweblk/backup            # Destination of backup file.
 tar -cvpzf $DESDIR/$FILENAME $SRCDIR 

#END 

