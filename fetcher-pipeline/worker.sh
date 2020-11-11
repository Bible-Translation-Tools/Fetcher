#!/bin/bash

directory=
hour=
minute=
trace=
verbose=

usage()
{
    echo "usage: worker.sh [[[-i directory ] [-hr hour ] [-mn minute ] [-t] [-v]] | [-h]]"
}

while [ "$1" != "" ]; do
    case $1 in
        -i | --input-dir )      shift
                                directory=$1
                                ;;
        -hr | --hour )          shift
                                hour=$1
                                ;;
        -mn | --minute )        shift
                                minute=$1
                                ;;
        -t | --trace )          trace="--trace"
                                ;;
        -v | --verbose )        verbose="--verbose"
                                ;;
        -h | --help )           usage
                                exit
                                ;;
        * )                     usage
                                exit 1
    esac
    shift
done

if [[ -z "$directory" ]]; then
  echo "You must specify input directory"
  exit 1
fi

if [[ -z "$hour" ]]; then
  hour=0
fi

if [[ -z "$minute" ]]; then
  minute=0
fi

python app.py -i "$directory" -hr "$hour" -mn "$minute" "$trace" "$verbose"

