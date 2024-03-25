#!/bin/bash

BASEDIR=$(dirname $(readlink -f $0))
echo "Base directory: $BASEDIR"

# Format: "$ZONE,$IP"
IPS_FILE=$BASEDIR/ips.csv
if [ ! -r "$IPS_FILE" ]
then
	echo "ERROR: unable to find IPs file '$IPS_FILE'"
	exit 1
fi
echo "IPs file: $IPS_FILE"

IFACES_FILE=$BASEDIR/ifaces.csv
echo "Ifaces file: $IFACES_FILE"
touch $IFACES_FILE

for IP in $(cut -d, -f2 $IPS_FILE)
do
	[ -z "$IP" ] && continue
	[ "${IP#IP}" != "$IP" ] && continue

	ZONE=$(grep "$IP$" $IPS_FILE | cut -d, -f1)
	[ -z "$ZONE" ] && continue

	UPDATE_IFACES_FILE=
	NODE=$(grep ",$IP," $IFACES_FILE | cut -d, -f1)
	IFACE=$(grep ",$IP," $IFACES_FILE | cut -d, -f3)
	if [ -z "$NODE" -o -z "$IFACE" ]
	then
		NODE=$(dig +short -x $IP | head -n1)
		NODE=${NODE%.}

		echo -n "Retrieving IFACE for $NODE: "
		IFACE=$(ssh $NODE ip -br addr show to 10.10.1.0/24)
		IFACE=$(echo $IFACE | cut -d' ' -f1)
		echo "$IFACE"

		UPDATE_IFACES_FILE=yes
	fi

	if [ -z "$NODE" -o -z "$IFACE" ]
	then
		echo "ERROR: unable to find NODE or IFACE for $IP"
		exit 3
	fi

	echo "- $NODE ($IP $IFACE) $ZONE"

	if [ -n "$UPDATE_IFACES_FILE" ]
	then
		echo "$NODE,$IP,$IFACE" >> $IFACES_FILE
	fi

	ssh -o StrictHostKeyChecking=accept-new $NODE "mkdir -p $BASEDIR;"
	scp -q -o StrictHostKeyChecking=accept-new $BASEDIR/* $NODE:$BASEDIR/

	COMMAND="ssh $NODE sudo python3 $BASEDIR/latsetter.py unset $IFACE"
	echo $ $COMMAND
	$COMMAND
	if [ "$?" != "0" ]
	then
		echo "ERROR: to unset node $NODE ($IP)"
		exit 4
	fi
done

echo "Done."
