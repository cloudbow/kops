#!/usr/bin/env sh

COUNTER=0
rm -rf $1
FULLCOMMAND="tar -czvf $1"
SPACE=" "
for var in "$@"
do
	COUNTER=$((COUNTER+1))
    if [ "$COUNTER" -eq 1 ]; then
		echo "Zipping to file $var"
	elif [ "$COUNTER" -eq 2 ]; then
		echo ""
	else
		cd $var
		cd ..
		folderName=$(basename $var)
		echo "zipping folder $folderName"
		ZIP_ONE="$FULLCOMMAND$SPACE$folderName"
		eval "$ZIP_ONE"
	fi
done
curl --upload-file $1 http://p1.dcos:9082/artifacts/$2
curl -v -o /tmp/y.o http://p1.dcos:9082/artifacts/$2