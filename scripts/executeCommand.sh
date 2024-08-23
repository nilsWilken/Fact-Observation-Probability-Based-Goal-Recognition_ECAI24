#!/bin/bash

ulimit -Sv $1
ulimit -v

echo $2

$2
