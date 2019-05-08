#!/bin/bash

if [ $# -lt 2 ];then
	echo -e "usage: sh timeTrackStat.sh statLogName apiName"
	echo -e "usage: sh timeTrackStat.sh statLogName apiName stepName\n"
	exit 1
fi

# input param and validate
stat_log_name=$1
api_name=$2
stat_type="a-c-s"
if [ $# -ge 3 ];then
	stat_type="as-c-s"
	api_name="$api_name-$3"
fi

echo "statLogName=$stat_log_name, api_name=$api_name, stat_type=$stat_type"
if [ ! -f "$stat_log_name" ];then
	echo -e "file:$stat_log_name does not exist\n"
	exit 1
fi
echo "stating now, please wait..."

# shell will create file
report_file_name="report-$api_name.rep"
acs_file_name="acs-$api_name.rep"
acs_file_name_demo="$acs_file_name.demo"
cost_file_name="cost.rep"
slowlevel_file_name="slowlevel.rep"

# 1, prepare or clean: direct file
if [ ! -d "stat" ];then
	mkdir "stat"
	echo "has mkdir stat"
fi

if [ -f "stat/$report_file_name" ];then
	cat /dev/null > stat/$report_file_name
	echo "has clean stat/report_file"
fi

# 2, get cost slowlevel data
echo "begin grep acs..."
cat $stat_log_name | grep "$stat_type;$api_name;" > stat/$acs_file_name
echo "has finish acs"
awk -F ';' '{print $3}' stat/$acs_file_name | sort -n > stat/$cost_file_name
awk -F ';' '{print $4}' stat/$acs_file_name | sort -n > stat/$slowlevel_file_name
# move big file to small file
head -n 10 stat/$acs_file_name > stat/$acs_file_name_demo
echo "." >> stat/$acs_file_name_demo
echo "." >> stat/$acs_file_name_demo
echo "." >> stat/$acs_file_name_demo
tail -n 10 stat/$acs_file_name >> stat/$acs_file_name_demo
rm stat/$acs_file_name

# 3, calculate tp99
echo "begin calculate tp99..."
cost_row=`cat stat/$cost_file_name | wc -l | sed 's/^[ \t]*//g'`
tp_arr=(10 20 30 40 50 60 70 80 90 95 99 999)
for tp in ${tp_arr[@]}
do
	denominator=100
	if [ $tp -gt 100 ];then
		denominator=1000
	fi
	row_tp=`expr $cost_row \* $tp / $denominator`
	row_cost=`head -n $row_tp stat/$cost_file_name | tail -n 1`
#	echo "tp$tp=$row_cost"
	echo "tp$tp=$row_cost" >> stat/$report_file_name
done
echo "has finish tp99"

# 4, statistics slowlevel distributed
echo "begin slowlevel distributed..."
echo "" >> stat/$report_file_name
echo "slowlevel   percent   num/allnum" >> stat/$report_file_name
slow_arr=(`cat stat/$slowlevel_file_name | uniq -c | awk -F ' ' '{print $2}'`)
num_arr=(`cat stat/$slowlevel_file_name | uniq -c | awk -F ' ' '{print $1}'`)
len=${#slow_arr[@]}
for ((i=0; i<$len; i++))
do
	percent=`expr ${num_arr[$i]} \* 100 / $cost_row`
#	echo "${i}=$percent"
	iPlus1=`expr $i + 1`
	echo "(${slow_arr[$i]} - ${slow_arr[$iPlus1]})     $percent%     ${num_arr[$i]}/$cost_row" >> stat/$report_file_name
done
echo "has finish distributed"

# 5, finish
echo -e "!!! finish, the stat report file name=stat/$report_file_name !!!\n"
