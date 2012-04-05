#!/usr/bin/env bash
# Copyright 2012 Citrix Systems, Inc. Licensed under the
# Apache License, Version 2.0 (the "License"); you may not use this
# file except in compliance with the License.  Citrix Systems, Inc.
# reserves all rights not expressly granted by the License.
# You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 
# Automatically generated by addcopyright.py at 04/03/2012



 


uuid=""
name=""
host=""

while getopts n:h: OPTION
do
  case $OPTION in
  n)    name="$OPTARG"
  		;;
  h)	host="$OPTARG"
  esac
done



if [ "$name" != "" ]
then
        uuid=`ssh root@$host "xe vm-list name-label=$name | grep uuid | awk '{print \\$5}'"`
fi

echo "uuid is $uuid"
var=`ssh root@$host "xe vm-shutdown uuid=$uuid; xe vm-destroy uuid=$uuid"`

if  [ "$var" != "" ]
then echo "Was unable to destroy the vm with name $name and uuid $uuid on host $host"
exit 2
else
exit 0
fi