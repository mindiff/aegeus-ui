## Build the AEG daemon image

### Prepare bootstrap data

Optionally, extract bootstrap data from a running container

```
export NVERSION=2.0

export MNNAME=aegd
export BLOCKCOUNT=`docker exec $MNNAME aegeus-cli getblockcount`
export MNARCHIVE="aegeusd-$NVERSION-$BLOCKCOUNT.tgz"

docker stop $MNNAME
docker cp $MNNAME:/var/lib/aegeusd aegeusd
docker start $MNNAME

cd aegeusd
rm -rf *.old *.dat *.log *.conf backups database
cd ..

tar czf $MNARCHIVE.tgz aegeusd
rm -rf aegeusd
```

### Build AEG daemon image with bootstrap

```
rm -rf docker
mkdir docker

export NVERSION=2.0
export MNARCHIVE=`ls aegeusd-$NVERSION-*`
tar xzf $MNARCHIVE -C docker/

cat << EOF > docker/aegeus-server.conf
rpcuser=aeg
rpcpassword=aegpass
rpcport=51473
rpcallowip=127.0.0.1
rpcallowip=172.17.0.1/24
listen=1
server=1
EOF

cat << EOF > docker/aegeus-client.conf
rpcuser=aeg
rpcpassword=aegpass
rpcport=51473
EOF

cat << EOF > docker/Dockerfile
# Based in Ubuntu 16.04
FROM ubuntu:16.04

# Install the binaries
COPY aegeusd/aegeusd /usr/bin/
COPY aegeusd/aegeus-cli /usr/bin/

# Prime blockchain data
COPY aegeusd /var/lib/aegeusd

# Install the config files
COPY aegeus-server.conf /etc/aegeus/aegeus.conf
COPY aegeus-client.conf /root/.aegeus/aegeus.conf

# System update/upgrade
RUN apt -y update
RUN apt -y upgrade
RUN apt -y install python3 software-properties-common
RUN apt-add-repository -y ppa:bitcoin/bitcoin
RUN apt -y update
RUN apt -y upgrade
RUN apt -y install make software-properties-common build-essential libtool autoconf libssl-dev libboost-dev libboost-chrono-dev libboost-filesystem-dev libboost-program-options-dev libboost-system-dev libboost-test-dev libboost-thread-dev automake git wget curl libdb4.8-dev bsdmainutils libdb4.8++-dev libminiupnpc-dev libgmp3-dev ufw pkg-config libevent-dev libdb5.3++ unzip libzmq5

# Expose the API port
EXPOSE 51473

# Set some default env vars
ENV RPCUSER=aeg
ENV RPCPASS=aegpass

# Use the daemon as entry point 
ENTRYPOINT ["aegeusd", "-datadir=/var/lib/aegeusd", "-conf=/etc/aegeus/aegeus.conf"]
EOF

docker rmi -f aegeus/aegeusd
docker build -t aegeus/aegeusd docker/
docker push aegeus/aegeusd

docker tag aegeus/aegeusd aegeus/aegeusd:$NVERSION
docker push aegeus/aegeusd:$NVERSION
```

### Run the AEG image

```
export MNNAME=aegd
export MNEXTIP=167.99.32.83

docker rm -f $MNNAME
docker run --detach \
    -p 29328:29328 \
    --memory=200m --memory-swap=2g \
    --name $MNNAME \
    aegeus/aegeusd

watch docker exec $MNNAME aegeus-cli getinfo

docker exec $MNNAME aegeus-cli addnode 94.16.118.207:29328 onetry
docker exec $MNNAME tail -f /var/lib/aegeusd/debug.log
```

