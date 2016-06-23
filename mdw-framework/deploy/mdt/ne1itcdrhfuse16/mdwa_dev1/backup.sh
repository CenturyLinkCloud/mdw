#!/bin/bash
 #Purpose = Backup of Important Data
 #Created on 04-30-2015
 #Author = Manoj Agrawal
 #Version 1.0
 #START

TIME=`date +%b-%d-%y`            # This Command will add date in Backup File Name.
 FILENAME=mdwa_dev2_1-backup-$TIME.tar.gz    # Here i define Backup file name format.
 SRCDIR=/foss/foss-fuse/instances/mdwa_dev2_1
 DESDIR=/home/mdwflkc/backup            # Destination of backup file.
 tar -cvpzf $DESDIR/$FILENAME $SRCDIR 

#END 

